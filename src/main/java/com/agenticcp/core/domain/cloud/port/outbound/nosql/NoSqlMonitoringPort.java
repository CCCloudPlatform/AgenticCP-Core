package com.agenticcp.core.domain.cloud.port.outbound.nosql;

import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlMetricQuery;

import java.util.Map;

/**
 * NoSQL 모니터링 포트
 *
 * 테이블/인덱스 수준의 메트릭(용량, 스로틀링, 지연시간 등)을 조회합니다.
 * 구현체는 CloudWatch, Stackdriver, Azure Monitor 등 외부 시스템을 사용할 수 있습니다.
 */
public interface NoSqlMonitoringPort {

    /**
     * 메트릭을 조회합니다.
     *
     * @param query 메트릭 조회 조건
     * @return 메트릭 결과 (metricName -> 값 집합 / 시계열 등)
     */
    Map<String, Object> queryMetrics(NoSqlMetricQuery query);
}


