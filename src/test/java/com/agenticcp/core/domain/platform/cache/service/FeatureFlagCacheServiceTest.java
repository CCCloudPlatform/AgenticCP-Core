package com.agenticcp.core.domain.platform.cache.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.cache.config.RedisCacheConfig.FeatureFlagCacheProperties;
import com.agenticcp.core.domain.platform.cache.dto.FeatureFlagCacheDto;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.service.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * FeatureFlagCacheService 단위 테스트
 * 캐시 조회/저장/무효화/Warm-up 및 분산 락 기능 검증
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagCacheService 단위 테스트")
class FeatureFlagCacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private FeatureFlagService featureFlagService;

    @Mock
    private FeatureFlagCacheProperties cacheProperties;

    @InjectMocks
    private FeatureFlagCacheService cacheService;

    private FeatureFlag testFlag;
    private FeatureFlagCacheDto testCacheDto;

    @BeforeEach
    void setUp() {
        // 테스트용 기본 플래그 데이터
        testFlag = FeatureFlag.builder()
                .flagKey("test-feature")
                .flagName("테스트 기능")
                .description("테스트용 기능 플래그")
                .isEnabled(true)
                .status(Status.ACTIVE)
                .rolloutPercentage(100)
                .cacheTtlSeconds(300)
                .build();

        testCacheDto = FeatureFlagCacheDto.from(testFlag);

        // 기본 프로퍼티 설정 (lenient로 설정하여 UnnecessaryStubbing 방지)
        lenient().when(cacheProperties.getDefaultTtlSeconds()).thenReturn(300);
        lenient().when(cacheProperties.getMinTtlSeconds()).thenReturn(10);
        lenient().when(cacheProperties.getMaxTtlSeconds()).thenReturn(3600);
        lenient().when(cacheManager.getCache(anyString())).thenReturn(cache);
    }

    @Nested
    @DisplayName("캐시 조회 테스트")
    class GetFromCacheTest {

        @Test
        @DisplayName("캐시 히트 시 캐시된 데이터 반환")
        void getFromCache_CacheHit_ReturnsCachedData() {
            // Given
            when(cache.get("test-feature", FeatureFlagCacheDto.class)).thenReturn(testCacheDto);

            // When
            FeatureFlagCacheDto result = cacheService.getFromCache("test-feature");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFlagKey()).isEqualTo("test-feature");
            assertThat(result.getIsEnabled()).isTrue();
            
            verify(cache).get("test-feature", FeatureFlagCacheDto.class);
            verify(featureFlagService, never()).getFlagByKey(anyString());
        }

        @Test
        @DisplayName("캐시 미스 시 DB에서 조회")
        void getFromCache_CacheMiss_LoadsFromDatabase() {
            // Given
            when(cache.get("test-feature", FeatureFlagCacheDto.class)).thenReturn(null);
            when(featureFlagService.getFlagByKey("test-feature"))
                    .thenReturn(java.util.Optional.of(testFlag));

            // When
            FeatureFlagCacheDto result = cacheService.getFromCache("test-feature");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFlagKey()).isEqualTo("test-feature");
            
            verify(cache).get("test-feature", FeatureFlagCacheDto.class);
            verify(featureFlagService).getFlagByKey("test-feature");
            verify(cache).put("test-feature", result);
        }

        @Test
        @DisplayName("캐시 예외 시 Fallback으로 DB 조회")
        void getFromCache_CacheException_FallbackToDatabase() {
            // Given
            when(cache.get("test-feature", FeatureFlagCacheDto.class))
                    .thenThrow(new RuntimeException("Redis connection failed"));
            when(featureFlagService.getFlagByKey("test-feature"))
                    .thenReturn(java.util.Optional.of(testFlag));

            // When
            FeatureFlagCacheDto result = cacheService.getFromCache("test-feature");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFlagKey()).isEqualTo("test-feature");
            
            verify(cache).get("test-feature", FeatureFlagCacheDto.class);
            verify(featureFlagService).getFlagByKey("test-feature");
        }

        @Test
        @DisplayName("캐시와 DB 모두 없으면 null 반환")
        void getFromCache_NotFoundAnywhere_ReturnsNull() {
            // Given
            when(cache.get("nonexistent", FeatureFlagCacheDto.class)).thenReturn(null);
            when(featureFlagService.getFlagByKey("nonexistent"))
                    .thenReturn(java.util.Optional.empty());

            // When
            FeatureFlagCacheDto result = cacheService.getFromCache("nonexistent");

            // Then
            assertThat(result).isNull();
            
            verify(cache).get("nonexistent", FeatureFlagCacheDto.class);
            verify(featureFlagService).getFlagByKey("nonexistent");
        }
    }

    @Nested
    @DisplayName("캐시 저장 테스트")
    class PutToCacheTest {

        @Test
        @DisplayName("개별 TTL로 캐시 저장 성공")
        void putToCache_WithCustomTTL_Success() {
            // Given
            testFlag.setCacheTtlSeconds(60);
            doNothing().when(cache).put(anyString(), any());

            // When
            cacheService.putToCache(testFlag);

            // Then
            verify(cache).put(eq("test-feature"), any(FeatureFlagCacheDto.class));
        }

        @Test
        @DisplayName("기본 TTL로 캐시 저장 성공")
        void putToCache_WithDefaultTTL_Success() {
            // Given
            testFlag.setCacheTtlSeconds(null);
            doNothing().when(cache).put(anyString(), any());

            // When
            cacheService.putToCache(testFlag);

            // Then
            verify(cache).put(eq("test-feature"), any(FeatureFlagCacheDto.class));
        }

        @Test
        @DisplayName("유효하지 않은 TTL로 저장 시 예외 발생")
        void putToCache_InvalidTTL_ThrowsException() {
            // Given
            testFlag.setCacheTtlSeconds(5); // 최소값(10) 미만

            // When & Then
            assertThatThrownBy(() -> cacheService.putToCache(testFlag))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("유효하지 않은 TTL 값");
            
            verify(cache, never()).put(anyString(), any());
        }

        @Test
        @DisplayName("최대 TTL 초과 시 예외 발생")
        void putToCache_TTLExceedsMax_ThrowsException() {
            // Given
            testFlag.setCacheTtlSeconds(4000); // 최대값(3600) 초과

            // When & Then
            assertThatThrownBy(() -> cacheService.putToCache(testFlag))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("유효하지 않은 TTL 값");
            
            verify(cache, never()).put(anyString(), any());
        }
    }

    @Nested
    @DisplayName("캐시 무효화 테스트")
    class InvalidateCacheTest {

        @Test
        @DisplayName("특정 플래그 캐시 무효화 성공")
        void invalidateCache_Success() {
            // Given
            doNothing().when(cache).evict("test-feature");

            // When
            cacheService.invalidateCache("test-feature");

            // Then
            verify(cache).evict("test-feature");
        }

        @Test
        @DisplayName("전체 캐시 무효화 성공")
        void invalidateAllCache_Success() {
            // Given
            doNothing().when(cache).clear();

            // When
            cacheService.invalidateAllCache();

            // Then
            verify(cache).clear();
        }

        @Test
        @DisplayName("캐시 무효화 실패 시 로그만 기록")
        void invalidateCache_Failure_LogsError() {
            // Given
            doThrow(new RuntimeException("Cache clear failed"))
                    .when(cache).evict("test-feature");

            // When & Then
            assertThatCode(() -> cacheService.invalidateCache("test-feature"))
                    .doesNotThrowAnyException();
            
            verify(cache).evict("test-feature");
        }
    }

    @Nested
    @DisplayName("캐시 Warm-up 테스트")
    class WarmupCacheTest {

        @Test
        @DisplayName("전체 플래그 캐시 Warm-up 성공")
        void warmupCache_Success() {
            // Given
            FeatureFlag flag1 = FeatureFlag.builder()
                    .flagKey("flag1")
                    .isEnabled(true)
                    .cacheTtlSeconds(300)
                    .build();
            
            FeatureFlag flag2 = FeatureFlag.builder()
                    .flagKey("flag2")
                    .isEnabled(false)
                    .cacheTtlSeconds(null)
                    .build();

            List<FeatureFlag> flags = Arrays.asList(flag1, flag2);
            
            when(featureFlagService.getAllFlags()).thenReturn(flags);
            doNothing().when(cache).put(anyString(), any());

            // When
            int result = cacheService.warmupCache();

            // Then
            assertThat(result).isEqualTo(2);
            verify(featureFlagService).getAllFlags();
            verify(cache, times(2)).put(anyString(), any(FeatureFlagCacheDto.class));
        }

        @Test
        @DisplayName("플래그가 없으면 0 반환")
        void warmupCache_NoFlags_ReturnsZero() {
            // Given
            when(featureFlagService.getAllFlags()).thenReturn(List.of());

            // When
            int result = cacheService.warmupCache();

            // Then
            assertThat(result).isZero();
            verify(featureFlagService).getAllFlags();
            verify(cache, never()).put(anyString(), any());
        }

        @Test
        @DisplayName("일부 플래그 캐싱 실패해도 계속 진행")
        void warmupCache_PartialFailure_ContinuesProcessing() {
            // Given
            FeatureFlag flag1 = FeatureFlag.builder()
                    .flagKey("flag1")
                    .cacheTtlSeconds(300)
                    .build();
            
            FeatureFlag flag2 = FeatureFlag.builder()
                    .flagKey("flag2")
                    .cacheTtlSeconds(5) // 유효하지 않은 TTL
                    .build();

            when(featureFlagService.getAllFlags()).thenReturn(Arrays.asList(flag1, flag2));
            doNothing().when(cache).put(eq("flag1"), any());

            // When
            int result = cacheService.warmupCache();

            // Then
            assertThat(result).isEqualTo(1); // flag1만 성공
            verify(cache).put(eq("flag1"), any());
            verify(cache, never()).put(eq("flag2"), any());
        }
    }

    @Nested
    @DisplayName("분산 락 테스트")
    class DistributedLockTest {

        @Test
        @DisplayName("분산 락 획득 후 업데이트 성공")
        void updateWithLock_Success() throws Exception {
            // Given
            when(redissonClient.getLock("agenticcp:ff:lock:test-feature")).thenReturn(rLock);
            when(rLock.tryLock(10, 30, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true); // 현재 스레드가 락 보유 중
            doNothing().when(cache).evict(anyString());
            doNothing().when(cache).put(anyString(), any());
            doNothing().when(rLock).unlock();

            // When
            cacheService.updateWithLock("test-feature", testFlag);

            // Then
            verify(rLock).tryLock(10, 30, TimeUnit.SECONDS);
            verify(cache).evict("test-feature");
            verify(cache).put(eq("test-feature"), any(FeatureFlagCacheDto.class));
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("분산 락 획득 타임아웃 시 예외 발생")
        void updateWithLock_LockTimeout_ThrowsException() throws Exception {
            // Given
            when(redissonClient.getLock("agenticcp:ff:lock:test-feature")).thenReturn(rLock);
            when(rLock.tryLock(10, 30, TimeUnit.SECONDS)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> cacheService.updateWithLock("test-feature", testFlag))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("분산 락 획득 시간이 초과");
            
            verify(rLock).tryLock(10, 30, TimeUnit.SECONDS);
            verify(cache, never()).evict(anyString());
            verify(rLock, never()).unlock();
        }

        @Test
        @DisplayName("분산 락 예외 발생 시에도 락 해제")
        void updateWithLock_Exception_UnlocksAnyway() throws Exception {
            // Given
            when(redissonClient.getLock("agenticcp:ff:lock:test-feature")).thenReturn(rLock);
            when(rLock.tryLock(10, 30, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true); // 현재 스레드가 락 보유 중
            doThrow(new RuntimeException("Cache eviction failed")).when(cache).evict(anyString());
            doNothing().when(rLock).unlock();

            // When & Then
            assertThatThrownBy(() -> cacheService.updateWithLock("test-feature", testFlag))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("캐시 작업이 실패");
            
            verify(rLock).tryLock(10, 30, TimeUnit.SECONDS);
            verify(cache).evict("test-feature");
            verify(rLock).unlock(); // 예외 발생해도 락 해제
        }
    }
}

