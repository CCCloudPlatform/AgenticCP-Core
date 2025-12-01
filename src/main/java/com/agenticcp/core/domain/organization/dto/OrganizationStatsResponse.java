package com.agenticcp.core.domain.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 조직 통계 응답 DTO
 * 
 * <p>조직 통계 정보를 표현하는 응답 DTO입니다.
 * 전체 조직 수, 활성/비활성 조직 수, 최대 깊이, 레벨별 통계 등의 정보를 포함합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "조직 통계 응답")
public class OrganizationStatsResponse {
    
    /** 전체 조직 수 */
    @Schema(description = "전체 조직 수", example = "100")
    private Long totalOrganizations;
    
    /** 활성 조직 수 */
    @Schema(description = "활성 조직 수", example = "80")
    private Long activeOrganizations;
    
    /** 비활성 조직 수 */
    @Schema(description = "비활성 조직 수", example = "20")
    private Long inactiveOrganizations;
    
    /** 최대 계층 깊이 */
    @Schema(description = "최대 계층 깊이", example = "5")
    private int maxDepth;
    
    /** 레벨별 통계 목록 */
    @Schema(description = "레벨별 통계 목록")
    private List<LevelStats> levelStats;
    
    /**
     * 레벨별 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Schema(description = "레벨별 통계")
    public static class LevelStats {
        
        /** 계층 레벨 */
        @Schema(description = "계층 레벨", example = "1")
        private int level;
        
        /** 해당 레벨의 조직 수 */
        @Schema(description = "해당 레벨의 조직 수", example = "10")
        private Long count;
        
        /** 레벨 설명 */
        @Schema(description = "레벨 설명", example = "1단계 조직")
        private String description;
    }
}
