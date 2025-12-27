package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Organization User 엔티티
 * 
 * <p>조직-사용자 매핑을 담당하는 엔티티입니다.
 * Organization ↔ User (M:N) 관계를 관리합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Entity
@Table(name = "organization_users", indexes = {
    @Index(name = "idx_ou_organization", columnList = "organization_id"),
    @Index(name = "idx_ou_user", columnList = "user_id"),
    @Index(name = "idx_ou_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_ou_organization_user", columnNames = {"organization_id", "user_id", "is_deleted"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrganizationUser extends BaseEntity {

    @NotNull(message = "Organization은 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotNull(message = "User는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "org_role", length = 50)
    private String orgRole; // 조직 내 역할 (ORG_ADMIN, ORG_MEMBER 등)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;
}

