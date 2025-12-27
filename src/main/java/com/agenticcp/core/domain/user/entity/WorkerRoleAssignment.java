package com.agenticcp.core.domain.user.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Worker Role Assignment 엔티티
 * 
 * <p>Worker에 Role을 할당하는 엔티티입니다.
 * Tenant 스코프를 포함하여 멀티 테넌트 환경에서 Worker의 Role을 관리합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Entity
@Table(name = "worker_role_assignments", indexes = {
    @Index(name = "idx_wra_tenant", columnList = "tenant_id"),
    @Index(name = "idx_wra_worker", columnList = "worker_id"),
    @Index(name = "idx_wra_role", columnList = "role_id"),
    @Index(name = "idx_wra_tenant_worker", columnList = "tenant_id, worker_id"),
    @Index(name = "idx_wra_expires", columnList = "expires_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_wra_tenant_worker_role", columnNames = {"tenant_id", "worker_id", "role_id", "is_deleted"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class WorkerRoleAssignment extends BaseEntity {

    @NotNull(message = "Tenant는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull(message = "Worker는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @NotNull(message = "Role은 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_by", length = 255)
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}

