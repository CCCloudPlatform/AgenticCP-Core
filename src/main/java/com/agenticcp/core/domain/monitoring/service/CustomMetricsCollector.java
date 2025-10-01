package com.agenticcp.core.domain.monitoring.service;

import java.util.HashMap;
import java.util.Map;

/**
 * 커스텀 메트릭 수집기 인터페이스
 * 
 * <p>사용자 정의 메트릭 수집기를 쉽게 구현할 수 있도록 지원합니다.
 * 
 * <p>사용 예시:
 * <pre>{@code
 * @Component
 * public class BusinessMetricsCollector implements CustomMetricsCollector {
 *     
 *     @Override
 *     public String getCollectorName() {
 *         return "비즈니스 메트릭 수집기";
 *     }
 *     
 *     @Override
 *     public List<Metric> collectApplicationMetrics() {
 *         // 주문 수, 활성 사용자 수 등 비즈니스 메트릭 수집
 *         return businessMetrics;
 *     }
 * }
 * }</pre>
 * 
 * <p>자동 등록:
 * <ul>
 *   <li>@Component 선언 시 MetricsCollectorRegistry에 자동 등록</li>
 *   <li>별도 설정 불필요</li>
 * </ul>
 * 
 * <p>Issue #39: Task 6 - 메트릭 수집기 플러그인 시스템 구현
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface CustomMetricsCollector extends MetricsCollector {
    
    /**
     * 수집기 고유 이름
     * 
     * <p>수집기를 식별하기 위한 고유한 이름을 반환합니다.
     * 
     * @return 수집기 이름 (예: "business-metrics", "http-metrics")
     */
    String getCollectorName();
    
    /**
     * 수집기 설명
     * 
     * <p>수집기가 어떤 메트릭을 수집하는지 설명합니다.
     * 
     * @return 수집기 설명
     */
    default String getCollectorDescription() {
        return "커스텀 메트릭 수집기";
    }
    
    /**
     * 수집기 설정 정보
     * 
     * <p>수집기별 고유 설정을 반환합니다.
     * 예: 수집 간격, 대상 리소스, API 엔드포인트 등
     * 
     * @return 설정 맵
     */
    default Map<String, Object> getCollectorConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("name", getCollectorName());
        config.put("description", getCollectorDescription());
        config.put("type", getCollectorType().name());
        config.put("enabled", isEnabled());
        return config;
    }
    
    /**
     * 수집기 초기화
     * 
     * <p>수집기 시작 시 필요한 초기화 작업을 수행합니다.
     * 예: 연결 설정, 리소스 로드 등
     * 
     * @param config 초기화 설정
     */
    default void initialize(Map<String, Object> config) {
        // 기본 구현: 아무 작업도 하지 않음
        // 필요한 경우 override하여 사용
    }
    
    /**
     * 수집기 종료
     * 
     * <p>수집기 종료 시 필요한 정리 작업을 수행합니다.
     * 예: 연결 해제, 리소스 해제 등
     */
    default void shutdown() {
        // 기본 구현: 아무 작업도 하지 않음
        // 필요한 경우 override하여 사용
    }
    
    /**
     * 수집기 상태 확인
     * 
     * <p>수집기가 정상적으로 동작하는지 확인합니다.
     * 
     * @return 정상 동작 여부
     */
    default boolean isHealthy() {
        return isEnabled();
    }
}

