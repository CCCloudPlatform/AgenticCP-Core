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
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    /**
     * 테넌트별 Alert 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 해당 테넌트의 Alert 목록
     */
    List<Alert> findByTenantId(String tenantId);
    
    /**
     * 상태별 Alert 조회
     * 
     * @param status Alert 상태
     * @return 해당 상태의 Alert 목록
     */
    List<Alert> findByStatus(AlertStatus status);
    
    /**
     * 알림 타입별 조회
     * 
     * @param alertType Alert 타입
     * @return 해당 타입의 Alert 목록
     */
    List<Alert> findByAlertType(AlertType alertType);
    
    /**
     * 심각도별 조회
     * 
     * @param severity Alert 심각도
     * @return 해당 심각도의 Alert 목록
     */
    List<Alert> findBySeverity(Severity severity);
    
    /**
     * 테넌트 + 상태별 조회
     * 
     * @param tenantId 테넌트 ID
     * @param status Alert 상태
     * @return 해당 테넌트와 상태의 Alert 목록
     */
    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId AND a.status = :status AND a.isDeleted = false")
    List<Alert> findByTenantIdAndStatus(@Param("tenantId") String tenantId, 
                                       @Param("status") AlertStatus status);
    
    /**
     * 최근 Alert 조회
     * 
     * @param since 조회 시작 시각
     * @return 해당 시각 이후의 Alert 목록 (최신순 정렬)
     */
    @Query("SELECT a FROM Alert a WHERE a.lastTriggered >= :since AND a.isDeleted = false ORDER BY a.lastTriggered DESC")
    List<Alert> findRecentAlerts(@Param("since") LocalDateTime since);
    
    /**
     * 활성화된 Alert 조회
     * 
     * @return 활성화된 Alert 목록
     */
    @Query("SELECT a FROM Alert a WHERE a.isEnabled = true AND a.isDeleted = false")
    List<Alert> findEnabledAlerts();
}

