package com.agenticcp.core.domain.tenant.controller.cloud.service;


import com.agenticcp.core.domain.tenant.controller.cloud.CloudProviderType;
import com.agenticcp.core.domain.tenant.controller.cloud.dto.CloudResourceResult;
import com.agenticcp.core.domain.tenant.controller.cloud.dto.SecurityGroupRequest;
import com.agenticcp.core.domain.tenant.controller.cloud.dto.SubnetRequest;
import com.agenticcp.core.domain.tenant.controller.cloud.dto.VpcRequest;

public interface CloudProviderService {

    CloudProviderType getCloudProviderType();

    CloudResourceResult createVpc(String tenantKey, VpcRequest vpcRequest);

    CloudResourceResult createSubnets(String tenantKey, SubnetRequest subnetRequest);

    CloudResourceResult createSecurityGroups(String tenantKey, SecurityGroupRequest securityGroupRequest);





}
