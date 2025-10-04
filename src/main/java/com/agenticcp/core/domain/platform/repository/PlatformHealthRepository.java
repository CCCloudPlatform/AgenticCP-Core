package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformHealthRepository extends TenantAwareRepository<PlatformHealth, Long> {

    Optional<PlatformHealth> findByServiceName(String serviceName);

    List<PlatformHealth> findByStatus(PlatformHealth.HealthStatus status);

    @Query("SELECT ph FROM PlatformHealth ph WHERE ph.lastCheckTime >= :since")
    List<PlatformHealth> findRecentHealthChecks(@Param("since") LocalDateTime since);

    @Query("SELECT ph FROM PlatformHealth ph WHERE ph.status IN :statuses ORDER BY ph.lastCheckTime DESC")
    List<PlatformHealth> findByStatusIn(@Param("statuses") List<PlatformHealth.HealthStatus> statuses);

    @Query("SELECT ph FROM PlatformHealth ph WHERE ph.lastCheckTime = " +
           "(SELECT MAX(ph2.lastCheckTime) FROM PlatformHealth ph2 WHERE ph2.serviceName = ph.serviceName)")
    List<PlatformHealth> findLatestHealthStatus();
}
