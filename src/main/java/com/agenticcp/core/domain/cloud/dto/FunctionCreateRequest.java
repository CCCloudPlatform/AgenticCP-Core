package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Serverless Function 생성 요청 DTO
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCreateRequest {

    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private ProviderType providerType;

    /**
     * 계정 스코프 (Account ID 등)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String accountScope;

    /**
     * 리전 (필수)
     * 예: us-east-1, ap-northeast-2
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;

    /**
     * 함수 이름 (CSP 중립적)
     * 예: my-function
     */
    @NotBlank(message = "함수 이름은 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9-_]{1,64}$", message = "함수 이름은 1-64자의 영문, 숫자, 하이픈, 언더스코어만 사용 가능합니다")
    private String functionName;

    /**
     * 런타임 (CSP 중립적)
     * 예: "nodejs18.x", "python3.11", "java17", "go1.x"
     */
    @NotBlank(message = "런타임은 필수입니다")
    private String runtime;

    /**
     * 핸들러 (CSP 중립적)
     * AWS(Handler), Azure(scriptFile.entryPoint), GCP(entryPoint)
     */
    @NotBlank(message = "핸들러는 필수입니다")
    private String handler;

    /**
     * 메모리 크기 (MB)
     */
    @Min(value = 128, message = "메모리는 최소 128MB입니다")
    @Max(value = 10240, message = "메모리는 최대 10240MB입니다")
    private Integer memorySize;

    /**
     * 타임아웃 (초)
     */
    @Min(value = 1, message = "타임아웃은 최소 1초입니다")
    @Max(value = 900, message = "타임아웃은 최대 900초입니다")
    private Integer timeout;

    /**
     * 실행 역할 (CSP 중립적)
     * AWS(Role ARN), Azure(identity), GCP(serviceAccountEmail)
     */
    @NotBlank(message = "실행 역할은 필수입니다")
    private String roleArn;

    /**
     * 환경 변수
     */
    private Map<String, String> environmentVariables;

    /**
     * 함수 설명
     */
    private String description;

    /**
     * VPC 연결 (선택적)
     */
    private String vpcId;

    /**
     * 코드 URI (S3, Blob Storage, GCS 등)
     */
    @NotBlank(message = "코드 URI는 필수입니다")
    private String codeUri;

    /**
     * 태그
     */
    private Map<String, String> tags;

    /**
     * 테넌트 키
     */
    private String tenantKey;

    /**
     * CSP별 특화 설정
     *
     * AWS 예시:
     *   - subnetIds: ["subnet-12345", "subnet-67890"] (VPC 연결 시)
     *   - securityGroupIds: ["sg-12345"] (VPC 연결 시)
     *   - layers: ["arn:aws:lambda:region:account:layer:layer-name:1"]
     *   - reservedConcurrentExecutions: 10
     *   - deadLetterQueueTargetArn: "arn:aws:sqs:region:account:dlq"
     *
     * Azure 예시:
     *   - hostingPlan: "Consumption" | "Premium" | "Dedicated"
     *   - appServicePlanId: "/subscriptions/.../resourceGroups/.../providers/..."
     *   - bindings: [{"type": "httpTrigger", "direction": "in", "authLevel": "function"}]
     *
     * GCP 예시:
     *   - serviceAccountEmail: "my-function@project.iam.gserviceaccount.com"
     *   - vpcConnector: "projects/project/locations/region/connectors/connector"
     *   - maxInstances: 10
     *   - minInstances: 0
     *   - ingressSettings: "ALLOW_ALL" | "ALLOW_INTERNAL_ONLY" | "ALLOW_INTERNAL_AND_GCLB"
     */
    private Map<String, Object> providerSpecificConfig;
}
