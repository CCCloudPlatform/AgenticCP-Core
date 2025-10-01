package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomMetricsCollector 단위 테스트
 * 
 * <p>Issue #39: Task 6 - 메트릭 수집기 플러그인 시스템 구현
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("CustomMetricsCollector 단위 테스트")
class CustomMetricsCollectorTest {
    
    private TestCustomCollector customCollector;
    
    @BeforeEach
    void setUp() {
        customCollector = new TestCustomCollector();
    }
    
    @Nested
    @DisplayName("커스텀 수집기 기본 동작")
    class BasicBehavior {
        
        @Test
        @DisplayName("수집기 이름을 반환한다")
        void getCollectorName_ShouldReturnName() {
            // When
            String name = customCollector.getCollectorName();
            
            // Then
            assertThat(name).isEqualTo("test-custom-collector");
        }
        
        @Test
        @DisplayName("수집기 설명을 반환한다")
        void getCollectorDescription_ShouldReturnDescription() {
            // When
            String description = customCollector.getCollectorDescription();
            
            // Then
            assertThat(description).isEqualTo("테스트용 커스텀 수집기");
        }
        
        @Test
        @DisplayName("수집기 타입은 CUSTOM이다")
        void getCollectorType_ShouldReturnCustom() {
            // When
            CollectorType type = customCollector.getCollectorType();
            
            // Then
            assertThat(type).isEqualTo(CollectorType.CUSTOM);
        }
        
        @Test
        @DisplayName("기본적으로 활성화되어 있다")
        void isEnabled_ShouldBeTrue() {
            // When
            boolean enabled = customCollector.isEnabled();
            
            // Then
            assertThat(enabled).isTrue();
        }
    }
    
    @Nested
    @DisplayName("메트릭 수집")
    class MetricCollection {
        
        @Test
        @DisplayName("애플리케이션 메트릭을 수집한다")
        void collectApplicationMetrics_ShouldReturnMetrics() {
            // When
            List<Metric> metrics = customCollector.collectApplicationMetrics();
            
            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics).hasSize(2);
            assertThat(metrics).extracting(Metric::getMetricName)
                    .containsExactlyInAnyOrder("custom.metric1", "custom.metric2");
        }
        
        @Test
        @DisplayName("시스템 메트릭은 null을 반환한다")
        void collectSystemMetrics_ShouldReturnNull() {
            // When
            SystemMetrics systemMetrics = customCollector.collectSystemMetrics();
            
            // Then
            assertThat(systemMetrics).isNull();
        }
    }
    
    @Nested
    @DisplayName("수집기 설정")
    class CollectorConfiguration {
        
        @Test
        @DisplayName("수집기 설정 정보를 반환한다")
        void getCollectorConfig_ShouldReturnConfig() {
            // When
            Map<String, Object> config = customCollector.getCollectorConfig();
            
            // Then
            assertThat(config).isNotNull();
            assertThat(config).containsEntry("name", "test-custom-collector");
            assertThat(config).containsEntry("description", "테스트용 커스텀 수집기");
            assertThat(config).containsEntry("type", "CUSTOM");
            assertThat(config).containsEntry("enabled", true);
        }
        
        @Test
        @DisplayName("초기화 메서드가 호출된다")
        void initialize_ShouldBeCallable() {
            // Given
            Map<String, Object> config = new HashMap<>();
            config.put("interval", 30000);
            
            // When & Then
            // 예외 발생하지 않음
            customCollector.initialize(config);
        }
        
        @Test
        @DisplayName("종료 메서드가 호출된다")
        void shutdown_ShouldBeCallable() {
            // When & Then
            // 예외 발생하지 않음
            customCollector.shutdown();
        }
    }
    
    @Nested
    @DisplayName("수집기 상태 확인")
    class HealthCheck {
        
        @Test
        @DisplayName("활성화된 수집기는 healthy 상태다")
        void isHealthy_WhenEnabled_ShouldReturnTrue() {
            // When
            boolean healthy = customCollector.isHealthy();
            
            // Then
            assertThat(healthy).isTrue();
        }
        
        @Test
        @DisplayName("비활성화된 수집기는 unhealthy 상태다")
        void isHealthy_WhenDisabled_ShouldReturnFalse() {
            // Given
            customCollector.setEnabled(false);
            
            // When
            boolean healthy = customCollector.isHealthy();
            
            // Then
            assertThat(healthy).isFalse();
        }
    }
    
    /**
     * 테스트용 CustomMetricsCollector 구현체
     */
    private static class TestCustomCollector implements CustomMetricsCollector {
        
        private boolean enabled = true;
        
        @Override
        public String getCollectorName() {
            return "test-custom-collector";
        }
        
        @Override
        public String getCollectorDescription() {
            return "테스트용 커스텀 수집기";
        }
        
        @Override
        public CollectorType getCollectorType() {
            return CollectorType.CUSTOM;
        }
        
        @Override
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        @Override
        public SystemMetrics collectSystemMetrics() {
            return null;
        }
        
        @Override
        public List<Metric> collectApplicationMetrics() {
            List<Metric> metrics = new ArrayList<>();
            
            metrics.add(Metric.builder()
                    .metricName("custom.metric1")
                    .metricValue(100.0)
                    .unit("count")
                    .metricType(Metric.MetricType.APPLICATION)
                    .collectedAt(LocalDateTime.now())
                    .build());
            
            metrics.add(Metric.builder()
                    .metricName("custom.metric2")
                    .metricValue(200.0)
                    .unit("count")
                    .metricType(Metric.MetricType.APPLICATION)
                    .collectedAt(LocalDateTime.now())
                    .build());
            
            return metrics;
        }
    }
}

