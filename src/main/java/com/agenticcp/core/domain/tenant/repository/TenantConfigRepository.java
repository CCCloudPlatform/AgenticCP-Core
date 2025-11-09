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
 * 테넌트 개별 설정 Repository
 * 
 * 개별 테넌트의 설정값을 조회하고 관리하는 데이터 액세스 레이어입니다.
 * TenantConfig 엔티티에 대한 CRUD 및 커스텀 쿼리를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-23
 */
@Repository
public interface TenantConfigRepository extends JpaRepository<TenantConfig, Long> {

    /**
     * 테넌트 엔티티로 모든 개별 설정 조회
     * 
     * 특정 테넌트의 모든 설정을 조회합니다.
     * 삭제되지 않은(isDeleted=false) 설정만 반환합니다.
     * 
     * @param tenant 조회할 테넌트 엔티티
     * @return 테넌트의 개별 설정 목록 (삭제되지 않은 것만)
     */
    List<TenantConfig> findByTenantAndIsDeletedFalse(Tenant tenant);

    /**
     * 테넌트 엔티티와 설정 키로 특정 개별 설정 조회
     * 
     * 특정 테넌트의 특정 설정 키에 해당하는 설정을 조회합니다.
     * 삭제되지 않은(isDeleted=false) 설정만 반환합니다.
     * 
     * @param tenant 조회할 테넌트 엔티티
     * @param configKey 조회할 설정 키
     * @return 테넌트의 특정 설정 (없으면 empty)
     */
    Optional<TenantConfig> findByTenantAndConfigKeyAndIsDeletedFalse(Tenant tenant, String configKey);

    /**
     * 테넌트 키로 모든 개별 설정 조회
     * 
     * 테넌트 엔티티 없이 tenantKey 문자열만으로 설정을 조회합니다.
     * Tenant 엔티티와 조인하여 tenantKey 기반으로 조회합니다.
     * 삭제되지 않은(isDeleted=false) 설정만 반환합니다.
     * 
     * @param tenantKey 조회할 테넌트 키 (예: "tenant-001")
     * @return 테넌트의 개별 설정 목록 (삭제되지 않은 것만)
     */
    @Query("SELECT tc FROM TenantConfig tc JOIN tc.tenant t WHERE t.tenantKey = :tenantKey AND tc.isDeleted = false")
    List<TenantConfig> findByTenantKey(@Param("tenantKey") String tenantKey);

    /**
     * 테넌트 키와 설정 키로 특정 개별 설정 조회
     * 
     * 테넌트 엔티티 없이 tenantKey와 configKey 문자열만으로 설정을 조회합니다.
     * Tenant 엔티티와 조인하여 tenantKey 기반으로 조회합니다.
     * 삭제되지 않은(isDeleted=false) 설정만 반환합니다.
     * 
     * @param tenantKey 조회할 테넌트 키 (예: "tenant-001")
     * @param configKey 조회할 설정 키 (예: "max_users")
     * @return 테넌트의 특정 설정 (없으면 empty)
     */
    @Query("SELECT tc FROM TenantConfig tc JOIN tc.tenant t WHERE t.tenantKey = :tenantKey AND tc.configKey = :configKey AND tc.isDeleted = false")
    Optional<TenantConfig> findByTenantKeyAndConfigKey(@Param("tenantKey") String tenantKey, @Param("configKey") String configKey);

    /**
     * 설정 키로 모든 테넌트의 설정 조회
     * 
     * 특정 설정 키를 가진 모든 테넌트의 설정을 조회합니다.
     * 동일한 설정 키를 사용하는 여러 테넌트의 설정을 비교하거나
     * 일괄 조회할 때 사용합니다. (예: 모든 테넌트의 "max_users" 설정 조회)
     * 삭제되지 않은(isDeleted=false) 설정만 반환합니다.
     * 
     * @param configKey 조회할 설정 키 (예: "max_users")
     * @return 해당 설정 키를 가진 모든 테넌트의 설정 목록
     */
    @Query("SELECT tc FROM TenantConfig tc JOIN tc.tenant t WHERE tc.configKey = :configKey AND tc.isDeleted = false")
    List<TenantConfig> findByConfigKey(@Param("configKey") String configKey);
}

