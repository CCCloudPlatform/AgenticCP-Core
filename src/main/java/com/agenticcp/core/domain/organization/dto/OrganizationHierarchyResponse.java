package com.agenticcp.core.domain.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationHierarchyResponse {
    private Long id;
    private String orgKey;
    private String orgName;
    private String description;
    private Long parentOrgId;
    private String status;
    private String orgType;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String website;
    private Integer maxUsers;
    private String settings;
    private LocalDateTime establishedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 계층 구조 관련 필드
    private int level;
    private String path;
    private List<OrganizationHierarchyResponse> children;
    private int childrenCount;
    private int totalUsers;
    
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
