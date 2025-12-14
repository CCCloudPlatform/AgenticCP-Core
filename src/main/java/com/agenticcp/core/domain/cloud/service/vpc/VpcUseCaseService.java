package com.agenticcp.core.domain.cloud.service.vpc;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.ListVpcsQueryRequest;
import com.agenticcp.core.domain.cloud.dto.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.dto.VpcQueryRequest;
import com.agenticcp.core.domain.cloud.dto.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudService;
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
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.cloud.repository.CloudServiceRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpcUseCaseService {

    private final VpcPortRouter vpcPortRouter;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort accountCredentialManagementPort;
    
    // DB 저장을 위한 Repository
    private final CloudResourceRepository cloudResourceRepository;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudServiceRepository cloudServiceRepository;
    private final TenantRepository tenantRepository;

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
        
        // DB에 CloudResource 저장
        saveCloudResource(vpc.getResourceId(), request);
        
        return vpc;
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
        softDeleteResourceIfExists(vpcId.getProviderResourceId());
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

    // ==================== DB 저장 헬퍼 메서드 ====================

    /**
     * CSP에서 생성된 VPC 정보를 CloudResource 엔티티로 저장합니다.
     * DB 저장 실패해도 CSP 생성은 완료되었으므로 별도 트랜잭션으로 분리하여
     * 메인 트랜잭션에 영향을 주지 않도록 합니다.
     *
     * @param vpcId 생성된 VPC ID
     * @param request 생성 요청 정보
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveCloudResource(String vpcId, VpcCreateRequest request) {
        try {
            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            ProviderType providerType = request.getProviderType();
            
            // Provider 조회
            CloudProvider provider = cloudProviderRepository.findFirstByProviderType(providerType)
                    .orElseThrow(() -> new IllegalStateException(
                            "CloudProvider not found for type: " + providerType));
            
            // Service 조회 (프로바이더별 서비스 키 사용)
            String serviceKey = getServiceKeyForProvider(providerType);
            CloudService cloudService = cloudServiceRepository
                    .findByProviderTypeAndServiceKey(providerType, serviceKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "CloudService not found for provider: " + providerType + ", serviceKey: " + serviceKey));
            
            // Tenant 조회
            Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Tenant not found for key: " + tenantKey));
            
            // 리소스 이름 결정
            String resourceName = request.getVpcName() != null ? request.getVpcName() : vpcId;
            
            // Factory Method를 사용한 CloudResource 엔티티 생성
            CloudResource cloudResource = CloudResource.createVpc(
                    vpcId,
                    resourceName,
                    provider,
                    cloudService,
                    tenant,
                    request.getCidrBlock(),
                    request.getTags()
            );
            
            cloudResourceRepository.save(cloudResource);
            log.debug("[VpcUseCaseService] CloudResource 저장 완료: vpcId={}", vpcId);
            
        } catch (Exception e) {
            // DB 저장 실패해도 CSP 생성은 완료되었으므로 경고 로그만 출력
            log.warn("[VpcUseCaseService] CloudResource 저장 실패 (CSP 생성은 완료됨): vpcId={}, error={}", 
                    vpcId, e.getMessage());
        }
    }

    /**
     * 리소스가 존재하면 소프트 삭제 처리합니다.
     *
     * @param resourceId 리소스 ID (VPC ID)
     */
    private void softDeleteResourceIfExists(String resourceId) {
        try {
            int deletedCount = cloudResourceRepository.softDeleteByResourceId(resourceId);
            
            if (deletedCount > 0) {
                log.debug("[VpcUseCaseService] 리소스 소프트 삭제 완료: resourceId={}", resourceId);
            } else {
                log.debug("[VpcUseCaseService] DB에 리소스가 없어 삭제 스킵: resourceId={}", resourceId);
            }
        } catch (Exception e) {
            log.warn("[VpcUseCaseService] 리소스 소프트 삭제 실패: resourceId={}, error={}", 
                    resourceId, e.getMessage());
        }
    }

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
