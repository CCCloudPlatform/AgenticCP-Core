package com.agenticcp.core.domain.cloud.port.model.function;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.Map;

/**
 * Serverless Function 실행 커맨드
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record FunctionInvokeCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,   // 함수 ARN/ID
        String invocationType,       // "RequestResponse", "Event", "DryRun"
        String payload,              // JSON 문자열 페이로드
        String qualifier,            // 함수 버전/별칭 (선택적)
        Map<String, String> context,  // 추가 컨텍스트 정보
        String tenantKey,
        CloudSessionCredential session
) {
}
