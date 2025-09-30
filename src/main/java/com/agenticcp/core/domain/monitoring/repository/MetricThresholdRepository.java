package com.agenticcp.core.domain.monitoring.repository;

import com.agenticcp.core.domain.monitoring.entity.MetricThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 메트릭 임계값 데이터 접근 계층
 */
@Repository
public interface MetricThresholdRepository extends JpaRepository<MetricThreshold, Long> {
    
    /**
     * 메트릭명으로 임계값 조회
     */
    List<MetricThreshold> findByMetricName(String metricName);
    
    /**
     * 활성화된 임계값 조회
     */
    List<MetricThreshold> findByIsActiveTrue();
    
    /**
     * 메트릭명과 활성화 상태로 임계값 조회
     */
    List<MetricThreshold> findByMetricNameAndIsActiveTrue(String metricName);
    
    /**
     * 임계값 타입별 조회
     */
    List<MetricThreshold> findByThresholdType(MetricThreshold.ThresholdType thresholdType);
    
    /**
     * 알림 활성화된 임계값 조회
     */
    List<MetricThreshold> findByAlertEnabledTrue();
    
    /**
     * 메트릭명과 알림 활성화 상태로 임계값 조회
     */
    @Query("SELECT t FROM MetricThreshold t WHERE t.metricName = :metricName AND t.alertEnabled = true AND t.isActive = true")
    List<MetricThreshold> findActiveAlertThresholdsByMetricName(@Param("metricName") String metricName);
}
