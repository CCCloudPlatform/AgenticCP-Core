package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.user.entity.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 조직별 역할 매핑 엔티티
 * 조직과 역할의 관계를 단일 진실원천으로 관리합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "organization_roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_organization_role", columnNames = {"organization_id", "role_id"})
        },
        indexes = {
                @Index(name = "idx_org_roles_org", columnList = "organization_id"),
                @Index(name = "idx_org_roles_role", columnList = "role_id"),
                @Index(name = "idx_org_roles_default", columnList = "is_default"),
                @Index(name = "idx_org_roles_priority", columnList = "priority")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrganizationRole extends BaseEntity {

    /** 조직 */
    @NotNull(message = "조직은 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** 역할 */
    @NotNull(message = "역할은 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** 기본 역할 여부 */
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /** 우선순위 */
    @Min(value = 0, message = "우선순위는 0 이상이어야 합니다")
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    /** 상태 */
    @NotNull(message = "상태는 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;
}


