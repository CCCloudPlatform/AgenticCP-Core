package com.agenticcp.core.common.entity;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 테넌트 인식 엔티티 - 테넌트 정보 포함
 * 
 * <p>
 * 테넌트별 격리가 필요한 기능(사용자, 조직, 리소스 등)에서 사용하는 베이스 엔티티입니다.
 * BaseEntity를 상속받아 공통 필드를 포함하며, 추가로 테넌트 정보를 관리합니다.
 * </p>
 * 
 * <p>
 * TenantAwareEntityListener를 통해 테넌트 컨텍스트 기반의 자동 설정 및 
 * 멀티 테넌시 데이터 격리를 지원합니다.
 * </p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, TenantAwareEntityListener.class})
public abstract class TenantAwareEntity extends BaseEntity {

    /**
     * 엔티티가 속한 테넌트
     * 멀티 테넌시 환경에서 데이터 격리를 위해 사용됩니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * 엔티티가 특정 테넌트에 속하는지 확인합니다.
     * 
     * @param tenant 확인할 테넌트 객체
     * @return 엔티티가 해당 테넌트에 속하면 true, 아니면 false
     */
    public boolean belongsToTenant(Tenant tenant) {
        return this.tenant != null && this.tenant.equals(tenant);
    }

    /**
     * 엔티티가 특정 테넌트 ID에 속하는지 확인합니다.
     * 
     * @param tenantId 확인할 테넌트 ID
     * @return 엔티티가 해당 테넌트 ID에 속하면 true, 아니면 false
     */
    public boolean belongsToTenant(Long tenantId) {
        return this.tenant != null && this.tenant.getId().equals(tenantId);
    }
}
