package com.agenticcp.core.domain.cloud.service.vpc;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.ListVpcsQueryRequest;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest.AttributeKeys;
import com.agenticcp.core.domain.cloud.dto.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.dto.VpcQueryRequest;
import com.agenticcp.core.domain.cloud.dto.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.vpc.CreateVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.DeleteVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.GetVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.UpdateVpcCommand;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.vpc.VpcManagementPort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpcUseCaseService {

    private final VpcPortRouter vpcPortRouter;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort accountCredentialManagementPort;
    private final CloudResourceManagementHelper resourceHelper;

    /**
     * VPC를 생성합니다.
     * CSP에서 VPC 생성 후 CloudResource 엔티티를 DB에 저장합니다.
     * 
     * 보상 트랜잭션: DB 저장 실패 시 CSP에 생성된 VPC를 삭제하여
     * 데이터 정합성(Ghost Resource 방지)을 보장합니다.
     *
     * @param request 생성 요청 정보
     * @return 생성된 VPC CloudResource
     * @throws BusinessException DB 저장 실패 및 보상 트랜잭션 실행 시
     */
    @Transactional
    public CloudResource createVpc(VpcCreateRequest request) {
        capabilityGuard.ensureSupported(
            request.getProviderType(), 
            VpcConstants.SERVICE_KEY,
            VpcConstants.RESOURCE_TYPE, 
            CapabilityGuard.Operation.TAGGING
        );
        
        // tenantKey 획득 (가이드라인 모범 사례 2: 항상 TenantContextHolder 사용)
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // accountScope 검증
        String accountScope = request.getAccountScope();
        validateAccountScope(accountScope);
        
        // JIT 세션 획득
        log.debug("세션 획득 시작: tenantKey={}, accountScope={}, providerType={}", 
                 tenantKey, accountScope, request.getProviderType());
        
        CloudSessionCredential session;
        try {
            session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, request.getProviderType());
            log.info("세션 획득 완료: expiresAt={}", session.getExpiresAt());
        } catch (BusinessException e) {
            if (e.getErrorCode() == CredentialErrorCode.CREDENTIAL_NOT_FOUND) {
                log.error("자격증명을 찾을 수 없습니다: tenantKey={}, accountScope={}", 
                         tenantKey, accountScope);
                throw new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_CONFIGURED,
                    "계정이 설정되지 않았습니다"
                );
            }
            throw e;
        }
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(request.getProviderType());
        CreateVpcCommand command = CreateVpcCommand.builder()
            .providerType(request.getProviderType())
            .accountScope(accountScope)
            .region(request.getRegion())
            .serviceKey(VpcConstants.SERVICE_KEY)
            .resourceType(VpcConstants.RESOURCE_TYPE)
            .vpcName(request.getVpcName())
            .cidrBlock(request.getCidrBlock())
            .description(request.getDescription())
            .tags(request.getTags())
            .tenantKey(tenantKey)
            .providerSpecificConfig(request.getProviderSpecificConfig())
            .session(session)
            .build();
        
        // CSP에서 VPC 생성
        CloudResource vpc = vpcPort.createVpc(command);
        
        // DB에 CloudResource 저장 (실패 시 보상 트랜잭션 실행)
        try {
            String resourceName = request.getVpcName() != null ? request.getVpcName() : vpc.getResourceId();
            ResourceRegistrationRequest registrationRequest = ResourceRegistrationRequest.builder()
                    .resourceId(vpc.getResourceId())
                    .resourceName(resourceName)
                    .resourceType("NETWORK")
                    .tags(request.getTags())
                    .attributes(Map.of(AttributeKeys.CONFIGURATION, request.getCidrBlock()))
                    .build();
            
            resourceHelper.registerResource(
                    request.getProviderType(),
                    getServiceKeyForProvider(request.getProviderType()),
                    registrationRequest
            );
        } catch (Exception e) {
            log.error("[VpcUseCaseService] DB 저장 실패, 보상 트랜잭션 실행: vpcId={}, error={}",
                    vpc.getResourceId(), e.getMessage());
            
            // 보상 트랜잭션: CSP에 생성된 VPC 삭제
            executeCompensatingTransaction(request.getProviderType(), vpcPort, vpc.getResourceId(), 
                    accountScope, request.getRegion(), session);
            
            throw new BusinessException(
                    CloudErrorCode.RESOURCE_CREATION_FAILED,
                    "VPC 생성 후 DB 저장 실패로 인해 롤백되었습니다: " + vpc.getResourceId()
            );
        }
        
        return vpc;
    }

    /**
     * 보상 트랜잭션: CSP에 생성된 VPC를 삭제합니다.
     * Ghost Resource 방지를 위해 DB 저장 실패 시 호출됩니다.
     */
    private void executeCompensatingTransaction(
            ProviderType providerType,
            VpcManagementPort vpcPort,
            String vpcId,
            String accountScope,
            String region,
            CloudSessionCredential session
    ) {
        try {
            log.warn("[VpcUseCaseService] 보상 트랜잭션 실행: CSP VPC 삭제 시도 - vpcId={}", vpcId);
            
            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            DeleteVpcCommand deleteCommand = DeleteVpcCommand.builder()
                    .providerType(providerType)
                    .accountScope(accountScope)
                    .region(region)
                    .providerResourceId(vpcId)
                    .serviceKey(VpcConstants.SERVICE_KEY)
                    .resourceType(VpcConstants.RESOURCE_TYPE)
                    .tenantKey(tenantKey)
                    .session(session)
                    .build();
            
            vpcPort.deleteVpc(deleteCommand);
            log.info("[VpcUseCaseService] 보상 트랜잭션 완료: CSP VPC 삭제 성공 - vpcId={}", vpcId);
        } catch (Exception compensationError) {
            // 보상 트랜잭션도 실패한 경우 - Ghost Resource 발생
            // 이 경우 별도의 모니터링/알림 시스템이나 배치 동기화로 처리 필요
            log.error("[VpcUseCaseService] 보상 트랜잭션 실패: Ghost Resource 발생 가능 - vpcId={}, error={}",
                    vpcId, compensationError.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Optional<CloudResource> getVpc(ResourceIdentity vpcId) {
        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // accountScope 검증
        String accountScope = vpcId.getAccountScope();
        validateAccountScope(accountScope);
        
        // JIT 세션 획득
        log.debug("세션 획득 시작: tenantKey={}, accountScope={}, providerType={}", 
                 tenantKey, accountScope, vpcId.getProviderType());
        
        CloudSessionCredential session;
        try {
            session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, vpcId.getProviderType());
            log.info("세션 획득 완료: expiresAt={}", session.getExpiresAt());
        } catch (BusinessException e) {
            if (e.getErrorCode() == CredentialErrorCode.CREDENTIAL_NOT_FOUND) {
                log.error("자격증명을 찾을 수 없습니다: tenantKey={}, accountScope={}", 
                         tenantKey, accountScope);
                throw new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_CONFIGURED,
                    "계정이 설정되지 않았습니다"
                );
            }
            throw e;
        }
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        GetVpcCommand command = GetVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(accountScope)
            .region(vpcId.getRegion())
            .providerResourceId(vpcId.getProviderResourceId())
            .serviceKey(vpcId.getServiceKey() != null ? vpcId.getServiceKey() : VpcConstants.SERVICE_KEY)
            .resourceType(vpcId.getResourceType() != null ? vpcId.getResourceType() : VpcConstants.RESOURCE_TYPE)
            .session(session)
            .build();
        return vpcPort.getVpc(command);
    }

    @Transactional(readOnly = true)
    public List<CloudResource> listVpcs(VpcQueryRequest query) {
        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // accountScope 검증
        String accountScope = query.getAccountScope();
        validateAccountScope(accountScope);
        
        // JIT 세션 획득
        log.debug("세션 획득 시작: tenantKey={}, accountScope={}, providerType={}", 
                 tenantKey, accountScope, query.getProviderType());
        
        CloudSessionCredential session;
        try {
            session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, query.getProviderType());
            log.info("세션 획득 완료: expiresAt={}", session.getExpiresAt());
        } catch (BusinessException e) {
            if (e.getErrorCode() == CredentialErrorCode.CREDENTIAL_NOT_FOUND) {
                log.error("자격증명을 찾을 수 없습니다: tenantKey={}, accountScope={}", 
                         tenantKey, accountScope);
                throw new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_CONFIGURED,
                    "계정이 설정되지 않았습니다"
                );
            }
            throw e;
        }
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(query.getProviderType());
        ListVpcsQueryRequest command = ListVpcsQueryRequest.builder()
            .providerType(query.getProviderType())
            .accountScope(accountScope)
            .region(query.getRegion())
            .vpcName(query.getVpcName())
            .cidrBlock(query.getCidrBlock())
            .tags(query.getTags())
            .tenantKey(tenantKey)
            .session(session)
            .build();
        return vpcPort.listVpcs(command);
    }

    @Transactional
    public CloudResource updateVpc(ResourceIdentity vpcId, VpcUpdateRequest request) {
        capabilityGuard.ensureSupported(
            vpcId.getProviderType(), 
            VpcConstants.SERVICE_KEY, 
            VpcConstants.RESOURCE_TYPE, 
            CapabilityGuard.Operation.TAGGING
        );
        
        // tenantKey 획득 (가이드라인 모범 사례 2: 항상 TenantContextHolder 사용)
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // accountScope 검증
        String accountScope = vpcId.getAccountScope();
        validateAccountScope(accountScope);
        
        // JIT 세션 획득
        log.debug("세션 획득 시작: tenantKey={}, accountScope={}, providerType={}", 
                 tenantKey, accountScope, vpcId.getProviderType());
        
        CloudSessionCredential session;
        try {
            session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, vpcId.getProviderType());
            log.info("세션 획득 완료: expiresAt={}", session.getExpiresAt());
        } catch (BusinessException e) {
            if (e.getErrorCode() == CredentialErrorCode.CREDENTIAL_NOT_FOUND) {
                log.error("자격증명을 찾을 수 없습니다: tenantKey={}, accountScope={}", 
                         tenantKey, accountScope);
                throw new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_CONFIGURED,
                    "계정이 설정되지 않았습니다"
                );
            }
            throw e;
        }
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        UpdateVpcCommand command = UpdateVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(accountScope)
            .region(vpcId.getRegion())
            .providerResourceId(vpcId.getProviderResourceId())
            .vpcName(request.getVpcName())
            .description(request.getDescription())
            .tags(request.getTags())
            .tenantKey(tenantKey)
            .providerSpecificConfig(request.getProviderSpecificConfig())
            .session(session)
            .build();
        return vpcPort.updateVpc(command);
    }

    @Transactional
    public void deleteVpc(ResourceIdentity vpcId) {
        capabilityGuard.ensureSupported(
            vpcId.getProviderType(), 
            VpcConstants.SERVICE_KEY, 
            VpcConstants.RESOURCE_TYPE, 
            CapabilityGuard.Operation.TAGGING
        );
        
        // tenantKey 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // accountScope 검증
        String accountScope = vpcId.getAccountScope();
        validateAccountScope(accountScope);
        
        // JIT 세션 획득
        log.debug("세션 획득 시작: tenantKey={}, accountScope={}, providerType={}", 
                 tenantKey, accountScope, vpcId.getProviderType());
        
        CloudSessionCredential session;
        try {
            session = accountCredentialManagementPort.getSession(
                tenantKey, accountScope, vpcId.getProviderType());
            log.info("세션 획득 완료: expiresAt={}", session.getExpiresAt());
        } catch (BusinessException e) {
            if (e.getErrorCode() == CredentialErrorCode.CREDENTIAL_NOT_FOUND) {
                log.error("자격증명을 찾을 수 없습니다: tenantKey={}, accountScope={}", 
                         tenantKey, accountScope);
                throw new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_CONFIGURED,
                    "계정이 설정되지 않았습니다"
                );
            }
            throw e;
        }
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        DeleteVpcCommand command = DeleteVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(accountScope)
            .region(vpcId.getRegion())
            .providerResourceId(vpcId.getProviderResourceId())
            .serviceKey(vpcId.getServiceKey() != null ? vpcId.getServiceKey() : VpcConstants.SERVICE_KEY)
            .resourceType(vpcId.getResourceType() != null ? vpcId.getResourceType() : VpcConstants.RESOURCE_TYPE)
            .tenantKey(tenantKey)
            .session(session)
            .build();
        
        // CSP에서 VPC 삭제
        vpcPort.deleteVpc(command);
        
        // DB 소프트 삭제
        resourceHelper.softDeleteResource(vpcId.getProviderResourceId());
    }
    
    /**
     * accountScope를 검증합니다.
     * 
     * @param accountScope 계정 범위 (AWS Account ID, Azure Subscription ID, GCP Project ID)
     * @throws BusinessException accountScope가 null이거나 비어있을 때
     */
    private void validateAccountScope(String accountScope) {
        if (accountScope == null || accountScope.trim().isEmpty()) {
            throw new BusinessException(
                CloudErrorCode.ACCOUNT_SCOPE_REQUIRED,
                "AccountScope가 필요합니다"
            );
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * 프로바이더 타입에 따른 서비스 키 반환
     * AWS: EC2 (VPC는 EC2 서비스에 속함), Azure: VirtualNetwork, GCP: VPCNetwork 등
     */
    private String getServiceKeyForProvider(ProviderType providerType) {
        return switch (providerType) {
            case AWS -> "EC2";  // VPC는 EC2 서비스에 속함
            case AZURE -> "VirtualNetwork";
            case GCP -> "VPCNetwork";
            default -> "Network";
        };
    }
}
