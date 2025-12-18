package com.agenticcp.core.domain.cloud.port.model.cdn;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * CDN Distribution 생성 명령
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record CreateDistributionCommand(
    CloudProvider.ProviderType providerType,
    String accountScope,
    String serviceKey,
    String resourceType,
    String distributionName,
    String comment,
    Boolean enabled,
    OriginConfig origin,
    List<CacheBehaviorConfig> cacheBehaviors,
    List<String> aliases,
    String sslCertificateId,
    Map<String, String> tags,
    String tenantKey,
    CloudSessionCredential session
) {
}

