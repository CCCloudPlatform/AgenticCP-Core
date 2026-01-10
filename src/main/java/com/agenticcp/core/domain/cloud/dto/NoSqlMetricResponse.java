package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * NoSQL 메트릭 응답 DTO
 *
 * NoSQL 테이블의 성능 메트릭 조회 결과를 반환합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class NoSqlMetricResponse {

    /**
     * 클라우드 프로바이더 타입
     */
    private CloudProvider.ProviderType providerType;

    /**
     * 계정 스코프
     */
    private String accountScope;

    /**
     * 테이블 이름
     */
    private String tableName;

    /**
     * 리전
     */
    private String region;

    /**
     * 조회 시작 시간
     */
    private Instant startTime;

    /**
     * 조회 종료 시간
     */
    private Instant endTime;

    /**
     * 메트릭 데이터 목록
     */
    private List<MetricData> metrics;

    /**
     * 개별 메트릭 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class MetricData {
        /**
         * 메트릭 타입
         */
        private NoSqlMetricQueryRequest.MetricType metricType;

        /**
         * 메트릭 이름 (CSP별 원본 이름)
         */
        private String metricName;

        /**
         * 단위 (예: Count, Bytes, Milliseconds)
         */
        private String unit;

        /**
         * 타임스탬프별 값 목록
         */
        private List<DataPoint> dataPoints;

        /**
         * 통계 요약
         */
        private Statistics statistics;
    }

    /**
     * 데이터 포인트
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class DataPoint {
        /**
         * 타임스탬프
         */
        private Instant timestamp;

        /**
         * 값
         */
        private Double value;

        /**
         * 단위
         */
        private String unit;
    }

    /**
     * 통계 요약
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class Statistics {
        /**
         * 합계
         */
        private Double sum;

        /**
         * 평균
         */
        private Double average;

        /**
         * 최소값
         */
        private Double minimum;

        /**
         * 최대값
         */
        private Double maximum;

        /**
         * 샘플 수
         */
        private Long sampleCount;
    }

    /**
     * Map<String, Object>에서 NoSqlMetricResponse로 변환
     */
    public static NoSqlMetricResponse from(Map<String, Object> metricsMap,
                                            CloudProvider.ProviderType providerType,
                                            String accountScope,
                                            String tableName,
                                            String region,
                                            Instant startTime,
                                            Instant endTime) {
        // 실제 변환 로직은 컨트롤러 또는 서비스에서 구현
        return NoSqlMetricResponse.builder()
                .providerType(providerType)
                .accountScope(accountScope)
                .tableName(tableName)
                .region(region)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}

