package com.agenticcp.core.domain.tenant.repository;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 테넌트 타입별 설정 Repository
 */
@Repository
public interface TenantTypeConfigRepository extends JpaRepository<TenantTypeConfig, Long> {

    /**
     * 테넌트 타입의 모든 설정 조회
     */
    List<TenantTypeConfig> findByTenantTypeAndIsDeletedFalse(Tenant.TenantType tenantType);

    /**
     * 테넌트 타입의 특정 설정 조회
     */
    Optional<TenantTypeConfig> findByTenantTypeAndConfigKeyAndIsDeletedFalse(Tenant.TenantType tenantType, String configKey);

    /**
     * 모든 테넌트 타입 설정 조회
     */
    List<TenantTypeConfig> findByIsDeletedFalse();
}



