package com.agenticcp.core.domain.platform.cache.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 캐시 메트릭 DTO
 * 캐시 시스템의 성능 지표 및 통계 정보
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캐시 메트릭")
public class CacheMetricsDto {

    @Schema(description = "전체 요청 수", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
    private long totalRequests;

    @Schema(description = "캐시 히트 수", example = "950", requiredMode = Schema.RequiredMode.REQUIRED)
    private long cacheHits;

    @Schema(description = "캐시 미스 수", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private long cacheMisses;

    @Schema(description = "캐시 히트율 (%)", example = "95.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private double hitRate;

    @Schema(description = "평균 응답 시간 (ms)", example = "8.5")
    private double avgResponseTimeMs;

    @Schema(description = "Fallback 모드 전환 횟수", example = "2")
    private long fallbackCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "메트릭 수집 시작 시간", example = "2025-10-17T00:00:00")
    private LocalDateTime metricsStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "마지막 업데이트 시간", example = "2025-10-17T15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime lastUpdatedTime;

    /**
     * 캐시 히트율 계산
     * 
     * @return 히트율 (0.0 ~ 100.0)
     */
    public double calculateHitRate() {
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) cacheHits / totalRequests * 100.0;
    }

    /**
     * 새로운 메트릭 객체 생성 (초기화)
     * 
     * @return 초기화된 메트릭 DTO
     */
    public static CacheMetricsDto initialize() {
        LocalDateTime now = LocalDateTime.now();
        return CacheMetricsDto.builder()
                .totalRequests(0)
                .cacheHits(0)
                .cacheMisses(0)
                .hitRate(0.0)
                .avgResponseTimeMs(0.0)
                .fallbackCount(0)
                .metricsStartTime(now)
                .lastUpdatedTime(now)
                .build();
    }

    /**
     * 현재 메트릭 스냅샷 생성
     * 
     * @param totalRequests 전체 요청 수
     * @param cacheHits 캐시 히트 수
     * @param cacheMisses 캐시 미스 수
     * @param avgResponseTimeMs 평균 응답 시간
     * @param fallbackCount Fallback 횟수
     * @param metricsStartTime 메트릭 시작 시간
     * @return 메트릭 스냅샷 DTO
     */
    public static CacheMetricsDto snapshot(long totalRequests, long cacheHits, long cacheMisses,
                                       double avgResponseTimeMs, long fallbackCount,
                                       LocalDateTime metricsStartTime) {
        double hitRate = totalRequests > 0 ? (double) cacheHits / totalRequests * 100.0 : 0.0;
        
        return CacheMetricsDto.builder()
                .totalRequests(totalRequests)
                .cacheHits(cacheHits)
                .cacheMisses(cacheMisses)
                .hitRate(Math.round(hitRate * 100.0) / 100.0) // 소수점 둘째자리까지
                .avgResponseTimeMs(Math.round(avgResponseTimeMs * 100.0) / 100.0)
                .fallbackCount(fallbackCount)
                .metricsStartTime(metricsStartTime)
                .lastUpdatedTime(LocalDateTime.now())
                .build();
    }
}

