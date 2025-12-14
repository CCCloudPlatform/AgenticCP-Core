package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Worker 엔티티
 * 
 * <p>User의 테넌트 내 ID를 나타내는 엔티티입니다.
 * 설계 B 기준: User 1:N Worker 관계이며, Worker는 오직 User 기반으로만 생성됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Entity
@Table(name = "workers", indexes = {
    @Index(name = "idx_worker_user", columnList = "user_id"),
    @Index(name = "idx_worker_tenant", columnList = "tenant_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_worker_user_tenant", columnNames = {"user_id", "tenant_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Worker extends BaseEntity {

    /**
     * 전역 User (User 1:N Worker)
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 소속 테넌트 (Tenant 1:N Worker)
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}

