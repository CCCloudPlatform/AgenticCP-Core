package com.agenticcp.core.domain.cloud.port.outbound.aws;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.port.model.VpcQuery;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;

import java.util.List;
import java.util.Optional;

public interface VpcManagementPort {
    
    /**
     * VPC 생성
     */
    CloudResource createVpc(VpcCreateRequest request);
    
    /**
     * VPC 조회 (단일)
     */
    Optional<CloudResource> getVpc(ResourceIdentity vpcId);
    
    /**
     * VPC 목록 조회
     */
    List<CloudResource> listVpcs(VpcQuery query);
    
    /**
     * VPC 수정
     */
    CloudResource updateVpc(ResourceIdentity vpcId, VpcUpdateRequest request);
    
    /**
     * VPC 삭제
     */
    void deleteVpc(ResourceIdentity vpcId);
}