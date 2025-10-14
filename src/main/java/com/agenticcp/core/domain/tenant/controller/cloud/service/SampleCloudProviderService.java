package com.agenticcp.core.domain.tenant.controller.cloud.service;

import com.agenticcp.core.domain.tenant.controller.cloud.CloudProviderType;
import com.agenticcp.core.domain.tenant.controller.cloud.dto.CloudResourceResult;
import com.agenticcp.core.domain.tenant.controller.cloud.dto.SecurityGroupRequest;
import com.agenticcp.core.domain.tenant.controller.cloud.dto.SubnetRequest;
import com.agenticcp.core.domain.tenant.controller.cloud.dto.VpcRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SampleCloudProviderService implements CloudProviderService{


    @Override
    public CloudProviderType getCloudProviderType() {
        return CloudProviderType.OTHER;
    }

    @Override
    public CloudResourceResult createVpc(String tenantKey, VpcRequest vpcRequest) {


        return CloudResourceResult.builder()
                .resourceId(tenantKey)
                .metadata(Map.of(
                        "name", tenantKey,
                        "cidrBlock", vpcRequest.cidrBlock(),
                        "region", vpcRequest.region()))
                .build();
    }

    

    @Override
    public CloudResourceResult createSubnets(String tenantKey, SubnetRequest subnetRequest) {


        return CloudResourceResult.builder()
                .resourceId(tenantKey)
                .metadata(Map.of(
                        "name", tenantKey,
                        "vpcId", subnetRequest.vpcId(),
                        "subnets", subnetRequest.subnets().stream()
                                .map(subnet -> Map.of(
                                        "subnetId", subnet.subnetId(),
                                        "availabilityZone", subnet.availabilityZone()
                                ))
                                .toList()
                ))
                .build();
    }

    @Override
    public CloudResourceResult createSecurityGroups(String tenantKey, SecurityGroupRequest securityGroupRequest){

        return CloudResourceResult.builder()
                .resourceId()
    }

}
