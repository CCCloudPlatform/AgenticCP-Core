package com.agenticcp.core.domain.monitoring.repository;

import com.agenticcp.core.domain.monitoring.entity.Alert;
import com.agenticcp.core.domain.monitoring.enums.AlertStatus;
import com.agenticcp.core.domain.monitoring.enums.AlertType;
import com.agenticcp.core.domain.monitoring.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Alert 데이터 접근 계층
 * 
 * <p>Issue #15 가이드: Alert 엔티티 CRUD</p>
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    /**
     * 테넌트별 Alert 조회
     */
    List<Alert> findByTenantId(String tenantId);
    
    /**
     * 상태별 Alert 조회
     */
    List<Alert> findByStatus(AlertStatus status);
    
    /**
     * 알림 타입별 조회
     */
    List<Alert> findByAlertType(AlertType alertType);
    
    /**
     * 심각도별 조회
     */
    List<Alert> findBySeverity(Severity severity);
    
    /**
     * 테넌트 + 상태별 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId AND a.status = :status AND a.isDeleted = false")
    List<Alert> findByTenantIdAndStatus(@Param("tenantId") String tenantId, 
                                       @Param("status") AlertStatus status);
    
    /**
     * 최근 Alert 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.lastTriggered >= :since AND a.isDeleted = false ORDER BY a.lastTriggered DESC")
    List<Alert> findRecentAlerts(@Param("since") LocalDateTime since);
    
    /**
     * 활성화된 Alert 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.isEnabled = true AND a.isDeleted = false")
    List<Alert> findEnabledAlerts();
}

