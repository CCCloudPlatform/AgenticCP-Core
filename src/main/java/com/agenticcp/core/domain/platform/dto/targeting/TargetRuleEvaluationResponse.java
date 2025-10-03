package com.agenticcp.core.domain.platform.dto.targeting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 타겟팅 규칙 평가 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "타겟팅 규칙 평가 응답")
public class TargetRuleEvaluationResponse {

    @Schema(description = "평가 결과", example = "true")
    private Boolean result;

    @Schema(description = "매칭된 규칙 ID", example = "1")
    private Long matchedRuleId;

    @Schema(description = "매칭된 규칙 이름", example = "AWS 프로바이더 타겟팅")
    private String matchedRuleName;

    @Schema(description = "매칭된 규칙 타입", example = "CLOUD_PROVIDER")
    private String matchedRuleType;

    @Schema(description = "평가된 모든 규칙 결과 목록")
    private List<RuleEvaluationDetailDto> evaluationDetails;

    @Schema(description = "평가 시간 (밀리초)", example = "150")
    private Long evaluationTimeMs;

    @Schema(description = "평가 메시지", example = "규칙 평가 완료")
    private String message;

    /**
     * 개별 규칙 평가 상세 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "규칙 평가 상세 정보")
    public static class RuleEvaluationDetailDto {

        @Schema(description = "규칙 ID", example = "1")
        private Long ruleId;

        @Schema(description = "규칙 이름", example = "AWS 프로바이더 타겟팅")
        private String ruleName;

        @Schema(description = "규칙 타입", example = "CLOUD_PROVIDER")
        private String ruleType;

        @Schema(description = "규칙 매칭 여부", example = "true")
        private Boolean matched;

        @Schema(description = "매칭 이유", example = "클라우드 프로바이더 'aws'가 타겟 리스트에 포함됨")
        private String reason;

        @Schema(description = "우선순위", example = "100")
        private Integer priority;
    }
}
