package com.agenticcp.core.domain.cloud.port.model.cdn;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * CDN Distribution 수정 명령
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record UpdateDistributionCommand(
    CloudProvider.ProviderType providerType,
    String accountScope,
    String distributionId,
    String etag,  // 동시성 제어용
    String comment,
    Boolean enabled,
    List<CacheBehaviorConfig> cacheBehaviors,
    List<String> aliases,
    String sslCertificateId,
    Map<String, String> tags,
    String tenantKey,
    CloudSessionCredential session
) {
}

