package com.agenticcp.core.domain.platform.dto.targeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 타겟팅 규칙 수정 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "타겟팅 규칙 수정 요청")
public class UpdateTargetRuleRequest {

    @NotBlank(message = "규칙 이름은 필수입니다")
    @Size(max = 255, message = "규칙 이름은 255자를 초과할 수 없습니다")
    @Schema(description = "규칙 이름", example = "AWS 프로바이더 타겟팅", required = true)
    private String ruleName;

    @Size(max = 1000, message = "규칙 설명은 1000자를 초과할 수 없습니다")
    @Schema(description = "규칙 설명", example = "AWS 클라우드 프로바이더 사용자를 대상으로 하는 규칙")
    private String ruleDescription;

    @Size(max = 4000, message = "규칙 조건은 4000자를 초과할 수 없습니다")
    @Schema(description = "규칙 조건 (JSON 형태)", example = "{\"operator\":\"equals\",\"field\":\"cloudProvider\"}")
    private String ruleCondition;

    @Size(max = 4000, message = "규칙 값은 4000자를 초과할 수 없습니다")
    @Schema(description = "규칙 값 (JSON 형태)", example = "[\"aws\",\"azure\"]")
    private String ruleValue;

    @Schema(description = "우선순위", example = "100")
    private Integer priority;

    @JsonProperty("isEnabled")
    @Schema(description = "활성화 여부", example = "true")
    private Boolean isEnabled;
}
