package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 메트릭 수집기
 * 
 * <p>API 요청/응답 메트릭을 수집합니다.
 * 
 * <p>수집 메트릭:
 * <ul>
 *   <li>API 응답 시간 (평균, 최대, 백분위)</li>
 *   <li>API 호출 횟수 (처리량)</li>
 *   <li>API 에러율 (4xx, 5xx)</li>
 *   <li>엔드포인트별 통계</li>
 * </ul>
 * 
 * <p>Micrometer의 http.server.requests 메트릭을 사용하여
 * Spring Boot Actuator가 자동 수집한 HTTP 메트릭을 가공합니다.
 * 
 * <p>Issue #39: 애플리케이션 메트릭 수집 (응답시간, 처리량, 에러율)
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class HttpMetricsCollector implements CustomMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    @Value("${metrics.collector.http.enabled:true}")
    private boolean enabled;
    
    /**
     * HttpMetricsCollector 생성자
     * 
     * @param meterRegistry Micrometer 메트릭 레지스트리
     */
    public HttpMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("HTTP 메트릭 수집기가 초기화되었습니다.");
    }
    
    @Override
    public String getCollectorName() {
        return "http-metrics";
    }
    
    @Override
    public String getCollectorDescription() {
        return "HTTP API 응답시간, 처리량, 에러율 수집기";
    }
    
    @Override
    public CollectorType getCollectorType() {
        return CollectorType.CUSTOM;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public SystemMetrics collectSystemMetrics() {
        // HTTP 수집기는 시스템 메트릭을 수집하지 않음
        return null;
    }
    
    @Override
    public List<Metric> collectApplicationMetrics() {
        if (!enabled) {
            log.debug("HTTP 메트릭 수집기가 비활성화되어 있습니다.");
            return new ArrayList<>();
        }
        
        try {
            log.debug("HTTP 메트릭 수집 시작...");
            
            List<Metric> metrics = new ArrayList<>();
            LocalDateTime collectedAt = LocalDateTime.now();
            
            // 1. API 총 호출 횟수 (처리량)
            collectRequestCountMetrics(metrics, collectedAt);
            
            // 2. API 평균 응답 시간
            collectResponseTimeMetrics(metrics, collectedAt);
            
            // 3. API 에러율
            collectErrorRateMetrics(metrics, collectedAt);
            
            // 4. 상태 코드별 통계
            collectStatusCodeMetrics(metrics, collectedAt);
            
            log.info("HTTP 메트릭 수집 완료: {}개 메트릭", metrics.size());
            return metrics;
            
        } catch (Exception e) {
            log.error("HTTP 메트릭 수집 실패", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * API 호출 횟수 수집 (처리량)
     * 
     * <p>Spring Boot Actuator의 http.server.requests 메트릭 사용
     * 
     * @param metrics 메트릭 목록
     * @param collectedAt 수집 시간
     */
    private void collectRequestCountMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            // 전체 요청 수
            Counter requestCounter = meterRegistry.find("http.server.requests").counter();
            
            if (requestCounter != null) {
                double totalRequests = requestCounter.count();
                metrics.add(createMetric(
                    "http.requests.total",
                    totalRequests,
                    "count",
                    collectedAt
                ));
                
                log.debug("총 요청 수: {}", totalRequests);
            }
            
        } catch (Exception e) {
            log.warn("요청 횟수 메트릭 수집 실패", e);
        }
    }
    
    /**
     * API 응답 시간 수집
     * 
     * @param metrics 메트릭 목록
     * @param collectedAt 수집 시간
     */
    private void collectResponseTimeMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            // http.server.requests Timer 조회
            Timer requestTimer = meterRegistry.find("http.server.requests").timer();
            
            if (requestTimer != null && requestTimer.count() > 0) {
                // 평균 응답 시간 (밀리초)
                double avgResponseTime = requestTimer.mean(TimeUnit.MILLISECONDS);
                metrics.add(createMetric(
                    "http.response.time.avg",
                    avgResponseTime,
                    "ms",
                    collectedAt
                ));
                
                // 최대 응답 시간 (밀리초)
                double maxResponseTime = requestTimer.max(TimeUnit.MILLISECONDS);
                metrics.add(createMetric(
                    "http.response.time.max",
                    maxResponseTime,
                    "ms",
                    collectedAt
                ));
                
                // 총 응답 시간 (초)
                double totalResponseTime = requestTimer.totalTime(TimeUnit.SECONDS);
                metrics.add(createMetric(
                    "http.response.time.total",
                    totalResponseTime,
                    "seconds",
                    collectedAt
                ));
                
                log.debug("평균 응답 시간: {}ms, 최대: {}ms", avgResponseTime, maxResponseTime);
            }
            
        } catch (Exception e) {
            log.warn("응답 시간 메트릭 수집 실패", e);
        }
    }
    
    /**
     * API 에러율 수집
     * 
     * @param metrics 메트릭 목록
     * @param collectedAt 수집 시간
     */
    private void collectErrorRateMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            // 4xx 에러 카운트
            Counter clientErrors = meterRegistry.find("http.server.requests")
                    .tag("status", "4xx")
                    .counter();
            
            if (clientErrors != null) {
                double clientErrorCount = clientErrors.count();
                metrics.add(createMetric(
                    "http.errors.4xx",
                    clientErrorCount,
                    "count",
                    collectedAt
                ));
            }
            
            // 5xx 에러 카운트
            Counter serverErrors = meterRegistry.find("http.server.requests")
                    .tag("status", "5xx")
                    .counter();
            
            if (serverErrors != null) {
                double serverErrorCount = serverErrors.count();
                metrics.add(createMetric(
                    "http.errors.5xx",
                    serverErrorCount,
                    "count",
                    collectedAt
                ));
            }
            
            // 에러율 계산
            Counter totalRequests = meterRegistry.find("http.server.requests").counter();
            if (totalRequests != null && totalRequests.count() > 0) {
                double total = totalRequests.count();
                double errors = (clientErrors != null ? clientErrors.count() : 0) + 
                               (serverErrors != null ? serverErrors.count() : 0);
                double errorRate = (errors / total) * 100.0;
                
                metrics.add(createMetric(
                    "http.error.rate",
                    errorRate,
                    "%",
                    collectedAt
                ));
                
                log.debug("에러율: {:.2f}%", errorRate);
            }
            
        } catch (Exception e) {
            log.warn("에러율 메트릭 수집 실패", e);
        }
    }
    
    /**
     * 상태 코드별 통계 수집
     * 
     * @param metrics 메트릭 목록
     * @param collectedAt 수집 시간
     */
    private void collectStatusCodeMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            // 2xx 성공
            Counter success = meterRegistry.find("http.server.requests")
                    .tag("status", "2xx")
                    .counter();
            if (success != null) {
                metrics.add(createMetric("http.status.2xx", success.count(), "count", collectedAt));
            }
            
            // 3xx 리다이렉트
            Counter redirect = meterRegistry.find("http.server.requests")
                    .tag("status", "3xx")
                    .counter();
            if (redirect != null) {
                metrics.add(createMetric("http.status.3xx", redirect.count(), "count", collectedAt));
            }
            
        } catch (Exception e) {
            log.warn("상태 코드 메트릭 수집 실패", e);
        }
    }
    
    /**
     * Metric 엔티티 생성 헬퍼 메서드
     * 
     * @param name 메트릭 이름
     * @param value 메트릭 값
     * @param unit 단위
     * @param collectedAt 수집 시간
     * @return 생성된 Metric 엔티티
     */
    private Metric createMetric(String name, Double value, String unit, LocalDateTime collectedAt) {
        return Metric.builder()
                .metricName(name)
                .metricValue(value)
                .unit(unit)
                .metricType(Metric.MetricType.APPLICATION)
                .collectedAt(collectedAt)
                .source("http")
                .build();
    }
}

