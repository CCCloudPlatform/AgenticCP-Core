package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Serverless Function 실행 요청 DTO
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionInvokeRequest {

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
     * 함수 ID/ARN (실행할 함수 식별자)
     */
    private String functionId;

    /**
     * 리전 (필수)
     * 예: us-east-1, ap-northeast-2
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;

    /**
     * 실행 타입
     * "RequestResponse" (동기), "Event" (비동기), "DryRun" (검증)
     */
    @Pattern(regexp = "RequestResponse|Event|DryRun", message = "RequestResponse, Event, DryRun 중 하나여야 합니다")
    @Builder.Default
    private String invocationType = "RequestResponse";

    /**
     * JSON 문자열 페이로드 (필수)
     */
    @NotBlank(message = "페이로드는 필수입니다")
    private String payload;

    /**
     * 함수 버전/별칭 (선택적)
     */
    private String qualifier;

    /**
     * 추가 컨텍스트 정보
     */
    private Map<String, String> context;

    /**
     * 테넌트 키
     */
    private String tenantKey;
}
