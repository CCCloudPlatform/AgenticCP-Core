package com.agenticcp.core.domain.organization.dto;

import com.agenticcp.core.domain.organization.entity.Organization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    
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
    
    /**
     * Organization 엔티티를 OrganizationResponse로 변환
     */
    public static OrganizationResponse from(Organization organization) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .orgKey(organization.getOrgKey())
                .orgName(organization.getOrgName())
                .description(organization.getDescription())
                .parentOrgId(organization.getParentOrganization() != null ? 
                    organization.getParentOrganization().getId() : null)
                .status(organization.getStatus() != null ? organization.getStatus().name() : null)
                .orgType(organization.getOrgType() != null ? organization.getOrgType().name() : null)
                .contactEmail(organization.getContactEmail())
                .contactPhone(organization.getContactPhone())
                .address(organization.getAddress())
                .website(organization.getWebsite())
                .maxUsers(organization.getMaxUsers())
                .settings(organization.getSettings())
                .establishedDate(organization.getEstablishedDate())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }
}
