package com.agenticcp.core.domain.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 조직 경로 응답 DTO
 * 
 * <p>루트부터 현재 조직까지의 경로를 표현하는 응답 DTO입니다.
 * 경로 목록, 전체 경로 문자열, 계층 레벨 등의 정보를 포함합니다.</p>
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
@Schema(description = "조직 경로 응답")
public class OrganizationPathResponse {
    
    /** 조직 경로 목록 (루트부터 현재까지) */
    @Schema(description = "조직 경로 목록 (루트부터 현재까지)", 
            example = "[{\"id\": 1, \"orgName\": \"회사\"}, {\"id\": 2, \"orgName\": \"개발팀\"}]")
    private List<OrganizationResponse> path;
    
    /** 전체 경로 문자열 */
    @Schema(description = "전체 경로 문자열", example = "회사 > 개발팀 > 백엔드팀")
    private String fullPath;
    
    /** 계층 레벨 (루트는 0) */
    @Schema(description = "계층 레벨 (루트는 0)", example = "2")
    private int level;
    
    /**
     * 조직 경로 목록을 OrganizationPathResponse로 변환
     * 
     * <p>조직 경로 목록을 받아 전체 경로 문자열과 계층 레벨을 계산하여
     * OrganizationPathResponse를 생성합니다.</p>
     * 
     * @param path 조직 경로 목록 (루트부터 현재까지)
     * @return 조직 경로 응답 DTO
     */
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
