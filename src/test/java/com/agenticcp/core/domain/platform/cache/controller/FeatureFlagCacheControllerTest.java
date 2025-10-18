package com.agenticcp.core.domain.platform.cache.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.platform.cache.dto.CacheHealthStatusDto;
import com.agenticcp.core.domain.platform.cache.dto.CacheMetricsDto;
import com.agenticcp.core.domain.platform.cache.service.FeatureFlagCacheHealthService;
import com.agenticcp.core.domain.platform.cache.service.FeatureFlagCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * FeatureFlagCacheController 단위 테스트
 * <p>
 * 캐시 관리 API 엔드포인트의 비즈니스 로직을 검증합니다.
 * 모든 Redis 관련 로직은 Mock으로 처리하여 단위 테스트로 구현합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagCacheController 단위 테스트")
class FeatureFlagCacheControllerTest {

    @Mock
    private FeatureFlagCacheService cacheService;

    @Mock
    private FeatureFlagCacheHealthService healthService;

    @InjectMocks
    private FeatureFlagCacheController cacheController;

    /**
     * 캐시 Warm-up 테스트 그룹
     * <p>
     * 테스트 시나리오:
     * 1. Warm-up 성공 - 여러 플래그 캐싱
     * 2. Warm-up 성공 - 플래그 없음 (0개)
     * </p>
     */
    @Nested
    @DisplayName("캐시 Warm-up 테스트")
    class WarmupCacheTest {

        @Test
        @DisplayName("캐시 Warm-up 성공 - 5개 플래그 캐싱")
        void warmupCache_Success_ReturnsCachedCount() {
            // 테스트 케이스: 캐시 Warm-up 성공
            // 목적: 모든 활성 플래그가 캐시에 정상적으로 로드되는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. cachedCount가 예상값과 일치하는지
            // 4. cacheService.warmupCache()가 호출되는지

            // Given
            int expectedCachedCount = 5;
            when(cacheService.warmupCache()).thenReturn(expectedCachedCount);

            // When
            ResponseEntity<ApiResponse<Map<String, Integer>>> response = cacheController.warmupCache();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().get("cachedCount")).isEqualTo(expectedCachedCount);
            assertThat(response.getBody().getMessage()).contains("Warm-up");

            verify(cacheService, times(1)).warmupCache();
        }

        @Test
        @DisplayName("캐시 Warm-up 성공 - 플래그 없음 (0개)")
        void warmupCache_WithNoFlags_ReturnsZero() {
            // 테스트 케이스: 플래그가 없는 경우 Warm-up
            // 목적: 플래그가 없어도 정상적으로 처리되는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. cachedCount가 0인지

            // Given
            when(cacheService.warmupCache()).thenReturn(0);

            // When
            ResponseEntity<ApiResponse<Map<String, Integer>>> response = cacheController.warmupCache();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().get("cachedCount")).isEqualTo(0);

            verify(cacheService, times(1)).warmupCache();
        }
    }

    /**
     * 전체 캐시 무효화 테스트 그룹
     * <p>
     * 테스트 시나리오:
     * 1. 전체 캐시 무효화 성공
     * </p>
     */
    @Nested
    @DisplayName("전체 캐시 무효화 테스트")
    class InvalidateAllCacheTest {

        @Test
        @DisplayName("전체 캐시 무효화 성공")
        void invalidateAllCache_Success() {
            // 테스트 케이스: 전체 캐시 무효화 성공
            // 목적: 모든 플래그의 캐시가 정상적으로 무효화되는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. cacheService.invalidateAllCache()가 호출되는지

            // Given
            doNothing().when(cacheService).invalidateAllCache();

            // When
            ResponseEntity<ApiResponse<Void>> response = cacheController.invalidateAllCache();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).contains("전체 캐시");

            verify(cacheService, times(1)).invalidateAllCache();
        }
    }

    /**
     * 특정 플래그 캐시 무효화 테스트 그룹
     * <p>
     * 테스트 시나리오:
     * 1. 특정 플래그 캐시 무효화 성공
     * 2. 잘못된 플래그 키 (null) - 400 Bad Request
     * 3. 잘못된 플래그 키 (빈 문자열) - 400 Bad Request
     * </p>
     */
    @Nested
    @DisplayName("특정 플래그 캐시 무효화 테스트")
    class InvalidateFlagCacheTest {

        @Test
        @DisplayName("특정 플래그 캐시 무효화 성공")
        void invalidateFlagCache_WithValidFlagKey_Success() {
            // 테스트 케이스: 특정 플래그 캐시 무효화 성공
            // 목적: 특정 플래그의 캐시가 정상적으로 무효화되는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. cacheService.invalidateCache(flagKey)가 호출되는지

            // Given
            String flagKey = "new-feature";
            doNothing().when(cacheService).invalidateCache(flagKey);

            // When
            ResponseEntity<ApiResponse<Void>> response = cacheController.invalidateFlagCache(flagKey);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).contains(flagKey);

            verify(cacheService, times(1)).invalidateCache(flagKey);
        }

        @Test
        @DisplayName("잘못된 플래그 키 (null) - 400 Bad Request")
        void invalidateFlagCache_WithNullFlagKey_ReturnsBadRequest() {
            // 테스트 케이스: null 플래그 키로 무효화 시도
            // 목적: null 키에 대한 검증이 정상적으로 작동하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 400 Bad Request인지
            // 2. 응답이 에러 상태인지
            // 3. cacheService.invalidateCache()가 호출되지 않는지

            // Given
            String flagKey = null;

            // When
            ResponseEntity<ApiResponse<Void>> response = cacheController.invalidateFlagCache(flagKey);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).contains("필수");

            verify(cacheService, never()).invalidateCache(anyString());
        }

        @Test
        @DisplayName("잘못된 플래그 키 (빈 문자열) - 400 Bad Request")
        void invalidateFlagCache_WithEmptyFlagKey_ReturnsBadRequest() {
            // 테스트 케이스: 빈 문자열 플래그 키로 무효화 시도
            // 목적: 빈 문자열 키에 대한 검증이 정상적으로 작동하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 400 Bad Request인지
            // 2. 응답이 에러 상태인지
            // 3. cacheService.invalidateCache()가 호출되지 않는지

            // Given
            String flagKey = "";

            // When
            ResponseEntity<ApiResponse<Void>> response = cacheController.invalidateFlagCache(flagKey);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();

            verify(cacheService, never()).invalidateCache(anyString());
        }

        @Test
        @DisplayName("잘못된 플래그 키 (공백만) - 400 Bad Request")
        void invalidateFlagCache_WithWhitespaceFlagKey_ReturnsBadRequest() {
            // 테스트 케이스: 공백만 있는 플래그 키로 무효화 시도
            // 목적: 공백 문자열 키에 대한 검증이 정상적으로 작동하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 400 Bad Request인지
            // 2. 응답이 에러 상태인지

            // Given
            String flagKey = "   ";

            // When
            ResponseEntity<ApiResponse<Void>> response = cacheController.invalidateFlagCache(flagKey);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();

            verify(cacheService, never()).invalidateCache(anyString());
        }
    }

    /**
     * 캐시 헬스체크 테스트 그룹
     * <p>
     * 테스트 시나리오:
     * 1. Redis 정상 상태 조회
     * 2. Fallback 모드 상태 조회 (연속 3회 실패)
     * </p>
     */
    @Nested
    @DisplayName("캐시 헬스체크 테스트")
    class CheckHealthTest {

        @Test
        @DisplayName("Redis 정상 상태 조회")
        void checkHealth_RedisHealthy_ReturnsHealthyStatus() {
            // 테스트 케이스: Redis 정상 상태 조회
            // 목적: Redis가 정상일 때 헬스체크 응답이 올바른지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. isHealthy가 true인지
            // 3. isFallbackMode가 false인지
            // 4. redisConnected가 true인지
            // 5. consecutiveFailures가 0인지

            // Given
            CacheHealthStatusDto healthyStatus = CacheHealthStatusDto.builder()
                    .isHealthy(true)
                    .isFallbackMode(false)
                    .redisConnected(true)
                    .lastCheckTime(LocalDateTime.now())
                    .consecutiveFailures(0)
                    .message("Redis 정상")
                    .build();

            when(healthService.checkHealth()).thenReturn(healthyStatus);

            // When
            ResponseEntity<ApiResponse<CacheHealthStatusDto>> response = cacheController.checkHealth();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().isHealthy()).isTrue();
            assertThat(response.getBody().getData().isFallbackMode()).isFalse();
            assertThat(response.getBody().getData().isRedisConnected()).isTrue();
            assertThat(response.getBody().getData().getConsecutiveFailures()).isEqualTo(0);

            verify(healthService, times(1)).checkHealth();
        }

        @Test
        @DisplayName("Fallback 모드 상태 조회 (연속 3회 실패)")
        void checkHealth_FallbackMode_ReturnsFallbackStatus() {
            // 테스트 케이스: Fallback 모드 상태 조회
            // 목적: Redis 장애 시 Fallback 모드로 전환되었는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. isHealthy가 false인지
            // 3. isFallbackMode가 true인지
            // 4. redisConnected가 false인지
            // 5. consecutiveFailures가 3인지

            // Given
            CacheHealthStatusDto fallbackStatus = CacheHealthStatusDto.builder()
                    .isHealthy(false)
                    .isFallbackMode(true)
                    .redisConnected(false)
                    .lastCheckTime(LocalDateTime.now())
                    .consecutiveFailures(3)
                    .message("Fallback 모드 활성화")
                    .build();

            when(healthService.checkHealth()).thenReturn(fallbackStatus);

            // When
            ResponseEntity<ApiResponse<CacheHealthStatusDto>> response = cacheController.checkHealth();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().isHealthy()).isFalse();
            assertThat(response.getBody().getData().isFallbackMode()).isTrue();
            assertThat(response.getBody().getData().isRedisConnected()).isFalse();
            assertThat(response.getBody().getData().getConsecutiveFailures()).isEqualTo(3);
            assertThat(response.getBody().getData().getMessage()).contains("Fallback");

            verify(healthService, times(1)).checkHealth();
        }
    }

    /**
     * 캐시 메트릭 조회 테스트 그룹
     * <p>
     * 테스트 시나리오:
     * 1. 메트릭 조회 성공 - 캐시 히트율 95%
     * 2. 메트릭 조회 성공 - 요청 없음 (초기 상태)
     * </p>
     */
    @Nested
    @DisplayName("캐시 메트릭 조회 테스트")
    class GetMetricsTest {

        @Test
        @DisplayName("메트릭 조회 성공 - 캐시 히트율 95%")
        void getMetrics_Success_ReturnsMetrics() {
            // 테스트 케이스: 메트릭 조회 성공
            // 목적: 캐시 성능 지표가 정상적으로 조회되는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 메트릭 데이터가 정확한지 (총 요청, 히트, 미스, 히트율)

            // Given
            CacheMetricsDto metrics = CacheMetricsDto.builder()
                    .totalRequests(1000L)
                    .cacheHits(950L)
                    .cacheMisses(50L)
                    .hitRate(95.0)
                    .avgResponseTimeMs(5.2)
                    .fallbackCount(0L)
                    .build();

            when(healthService.getMetrics()).thenReturn(metrics);

            // When
            ResponseEntity<ApiResponse<CacheMetricsDto>> response = cacheController.getMetrics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().getTotalRequests()).isEqualTo(1000L);
            assertThat(response.getBody().getData().getCacheHits()).isEqualTo(950L);
            assertThat(response.getBody().getData().getCacheMisses()).isEqualTo(50L);
            assertThat(response.getBody().getData().getHitRate()).isEqualTo(95.0);
            assertThat(response.getBody().getData().getAvgResponseTimeMs()).isEqualTo(5.2);
            assertThat(response.getBody().getData().getFallbackCount()).isEqualTo(0L);

            verify(healthService, times(1)).getMetrics();
        }

        @Test
        @DisplayName("메트릭 조회 성공 - 요청 없음 (초기 상태)")
        void getMetrics_WithNoRequests_ReturnsZeroMetrics() {
            // 테스트 케이스: 요청이 없는 초기 상태 메트릭 조회
            // 목적: 초기 상태에서도 정상적으로 메트릭이 조회되는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 모든 메트릭 값이 0인지

            // Given
            CacheMetricsDto metrics = CacheMetricsDto.builder()
                    .totalRequests(0L)
                    .cacheHits(0L)
                    .cacheMisses(0L)
                    .hitRate(0.0)
                    .avgResponseTimeMs(0.0)
                    .fallbackCount(0L)
                    .build();

            when(healthService.getMetrics()).thenReturn(metrics);

            // When
            ResponseEntity<ApiResponse<CacheMetricsDto>> response = cacheController.getMetrics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().getTotalRequests()).isEqualTo(0L);
            assertThat(response.getBody().getData().getCacheHits()).isEqualTo(0L);
            assertThat(response.getBody().getData().getCacheMisses()).isEqualTo(0L);
            assertThat(response.getBody().getData().getHitRate()).isEqualTo(0.0);

            verify(healthService, times(1)).getMetrics();
        }
    }

    /**
     * 캐시 메트릭 리셋 테스트 그룹
     * <p>
     * 테스트 시나리오:
     * 1. 메트릭 리셋 성공
     * </p>
     */
    @Nested
    @DisplayName("캐시 메트릭 리셋 테스트")
    class ResetMetricsTest {

        @Test
        @DisplayName("메트릭 리셋 성공")
        void resetMetrics_Success() {
            // 테스트 케이스: 메트릭 리셋 성공
            // 목적: 캐시 메트릭이 정상적으로 초기화되는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. healthService.resetMetrics()가 호출되는지

            // Given
            doNothing().when(healthService).resetMetrics();

            // When
            ResponseEntity<ApiResponse<Map<String, String>>> response = cacheController.resetMetrics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().get("status")).isEqualTo("success");

            verify(healthService, times(1)).resetMetrics();
        }
    }
}

