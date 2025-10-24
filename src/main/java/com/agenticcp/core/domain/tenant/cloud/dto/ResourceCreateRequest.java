package com.agenticcp.core.domain.tenant.cloud.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record ResourceCreateRequest(
      Map<String, Object> metadata
) {
}
