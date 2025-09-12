package com.agenticcp.core.domain.tenant.repository;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantKey(String tenantKey);

    List<Tenant> findByStatus(Status status);

    List<Tenant> findByTenantType(Tenant.TenantType tenantType);

    @Query("SELECT t FROM Tenant t WHERE t.status = :status AND t.isDeleted = false")
    List<Tenant> findActiveTenants(@Param("status") Status status);

    @Query("SELECT t FROM Tenant t WHERE t.contactEmail = :email AND t.isDeleted = false")
    Optional<Tenant> findByContactEmail(@Param("email") String email);

    @Query("SELECT t FROM Tenant t WHERE t.isTrial = true AND t.trialEndDate > :now")
    List<Tenant> findActiveTrialTenants(@Param("now") LocalDateTime now);

    @Query("SELECT t FROM Tenant t WHERE t.subscriptionEndDate < :now AND t.status = :status")
    List<Tenant> findExpiredTenants(@Param("now") LocalDateTime now, @Param("status") Status status);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = :status AND t.isDeleted = false")
    Long countActiveTenants(@Param("status") Status status);
}
