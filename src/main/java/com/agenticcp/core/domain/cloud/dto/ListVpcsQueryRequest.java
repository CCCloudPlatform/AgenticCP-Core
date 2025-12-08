package com.agenticcp.core.domain.cloud.dto;

import java.util.Map;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;

import lombok.Builder;

/**
 * VPC 목록 조회 명령.
 */
@Builder
public record ListVpcsQueryRequest(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String vpcName,
        String cidrBlock,
        Map<String, String> tags,
        String tenantKey,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection,
        CloudSessionCredential session
) {
}

