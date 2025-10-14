package com.agenticcp.core.domain.tenant.controller.cloud.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record SecurityGroupRequest(
        String vpcId,
        String name,
        List<SecurityGroupRule> securityGroupRules
) {
    public record SecurityGroupRule(
            String protocol,
            String portRange,
            String cidrIp
    ) {
    }
}
