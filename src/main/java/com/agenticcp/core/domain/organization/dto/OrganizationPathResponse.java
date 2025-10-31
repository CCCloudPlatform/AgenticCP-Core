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
public class OrganizationPathResponse {
    private List<OrganizationResponse> path;
    private String fullPath;
    private int level;
    
    public static OrganizationPathResponse from(List<OrganizationResponse> path) {
        String fullPath = path.stream()
                .map(OrganizationResponse::getOrgName)
                .reduce((a, b) -> a + " > " + b)
                .orElse("");
        
        return OrganizationPathResponse.builder()
                .path(path)
                .fullPath(fullPath)
                .level(path.size() - 1)
                .build();
    }
}
