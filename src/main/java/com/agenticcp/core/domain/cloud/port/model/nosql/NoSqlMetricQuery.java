package com.agenticcp.core.domain.cloud.port.model.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * NoSQL 테이블 메트릭 조회 모델
 *
 * CloudWatch / Stackdriver / Azure Monitor 등 외부 모니터링 시스템에
 * 의존하지 않는 범위에서, 포트/어댑터 간에 공통으로 사용할 조회 조건입니다.
 */
@Value
@Builder
public class NoSqlMetricQuery {

    public enum MetricType {
        READ_CAPACITY,
        WRITE_CAPACITY,
        THROTTLED_REQUESTS,
        LATENCY,
        ERROR_COUNT
    }

    private final CloudProvider.ProviderType providerType;
    private final String accountScope;

    private final String tableName;
    private final String region;

    /**
     * 조회할 메트릭 타입 목록
     */
    private final List<MetricType> metricTypes;

    /**
     * 조회 시작/종료 시각
     */
    private final Instant startTime;
    private final Instant endTime;

    /**
     * 샘플 간격 (예: 1분, 5분)
     */
    private final Duration period;
}


