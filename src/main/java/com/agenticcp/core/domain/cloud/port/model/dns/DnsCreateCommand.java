package com.agenticcp.core.domain.cloud.port.model.dns;

import java.util.Map;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;

import lombok.Builder;

/**
 * DNS 호스팅 존 생성 도메인 커맨드
 *
 * UseCase Service에서 Adapter로 전달되는 내부 명령 모델입니다.
 * 각 CSP Adapter의 Mapper에서 CSP 특화 요청으로 변환합니다.
 */
@Builder
public record DnsCreateCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String serviceKey,
        String resourceType,
        String zoneName,
        String zoneType,
        String vpcId,
        String comment,
        Map<String, String> tags,
        String tenantKey,
        Map<String, Object> providerSpecificConfig,
        CloudSessionCredential session
) {
}
