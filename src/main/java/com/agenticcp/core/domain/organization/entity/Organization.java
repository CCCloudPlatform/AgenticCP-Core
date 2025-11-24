package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * <p>조직 정보를 관리하는 엔티티입니다. 계층 구조를 지원하며
 * 테넌트와의 관계를 관리합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "organizations", indexes = {
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

    /** 조직 키 */
    @NotBlank(message = "조직 키는 필수입니다")
    @Size(max = 100, message = "조직 키는 100자를 초과할 수 없습니다")
    @Column(name = "org_key", nullable = false, unique = true, length = 100)
    private String orgKey;

    /** 조직명 */
    @NotBlank(message = "조직명은 필수입니다")
    @Size(max = 255, message = "조직명은 255자를 초과할 수 없습니다")
    @Column(name = "org_name", nullable = false, length = 255)
    private String orgName;

    /** 조직 설명 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 테넌트 (1:1 관계) */
    @OneToOne(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Tenant tenant;

    /** 상위 조직 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_org_id")
    private Organization parentOrganization;

    /** 상태 */
    @NotNull(message = "상태는 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    /** 조직 타입 */
    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", length = 20)
    private OrganizationType orgType;

    /** 연락처 이메일 */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /** 연락처 전화번호 */
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    /** 주소 */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /** 웹사이트 */
    @Column(name = "website", length = 255)
    private String website;

    /** 최대 사용자 수 */
    @Min(value = 1, message = "최대 사용자 수는 1 이상이어야 합니다")
    @Column(name = "max_users")
    private Integer maxUsers;

    /** 조직별 설정 (JSON) */
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;

    /** 설립일 */
    @Column(name = "established_date")
    private LocalDateTime establishedDate;

    /**
     * 조직 타입 열거형
     */
    public enum OrganizationType {
        /** 회사 */
        COMPANY,
        /** 부서 */
        DEPARTMENT,
        /** 팀 */
        TEAM,
        /** 프로젝트 */
        PROJECT,
        /** 사업부 */
        DIVISION
    }
}
