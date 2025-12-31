package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.domain.organization.entity.Worker;
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
 * CloudResourceWorkerMap 엔티티
 * 
 * <p>클라우드 리소스와 Worker 간의 매핑을 정의하는 엔티티입니다.
 * 설계 C 기준: 리소스 단위로 Worker 접근 권한을 관리합니다.
 * access_scope는 불필요합니다 (리소스 단위로 관리하므로).</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Entity
@Table(name = "cloud_resource_worker_map", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cloud_resource_worker", columnNames = {"cloud_resource_id", "worker_id"})
}, indexes = {
    @Index(name = "idx_cloud_resource_worker_resource", columnList = "cloud_resource_id"),
    @Index(name = "idx_cloud_resource_worker_worker", columnList = "worker_id")
})
@IdClass(CloudResourceWorkerMapId.class)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CloudResourceWorkerMap {
    
    /**
     * 클라우드 리소스 (복합 PK의 일부)
     */
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_resource_id", nullable = false)
    private CloudResource cloudResource;
    
    /**
     * Worker (복합 PK의 일부)
     */
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
    
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

