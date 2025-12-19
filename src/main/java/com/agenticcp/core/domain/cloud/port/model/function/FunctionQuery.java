package com.agenticcp.core.domain.cloud.port.model.function;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

/**
 * Serverless Function 조회 쿼리
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record FunctionQuery(
        CloudProvider.ProviderType providerType,
        String accountScope,
        Set<String> regions,
        String functionName,         // 함수 이름으로 필터링 (CSP 중립적)
        String runtime,              // 런타임으로 필터링
        String vpcId,                // VPC ID로 필터링
        Map<String, String> tagsEquals,  // 태그로 필터링
        int page,
        int size
) {
}
