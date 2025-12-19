package com.agenticcp.core.domain.cloud.port.model.dns;

import java.util.Map;
import java.util.Set;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;

import lombok.Builder;

/**
 * DNS 호스팅 존 조회 쿼리
 */
@Builder
public record DnsQuery(
        CloudProvider.ProviderType providerType,
        String accountScope,
        Set<String> regions,
        String zoneName,
        String zoneType,
        String vpcId,
        Map<String, String> tagsEquals,
        int page,
        int size
) {
}
