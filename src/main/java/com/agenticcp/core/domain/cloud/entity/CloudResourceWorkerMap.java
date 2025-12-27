package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.user.entity.Worker;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cloud Resource Worker Map 엔티티
 * 
 * <p>리소스-워커 접근 권한 매핑을 담당하는 엔티티입니다.
 * 쿼리 레벨 필터링을 위해 사용됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Entity
@Table(name = "cloud_resource_worker_map", indexes = {
    @Index(name = "idx_crwm_resource", columnList = "cloud_resource_id"),
    @Index(name = "idx_crwm_worker", columnList = "worker_id"),
    @Index(name = "idx_crwm_access_type", columnList = "access_type"),
    @Index(name = "idx_crwm_expires", columnList = "expires_at"),
    @Index(name = "idx_crwm_worker_deleted", columnList = "worker_id, is_deleted")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_crwm_resource_worker", columnNames = {"cloud_resource_id", "worker_id", "is_deleted"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CloudResourceWorkerMap extends BaseEntity {

    @NotNull(message = "Cloud Resource는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_resource_id", nullable = false)
    private CloudResource cloudResource;

    @NotNull(message = "Worker는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @NotNull(message = "접근 타입은 필수입니다")
    @Column(name = "access_type", nullable = false, length = 50)
    private String accessType; // CREATOR, ORGANIZATION, GRANTED

    @Column(name = "grant_reason", length = 255)
    private String grantReason;

    @Column(name = "granted_by", length = 255)
    private String grantedBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}

