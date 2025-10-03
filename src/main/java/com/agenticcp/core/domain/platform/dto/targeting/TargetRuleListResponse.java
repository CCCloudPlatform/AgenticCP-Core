package com.agenticcp.core.domain.platform.dto.targeting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 타겟팅 규칙 목록 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "타겟팅 규칙 목록 응답")
public class TargetRuleListResponse {

    @Schema(description = "타겟팅 규칙 목록")
    private List<TargetRuleResponse> rules;

    @Schema(description = "총 규칙 수", example = "10")
    private Long totalCount;

    @Schema(description = "활성화된 규칙 수", example = "8")
    private Long activeCount;

    @Schema(description = "비활성화된 규칙 수", example = "2")
    private Long inactiveCount;
}
