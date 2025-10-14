package com.agenticcp.core.domain.tenant.controller.cloud.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record SubnetRequest(
        String vpcId,
        List<SubnetConfig> subnets
) {
    public record SubnetConfig(
            String subnetId,
            String availabilityZone
    ) {
    }
}
