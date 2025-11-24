package com.agenticcp.core.domain.cloud.port.outbound.vpc;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.command.vpc.CreateVpcCommand;
import com.agenticcp.core.domain.cloud.port.command.vpc.DeleteVpcCommand;
import com.agenticcp.core.domain.cloud.port.command.vpc.GetVpcCommand;
import com.agenticcp.core.domain.cloud.port.command.vpc.ListVpcsQuery;
import com.agenticcp.core.domain.cloud.port.command.vpc.UpdateVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VpcQuery;
import com.agenticcp.core.domain.cloud.port.model.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpcUseCaseService {

    private final VpcPortRouter vpcPortRouter;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort accountCredentialManagementPort;

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
        return vpcPort.createVpc(command);
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
    public List<CloudResource> listVpcs(VpcQuery query) {
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
        ListVpcsQuery command = ListVpcsQuery.builder()
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
        vpcPort.deleteVpc(command);
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
}
