package com.agenticcp.core.domain.cloud.service.aws;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.port.model.VpcQuery;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VpcManagementPort;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;

@Service
@RequiredArgsConstructor
public class VpcUseCaseService {

    private final VpcPortRouter vpcPortRouter;
    private final CapabilityGuard capabilityGuard;

    @Transactional
    public CloudResource createVpc(VpcCreateRequest request) {
        capabilityGuard.ensureSupported(request.getProviderType(), "vpc", "create", CapabilityGuard.Operation.TAGGING);
        VpcManagementPort vpcPort = vpcPortRouter.getPort(request.getProviderType());
        CreateVpcCommand command = CreateVpcCommand.builder()
            .providerType(request.getProviderType())
            .accountScope(request.getAccountScope())
            .region(request.getRegion())
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
            .serviceKey(vpcId.getServiceKey())
            .resourceType(vpcId.getResourceType())
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
        capabilityGuard.ensureSupported(vpcId.getProviderType(), "vpc", "update", CapabilityGuard.Operation.TAGGING);
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        UpdateVpcCommand command = UpdateVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(vpcId.getAccountScope())
            .region(vpcId.getRegion())
            .vpcName(vpcId.getVpcName())
            .description(request.getDescription())
            .tags(request.getTags())
            .tenantKey(vpcId.getTenantKey())
            .providerSpecificConfig(request.getProviderSpecificConfig())
            .build();
        return vpcPort.updateVpc(command);
    }

    @Transactional
    public void deleteVpc(ResourceIdentity vpcId) {
        capabilityGuard.ensureSupported(vpcId.getProviderType(), "vpc", "delete", CapabilityGuard.Operation.TAGGING);
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        DeleteVpcCommand command = DeleteVpcCommand.builder()
            .providerType(vpcId.getProviderType())
            .accountScope(vpcId.getAccountScope())
            .region(vpcId.getRegion())
            .providerResourceId(vpcId.getProviderResourceId())
            .serviceKey(vpcId.getServiceKey())
            .resourceType(vpcId.getResourceType())
            .tenantKey(vpcId.getTenantKey())
            .build();
        vpcPort.deleteVpc(command);
    }
}
