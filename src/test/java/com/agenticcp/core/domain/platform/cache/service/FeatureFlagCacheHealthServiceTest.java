package com.agenticcp.core.domain.platform.cache.service;

import com.agenticcp.core.domain.platform.cache.dto.CacheHealthStatusDto;
import com.agenticcp.core.domain.platform.cache.dto.CacheMetricsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FeatureFlagCacheHealthService 단위 테스트
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagCacheHealthService 단위 테스트")
class FeatureFlagCacheHealthServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private Cache cache;

    @InjectMocks
    private FeatureFlagCacheHealthService healthService;

    @Nested
    @DisplayName("헬스체크 테스트")
    class HealthCheckTest {

        @Test
        @DisplayName("Redis 정상 연결 시 healthy 상태 반환")
        void checkHealth_RedisConnected_ReturnsHealthy() {
            // Given
            when(cacheManager.getCache("feature-flags")).thenReturn(cache);
            when(redisConnectionFactory.getConnection()).thenReturn(mock(org.springframework.data.redis.connection.RedisConnection.class));

            // When
            CacheHealthStatusDto status = healthService.checkHealth();

            // Then
            assertThat(status).isNotNull();
            assertThat(status.isHealthy()).isTrue();
            assertThat(status.isFallbackMode()).isFalse();
            assertThat(status.isRedisConnected()).isTrue();
            assertThat(status.getConsecutiveFailures()).isZero();
            assertThat(status.getMessage()).contains("healthy");

            verify(cacheManager).getCache("feature-flags");
            verify(redisConnectionFactory).getConnection();
        }

        @Test
        @DisplayName("캐시를 찾을 수 없는 경우 에러 상태 반환")
        void checkHealth_CacheNotFound_ReturnsError() {
            // Given
            when(cacheManager.getCache("feature-flags")).thenReturn(null);

            // When
            CacheHealthStatusDto status = healthService.checkHealth();

            // Then
            assertThat(status).isNotNull();
            assertThat(status.isHealthy()).isFalse();
            assertThat(status.getConsecutiveFailures()).isEqualTo(1);
            assertThat(status.getMessage()).contains("캐시를 찾을 수 없습니다");

            verify(cacheManager).getCache("feature-flags");
            verify(redisConnectionFactory, never()).getConnection();
        }

        @Test
        @DisplayName("Redis 연결 실패 시 실패 카운트 증가")
        void checkHealth_RedisConnectionFailed_IncrementsFailureCount() {
            // Given
            when(cacheManager.getCache("feature-flags")).thenReturn(cache);
            when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Connection failed"));

            // When
            CacheHealthStatusDto status = healthService.checkHealth();

            // Then
            assertThat(status).isNotNull();
            assertThat(status.isHealthy()).isFalse();
            assertThat(status.isRedisConnected()).isFalse();
            assertThat(status.getConsecutiveFailures()).isEqualTo(1);
            assertThat(status.getMessage()).contains("실패");

            verify(cacheManager).getCache("feature-flags");
            verify(redisConnectionFactory).getConnection();
        }
    }

    @Nested
    @DisplayName("Fallback 모드 전환 테스트")
    class FallbackModeTest {

        @Test
        @DisplayName("연속 3회 실패 시 Fallback 모드로 전환")
        void checkHealth_ThreeConsecutiveFailures_SwitchesToFallbackMode() {
            // Given
            when(cacheManager.getCache("feature-flags")).thenReturn(cache);
            when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Connection failed"));

            // When - 3번 연속 실패
            CacheHealthStatusDto status1 = healthService.checkHealth();
            CacheHealthStatusDto status2 = healthService.checkHealth();
            CacheHealthStatusDto status3 = healthService.checkHealth();

            // Then
            assertThat(status1.getConsecutiveFailures()).isEqualTo(1);
            assertThat(status1.isFallbackMode()).isFalse();

            assertThat(status2.getConsecutiveFailures()).isEqualTo(2);
            assertThat(status2.isFallbackMode()).isFalse();

            assertThat(status3.getConsecutiveFailures()).isEqualTo(3);
            assertThat(status3.isFallbackMode()).isTrue();
            assertThat(status3.getMessage()).contains("Fallback");

            verify(cacheManager, times(3)).getCache("feature-flags");
            verify(redisConnectionFactory, times(3)).getConnection();
        }

        @Test
        @DisplayName("Fallback 모드에서 성공 시 정상 상태로 복구")
        void checkHealth_FallbackModeWithSuccess_Recovers() {
            // Given
            when(cacheManager.getCache("feature-flags")).thenReturn(cache);
            when(redisConnectionFactory.getConnection())
                    .thenThrow(new RuntimeException("Failed"))
                    .thenThrow(new RuntimeException("Failed"))
                    .thenThrow(new RuntimeException("Failed"))
                    .thenReturn(mock(org.springframework.data.redis.connection.RedisConnection.class));

            // When - 3번 실패 후 1번 성공
            healthService.checkHealth(); // 1st failure
            healthService.checkHealth(); // 2nd failure
            healthService.checkHealth(); // 3rd failure -> Fallback
            CacheHealthStatusDto status = healthService.checkHealth(); // Success -> Healthy

            // Then
            assertThat(status.isFallbackMode()).isFalse();
            assertThat(status.isHealthy()).isTrue();
            assertThat(status.getConsecutiveFailures()).isZero();
            assertThat(status.getMessage()).contains("healthy");

            verify(cacheManager, times(4)).getCache("feature-flags");
            verify(redisConnectionFactory, times(4)).getConnection();
        }

        @Test
        @DisplayName("Fallback 모드가 아닌 경우 성공 시 실패 카운트 리셋")
        void checkHealth_NotInFallbackWithSuccess_ResetsFailureCount() {
            // Given
            when(cacheManager.getCache("feature-flags")).thenReturn(cache);
            when(redisConnectionFactory.getConnection())
                    .thenThrow(new RuntimeException("Failed"))
                    .thenReturn(mock(org.springframework.data.redis.connection.RedisConnection.class));

            // When - 1번 실패 후 1번 성공
            CacheHealthStatusDto status1 = healthService.checkHealth(); // Failure
            CacheHealthStatusDto status2 = healthService.checkHealth(); // Success

            // Then
            assertThat(status1.getConsecutiveFailures()).isEqualTo(1);
            assertThat(status2.getConsecutiveFailures()).isZero();
            assertThat(status2.isHealthy()).isTrue();
            assertThat(status2.isFallbackMode()).isFalse();

            verify(cacheManager, times(2)).getCache("feature-flags");
            verify(redisConnectionFactory, times(2)).getConnection();
        }
    }

    @Nested
    @DisplayName("메트릭 수집 테스트")
    class MetricsTest {

        @Test
        @DisplayName("메트릭 초기화 시 모든 값이 0")
        void getMetrics_Initial_AllZero() {
            // When
            CacheMetricsDto metrics = healthService.getMetrics();

            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics.getTotalRequests()).isZero();
            assertThat(metrics.getCacheHits()).isZero();
            assertThat(metrics.getCacheMisses()).isZero();
            assertThat(metrics.getHitRate()).isZero();
            assertThat(metrics.getFallbackCount()).isZero();
            assertThat(metrics.getAvgResponseTimeMs()).isZero();
        }

        @Test
        @DisplayName("캐시 히트 기록 시 메트릭 업데이트")
        void recordCacheHit_UpdatesMetrics() {
            // When
            healthService.recordCacheHit(10L);
            healthService.recordCacheHit(20L);
            healthService.recordCacheHit(30L);
            CacheMetricsDto metrics = healthService.getMetrics();

            // Then
            assertThat(metrics.getTotalRequests()).isEqualTo(3);
            assertThat(metrics.getCacheHits()).isEqualTo(3);
            assertThat(metrics.getCacheMisses()).isZero();
            assertThat(metrics.getHitRate()).isEqualTo(100.0);
            assertThat(metrics.getAvgResponseTimeMs()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("캐시 미스 기록 시 메트릭 업데이트")
        void recordCacheMiss_UpdatesMetrics() {
            // When
            healthService.recordCacheMiss(50L);
            healthService.recordCacheMiss(60L);
            CacheMetricsDto metrics = healthService.getMetrics();

            // Then
            assertThat(metrics.getTotalRequests()).isEqualTo(2);
            assertThat(metrics.getCacheHits()).isZero();
            assertThat(metrics.getCacheMisses()).isEqualTo(2);
            assertThat(metrics.getHitRate()).isZero();
            assertThat(metrics.getAvgResponseTimeMs()).isEqualTo(55.0);
        }

        @Test
        @DisplayName("캐시 히트와 미스 혼합 시 Hit Rate 계산")
        void recordMixed_CalculatesHitRate() {
            // When
            healthService.recordCacheHit(10L);
            healthService.recordCacheHit(20L);
            healthService.recordCacheHit(30L);
            healthService.recordCacheMiss(40L);
            CacheMetricsDto metrics = healthService.getMetrics();

            // Then
            assertThat(metrics.getTotalRequests()).isEqualTo(4);
            assertThat(metrics.getCacheHits()).isEqualTo(3);
            assertThat(metrics.getCacheMisses()).isEqualTo(1);
            assertThat(metrics.getHitRate()).isEqualTo(75.0);
            assertThat(metrics.getAvgResponseTimeMs()).isEqualTo(25.0); // (10+20+30+40)/4
        }

        @Test
        @DisplayName("Fallback 발생 시 카운트 증가")
        void recordFallback_IncrementsFallbackCount() {
            // When
            healthService.recordFallback();
            healthService.recordFallback();
            healthService.recordFallback();
            CacheMetricsDto metrics = healthService.getMetrics();

            // Then
            assertThat(metrics.getFallbackCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("메트릭 리셋 시 모든 값 초기화")
        void resetMetrics_ResetsAllValues() {
            // Given
            healthService.recordCacheHit(10L);
            healthService.recordCacheMiss(20L);
            healthService.recordFallback();

            // When
            healthService.resetMetrics();
            CacheMetricsDto metrics = healthService.getMetrics();

            // Then
            assertThat(metrics.getTotalRequests()).isZero();
            assertThat(metrics.getCacheHits()).isZero();
            assertThat(metrics.getCacheMisses()).isZero();
            assertThat(metrics.getHitRate()).isZero();
            assertThat(metrics.getFallbackCount()).isZero();
            assertThat(metrics.getAvgResponseTimeMs()).isZero();
        }
    }

    @Nested
    @DisplayName("Fallback 모드 확인 테스트")
    class IsFallbackModeTest {

        @Test
        @DisplayName("초기 상태는 Fallback 모드 아님")
        void isFallbackMode_Initial_ReturnsFalse() {
            // When
            boolean isFallback = healthService.isFallbackMode();

            // Then
            assertThat(isFallback).isFalse();
        }

        @Test
        @DisplayName("연속 3회 실패 후 Fallback 모드 확인")
        void isFallbackMode_AfterThreeFailures_ReturnsTrue() {
            // Given
            when(cacheManager.getCache("feature-flags")).thenReturn(cache);
            when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Failed"));

            // When
            healthService.checkHealth();
            healthService.checkHealth();
            healthService.checkHealth();

            // Then
            assertThat(healthService.isFallbackMode()).isTrue();
        }

        @Test
        @DisplayName("Fallback 모드에서 복구 후 Fallback 모드 해제")
        void isFallbackMode_AfterRecovery_ReturnsFalse() {
            // Given
            when(cacheManager.getCache("feature-flags")).thenReturn(cache);
            when(redisConnectionFactory.getConnection())
                    .thenThrow(new RuntimeException("Failed"))
                    .thenThrow(new RuntimeException("Failed"))
                    .thenThrow(new RuntimeException("Failed"))
                    .thenReturn(mock(org.springframework.data.redis.connection.RedisConnection.class));

            // When
            healthService.checkHealth(); // Failure 1
            healthService.checkHealth(); // Failure 2
            healthService.checkHealth(); // Failure 3 -> Fallback
            healthService.checkHealth(); // Success -> Recovering

            // Then
            assertThat(healthService.isFallbackMode()).isFalse();
        }
    }
}

