package com.agenticcp.core.domain.user.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Worker 엔티티
 * 
 * <p>User와 Tenant를 연결하는 엔티티입니다.
 * User ↔ Worker (1:N): 한 User는 여러 Worker를 가질 수 있음
 * Worker → Tenant (N:1): Worker는 하나의 Tenant에만 속함</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Entity
@Table(name = "workers", indexes = {
    @Index(name = "idx_workers_tenant", columnList = "tenant_id"),
    @Index(name = "idx_workers_user", columnList = "user_id"),
    @Index(name = "idx_workers_organization", columnList = "organization_id"),
    @Index(name = "idx_workers_key", columnList = "worker_key"),
    @Index(name = "idx_workers_user_tenant", columnList = "user_id, tenant_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_workers_key", columnNames = "worker_key")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Worker extends BaseEntity {

    @NotBlank(message = "Worker 키는 필수입니다")
    @Column(name = "worker_key", nullable = false, unique = true, length = 100)
    private String workerKey;

    @NotBlank(message = "Worker 이름은 필수입니다")
    @Column(name = "worker_name", nullable = false, length = 255)
    private String workerName;

    @NotNull(message = "Tenant는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @NotNull(message = "User는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<WorkerRoleAssignment> roleAssignments;
}

