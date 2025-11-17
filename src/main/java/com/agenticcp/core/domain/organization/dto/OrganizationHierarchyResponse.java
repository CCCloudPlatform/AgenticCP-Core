package com.agenticcp.core.domain.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 조직 계층 구조 응답 DTO
 * 
 * <p>조직의 계층 구조를 표현하는 응답 DTO입니다.
 * 계층 레벨, 경로, 하위 조직 목록 등의 정보를 포함합니다.</p>
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
@Schema(description = "조직 계층 구조 응답")
public class OrganizationHierarchyResponse {
    
    /** 조직 ID */
    @Schema(description = "조직 ID", example = "1")
    private Long id;
    
    /** 조직 키 */
    @Schema(description = "조직 키", example = "dev-team")
    private String orgKey;
    
    /** 조직명 */
    @Schema(description = "조직명", example = "개발팀")
    private String orgName;
    
    /** 조직 설명 */
    @Schema(description = "조직 설명", example = "개발 관련 업무를 담당하는 조직")
    private String description;
    
    /** 상위 조직 ID */
    @Schema(description = "상위 조직 ID", example = "1")
    private Long parentOrgId;
    
    /** 상태 */
    @Schema(description = "상태", example = "ACTIVE")
    private String status;
    
    /** 조직 타입 */
    @Schema(description = "조직 타입", example = "DEPARTMENT")
    private String orgType;
    
    /** 연락처 이메일 */
    @Schema(description = "연락처 이메일", example = "contact@example.com")
    private String contactEmail;
    
    /** 연락처 전화번호 */
    @Schema(description = "연락처 전화번호", example = "02-1234-5678")
    private String contactPhone;
    
    /** 주소 */
    @Schema(description = "주소", example = "서울시 강남구")
    private String address;
    
    /** 웹사이트 */
    @Schema(description = "웹사이트", example = "https://example.com")
    private String website;
    
    /** 최대 사용자 수 */
    @Schema(description = "최대 사용자 수", example = "100")
    private Integer maxUsers;
    
    /** 조직별 설정 (JSON) */
    @Schema(description = "조직별 설정 (JSON)", example = "{\"theme\": \"dark\"}")
    private String settings;
    
    /** 설립일 */
    @Schema(description = "설립일", example = "2024-01-01T00:00:00")
    private LocalDateTime establishedDate;
    
    /** 생성일시 */
    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    
    /** 수정일시 */
    @Schema(description = "수정일시", example = "2024-01-01T00:00:00")
    private LocalDateTime updatedAt;
    
    /** 계층 레벨 (루트는 0) */
    @Schema(description = "계층 레벨 (루트는 0)", example = "1")
    private int level;
    
    /** 조직 경로 (루트부터 현재까지) */
    @Schema(description = "조직 경로 (루트부터 현재까지)", example = "/1/2/3")
    private String path;
    
    /** 하위 조직 목록 */
    @Schema(description = "하위 조직 목록")
    private List<OrganizationHierarchyResponse> children;
    
    /** 하위 조직 수 */
    @Schema(description = "하위 조직 수", example = "5")
    private int childrenCount;
    
    /** 총 사용자 수 */
    @Schema(description = "총 사용자 수", example = "50")
    private int totalUsers;
    
    /**
     * OrganizationResponse를 OrganizationHierarchyResponse로 변환
     * 
     * @param org 조직 응답 DTO
     * @param level 계층 레벨
     * @param path 조직 경로
     * @return 조직 계층 구조 응답 DTO
     */
    public static OrganizationHierarchyResponse from(OrganizationResponse org, int level, String path) {
        return OrganizationHierarchyResponse.builder()
                .id(org.getId())
                .orgKey(org.getOrgKey())
                .orgName(org.getOrgName())
                .description(org.getDescription())
                .parentOrgId(org.getParentOrgId())
                .status(org.getStatus())
                .orgType(org.getOrgType())
                .contactEmail(org.getContactEmail())
                .contactPhone(org.getContactPhone())
                .address(org.getAddress())
                .website(org.getWebsite())
                .maxUsers(org.getMaxUsers())
                .settings(org.getSettings())
                .establishedDate(org.getEstablishedDate())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .level(level)
                .path(path)
                .children(List.of())
                .childrenCount(0)
                .totalUsers(0)
                .build();
    }
}
