package com.agenticcp.core.common.repository;

import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * 테넌트 ID로 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByTenantId(String tenantId, Pageable pageable);

    /**
     * 사용자 ID로 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    /**
     * 사용자 ID로 감사 로그 조회 (전체)
     */
    List<AuditLog> findByUserId(String userId);

    /**
     * 요청 ID로 감사 로그 조회
     */
    List<AuditLog> findByRequestId(String requestId);

    /**
     * 액션으로 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * 리소스 타입으로 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByResourceType(AuditResourceType resourceType, Pageable pageable);

    /**
     * 심각도로 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findBySeverity(AuditSeverity severity, Pageable pageable);

    /**
     * 성공 여부로 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findBySuccess(Boolean success, Pageable pageable);

    /**
     * 기간별 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByTimestampBetween(Instant startTime, Instant endTime, Pageable pageable);

    /**
     * 테넌트 ID와 기간별 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByTenantIdAndTimestampBetween(
        String tenantId, 
        Instant startTime, 
        Instant endTime, 
        Pageable pageable
    );

    /**
     * 사용자 ID와 기간별 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByUserIdAndTimestampBetween(
        String userId, 
        Instant startTime, 
        Instant endTime, 
        Pageable pageable
    );

    /**
     * 실패한 감사 로그 조회 (최근순, 페이징)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.success = false ORDER BY a.timestamp DESC")
    Page<AuditLog> findFailedAudits(Pageable pageable);

    /**
     * 테넌트별 액션 통계 조회
     */
    @Query("SELECT a.action, COUNT(a) as count FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.timestamp BETWEEN :startTime AND :endTime GROUP BY a.action")
    List<Object[]> getActionStatisticsByTenant(
        @Param("tenantId") String tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * 특정 기간 동안의 심각도별 통계 조회
     */
    @Query("SELECT a.severity, COUNT(a) as count FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :startTime AND :endTime GROUP BY a.severity")
    List<Object[]> getSeverityStatistics(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * 대상 리소스 ID로 감사 로그 조회 (최근순)
     */
    List<AuditLog> findByTargetResourceIdOrderByTimestampDesc(String targetResourceId);

    /**
     * 리소스 타입과 대상 ID로 감사 로그 조회 (페이징)
     */
    Page<AuditLog> findByResourceTypeAndTargetResourceId(
        AuditResourceType resourceType,
        String targetResourceId,
        Pageable pageable
    );

    /**
     * 변경 추적이 있는 감사 로그만 조회 (oldValue 또는 newValue가 null이 아닌 것)
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(a.oldValue IS NOT NULL OR a.newValue IS NOT NULL) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findChangeTrackedAudits(Pageable pageable);

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
}

