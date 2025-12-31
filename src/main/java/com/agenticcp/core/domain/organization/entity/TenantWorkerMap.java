package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * TenantWorkerMap 엔티티
 * 
 * <p>Worker가 어떤 테넌트에 소속되는지 정의하는 엔티티입니다.
 * 설계 B 기준: 복합 PK (tenant_id, worker_id)를 사용합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Entity
@Table(name = "tenant_worker_map", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tenant_worker", columnNames = {"tenant_id", "worker_id"})
}, indexes = {
    @Index(name = "idx_tenant_worker_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tenant_worker_worker", columnList = "worker_id")
})
@IdClass(TenantWorkerMapId.class)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TenantWorkerMap {
    
    // 복합 PK의 일부 - 관계 필드에서 ID 추출
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
    
    /**
     * 공유 테넌트에서의 참여 범위
     */
    @Column(name = "access_scope", length = 30)
    private String accessScope;
    
    /**
     * 가입일시
     */
    @Column(name = "joined_at")
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    /**
     * 생성일시
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 생성자
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * 수정자
     */
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * 삭제 여부
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}

