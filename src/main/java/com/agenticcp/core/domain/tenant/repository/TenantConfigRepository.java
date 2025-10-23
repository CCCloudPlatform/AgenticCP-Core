package com.agenticcp.core.domain.tenant.repository;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 테넌트 설정 Repository
 */
@Repository
public interface TenantConfigRepository extends JpaRepository<TenantConfig, Long> {

    /**
     * 테넌트의 모든 설정 조회
     */
    List<TenantConfig> findByTenantAndIsDeletedFalse(Tenant tenant);

    /**
     * 테넌트의 특정 설정 조회
     */
    Optional<TenantConfig> findByTenantAndConfigKeyAndIsDeletedFalse(Tenant tenant, String configKey);

    /**
     * 테넌트 키로 모든 설정 조회
     */
    @Query("SELECT tc FROM TenantConfig tc JOIN tc.tenant t WHERE t.tenantKey = :tenantKey AND tc.isDeleted = false")
    List<TenantConfig> findByTenantKey(@Param("tenantKey") String tenantKey);

    /**
     * 테넌트 키와 설정 키로 특정 설정 조회
     */
    @Query("SELECT tc FROM TenantConfig tc JOIN tc.tenant t WHERE t.tenantKey = :tenantKey AND tc.configKey = :configKey AND tc.isDeleted = false")
    Optional<TenantConfig> findByTenantKeyAndConfigKey(@Param("tenantKey") String tenantKey, @Param("configKey") String configKey);

    /**
     * 설정 키로 모든 테넌트의 설정 조회
     */
    @Query("SELECT tc FROM TenantConfig tc JOIN tc.tenant t WHERE tc.configKey = :configKey AND tc.isDeleted = false")
    List<TenantConfig> findByConfigKey(@Param("configKey") String configKey);
}

