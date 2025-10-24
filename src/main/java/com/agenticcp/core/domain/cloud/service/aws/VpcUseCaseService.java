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
    @AuditRequired(
        action = "CREATE_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 생성",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.MEDIUM
    )
    public CloudResource createVpc(VpcCreateRequest request) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(request.getProviderType());
        return vpcPort.createVpc(request);
    }

    @Transactional(readOnly = true)
    @AuditRequired(
        action = "GET_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 조회",
        includeRequestData = false,
        includeResponseData = true,
        severity = AuditSeverity.LOW
    )
    public Optional<CloudResource> getVpc(ResourceIdentity vpcId) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        return vpcPort.getVpc(vpcId);
    }

    @Transactional(readOnly = true)
    @AuditRequired(
        action = "LIST_VPCS",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 목록 조회",
        includeRequestData = false,
        includeResponseData = false,
        severity = AuditSeverity.LOW
    )
    public List<CloudResource> listVpcs(VpcQuery query) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(query.getProviderType());
        return vpcPort.listVpcs(query);
    }

    @Transactional
    @AuditRequired(
        action = "UPDATE_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 수정",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.MEDIUM
    )
    public CloudResource updateVpc(ResourceIdentity vpcId, VpcUpdateRequest request) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        return vpcPort.updateVpc(vpcId, request);
    }

    @Transactional
    @AuditRequired(
        action = "DELETE_VPC",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "VPC 삭제",
        includeRequestData = true,
        includeResponseData = false,
        severity = AuditSeverity.HIGH
    )
    public void deleteVpc(ResourceIdentity vpcId) {
        VpcManagementPort vpcPort = vpcPortRouter.getPort(vpcId.getProviderType());
        vpcPort.deleteVpc(vpcId);
    }
}
