package com.agenticcp.core.domain.monitoring.repository;

import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메트릭 데이터를 관리하는 Repository
 * - 자동 테넌트 필터링 지원
 * - 테넌트별 데이터 격리
 */
@Repository
public interface MetricRepository extends TenantAwareRepository<Metric, Long> {

    /**
     * 특정 메트릭 이름으로 최신 데이터 조회 (테넌트 필터링 적용)
     */
    @Query("SELECT m FROM Metric m WHERE m.metricName = :metricName AND m.tenantId = :tenantId ORDER BY m.collectedAt DESC")
    List<Metric> findLatestByMetricName(@Param("metricName") String metricName, @Param("tenantId") String tenantId, Pageable pageable);

    /**
     * 특정 메트릭 이름과 시간 범위로 조회 (테넌트 필터링 적용)
     */
    @Query("SELECT m FROM Metric m WHERE m.metricName = :metricName AND m.tenantId = :tenantId AND m.collectedAt BETWEEN :startTime AND :endTime ORDER BY m.collectedAt ASC")
    List<Metric> findByMetricNameAndTimeRange(@Param("metricName") String metricName, 
                                           @Param("tenantId") String tenantId,
                                           @Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 메트릭 타입별 조회 (테넌트 필터링 적용)
     */
    @Query("SELECT m FROM Metric m WHERE m.metricType = :metricType AND m.tenantId = :tenantId ORDER BY m.collectedAt DESC")
    Page<Metric> findByMetricType(@Param("metricType") Metric.MetricType metricType, @Param("tenantId") String tenantId, Pageable pageable);

    /**
     * 특정 시간 이후의 메트릭 조회 (테넌트 필터링 적용)
     */
    @Query("SELECT m FROM Metric m WHERE m.tenantId = :tenantId AND m.collectedAt >= :since ORDER BY m.collectedAt DESC")
    List<Metric> findSince(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    /**
     * 메트릭 이름 목록 조회 (중복 제거, 테넌트 필터링 적용)
     */
    @Query("SELECT DISTINCT m.metricName FROM Metric m WHERE m.tenantId = :tenantId ORDER BY m.metricName")
    List<String> findDistinctMetricNames(@Param("tenantId") String tenantId);

    /**
     * 특정 메트릭의 최신 값 조회 (테넌트 필터링 적용)
     */
    @Query("SELECT m FROM Metric m WHERE m.metricName = :metricName AND m.tenantId = :tenantId ORDER BY m.collectedAt DESC LIMIT 1")
    Metric findLatestByMetricName(@Param("metricName") String metricName, @Param("tenantId") String tenantId);

    /**
     * 오래된 메트릭 데이터 삭제 (데이터 보관 정책, 테넌트 필터링 적용)
     */
    @Query("DELETE FROM Metric m WHERE m.tenantId = :tenantId AND m.collectedAt < :cutoffDate")
    int deleteOldMetrics(@Param("tenantId") String tenantId, @Param("cutoffDate") LocalDateTime cutoffDate);
}
