package com.agenticcp.core.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 임시 메트릭 DTO (모니터링 도메인 머지 전까지 사용)
 * 
 * <p>feature/39 브랜치의 Metric 엔티티와 동일한 구조를 가진 DTO입니다.</p>
 * <p>모니터링 도메인이 머지되면 실제 Metric 엔티티로 교체됩니다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDto {

    /**
     * 메트릭 ID (Primary Key)
     */
    private Long id;

    /**
     * 메트릭 이름 (예: cpu.usage, memory.used, disk.free)
     */
    private String metricName;

    /**
     * 메트릭 값
     */
    private Double metricValue;

    /**
     * 메트릭 단위 (예: %, MB, GB, ms)
     */
    private String unit;

    /**
     * 메트릭 타입 (SYSTEM, APPLICATION)
     */
    private MetricType metricType;

    /**
     * 메트릭 수집 시간
     */
    private LocalDateTime collectedAt;

    /**
     * 메트릭 소스 (예: system, application, custom)
     */
    private String source;

    /**
     * 메트릭 상태 (ACTIVE, INACTIVE, ARCHIVED)
     */
    private Status status;

    /**
     * 추가 메타데이터 (JSON 형태)
     */
    private String metadata;

    /**
     * 테넌트 ID
     */
    private String tenantId;

    /**
     * 메트릭 태그 (JSON 형태)
     */
    private String tags;

    /**
     * 메트릭 타입 열거형
     */
    public enum MetricType {
        SYSTEM,        // 시스템 리소스 메트릭 (CPU, 메모리, 디스크)
        APPLICATION    // 애플리케이션 메트릭 (응답시간, 처리량, 에러율)
    }

    /**
     * 메트릭 상태 열거형
     */
    public enum Status {
        ACTIVE,        // 활성
        INACTIVE,      // 비활성
        ARCHIVED       // 아카이브
    }
}
