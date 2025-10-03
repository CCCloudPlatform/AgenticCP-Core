package com.agenticcp.core.domain.monitoring.health.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * HealthCheckCacheService 단위 테스트
 * 
 * 헬스체크 캐시 관리 서비스의 기능을 테스트합니다.
 * 
 * 테스트 시나리오:
 * - 전체 헬스체크 캐시 클리어 기능
 * - 개별 컴포넌트 캐시 제거 기능
 * - 전체 캐시 제거 기능
 * - CacheManager null 처리
 * - Cache null 처리
 * 
 * 주의사항:
 * - Mock을 사용한 순수 단위 테스트
 * - 실제 캐시 구현체와 무관
 * - Spring 컨텍스트 로드 없음
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthCheckCacheService 단위 테스트")
class HealthCheckCacheServiceTest {

    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private Cache cache;
    
    private HealthCheckCacheService healthCheckCacheService;

    @BeforeEach
    void setUp() {
        healthCheckCacheService = new HealthCheckCacheService(cacheManager);
    }

    @Nested
    @DisplayName("전체 헬스체크 캐시 클리어 테스트")
    class EvictHealthCheckCacheTest {

        @Test
        @DisplayName("캐시가 존재할 때 전체 캐시를 클리어해야 함")
        void evictHealthCheckCache_WhenCacheExists_ShouldClearCache() {
            // Given
            when(cacheManager.getCache("healthCheck")).thenReturn(cache);
            
            // When
            healthCheckCacheService.evictHealthCheckCache();
            
            // Then
            verify(cacheManager).getCache("healthCheck");
            verify(cache).clear();
            verifyNoMoreInteractions(cacheManager, cache);
        }

        @Test
        @DisplayName("캐시가 null일 때 예외 없이 처리해야 함")
        void evictHealthCheckCache_WhenCacheIsNull_ShouldHandleGracefully() {
            // Given
            when(cacheManager.getCache("healthCheck")).thenReturn(null);
            
            // When & Then - 예외가 발생하지 않아야 함
            healthCheckCacheService.evictHealthCheckCache();
            
            // Then
            verify(cacheManager).getCache("healthCheck");
            verifyNoInteractions(cache);
        }
    }

    @Nested
    @DisplayName("개별 컴포넌트 캐시 제거 테스트")
    class EvictComponentCacheTest {

        @Test
        @DisplayName("특정 컴포넌트 캐시를 제거해야 함")
        void evictComponentCache_WithValidComponent_ShouldEvictComponentCache() {
            // Given
            String componentName = "database";
            when(cacheManager.getCache("healthCheck")).thenReturn(cache);
            
            // When
            healthCheckCacheService.evictComponentCache(componentName);
            
            // Then
            verify(cacheManager).getCache("healthCheck");
            verify(cache).evict(componentName);
            verifyNoMoreInteractions(cacheManager, cache);
        }

        @Test
        @DisplayName("다양한 컴포넌트명으로 캐시 제거가 가능해야 함")
        void evictComponentCache_WithDifferentComponents_ShouldEvictCorrectComponent() {
            // Given
            when(cacheManager.getCache("healthCheck")).thenReturn(cache);
            
            // When & Then
            healthCheckCacheService.evictComponentCache("database");
            healthCheckCacheService.evictComponentCache("system");
            healthCheckCacheService.evictComponentCache("application");
            
            // Then
            verify(cacheManager, times(3)).getCache("healthCheck");
            verify(cache).evict("database");
            verify(cache).evict("system");
            verify(cache).evict("application");
        }

        @Test
        @DisplayName("캐시가 null일 때 컴포넌트 캐시 제거가 예외 없이 처리되어야 함")
        void evictComponentCache_WhenCacheIsNull_ShouldHandleGracefully() {
            // Given
            String componentName = "database";
            when(cacheManager.getCache("healthCheck")).thenReturn(null);
            
            // When & Then - 예외가 발생하지 않아야 함
            healthCheckCacheService.evictComponentCache(componentName);
            
            // Then
            verify(cacheManager).getCache("healthCheck");
            verifyNoInteractions(cache);
        }
    }

    @Nested
    @DisplayName("전체 캐시 제거 테스트")
    class EvictOverallCacheTest {

        @Test
        @DisplayName("전체 캐시를 제거해야 함")
        void evictOverallCache_WhenCacheExists_ShouldEvictOverallCache() {
            // Given
            when(cacheManager.getCache("healthCheck")).thenReturn(cache);
            
            // When
            healthCheckCacheService.evictOverallCache();
            
            // Then
            verify(cacheManager).getCache("healthCheck");
            verify(cache).evict("overall");
            verifyNoMoreInteractions(cacheManager, cache);
        }

        @Test
        @DisplayName("캐시가 null일 때 전체 캐시 제거가 예외 없이 처리되어야 함")
        void evictOverallCache_WhenCacheIsNull_ShouldHandleGracefully() {
            // Given
            when(cacheManager.getCache("healthCheck")).thenReturn(null);
            
            // When & Then - 예외가 발생하지 않아야 함
            healthCheckCacheService.evictOverallCache();
            
            // Then
            verify(cacheManager).getCache("healthCheck");
            verifyNoInteractions(cache);
        }
    }

    @Nested
    @DisplayName("캐시 관리 통합 테스트")
    class CacheManagementIntegrationTest {

        @Test
        @DisplayName("여러 캐시 작업을 순차적으로 수행할 수 있어야 함")
        void performMultipleCacheOperations_ShouldWorkCorrectly() {
            // Given
            when(cacheManager.getCache("healthCheck")).thenReturn(cache);
            
            // When
            healthCheckCacheService.evictHealthCheckCache();
            healthCheckCacheService.evictComponentCache("database");
            healthCheckCacheService.evictComponentCache("system");
            healthCheckCacheService.evictOverallCache();
            
            // Then
            verify(cacheManager, times(4)).getCache("healthCheck");
            verify(cache).clear();
            verify(cache).evict("database");
            verify(cache).evict("system");
            verify(cache).evict("overall");
        }

        @Test
        @DisplayName("동일한 컴포넌트에 대해 여러 번 캐시 제거가 가능해야 함")
        void evictSameComponentMultipleTimes_ShouldWorkCorrectly() {
            // Given
            String componentName = "database";
            when(cacheManager.getCache("healthCheck")).thenReturn(cache);
            
            // When
            healthCheckCacheService.evictComponentCache(componentName);
            healthCheckCacheService.evictComponentCache(componentName);
            healthCheckCacheService.evictComponentCache(componentName);
            
            // Then
            verify(cacheManager, times(3)).getCache("healthCheck");
            verify(cache, times(3)).evict(componentName);
        }
    }
}
