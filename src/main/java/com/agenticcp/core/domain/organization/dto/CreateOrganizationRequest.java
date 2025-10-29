package com.agenticcp.core.domain.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequest {
    
    @NotBlank(message = "조직명은 필수입니다")
    @Size(max = 255, message = "조직명은 255자를 초과할 수 없습니다")
    private String orgName;
    
    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    private String description;
    
    private Long parentOrganizationId; // 상위 조직 ID (선택 사항)
    private String orgType; // 조직 타입 (선택 사항)
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String website;
    private Integer maxUsers;
    private String settings; // JSON for organization-specific settings
}
