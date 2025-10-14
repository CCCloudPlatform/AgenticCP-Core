package com.agenticcp.core.domain.tenant.controller.cloud.dto;

import java.util.Map;

public record VpcRequest(
        String cidrBlock,
        String region,
        Map<String, String> tags
) {
}
