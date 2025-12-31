package com.agenticcp.core.domain.organization.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Worker 엔티티
 * 
 * <p>User 또는 Organization을 Worker로 변환하는 엔티티입니다.
 * 설계 C 기준: User와 Organization 모두 Worker로 변환 가능하며, 
 * user_id와 organization_id 중 하나만 NOT NULL이어야 합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Entity
@Table(name = "workers", indexes = {
    @Index(name = "idx_worker_user", columnList = "user_id"),
    @Index(name = "idx_worker_organization", columnList = "organization_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Worker extends BaseEntity {

    /**
     * 전역 User (User 기반 Worker)
     * user_id와 organization_id 중 하나만 NOT NULL이어야 함
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 조직 (Organization 기반 Worker)
     * user_id와 organization_id 중 하나만 NOT NULL이어야 함
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * user_id와 organization_id 중 하나만 NOT NULL인지 검증
     */
    @AssertTrue(message = "user_id와 organization_id 중 하나만 설정되어야 합니다")
    private boolean isValidWorkerType() {
        return (user != null && organization == null) || 
               (user == null && organization != null);
    }
}

