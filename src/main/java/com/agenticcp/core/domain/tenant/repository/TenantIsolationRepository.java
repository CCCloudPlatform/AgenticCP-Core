package com.agenticcp.core.domain.tenant.repository;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 테넌트 격리 Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface TenantIsolationRepository extends JpaRepository<TenantIsolation, Long> {

    /**
     * 테넌트로 격리 정보 조회
     * 
     * @param tenant 테넌트
     * @return 격리 정보
     */
    Optional<TenantIsolation> findByTenantAndIsDeletedFalse(Tenant tenant);

    /**
     * 테넌트 ID로 격리 정보 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 격리 정보
     */
    Optional<TenantIsolation> findByTenantIdAndIsDeletedFalse(Long tenantId);
}

