package com.agenticcp.core.domain.cloud.port.outbound.aws;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;


public interface VpcManagementPort {
    CloudResource createVpc(VpcCreateRequest request);
}