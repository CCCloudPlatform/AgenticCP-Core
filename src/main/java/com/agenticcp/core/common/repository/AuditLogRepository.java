package com.agenticcp.core.common.repository;

import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 감사 로그 리포지토리
 * 
 * 감사 로그 데이터의 영속성을 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 대상 리소스 ID로 감사 로그 조회 (최근순)
     */
    List<AuditLog> findByTargetResourceIdOrderByTimestampDesc(String targetResourceId);


    /**
     * 특정 대상의 변경 이력 조회 (변경 추적이 있는 것만)
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.targetResourceId = :targetResourceId AND " +
           "(a.oldValue IS NOT NULL OR a.newValue IS NOT NULL) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findChangeHistoryByTargetResourceId(
        @Param("targetResourceId") String targetResourceId
    );

    /**
     * 리소스 타입과 대상 ID로 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByResourceTypeAndTargetResourceId(
        AuditResourceType resourceType,
        String targetResourceId,
        Pageable pageable
    );

    /**
     * 테넌트별 필터링된 감사 로그 조회
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.tenantId = :tenantId " +
           "AND (:startDate IS NULL OR a.timestamp >= :startDate) " +
           "AND (:endDate IS NULL OR a.timestamp <= :endDate) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:resourceType IS NULL OR a.resourceType = :resourceType) " +
           "AND (:severity IS NULL OR a.severity = :severity) " +
           "AND (:userId IS NULL OR a.userId = :userId) " +
           "AND (:success IS NULL OR a.success = :success) " +
           "AND (:targetResourceId IS NULL OR a.targetResourceId = :targetResourceId)")
    Page<AuditLog> findByTenantIdAndFilters(
        @Param("tenantId") String tenantId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("action") String action,
        @Param("resourceType") AuditResourceType resourceType,
        @Param("severity") AuditSeverity severity,
        @Param("userId") String userId,
        @Param("success") Boolean success,
        @Param("targetResourceId") String targetResourceId,
        Pageable pageable
    );

    /**
     * 특정 리소스의 감사 로그 조회 (테넌트별 필터링)
     */
    List<AuditLog> findByTargetResourceIdAndTenantId(String targetResourceId, String tenantId);

    /**
     * 테넌트별 기간 내 감사 로그 조회 (대시보드용)
     */
    List<AuditLog> findByTenantIdAndTimestampBetween(String tenantId, Instant startDate, Instant endDate);
}

