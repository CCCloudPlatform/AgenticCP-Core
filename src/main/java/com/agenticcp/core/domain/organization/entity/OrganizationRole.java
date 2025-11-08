package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.user.entity.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 조직별 역할 매핑 엔티티
 * 조직과 역할의 관계를 단일 진실원천으로 관리합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-28
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
public class OrganizationRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "priority")
    private Integer priority = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;
}


