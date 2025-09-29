package com.agenticcp.core.domain.platform.dto.targeting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 타겟팅 규칙 평가 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "타겟팅 규칙 평가 요청")
public class TargetRuleEvaluationRequest {

    @NotBlank(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID", example = "user123", required = true)
    private String userId;

    @NotBlank(message = "테넌트 ID는 필수입니다")
    @Schema(description = "테넌트 ID", example = "tenant123", required = true)
    private String tenantId;

    @Size(max = 100, message = "클라우드 프로바이더는 100자를 초과할 수 없습니다")
    @Schema(description = "클라우드 프로바이더", example = "aws")
    private String cloudProvider;

    @Size(max = 100, message = "리전은 100자를 초과할 수 없습니다")
    @Schema(description = "클라우드 리전", example = "us-east-1")
    private String region;

    @Size(max = 100, message = "테넌트 타입은 100자를 초과할 수 없습니다")
    @Schema(description = "테넌트 타입", example = "enterprise")
    private String tenantType;

    @Size(max = 100, message = "테넌트 등급은 100자를 초과할 수 없습니다")
    @Schema(description = "테넌트 등급", example = "premium")
    private String tenantTier;

    @Size(max = 100, message = "사용자 역할은 100자를 초과할 수 없습니다")
    @Schema(description = "사용자 역할", example = "admin")
    private String userRole;

    @Schema(description = "사용자 속성 (키-값 쌍)", example = "{\"department\":\"engineering\",\"level\":\"senior\"}")
    private Map<String, Object> userAttributes;

    @Schema(description = "커스텀 속성 (키-값 쌍)", example = "{\"feature_preference\":\"advanced\",\"beta_tester\":true}")
    private Map<String, Object> customAttributes;

    @Schema(description = "평가 컨텍스트 추가 정보", example = "{\"session_id\":\"sess123\",\"request_id\":\"req456\"}")
    private Map<String, Object> context;
}
