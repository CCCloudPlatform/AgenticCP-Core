package com.agenticcp.core.domain.monitoring.dto;

import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * 테넌트별 수집기 설정 DTO
 */
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class TenantCollectorConfigDto {

    /**
     * 테넌트 ID
     */
    private final String tenantId;

    /**
     * 수집기 타입
     */
    private final CollectorType collectorType;

    /**
     * 활성화 여부
     */
    private final Boolean isEnabled;

    /**
     * 수집 주기 (밀리초)
     */
    private final Long collectionInterval;

    /**
     * 재시도 횟수
     */
    private final Integer retryCount;

    /**
     * 타임아웃 (밀리초)
     */
    private final Long timeout;

    /**
     * 수집할 메트릭 목록
     */
    private final List<String> targetMetrics;

    /**
     * 수집기별 추가 설정
     */
    private final Map<String, Object> collectorSettings;

    /**
     * 우선순위
     */
    private final Integer priority;

    /**
     * 메타데이터
     */
    private final Map<String, String> metadata;
}
