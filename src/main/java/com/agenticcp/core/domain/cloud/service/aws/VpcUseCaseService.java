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

@Service
@RequiredArgsConstructor
public class VpcUseCaseService {

    private final VpcPortRouter vpcPortRouter;

    @Transactional
    public CloudResource createVpc(VpcCreateRequest request) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(request.getProviderType());
        return vpcPort.createVpc(request);
    }

    @Transactional(readOnly = true)
    public Optional<CloudResource> getVpc(ResourceIdentity vpcId) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        return vpcPort.getVpc(vpcId);
    }

    @Transactional(readOnly = true)
    public List<CloudResource> listVpcs(VpcQuery query) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(query.getProviderType());
        return vpcPort.listVpcs(query);
    }

    @Transactional
    public CloudResource updateVpc(ResourceIdentity vpcId, VpcUpdateRequest request) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        return vpcPort.updateVpc(vpcId, request);
    }

    @Transactional
    public void deleteVpc(ResourceIdentity vpcId) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        vpcPort.deleteVpc(vpcId);
    }
}
