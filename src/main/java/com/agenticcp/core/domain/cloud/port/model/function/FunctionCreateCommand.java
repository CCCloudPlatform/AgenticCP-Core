package com.agenticcp.core.domain.cloud.port.model.function;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.Map;

/**
 * Serverless Function 생성 도메인 커맨드
 *
 * UseCase Service에서 Adapter로 전달되는 내부 명령 모델입니다.
 * CSP 중립적인 필드를 사용하며, 각 CSP Adapter의 Mapper에서 CSP 특화 요청으로 변환합니다.
 *
 * 필드 매핑 예시:
 * - functionName: AWS(FunctionName), Azure(FunctionName), GCP(name)
 * - runtime: AWS(Runtime), Azure(runtime), GCP(runtime)
 * - handler: AWS(Handler), Azure(scriptFile.entryPoint), GCP(entryPoint)
 * - memorySize: AWS(MemorySize), Azure(functionAppConfig), GCP(availableMemoryMb)
 * - timeout: AWS(Timeout), Azure(functionTimeout), GCP(timeout)
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record FunctionCreateCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String serviceKey,           // "LAMBDA", "AZURE_FUNCTIONS", "CLOUD_FUNCTIONS"
        String resourceType,         // "FUNCTION"
        String functionName,         // CSP 중립적: AWS(FunctionName), Azure(FunctionName), GCP(name)
        String runtime,              // CSP 중립적: "nodejs18.x", "python3.11", "java17", "go1.x"
        String handler,              // CSP 중립적: AWS(Handler), Azure(scriptFile.entryPoint), GCP(entryPoint)
        Integer memorySize,          // MB (모든 CSP 공통)
        Integer timeout,             // 초 (모든 CSP 공통, 최대값 CSP별로 다를 수 있음)
        String roleArn,              // CSP 중립적: AWS(Role ARN), Azure(identity), GCP(serviceAccountEmail)
        Map<String, String> environmentVariables,  // 환경 변수
        String description,          // 함수 설명
        String vpcId,                // VPC 연결 (선택적, CSP별 구현 다를 수 있음)
        String codeUri,              // 코드 URI (S3, Blob Storage, GCS 등)
        byte[] codeZip,              // 코드 ZIP 바이너리 (선택적, URI 대신 사용)
        Map<String, String> tags,
        String tenantKey,
        Map<String, Object> providerSpecificConfig,  // CSP별 특화 설정
        CloudSessionCredential session
) {
}
