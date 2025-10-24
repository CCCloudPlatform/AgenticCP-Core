package com.agenticcp.core.domain.cloud.service.aws;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VpcManagementPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VpcUseCaseService {

    private final VpcPortRouter vpcPortRouter;

    @Transactional
    public CloudResource createVpc(VpcCreateRequest request) {
    
    // 포트 선택 
    VpcManagementPort vpcPort = vpcPortRouter.getPort(request.getProviderType());

    // VPC 생성 
    CloudResource vpc = vpcPort.createVpc(request);
    
    return vpc;
    } 
}
