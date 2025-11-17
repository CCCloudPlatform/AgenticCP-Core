package com.agenticcp.core.domain.organization.dto;

import com.agenticcp.core.domain.organization.entity.Organization;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 조직 응답 DTO
 * 
 * <p>조직 정보를 표현하는 응답 DTO입니다.
 * Organization 엔티티를 클라이언트에 전달하기 위한 변환 객체입니다.</p>
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
@Schema(description = "조직 응답")
public class OrganizationResponse {
    
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
    
    /**
     * Organization 엔티티를 OrganizationResponse로 변환
     * 
     * @param organization 조직 엔티티
     * @return 조직 응답 DTO
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
