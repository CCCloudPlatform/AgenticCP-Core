package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 조직 엔티티
 * 
 * <p>조직 정보를 관리하는 엔티티입니다.
 * ERD 기준: id, name, created_at</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_organizations_name", columnList = "name"),
    // [DEPRECATED] 아래 인덱스들은 컬럼 제거 시 함께 제거 예정
    @Index(name = "idx_organizations_org_key", columnList = "org_key"),
    @Index(name = "idx_organizations_parent", columnList = "parent_org_id"),
    @Index(name = "idx_organizations_status", columnList = "status"),
    @Index(name = "idx_organizations_type", columnList = "org_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Organization extends BaseEntity {

    /** 조직명 (ERD: name) */
    @NotBlank(message = "조직명은 필수입니다")
    @Size(max = 255, message = "조직명은 255자를 초과할 수 없습니다")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** 테넌트 (1:1 관계) - ERD: organization_id FK in TENANT */
    @OneToOne(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Tenant tenant;

    // ========== [DEPRECATED] ERD에 없는 필드들 ==========
    // TODO: 마이그레이션 완료 후 제거 예정
    
    /** @deprecated ERD에 없음 - 호환성을 위해 임시 유지 */
    @Deprecated
    @Column(name = "org_key", unique = true, length = 100)
    private String orgKey;

    /** @deprecated ERD에 없음 - 호환성을 위해 임시 유지 */
    @Deprecated
    @Column(name = "org_name", length = 255)
    private String orgName;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** @deprecated ERD에 계층 구조 없음 */
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_org_id")
    private Organization parentOrganization;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", length = 20)
    private OrganizationType orgType;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Column(name = "website", length = 255)
    private String website;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Column(name = "max_users")
    private Integer maxUsers;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;

    /** @deprecated ERD에 없음 */
    @Deprecated
    @Column(name = "established_date")
    private LocalDateTime establishedDate;

    /**
     * @deprecated ERD에 없음
     */
    @Deprecated
    public enum OrganizationType {
        COMPANY,
        DEPARTMENT,
        TEAM,
        PROJECT,
        DIVISION
    }
}
