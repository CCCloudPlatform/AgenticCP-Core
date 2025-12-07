package com.agenticcp.core.domain.cloud.port.outbound.vpc;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.vpc.CreateVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.DeleteVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.GetVpcCommand;
import com.agenticcp.core.domain.cloud.dto.ListVpcsQueryRequest;
import com.agenticcp.core.domain.cloud.port.model.vpc.UpdateVpcCommand;

import java.util.List;
import java.util.Optional;

public interface VpcManagementPort {
    
    /**
     * VPC 생성
     * @param command 생성 명령
     * @return 생성된 VPC
     */
    CloudResource createVpc(CreateVpcCommand command);
    
    /**
     * VPC 조회 (단일)
     */
    Optional<CloudResource> getVpc(GetVpcCommand command);
    
    /**
     * VPC 목록 조회
     */
    List<CloudResource> listVpcs(ListVpcsQueryRequest command);
    
    /**
     * VPC 수정
     */
    CloudResource updateVpc(UpdateVpcCommand command);
    
    /**
     * VPC 삭제
     */
    void deleteVpc(DeleteVpcCommand command);
}