package com.agenticcp.core.domain.platform.dto.targeting;

import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 타겟팅 규칙 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "타겟팅 규칙 응답")
public class TargetRuleResponse {

    @Schema(description = "규칙 ID", example = "1")
    private Long id;

    @Schema(description = "기능 플래그 ID", example = "1")
    private Long featureFlagId;

    @Schema(description = "기능 플래그 키", example = "new-feature")
    private String featureFlagKey;

    @Schema(description = "기능 플래그 이름", example = "새로운 기능")
    private String featureFlagName;

    @Schema(description = "규칙 이름", example = "AWS 프로바이더 타겟팅")
    private String ruleName;

    @Schema(description = "규칙 설명", example = "AWS 클라우드 프로바이더 사용자를 대상으로 하는 규칙")
    private String ruleDescription;

    @Schema(description = "규칙 타입", example = "CLOUD_PROVIDER")
    private FeatureFlagTargetRule.RuleType ruleType;

    @Schema(description = "규칙 조건 (JSON 형태)", example = "{\"operator\":\"equals\",\"field\":\"cloudProvider\"}")
    private String ruleCondition;

    @Schema(description = "규칙 값 (JSON 형태)", example = "[\"aws\",\"azure\"]")
    private String ruleValue;

    @Schema(description = "우선순위", example = "100")
    private Integer priority;

    @JsonProperty("isEnabled")
    @Schema(description = "활성화 여부", example = "true")
    private Boolean isEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "생성 일시", example = "2025-01-01T00:00:00")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "수정 일시", example = "2025-01-01T00:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "생성자", example = "admin")
    private String createdBy;

    @Schema(description = "수정자", example = "admin")
    private String updatedBy;
}
