package com.agenticcp.core.domain.cloud.port.outbound.vpc;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.command.vpc.CreateVpcCommand;
import com.agenticcp.core.domain.cloud.port.command.vpc.DeleteVpcCommand;
import com.agenticcp.core.domain.cloud.port.command.vpc.GetVpcCommand;
import com.agenticcp.core.domain.cloud.port.command.vpc.ListVpcsQuery;
import com.agenticcp.core.domain.cloud.port.command.vpc.UpdateVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VpcQuery;
import com.agenticcp.core.domain.cloud.port.model.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VpcUseCaseService {

    private final VpcPortRouter vpcPortRouter;
    private final CapabilityGuard capabilityGuard;
    private final CredentialProviderPort credentialProviderPort;
    private final CloudAccountRepository cloudAccountRepository;

    @Transactional
    public CloudResource createVpc(VpcCreateRequest request) {
        capabilityGuard.ensureSupported(
            request.getProviderType(), 
            VpcConstants.SERVICE_KEY, 
            VpcConstants.RESOURCE_TYPE, 
            CapabilityGuard.Operation.TAGGING
        );
        
        // JIT 세션 획득
        String tenantKey = request.getTenantKey() != null 
            ? request.getTenantKey() 
            : TenantContextHolder.getCurrentTenantKeyOrThrow();
        Long accountId = getAccountIdFromScope(request.getAccountScope(), request.getProviderType());
        CloudSessionCredential session = credentialProviderPort.getSession(tenantKey, accountId, request.getProviderType());
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(request.getProviderType());
        CreateVpcCommand command = CreateVpcCommand.builder()
            .providerType(request.getProviderType())
            .accountScope(request.getAccountScope())
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
        // JIT 세션 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        Long accountId = getAccountIdFromScope(vpcId.getAccountScope(), vpcId.getProviderType());
        CloudSessionCredential session = credentialProviderPort.getSession(tenantKey, accountId, vpcId.getProviderType());
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        GetVpcCommand command = GetVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(vpcId.getAccountScope())
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
        // JIT 세션 획득
        String tenantKey = query.getTenantKey() != null 
            ? query.getTenantKey() 
            : TenantContextHolder.getCurrentTenantKeyOrThrow();
        Long accountId = getAccountIdFromScope(query.getAccountScope(), query.getProviderType());
        CloudSessionCredential session = credentialProviderPort.getSession(tenantKey, accountId, query.getProviderType());
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(query.getProviderType());
        ListVpcsQuery command = ListVpcsQuery.builder()
            .providerType(query.getProviderType())
            .accountScope(query.getAccountScope())
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
        
        // JIT 세션 획득
        String tenantKey = request.getTenantKey() != null 
            ? request.getTenantKey() 
            : TenantContextHolder.getCurrentTenantKeyOrThrow();
        Long accountId = getAccountIdFromScope(vpcId.getAccountScope(), vpcId.getProviderType());
        CloudSessionCredential session = credentialProviderPort.getSession(tenantKey, accountId, vpcId.getProviderType());
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        UpdateVpcCommand command = UpdateVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(vpcId.getAccountScope())
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
        
        // JIT 세션 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        Long accountId = getAccountIdFromScope(vpcId.getAccountScope(), vpcId.getProviderType());
        CloudSessionCredential session = credentialProviderPort.getSession(tenantKey, accountId, vpcId.getProviderType());
        
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        DeleteVpcCommand command = DeleteVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(vpcId.getAccountScope())
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
     * accountScope로 CloudAccount의 ID를 조회합니다.
     * 
     * @param accountScope 계정 범위 (AWS AccountId, Azure SubscriptionId, GCP ProjectId)
     * @param providerType 프로바이더 타입
     * @return CloudAccount ID
     */
    private Long getAccountIdFromScope(String accountScope, com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType providerType) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        return cloudAccountRepository.findByTenantKeyAndProviderType(tenantKey, providerType)
                .stream()
                .filter(account -> account.getAccountId() != null && account.getAccountId().equals(accountScope))
                .findFirst()
                .map(account -> account.getId())
                .orElseThrow(() -> new BusinessException(
                    CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountScope
                ));
    }
}
