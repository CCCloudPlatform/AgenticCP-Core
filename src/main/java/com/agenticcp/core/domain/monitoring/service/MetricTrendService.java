package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 메트릭 트렌드 분석 서비스
 * 
 * @author AgenticCP Team
 * @since 2025-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricTrendService {

    private final MetricRepository metricRepository;

    /**
     * 메트릭 트렌드 조회
     * 기존 MetricRepository의 findByMetricNameAndTimeRange 메서드 활용
     */
    @Cacheable(value = "metricTrends", key = "#tenantId + '_' + #metricName + '_' + #startTime + '_' + #endTime + '_' + #interval")
    public List<Metric> getMetricsWithTrend(String tenantId, String metricName, 
                                          LocalDateTime startTime, LocalDateTime endTime, 
                                          String interval) {
        
        log.info("메트릭 트렌드 조회 - tenantId: {}, metricName: {}, interval: {}", 
                tenantId, metricName, interval);
        
        // 1. 기존 Repository 메서드로 시간 범위별 메트릭 조회
        List<Metric> metrics = metricRepository.findByMetricNameAndTimeRange(
                metricName, tenantId, startTime, endTime);
        
        // 2. 시간 간격별 집계 및 트렌드 분석
        return aggregateMetricsForTrend(metrics, interval);
    }
    
    /**
     * 시간 간격별 메트릭 집계 (트렌드 분석용)
     * 기존 Metric 엔티티의 collectedAt 필드 활용
     */
    private List<Metric> aggregateMetricsForTrend(List<Metric> metrics, String interval) {
        if (metrics.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 시간 간격에 따른 그룹핑
        Map<LocalDateTime, List<Metric>> groupedMetrics = new HashMap<>();
        
        for (Metric metric : metrics) {
            LocalDateTime groupKey = getGroupKey(metric.getCollectedAt(), interval);
            groupedMetrics.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(metric);
        }
        
        // 각 그룹별로 평균값 계산하여 새로운 Metric 객체 생성
        return groupedMetrics.entrySet().stream()
                .map(entry -> {
                    LocalDateTime timestamp = entry.getKey();
                    List<Metric> groupMetrics = entry.getValue();
                    
                    // 평균값 계산
                    double avgValue = groupMetrics.stream()
                            .mapToDouble(Metric::getMetricValue)
                            .average()
                            .orElse(0.0);
                    
                    // 첫 번째 메트릭의 메타데이터 사용하여 새로운 Metric 생성
                    Metric firstMetric = groupMetrics.get(0);
                    
                    return Metric.builder()
                            .metricName(firstMetric.getMetricName())
                            .metricValue(avgValue)
                            .unit(firstMetric.getUnit())
                            .metricType(firstMetric.getMetricType())
                            .collectedAt(timestamp)
                            .source(firstMetric.getSource())
                            .status(firstMetric.getStatus())
                            .metadata(firstMetric.getMetadata())
                            .tenantId(firstMetric.getTenantId())
                            .build();
                })
                .sorted(Comparator.comparing(Metric::getCollectedAt))
                .collect(Collectors.toList());
    }
    
    /**
     * 시간 간격에 따른 그룹 키 생성
     */
    private LocalDateTime getGroupKey(LocalDateTime timestamp, String interval) {
        switch (interval) {
            case "1m":
                return timestamp.truncatedTo(ChronoUnit.MINUTES);
            case "5m":
                return timestamp.truncatedTo(ChronoUnit.MINUTES)
                        .withMinute((timestamp.getMinute() / 5) * 5);
            case "1h":
                return timestamp.truncatedTo(ChronoUnit.HOURS);
            case "1d":
                return timestamp.truncatedTo(ChronoUnit.DAYS);
            default:
                return timestamp.truncatedTo(ChronoUnit.HOURS);
        }
    }
}
