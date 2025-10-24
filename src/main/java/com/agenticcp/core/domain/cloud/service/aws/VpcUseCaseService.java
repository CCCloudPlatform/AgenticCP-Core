package com.agenticcp.core.domain.cloud.service.aws;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VpcManagementPort;

import lombok.RequiredArgsConstructor;

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
    
    // 포트 선택 
    VpcManagementPort vpcPort = vpcPortRouter.getPort(request.getProviderType());

    // VPC 생성 
    CloudResource vpc = vpcPort.createVpc(request);
    
    return vpc;
    } 
}
