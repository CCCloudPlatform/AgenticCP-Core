package com.agenticcp.core.domain.security.repository;

import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 보안 정책 리포지토리
 *
 * <p>SecurityPolicy 엔티티에 대한 조회 및 통계 기능을 제공합니다.</p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Repository
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, Long> {

    String TENANT_ID_PARAM = "tenantId";
    String STATUS_PARAM = "status";
    String POLICY_TYPE_PARAM = "policyType";
    String NOW_PARAM = "now";

    Optional<SecurityPolicy> findByPolicyKey(String policyKey);

    List<SecurityPolicy> findByStatus(Status status);

    List<SecurityPolicy> findByTenant(Tenant tenant);

    List<SecurityPolicy> findByPolicyType(SecurityPolicy.PolicyType policyType);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.status = :" + STATUS_PARAM + " AND sp.isEnabled = true AND sp.isDeleted = false")
    List<SecurityPolicy> findActivePolicies(@Param(STATUS_PARAM) Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.isGlobal = true AND sp.status = :" + STATUS_PARAM + " AND sp.isEnabled = true")
    List<SecurityPolicy> findGlobalPolicies(@Param(STATUS_PARAM) Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.isSystem = true AND sp.status = :" + STATUS_PARAM)
    List<SecurityPolicy> findSystemPolicies(@Param(STATUS_PARAM) Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.tenant = :tenant AND sp.status = :" + STATUS_PARAM + " AND sp.isEnabled = true")
    List<SecurityPolicy> findActivePoliciesByTenant(@Param("tenant") Tenant tenant, @Param(STATUS_PARAM) Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.effectiveFrom <= :" + NOW_PARAM + " AND (sp.effectiveUntil IS NULL OR sp.effectiveUntil >= :" + NOW_PARAM + ") AND sp.status = :" + STATUS_PARAM)
    List<SecurityPolicy> findEffectivePolicies(@Param(NOW_PARAM) LocalDateTime now, @Param(STATUS_PARAM) Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.policyType = :" + POLICY_TYPE_PARAM + " AND sp.status = :" + STATUS_PARAM + " AND sp.isEnabled = true ORDER BY sp.priority DESC")
    List<SecurityPolicy> findPoliciesByTypeOrderedByPriority(@Param(POLICY_TYPE_PARAM) SecurityPolicy.PolicyType policyType, @Param(STATUS_PARAM) Status status);

    @Query("SELECT COUNT(sp) FROM SecurityPolicy sp WHERE sp.tenant = :tenant AND sp.status = :" + STATUS_PARAM)
    Long countPoliciesByTenant(@Param("tenant") Tenant tenant, @Param(STATUS_PARAM) Status status);
    
    /**
     * 테넌트별 활성화된 정책 조회 (Feature 2 & 3)
     * 우선순위 관리를 위한 메서드들
     * @param tenantId 테넌트 ID
     * @return 활성화된 정책 목록
     */
    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.tenant.id = :" + TENANT_ID_PARAM + " AND sp.isEnabled = true AND sp.isDeleted = false")
    List<SecurityPolicy> findByTenantIdAndIsEnabledTrue(@Param(TENANT_ID_PARAM) Long tenantId);
    
    /**
     * 글로벌 활성화된 정책 조회 (Feature 2 & 3)
     * @return 글로벌 활성화 정책 목록
     */
    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.isGlobal = true AND sp.isEnabled = true AND sp.isDeleted = false")
    List<SecurityPolicy> findByIsGlobalTrueAndIsEnabledTrue();
}
