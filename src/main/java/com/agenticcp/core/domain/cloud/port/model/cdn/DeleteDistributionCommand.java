package com.agenticcp.core.domain.cloud.port.model.cdn;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

/**
 * CDN Distribution 삭제 명령
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record DeleteDistributionCommand(
    CloudProvider.ProviderType providerType,
    String accountScope,
    String distributionId,
    String etag,  // 동시성 제어용
    CloudSessionCredential session
) {
}

