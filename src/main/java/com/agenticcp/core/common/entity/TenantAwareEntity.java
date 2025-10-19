package com.agenticcp.core.common.entity;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 테넌트 인식 엔티티 - 테넌트 정보 포함
 * 테넌트별 격리가 필요한 기능(사용자, 조직 등)에서 사용
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, TenantAwareEntityListener.class})
public abstract class TenantAwareEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // Getters and Setters
    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    // 비즈니스 메서드
    public boolean belongsToTenant(Tenant tenant) {
        return this.tenant != null && this.tenant.equals(tenant);
    }

    public boolean belongsToTenant(Long tenantId) {
        return this.tenant != null && this.tenant.getId().equals(tenantId);
    }
}
