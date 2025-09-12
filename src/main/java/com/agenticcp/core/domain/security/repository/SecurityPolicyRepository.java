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

@Repository
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, Long> {

    Optional<SecurityPolicy> findByPolicyKey(String policyKey);

    List<SecurityPolicy> findByStatus(Status status);

    List<SecurityPolicy> findByTenant(Tenant tenant);

    List<SecurityPolicy> findByPolicyType(SecurityPolicy.PolicyType policyType);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.status = :status AND sp.isEnabled = true AND sp.isDeleted = false")
    List<SecurityPolicy> findActivePolicies(@Param("status") Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.isGlobal = true AND sp.status = :status AND sp.isEnabled = true")
    List<SecurityPolicy> findGlobalPolicies(@Param("status") Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.isSystem = true AND sp.status = :status")
    List<SecurityPolicy> findSystemPolicies(@Param("status") Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.tenant = :tenant AND sp.status = :status AND sp.isEnabled = true")
    List<SecurityPolicy> findActivePoliciesByTenant(@Param("tenant") Tenant tenant, @Param("status") Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.effectiveFrom <= :now AND (sp.effectiveUntil IS NULL OR sp.effectiveUntil >= :now) AND sp.status = :status")
    List<SecurityPolicy> findEffectivePolicies(@Param("now") LocalDateTime now, @Param("status") Status status);

    @Query("SELECT sp FROM SecurityPolicy sp WHERE sp.policyType = :policyType AND sp.status = :status AND sp.isEnabled = true ORDER BY sp.priority DESC")
    List<SecurityPolicy> findPoliciesByTypeOrderedByPriority(@Param("policyType") SecurityPolicy.PolicyType policyType, @Param("status") Status status);

    @Query("SELECT COUNT(sp) FROM SecurityPolicy sp WHERE sp.tenant = :tenant AND sp.status = :status")
    Long countPoliciesByTenant(@Param("tenant") Tenant tenant, @Param("status") Status status);
}
