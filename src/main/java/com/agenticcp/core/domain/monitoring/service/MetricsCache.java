package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 메트릭 캐시 서비스
 * 
 * <p>메트릭 수집 실패 시 폴백(Fallback)을 위해 마지막 성공한 메트릭을 캐시합니다.
 * 
 * <p>주요 기능:
 * <ul>
 *   <li>시스템 메트릭 캐싱</li>
 *   <li>애플리케이션 메트릭 캐싱</li>
 *   <li>캐시 만료 시간 관리</li>
 *   <li>폴백 데이터 제공</li>
 * </ul>
 * 
 * <p>Issue #39: Task 8 - 재시도 로직 및 오류 처리 구현
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class MetricsCache {
    
    /**
     * 캐시 만료 시간 (분)
     * 5분 이상 된 캐시는 사용하지 않음
     */
    private static final long CACHE_EXPIRATION_MINUTES = 5L;
    
    /**
     * 마지막 성공한 시스템 메트릭
     */
    private volatile SystemMetrics lastSuccessfulSystemMetrics;
    
    /**
     * 마지막 성공한 시스템 메트릭 수집 시간
     */
    private volatile LocalDateTime lastSystemMetricsTimestamp;
    
    /**
     * 마지막 성공한 애플리케이션 메트릭
     */
    private final ConcurrentMap<String, Metric> lastSuccessfulApplicationMetrics;
    
    /**
     * 마지막 성공한 애플리케이션 메트릭 수집 시간
     */
    private volatile LocalDateTime lastApplicationMetricsTimestamp;
    
    /**
     * MetricsCache 생성자
     */
    public MetricsCache() {
        this.lastSuccessfulApplicationMetrics = new ConcurrentHashMap<>();
        log.info("MetricsCache가 초기화되었습니다. 캐시 만료 시간: {}분", CACHE_EXPIRATION_MINUTES);
    }
    
    /**
     * 시스템 메트릭을 캐시에 저장
     * 
     * @param systemMetrics 저장할 시스템 메트릭
     */
    public void cacheSystemMetrics(SystemMetrics systemMetrics) {
        if (systemMetrics == null) {
            log.warn("캐시에 저장할 시스템 메트릭이 null입니다.");
            return;
        }
        
        this.lastSuccessfulSystemMetrics = systemMetrics;
        this.lastSystemMetricsTimestamp = LocalDateTime.now();
        
        log.debug("시스템 메트릭이 캐시에 저장되었습니다. timestamp={}", lastSystemMetricsTimestamp);
    }
    
    /**
     * 애플리케이션 메트릭을 캐시에 저장
     * 
     * @param metrics 저장할 애플리케이션 메트릭 목록
     */
    public void cacheApplicationMetrics(List<Metric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            log.warn("캐시에 저장할 애플리케이션 메트릭이 비어있습니다.");
            return;
        }
        
        for (Metric metric : metrics) {
            if (metric != null && metric.getMetricName() != null) {
                lastSuccessfulApplicationMetrics.put(metric.getMetricName(), metric);
            }
        }
        
        this.lastApplicationMetricsTimestamp = LocalDateTime.now();
        
        log.debug("애플리케이션 메트릭 {}개가 캐시에 저장되었습니다. timestamp={}", 
                metrics.size(), lastApplicationMetricsTimestamp);
    }
    
    /**
     * 캐시된 시스템 메트릭 조회
     * 
     * @return 캐시된 시스템 메트릭 (만료되었거나 없으면 null)
     */
    public SystemMetrics getLastSuccessfulSystemMetrics() {
        if (lastSuccessfulSystemMetrics == null) {
            log.debug("캐시된 시스템 메트릭이 없습니다.");
            return null;
        }
        
        if (isCacheExpired(lastSystemMetricsTimestamp)) {
            log.warn("캐시된 시스템 메트릭이 만료되었습니다. timestamp={}", lastSystemMetricsTimestamp);
            return null;
        }
        
        log.debug("캐시된 시스템 메트릭을 반환합니다. timestamp={}", lastSystemMetricsTimestamp);
        return lastSuccessfulSystemMetrics;
    }
    
    /**
     * 캐시된 애플리케이션 메트릭 조회
     * 
     * @return 캐시된 애플리케이션 메트릭 목록 (만료되었거나 없으면 빈 리스트)
     */
    public List<Metric> getLastSuccessfulApplicationMetrics() {
        if (lastSuccessfulApplicationMetrics.isEmpty()) {
            log.debug("캐시된 애플리케이션 메트릭이 없습니다.");
            return new ArrayList<>();
        }
        
        if (isCacheExpired(lastApplicationMetricsTimestamp)) {
            log.warn("캐시된 애플리케이션 메트릭이 만료되었습니다. timestamp={}", lastApplicationMetricsTimestamp);
            return new ArrayList<>();
        }
        
        log.debug("캐시된 애플리케이션 메트릭 {}개를 반환합니다. timestamp={}", 
                lastSuccessfulApplicationMetrics.size(), lastApplicationMetricsTimestamp);
        return new ArrayList<>(lastSuccessfulApplicationMetrics.values());
    }
    
    /**
     * 캐시 만료 여부 확인
     * 
     * @param timestamp 캐시 저장 시간
     * @return 만료되었으면 true, 아니면 false
     */
    private boolean isCacheExpired(LocalDateTime timestamp) {
        if (timestamp == null) {
            return true;
        }
        
        LocalDateTime expirationTime = timestamp.plusMinutes(CACHE_EXPIRATION_MINUTES);
        boolean expired = LocalDateTime.now().isAfter(expirationTime);
        
        if (expired) {
            log.debug("캐시가 만료되었습니다. timestamp={}, expirationTime={}", timestamp, expirationTime);
        }
        
        return expired;
    }
    
    /**
     * 시스템 메트릭 캐시 초기화
     */
    public void clearSystemMetricsCache() {
        this.lastSuccessfulSystemMetrics = null;
        this.lastSystemMetricsTimestamp = null;
        log.info("시스템 메트릭 캐시가 초기화되었습니다.");
    }
    
    /**
     * 애플리케이션 메트릭 캐시 초기화
     */
    public void clearApplicationMetricsCache() {
        this.lastSuccessfulApplicationMetrics.clear();
        this.lastApplicationMetricsTimestamp = null;
        log.info("애플리케이션 메트릭 캐시가 초기화되었습니다.");
    }
    
    /**
     * 모든 캐시 초기화
     */
    public void clearAll() {
        clearSystemMetricsCache();
        clearApplicationMetricsCache();
        log.info("모든 메트릭 캐시가 초기화되었습니다.");
    }
    
    /**
     * 캐시 상태 확인
     * 
     * @return 캐시에 유효한 데이터가 있으면 true
     */
    public boolean hasCachedData() {
        boolean hasSystemMetrics = lastSuccessfulSystemMetrics != null && !isCacheExpired(lastSystemMetricsTimestamp);
        boolean hasApplicationMetrics = !lastSuccessfulApplicationMetrics.isEmpty() && !isCacheExpired(lastApplicationMetricsTimestamp);
        
        return hasSystemMetrics || hasApplicationMetrics;
    }
}

