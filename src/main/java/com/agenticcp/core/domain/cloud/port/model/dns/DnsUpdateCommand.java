package com.agenticcp.core.domain.cloud.port.model.dns;

import java.util.Map;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;

import lombok.Builder;

/**
 * DNS 호스팅 존 수정 도메인 커맨드
 */
@Builder
public record DnsUpdateCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,
        String comment,
        Map<String, String> tags,
        String tenantKey,
        Map<String, Object> providerSpecificConfig,
        CloudSessionCredential session
) {
}
