package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MetricsCollectorRegistry 단위 테스트
 * 
 * <p>Issue #39: Task 6 - 메트릭 수집기 플러그인 시스템 구현
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("MetricsCollectorRegistry 단위 테스트")
class MetricsCollectorRegistryTest {
    
    private MetricsCollectorRegistry registry;
    private MetricsCollector testCollector1;
    private MetricsCollector testCollector2;
    
    @BeforeEach
    void setUp() {
        registry = new MetricsCollectorRegistry();
        
        // 테스트용 Mock 수집기 생성
        testCollector1 = mock(MetricsCollector.class);
        when(testCollector1.getCollectorType()).thenReturn(CollectorType.SYSTEM);
        when(testCollector1.isEnabled()).thenReturn(true);
        
        testCollector2 = mock(MetricsCollector.class);
        when(testCollector2.getCollectorType()).thenReturn(CollectorType.APPLICATION);
        when(testCollector2.isEnabled()).thenReturn(false);
    }
    
    @Nested
    @DisplayName("수집기 등록")
    class CollectorRegistration {
        
        @Test
        @DisplayName("수집기를 등록할 수 있다")
        void registerCollector_ShouldRegisterSuccessfully() {
            // When
            registry.registerCollector("test-collector", testCollector1);
            
            // Then
            assertThat(registry.getCollectorCount()).isEqualTo(1);
            assertThat(registry.hasCollector("test-collector")).isTrue();
        }
        
        @Test
        @DisplayName("동일한 이름으로 수집기를 재등록하면 덮어쓴다")
        void registerCollector_WithSameName_ShouldOverwrite() {
            // Given
            registry.registerCollector("test", testCollector1);
            
            // When
            registry.registerCollector("test", testCollector2);
            
            // Then
            assertThat(registry.getCollectorCount()).isEqualTo(1);
            Optional<MetricsCollector> collector = registry.getCollector("test");
            assertThat(collector).isPresent();
            assertThat(collector.get()).isEqualTo(testCollector2);
        }
        
        @Test
        @DisplayName("null 이름으로 등록 시 예외를 발생시킨다")
        void registerCollector_WithNullName_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> registry.registerCollector(null, testCollector1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수집기 이름은 필수입니다");
        }
        
        @Test
        @DisplayName("빈 문자열 이름으로 등록 시 예외를 발생시킨다")
        void registerCollector_WithEmptyName_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> registry.registerCollector("  ", testCollector1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수집기 이름은 필수입니다");
        }
        
        @Test
        @DisplayName("null 수집기로 등록 시 예외를 발생시킨다")
        void registerCollector_WithNullCollector_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> registry.registerCollector("test", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수집기 인스턴스는 필수입니다");
        }
    }
    
    @Nested
    @DisplayName("수집기 해제")
    class CollectorUnregistration {
        
        @Test
        @DisplayName("등록된 수집기를 해제할 수 있다")
        void unregisterCollector_WhenExists_ShouldRemoveSuccessfully() {
            // Given
            registry.registerCollector("test", testCollector1);
            
            // When
            boolean result = registry.unregisterCollector("test");
            
            // Then
            assertThat(result).isTrue();
            assertThat(registry.getCollectorCount()).isEqualTo(0);
            assertThat(registry.hasCollector("test")).isFalse();
        }
        
        @Test
        @DisplayName("존재하지 않는 수집기 해제 시 false를 반환한다")
        void unregisterCollector_WhenNotExists_ShouldReturnFalse() {
            // When
            boolean result = registry.unregisterCollector("non-existent");
            
            // Then
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("수집기 조회")
    class CollectorRetrieval {
        
        @Test
        @DisplayName("이름으로 수집기를 조회할 수 있다")
        void getCollector_WhenExists_ShouldReturnCollector() {
            // Given
            registry.registerCollector("test", testCollector1);
            
            // When
            Optional<MetricsCollector> collector = registry.getCollector("test");
            
            // Then
            assertThat(collector).isPresent();
            assertThat(collector.get()).isEqualTo(testCollector1);
        }
        
        @Test
        @DisplayName("존재하지 않는 이름으로 조회 시 empty를 반환한다")
        void getCollector_WhenNotExists_ShouldReturnEmpty() {
            // When
            Optional<MetricsCollector> collector = registry.getCollector("non-existent");
            
            // Then
            assertThat(collector).isEmpty();
        }
        
        @Test
        @DisplayName("모든 수집기를 조회할 수 있다")
        void getAllCollectors_ShouldReturnAllCollectors() {
            // Given
            registry.registerCollector("test1", testCollector1);
            registry.registerCollector("test2", testCollector2);
            
            // When
            List<MetricsCollector> collectors = registry.getAllCollectors();
            
            // Then
            assertThat(collectors).hasSize(2);
            assertThat(collectors).containsExactlyInAnyOrder(testCollector1, testCollector2);
        }
    }
    
    @Nested
    @DisplayName("활성화된 수집기 조회")
    class EnabledCollectorRetrieval {
        
        @Test
        @DisplayName("활성화된 수집기만 조회한다")
        void getEnabledCollectors_ShouldReturnOnlyEnabled() {
            // Given
            registry.registerCollector("enabled", testCollector1);   // enabled = true
            registry.registerCollector("disabled", testCollector2);  // enabled = false
            
            // When
            List<MetricsCollector> enabledCollectors = registry.getEnabledCollectors();
            
            // Then
            assertThat(enabledCollectors).hasSize(1);
            assertThat(enabledCollectors).containsExactly(testCollector1);
        }
        
        @Test
        @DisplayName("활성화된 수집기가 없으면 빈 리스트를 반환한다")
        void getEnabledCollectors_WhenNoneEnabled_ShouldReturnEmptyList() {
            // Given
            registry.registerCollector("disabled", testCollector2);  // enabled = false
            
            // When
            List<MetricsCollector> enabledCollectors = registry.getEnabledCollectors();
            
            // Then
            assertThat(enabledCollectors).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("타입별 수집기 조회")
    class CollectorRetrievalByType {
        
        @Test
        @DisplayName("특정 타입의 수집기만 조회한다")
        void getCollectorsByType_ShouldReturnMatchingType() {
            // Given
            registry.registerCollector("system", testCollector1);       // SYSTEM
            registry.registerCollector("application", testCollector2);  // APPLICATION
            
            // When
            List<MetricsCollector> systemCollectors = registry.getCollectorsByType(CollectorType.SYSTEM);
            
            // Then
            assertThat(systemCollectors).hasSize(1);
            assertThat(systemCollectors.get(0).getCollectorType()).isEqualTo(CollectorType.SYSTEM);
        }
        
        @Test
        @DisplayName("해당 타입의 수집기가 없으면 빈 리스트를 반환한다")
        void getCollectorsByType_WhenNoMatch_ShouldReturnEmptyList() {
            // Given
            registry.registerCollector("system", testCollector1);  // SYSTEM
            
            // When
            List<MetricsCollector> customCollectors = registry.getCollectorsByType(CollectorType.CUSTOM);
            
            // Then
            assertThat(customCollectors).isEmpty();
        }
        
        @Test
        @DisplayName("null 타입으로 조회 시 빈 리스트를 반환한다")
        void getCollectorsByType_WithNullType_ShouldReturnEmptyList() {
            // When
            List<MetricsCollector> collectors = registry.getCollectorsByType(null);
            
            // Then
            assertThat(collectors).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("수집기 상태 관리")
    class CollectorStatusManagement {
        
        @Test
        @DisplayName("수집기 개수를 조회할 수 있다")
        void getCollectorCount_ShouldReturnCorrectCount() {
            // Given
            registry.registerCollector("test1", testCollector1);
            registry.registerCollector("test2", testCollector2);
            
            // When
            int count = registry.getCollectorCount();
            
            // Then
            assertThat(count).isEqualTo(2);
        }
        
        @Test
        @DisplayName("활성화된 수집기 개수를 조회할 수 있다")
        void getEnabledCollectorCount_ShouldReturnCorrectCount() {
            // Given
            registry.registerCollector("enabled", testCollector1);   // enabled = true
            registry.registerCollector("disabled", testCollector2);  // enabled = false
            
            // When
            int count = registry.getEnabledCollectorCount();
            
            // Then
            assertThat(count).isEqualTo(1);
        }
        
        @Test
        @DisplayName("수집기 상태 맵을 조회할 수 있다")
        void getCollectorStatus_ShouldReturnStatusMap() {
            // Given
            registry.registerCollector("enabled", testCollector1);
            registry.registerCollector("disabled", testCollector2);
            
            // When
            Map<String, Boolean> status = registry.getCollectorStatus();
            
            // Then
            assertThat(status).hasSize(2);
            assertThat(status.get("enabled")).isTrue();
            assertThat(status.get("disabled")).isFalse();
        }
        
        @Test
        @DisplayName("수집기 존재 여부를 확인할 수 있다")
        void hasCollector_ShouldReturnCorrectResult() {
            // Given
            registry.registerCollector("test", testCollector1);
            
            // When & Then
            assertThat(registry.hasCollector("test")).isTrue();
            assertThat(registry.hasCollector("non-existent")).isFalse();
        }
    }
    
    @Nested
    @DisplayName("자동 등록")
    class AutoRegistration {
        
        @Test
        @DisplayName("Spring Bean 수집기들을 자동으로 등록한다")
        void autoRegisterCollectors_ShouldRegisterAllBeans() {
            // Given
            List<MetricsCollector> beans = new ArrayList<>();
            beans.add(testCollector1);
            beans.add(testCollector2);
            
            // When
            registry.autoRegisterCollectors(beans);
            
            // Then
            assertThat(registry.getCollectorCount()).isEqualTo(2);
        }
        
        @Test
        @DisplayName("빈 리스트로 자동 등록 시 아무것도 등록되지 않는다")
        void autoRegisterCollectors_WithEmptyList_ShouldRegisterNothing() {
            // Given
            List<MetricsCollector> beans = new ArrayList<>();
            
            // When
            registry.autoRegisterCollectors(beans);
            
            // Then
            assertThat(registry.getCollectorCount()).isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("수집기 정보 조회")
    class CollectorInfoRetrieval {
        
        @Test
        @DisplayName("수집기 정보 목록을 조회할 수 있다")
        void getCollectorInfoList_ShouldReturnInfoList() {
            // Given
            registry.registerCollector("system", testCollector1);
            registry.registerCollector("application", testCollector2);
            
            // When
            List<MetricsCollectorRegistry.CollectorInfo> infoList = registry.getCollectorInfoList();
            
            // Then
            assertThat(infoList).hasSize(2);
            assertThat(infoList).extracting(MetricsCollectorRegistry.CollectorInfo::getName)
                    .containsExactlyInAnyOrder("system", "application");
        }
        
        @Test
        @DisplayName("CollectorInfo가 올바른 정보를 담고 있다")
        void collectorInfo_ShouldContainCorrectData() {
            // Given
            registry.registerCollector("test", testCollector1);
            
            // When
            List<MetricsCollectorRegistry.CollectorInfo> infoList = registry.getCollectorInfoList();
            MetricsCollectorRegistry.CollectorInfo info = infoList.get(0);
            
            // Then
            assertThat(info.getName()).isEqualTo("test");
            assertThat(info.getType()).isEqualTo(CollectorType.SYSTEM);
            assertThat(info.isEnabled()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("초기화")
    class ClearAll {
        
        @Test
        @DisplayName("모든 수집기를 초기화할 수 있다")
        void clearAll_ShouldRemoveAllCollectors() {
            // Given
            registry.registerCollector("test1", testCollector1);
            registry.registerCollector("test2", testCollector2);
            
            // When
            registry.clearAll();
            
            // Then
            assertThat(registry.getCollectorCount()).isEqualTo(0);
            assertThat(registry.getAllCollectors()).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTest {
        
        @Test
        @DisplayName("여러 스레드에서 동시에 수집기를 등록해도 안전하다")
        void registerCollector_FromMultipleThreads_ShouldBeThreadSafe() throws InterruptedException {
            // Given
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            
            // When
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    MetricsCollector collector = mock(MetricsCollector.class);
                    when(collector.getCollectorType()).thenReturn(CollectorType.CUSTOM);
                    when(collector.isEnabled()).thenReturn(true);
                    registry.registerCollector("collector-" + index, collector);
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Then
            assertThat(registry.getCollectorCount()).isEqualTo(threadCount);
        }
    }
}

