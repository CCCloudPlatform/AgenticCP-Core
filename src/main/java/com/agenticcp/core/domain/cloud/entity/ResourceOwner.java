package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 리소스 소유자 엔티티
 * 
 * <p>리소스와 사용자 간의 소유권 관계를 관리하는 중간 테이블입니다.
 * DEDICATED 격리 모드에서 리소스 접근 권한을 제어하는데 사용됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Entity
@Table(name = "resource_owners",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_resource_user_deleted",
           columnNames = {"resource_id", "user_id", "is_deleted"}
       ),
       indexes = {
           @Index(name = "idx_resource_owner_user_tenant", columnList = "user_id, tenant_id, is_deleted"),
           @Index(name = "idx_resource_owner_resource_tenant", columnList = "resource_id, tenant_id, is_deleted")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceOwner extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private CloudResource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 20)
    @Builder.Default
    private AccessType accessType = AccessType.OWNER;

    /**
     * 접근 타입 열거형
     */
    public enum AccessType {
        /** 소유자 (리소스 생성자) */
        OWNER,
        /** 공유 접근 (SHARED 모드에서 자동 추가, 향후 확장용) */
        SHARED,
        /** 읽기 전용 (향후 확장용) */
        READ_ONLY
    }
}

