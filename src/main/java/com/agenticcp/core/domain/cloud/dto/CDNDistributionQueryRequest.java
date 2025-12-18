package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.Map;

/**
 * CDN Distribution 목록 조회 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record CDNDistributionQueryRequest(
    CloudProvider.ProviderType providerType,
    String accountScope,
    String distributionName,
    Boolean enabled,
    Map<String, String> tags,
    String tenantKey,
    Integer page,
    Integer size,
    String sortBy,
    String sortDirection,
    CloudSessionCredential session
) {
}

