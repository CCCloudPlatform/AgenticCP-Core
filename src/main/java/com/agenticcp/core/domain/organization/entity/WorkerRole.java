package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.domain.user.entity.Role;
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
 * WorkerRole 엔티티
 * 
 * <p>Worker가 테넌트 내에서 수행할 역할을 정의하는 엔티티입니다.
 * 설계 C 기준: 복합 PK (worker_id, role_id)를 사용합니다.
 * tenant_id는 제거되었으며, Role이 이미 tenant_id를 가지므로 중복입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Entity
@Table(name = "worker_role", uniqueConstraints = {
    @UniqueConstraint(name = "uk_worker_role", columnNames = {"worker_id", "role_id"})
}, indexes = {
    @Index(name = "idx_worker_role_worker", columnList = "worker_id"),
    @Index(name = "idx_worker_role_role", columnList = "role_id")
})
@IdClass(WorkerRoleId.class)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WorkerRole {
    
    // 복합 PK의 일부 - 관계 필드에서 ID 추출
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
    
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
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

