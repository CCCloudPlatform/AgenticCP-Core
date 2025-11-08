package com.agenticcp.core.domain.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationStatsResponse {
    private Long totalOrganizations;
    private Long activeOrganizations;
    private Long inactiveOrganizations;
    private int maxDepth;
    private List<LevelStats> levelStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelStats {
        private int level;
        private Long count;
        private String description;
    }
}
