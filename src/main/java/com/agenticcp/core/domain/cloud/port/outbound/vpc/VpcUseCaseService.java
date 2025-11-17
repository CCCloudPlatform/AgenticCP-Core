package com.agenticcp.core.domain.cloud.port.outbound.vpc;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.command.vpc.CreateVpcCommand;
import com.agenticcp.core.domain.cloud.port.command.vpc.DeleteVpcCommand;
import com.agenticcp.core.domain.cloud.port.command.vpc.GetVpcCommand;
import com.agenticcp.core.domain.cloud.port.command.vpc.ListVpcsQuery;
import com.agenticcp.core.domain.cloud.port.command.vpc.UpdateVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VpcQuery;
import com.agenticcp.core.domain.cloud.port.model.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VpcUseCaseService {

    private final VpcPortRouter vpcPortRouter;
    private final CapabilityGuard capabilityGuard;

    @Transactional
    public CloudResource createVpc(VpcCreateRequest request) {
        capabilityGuard.ensureSupported(
            request.getProviderType(), 
            VpcConstants.SERVICE_KEY, 
            VpcConstants.RESOURCE_TYPE, 
            CapabilityGuard.Operation.TAGGING
        );
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
            .tenantKey(request.getTenantKey())
            .providerSpecificConfig(request.getProviderSpecificConfig())
            .build();
        return vpcPort.createVpc(command);
    }

    @Transactional(readOnly = true)
    public Optional<CloudResource> getVpc(ResourceIdentity vpcId) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        GetVpcCommand command = GetVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(vpcId.getAccountScope())
            .region(vpcId.getRegion())
            .providerResourceId(vpcId.getProviderResourceId())
            .serviceKey(vpcId.getServiceKey() != null ? vpcId.getServiceKey() : VpcConstants.SERVICE_KEY)
            .resourceType(vpcId.getResourceType() != null ? vpcId.getResourceType() : VpcConstants.RESOURCE_TYPE)
            .build();
        return vpcPort.getVpc(command);
    }

    @Transactional(readOnly = true)
    public List<CloudResource> listVpcs(VpcQuery query) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(query.getProviderType());
        ListVpcsQuery command = ListVpcsQuery.builder()
            .providerType(query.getProviderType())
            .accountScope(query.getAccountScope())
            .region(query.getRegion())
            .vpcName(query.getVpcName())
            .cidrBlock(query.getCidrBlock())
            .tags(query.getTags())
            .tenantKey(query.getTenantKey())
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
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        UpdateVpcCommand command = UpdateVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(vpcId.getAccountScope())
            .region(vpcId.getRegion())
            .providerResourceId(vpcId.getProviderResourceId())
            .vpcName(request.getVpcName())
            .description(request.getDescription())
            .tags(request.getTags())
            .tenantKey(request.getTenantKey())
            .providerSpecificConfig(request.getProviderSpecificConfig())
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
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        DeleteVpcCommand command = DeleteVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(vpcId.getAccountScope())
            .region(vpcId.getRegion())
            .providerResourceId(vpcId.getProviderResourceId())
            .serviceKey(vpcId.getServiceKey() != null ? vpcId.getServiceKey() : VpcConstants.SERVICE_KEY)
            .resourceType(vpcId.getResourceType() != null ? vpcId.getResourceType() : VpcConstants.RESOURCE_TYPE)
            .tenantKey(null) // ResourceIdentity에 tenantKey가 없으므로 null 처리
            .build();
        vpcPort.deleteVpc(command);
    }
}
