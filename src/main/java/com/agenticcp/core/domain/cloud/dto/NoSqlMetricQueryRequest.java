package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * NoSQL 메트릭 조회 요청 DTO
 *
 * NoSQL 테이블의 성능 메트릭 조회 요청을 정의합니다.
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
public class NoSqlMetricQueryRequest {

    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private CloudProvider.ProviderType providerType;

    /**
     * 계정 스코프 (Account ID 등)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String accountScope;

    /**
     * 대상 테이블 이름
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String tableName;

    /**
     * 리전
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;

    /**
     * 조회할 메트릭 타입 목록
     */
    @NotEmpty(message = "조회할 메트릭 타입은 필수입니다")
    private List<MetricType> metricTypes;

    /**
     * 조회 시작 시간
     */
    @NotNull(message = "조회 시작 시간은 필수입니다")
    private Instant startTime;

    /**
     * 조회 종료 시간
     */
    @NotNull(message = "조회 종료 시간은 필수입니다")
    private Instant endTime;

    /**
     * 샘플 간격 (초 단위)
     * 기본값: 300초 (5분)
     */
    @Builder.Default
    private Integer periodSeconds = 300;

    /**
     * 통계 유형
     * 기본값: AVERAGE
     */
    @Builder.Default
    private StatisticType statisticType = StatisticType.AVERAGE;

    /**
     * 메트릭 타입
     */
    public enum MetricType {
        /**
         * 소비된 읽기 용량 단위
         */
        READ_CAPACITY,
        
        /**
         * 소비된 쓰기 용량 단위
         */
        WRITE_CAPACITY,
        
        /**
         * 스로틀된 요청 수
         */
        THROTTLED_REQUESTS,
        
        /**
         * 요청 지연 시간
         */
        LATENCY,
        
        /**
         * 시스템 오류 수
         */
        ERROR_COUNT,
        
        /**
         * 성공적인 요청 수
         */
        SUCCESSFUL_REQUESTS,
        
        /**
         * 반환된 아이템 수
         */
        RETURNED_ITEM_COUNT,
        
        /**
         * 조건부 검사 실패 수
         */
        CONDITIONAL_CHECK_FAILED
    }

    /**
     * 통계 유형
     */
    public enum StatisticType {
        SUM,
        AVERAGE,
        MINIMUM,
        MAXIMUM,
        SAMPLE_COUNT
    }

    /**
     * periodSeconds를 Duration으로 변환
     */
    public Duration getPeriod() {
        return Duration.ofSeconds(periodSeconds != null ? periodSeconds : 300);
    }
}

