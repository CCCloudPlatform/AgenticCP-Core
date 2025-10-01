package com.agenticcp.core.domain.monitoring.enums;

/**
 * 메트릭 수집기 타입 열거형
 * 
 * <p>다양한 메트릭 수집기를 구분하기 위한 타입 정의</p>
 * 
 * <p>Issue #39: Task 6 - 메트릭 수집기 플러그인 시스템 구현
 * 
 * @author AgenticCP Team
 * @version 1.1.0
 * @since 2024-01-01
 */
public enum CollectorType {
    
    /**
     * 시스템 리소스 메트릭 수집기
     * CPU, 메모리, 디스크 등 시스템 리소스 정보 수집
     */
    SYSTEM("시스템 리소스 수집기"),
    
    /**
     * 애플리케이션 메트릭 수집기 (Micrometer 기반)
     * JVM, 커스텀 메트릭 등 애플리케이션 관련 정보 수집
     * Micrometer를 활용한 고급 메트릭 수집 및 모니터링
     */
    APPLICATION("Micrometer 기반 애플리케이션 수집기"),
    
    /**
     * 커스텀 메트릭 수집기
     * 사용자 정의 메트릭 수집기
     * 비즈니스 로직, DB, HTTP 등 다양한 소스에서 메트릭 수집 가능
     * 
     * <p>Issue #39: Task 6 - 플러그인 방식 커스텀 수집기 지원
     */
    CUSTOM("커스텀 메트릭 수집기"),
    
    /**
     * 외부 시스템 메트릭 수집기
     * Prometheus, CloudWatch 등 외부 모니터링 시스템에서 메트릭 수집
     * 
     * <p>하위 타입:
     * <ul>
     *   <li>PrometheusCollector: 외부 Prometheus 서버 조회</li>
     *   <li>CloudWatchCollector: AWS CloudWatch 조회 (TODO: 테넌트 연동 후)</li>
     *   <li>GcpMonitoringCollector: GCP Monitoring 조회 (추후)</li>
     * </ul>
     * 
     * <p>Issue #39: Task 6 - 외부 메트릭 수집기 지원
     */
    EXTERNAL("외부 시스템 메트릭 수집기");
    
    private final String description;
    
    CollectorType(String description) {
        this.description = description;
    }
    
    /**
     * 수집기 타입 설명 조회
     * 
     * @return 수집기 타입 설명
     */
    public String getDescription() {
        return description;
    }
}
