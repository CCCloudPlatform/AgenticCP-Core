package com.agenticcp.core.domain.cloud.port.model.function;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.Map;

/**
 * Serverless Function 수정 도메인 커맨드
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record FunctionUpdateCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,   // 함수 ARN/ID (CSP별 형식 다를 수 있음)
        String runtime,              // 런타임 변경
        String handler,              // 핸들러 변경
        Integer memorySize,          // 메모리 크기 변경 (MB)
        Integer timeout,             // 타임아웃 변경 (초)
        String roleArn,              // 실행 역할 변경
        Map<String, String> environmentVariables,  // 환경 변수 변경
        String description,          // 설명 변경
        String codeUri,              // 코드 업데이트 URI
        byte[] codeZip,              // 코드 ZIP 바이너리
        Map<String, String> tags,
        String tenantKey,
        Map<String, Object> providerSpecificConfig,  // CSP별 특화 설정
        CloudSessionCredential session
) {
}
