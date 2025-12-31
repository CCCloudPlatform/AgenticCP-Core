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
 * <p>ERD 기준 필드: id, name, created_at + tenant 정보 (1:1)</p>
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
    
    /** 조직명 (ERD: name) */
    @Schema(description = "조직명", example = "개발팀")
    private String name;
    
    // ========== 테넌트 정보 (1:1 관계) ==========
    
    /** 테넌트 ID */
    @Schema(description = "연결된 테넌트 ID", example = "1")
    private Long tenantId;
    
    /** 테넌트 키 */
    @Schema(description = "연결된 테넌트 키", example = "tenant-dev")
    private String tenantKey;
    
    /** 테넌트 타입 (DEDICATED/SHARED) */
    @Schema(description = "테넌트 타입", example = "DEDICATED")
    private String tenantType;
    
    // ========== [DEPRECATED] 호환성을 위해 유지 ==========
    
    /** @deprecated ERD에 없음 - name 필드 사용 권장 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 조직 키", example = "dev-team")
    private String orgKey;
    
    /** @deprecated ERD에 없음 - name 필드 사용 권장 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 조직명", example = "개발팀")
    private String orgName;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 조직 설명")
    private String description;
    
    /** @deprecated ERD에 계층 구조 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 상위 조직 ID")
    private Long parentOrgId;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 상태")
    private String status;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 조직 타입")
    private String orgType;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 연락처 이메일")
    private String contactEmail;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 연락처 전화번호")
    private String contactPhone;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 주소")
    private String address;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 웹사이트")
    private String website;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 최대 사용자 수")
    private Integer maxUsers;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 조직별 설정 (JSON)")
    private String settings;
    
    /** @deprecated ERD에 없음 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 설립일")
    private LocalDateTime establishedDate;
    
    /** 생성일시 (ERD: created_at) */
    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    
    /** @deprecated ERD에 없음 - BaseEntity 상속으로 존재하지만 ERD에 미포함 */
    @Deprecated
    @Schema(description = "[DEPRECATED] 수정일시")
    private LocalDateTime updatedAt;
    
    /**
     * Organization 엔티티를 OrganizationResponse로 변환
     * 
     * @param organization 조직 엔티티
     * @return 조직 응답 DTO
     */
    public static OrganizationResponse from(Organization organization) {
        OrganizationResponseBuilder builder = OrganizationResponse.builder()
                .id(organization.getId())
                // ERD 기준 필드 (설계 B: name만 존재)
                .name(organization.getName())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt());
        
        // 설계 B: Organization은 tenant 필드가 없음
        // 테넌트 정보는 별도로 조회 필요
        
        return builder.build();
    }
}
