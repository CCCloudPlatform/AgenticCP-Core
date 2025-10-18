package com.agenticcp.core.domain.platform.cache.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.platform.cache.dto.CacheHealthStatusDto;
import com.agenticcp.core.domain.platform.cache.dto.CacheMetricsDto;
import com.agenticcp.core.domain.platform.cache.service.FeatureFlagCacheHealthService;
import com.agenticcp.core.domain.platform.cache.service.FeatureFlagCacheService;
import com.agenticcp.core.domain.platform.exception.FeatureFlagCacheErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 기능 플래그 캐시 관리 컨트롤러
 * <p>
 * `app.redis.enabled` 프로퍼티가 `true`일 때만 활성화됩니다.
 * 캐시 Warm-up, 무효화, 헬스체크 등의 관리 API를 제공합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/feature-flags/cache")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
@Tag(name = "Feature Flag Cache", description = "기능 플래그 캐시 관리 API")
public class FeatureFlagCacheController {

    private final FeatureFlagCacheService cacheService;
    private final FeatureFlagCacheHealthService healthService;

    /**
     * 캐시 Warm-up
     * <p>
     * 모든 활성 플래그를 DB에서 조회하여 캐시에 적재합니다.
     * 애플리케이션 시작 시 또는 캐시 전체 무효화 후 수동으로 호출할 수 있습니다.
     * </p>
     *
     * @return 캐싱된 플래그 수
     */
    @PostMapping("/warmup")
    @Operation(
            summary = "캐시 Warm-up",
            description = "모든 활성 플래그를 캐시에 적재합니다. 애플리케이션 시작 시 또는 캐시 무효화 후 수동 호출 가능합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Integer>>> warmupCache() {
        log.info("[FeatureFlagCacheController] POST /cache/warmup - Warm-up requested");

        int cachedCount = cacheService.warmupCache();

        log.info("[FeatureFlagCacheController] POST /cache/warmup - success, cachedCount={}", cachedCount);
        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of("cachedCount", cachedCount),
                        "캐시 Warm-up이 완료되었습니다."
                )
        );
    }

    /**
     * 전체 캐시 무효화
     * <p>
     * 모든 플래그의 캐시를 무효화합니다.
     * 대량 플래그 업데이트 후 또는 캐시 리셋이 필요한 경우 사용합니다.
     * </p>
     *
     * @return 성공 메시지
     */
    @DeleteMapping
    @Operation(
            summary = "전체 캐시 무효화",
            description = "모든 플래그의 캐시를 무효화합니다. 대량 업데이트 후 또는 캐시 리셋 시 사용합니다."
    )
    public ResponseEntity<ApiResponse<Void>> invalidateAllCache() {
        log.info("[FeatureFlagCacheController] DELETE /cache - Invalidate all cache requested");

        cacheService.invalidateAllCache();

        log.info("[FeatureFlagCacheController] DELETE /cache - success");
        return ResponseEntity.ok(
                ApiResponse.success(null, "전체 캐시가 무효화되었습니다.")
        );
    }

    /**
     * 특정 플래그 캐시 무효화
     * <p>
     * 지정된 flagKey에 해당하는 플래그의 캐시만 무효화합니다.
     * 단일 플래그 업데이트 시 사용합니다.
     * </p>
     *
     * @param flagKey 무효화할 플래그 키
     * @return 성공 메시지
     */
    @DeleteMapping("/{flagKey}")
    @Operation(
            summary = "특정 플래그 캐시 무효화",
            description = "지정된 flagKey에 해당하는 플래그의 캐시를 무효화합니다."
    )
    public ResponseEntity<ApiResponse<Void>> invalidateFlagCache(
            @Parameter(description = "플래그 키", example = "new-feature", required = true)
            @PathVariable String flagKey) {

        // flagKey 검증
        if (flagKey == null || flagKey.trim().isEmpty()) {
            log.warn("[FeatureFlagCacheController] DELETE /cache/{flagKey} - Invalid flagKey: {}", flagKey);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(FeatureFlagCacheErrorCode.INVALID_CACHE_KEY, "플래그 키는 필수입니다."));
        }

        log.info("[FeatureFlagCacheController] DELETE /cache/{} - Invalidate cache requested", flagKey);

        cacheService.invalidateCache(flagKey);

        log.info("[FeatureFlagCacheController] DELETE /cache/{} - success", flagKey);
        return ResponseEntity.ok(
                ApiResponse.success(null, String.format("플래그 '%s'의 캐시가 무효화되었습니다.", flagKey))
        );
    }

    /**
     * 캐시 헬스체크
     * <p>
     * Redis 캐시의 건강 상태를 확인합니다.
     * 연속 3회 실패 시 Fallback 모드로 전환됩니다.
     * </p>
     *
     * @return 캐시 헬스 상태
     */
    @GetMapping("/health")
    @Operation(
            summary = "캐시 헬스체크",
            description = "Redis 캐시의 건강 상태를 확인합니다. 연속 3회 실패 시 Fallback 모드로 전환됩니다."
    )
    public ResponseEntity<ApiResponse<CacheHealthStatusDto>> checkHealth() {
        log.debug("[FeatureFlagCacheController] GET /cache/health - Health check requested");

        CacheHealthStatusDto status = healthService.checkHealth();

        log.debug("[FeatureFlagCacheController] GET /cache/health - healthy={}, fallback={}",
                status.isHealthy(), status.isFallbackMode());
        return ResponseEntity.ok(ApiResponse.success(status, "캐시 헬스체크 완료"));
    }

    /**
     * 캐시 메트릭 조회
     * <p>
     * 캐시 히트율, 응답 시간 등의 성능 지표를 조회합니다.
     * </p>
     *
     * @return 캐시 성능 메트릭
     */
    @GetMapping("/metrics")
    @Operation(
            summary = "캐시 메트릭 조회",
            description = "캐시 히트율, 응답 시간, Fallback 횟수 등의 성능 지표를 조회합니다."
    )
    public ResponseEntity<ApiResponse<CacheMetricsDto>> getMetrics() {
        log.debug("[FeatureFlagCacheController] GET /cache/metrics - Metrics requested");

        CacheMetricsDto metrics = healthService.getMetrics();

        log.debug("[FeatureFlagCacheController] GET /cache/metrics - totalRequests={}, hitRate={}%",
                metrics.getTotalRequests(), metrics.getHitRate());
        return ResponseEntity.ok(ApiResponse.success(metrics, "캐시 메트릭 조회 완료"));
    }

    /**
     * 캐시 메트릭 리셋
     * <p>
     * 캐시 성능 메트릭을 초기화합니다.
     * 통계 초기화가 필요한 경우 사용합니다.
     * </p>
     *
     * @return 리셋 결과
     */
    @DeleteMapping("/metrics")
    @Operation(
            summary = "캐시 메트릭 리셋",
            description = "캐시 성능 메트릭을 초기화합니다. 통계 초기화가 필요한 경우 사용합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> resetMetrics() {
        log.info("[FeatureFlagCacheController] DELETE /cache/metrics - Reset metrics requested");

        healthService.resetMetrics();

        Map<String, String> result = Map.of(
                "status", "success",
                "message", "Cache metrics reset successfully"
        );

        log.info("[FeatureFlagCacheController] DELETE /cache/metrics - success");
        return ResponseEntity.ok(ApiResponse.success(result, "메트릭 리셋 성공"));
    }
}

