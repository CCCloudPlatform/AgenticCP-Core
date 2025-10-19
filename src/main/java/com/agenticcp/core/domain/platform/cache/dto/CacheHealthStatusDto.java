package com.agenticcp.core.domain.platform.cache.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 캐시 헬스 상태 DTO
 * 캐시 시스템의 건강 상태 및 Fallback 모드 정보
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캐시 헬스 상태")
public class CacheHealthStatusDto {

    @Schema(description = "캐시 정상 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isHealthy;

    @Schema(description = "Fallback 모드 활성화 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isFallbackMode;

    @Schema(description = "Redis 연결 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean redisConnected;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "마지막 체크 시간", example = "2025-10-17T15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime lastCheckTime;

    @Schema(description = "연속 실패 횟수", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private int consecutiveFailures;

    @Schema(description = "상태 메시지", example = "Cache is healthy")
    private String message;

    /**
     * 정상 상태 생성
     * 
     * @return 정상 상태 DTO
     */
    public static CacheHealthStatusDto healthy() {
        return CacheHealthStatusDto.builder()
                .isHealthy(true)
                .isFallbackMode(false)
                .redisConnected(true)
                .lastCheckTime(LocalDateTime.now())
                .consecutiveFailures(0)
                .message("Cache is healthy")
                .build();
    }

    /**
     * Fallback 모드 상태 생성
     * 
     * @param consecutiveFailures 연속 실패 횟수
     * @return Fallback 모드 상태 DTO
     */
    public static CacheHealthStatusDto fallback(int consecutiveFailures) {
        return CacheHealthStatusDto.builder()
                .isHealthy(false)
                .isFallbackMode(true)
                .redisConnected(false)
                .lastCheckTime(LocalDateTime.now())
                .consecutiveFailures(consecutiveFailures)
                .message(String.format("Fallback mode activated after %d consecutive failures", consecutiveFailures))
                .build();
    }

    /**
     * 복구 중 상태 생성
     * 
     * @return 복구 중 상태 DTO
     */
    public static CacheHealthStatusDto recovering() {
        return CacheHealthStatusDto.builder()
                .isHealthy(false)
                .isFallbackMode(true)
                .redisConnected(true)
                .lastCheckTime(LocalDateTime.now())
                .consecutiveFailures(0)
                .message("Cache is recovering, fallback mode will be disabled soon")
                .build();
    }

    /**
     * 에러 상태 생성
     * 
     * @param errorMessage 에러 메시지
     * @param consecutiveFailures 연속 실패 횟수
     * @return 에러 상태 DTO
     */
    public static CacheHealthStatusDto error(String errorMessage, int consecutiveFailures) {
        return CacheHealthStatusDto.builder()
                .isHealthy(false)
                .isFallbackMode(consecutiveFailures >= 3)
                .redisConnected(false)
                .lastCheckTime(LocalDateTime.now())
                .consecutiveFailures(consecutiveFailures)
                .message(errorMessage)
                .build();
    }
}

