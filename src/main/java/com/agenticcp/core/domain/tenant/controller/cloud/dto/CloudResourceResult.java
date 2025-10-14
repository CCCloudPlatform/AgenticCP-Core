package com.agenticcp.core.domain.tenant.controller.cloud.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record CloudResourceResult(
        String resourceId,
        Map<String, Object> metadata
) {
}
