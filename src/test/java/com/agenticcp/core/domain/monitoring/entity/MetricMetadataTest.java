package com.agenticcp.core.domain.monitoring.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MetricMetadata 엔티티 단위 테스트
 * 메트릭 메타데이터 엔티티의 비즈니스 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@DisplayName("MetricMetadata 엔티티 단위 테스트")
class MetricMetadataTest {

    @Nested
    @DisplayName("빌더 패턴 테스트")
    class BuilderPatternTest {

        @Test
        @DisplayName("정상적인 MetricMetadata 생성")
        void buildMetricMetadata_WhenValidData_ShouldCreateMetadata() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();
            String key = "hostname";
            String value = "server-001";

            // When
            MetricMetadata metadata = MetricMetadata.builder()
                    .metric(metric)
                    .key(key)
                    .value(value)
                    .build();

            // Then
            assertThat(metadata.getMetric()).isEqualTo(metric);
            assertThat(metadata.getKey()).isEqualTo(key);
            assertThat(metadata.getValue()).isEqualTo(value);
            assertThat(metadata.getDataType()).isEqualTo("string"); // 기본값
        }

        @Test
        @DisplayName("기본값이 올바르게 설정됨")
        void buildMetricMetadata_WhenDefaultValues_ShouldSetDefaults() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();

            // When
            MetricMetadata metadata = MetricMetadata.builder()
                    .metric(metric)
                    .key("region")
                    .build();

            // Then
            assertThat(metadata.getDataType()).isEqualTo("string");
        }

        @Test
        @DisplayName("다양한 데이터 타입으로 생성")
        void buildMetricMetadata_WithDifferentDataTypes_ShouldCreateMetadata() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();

            // When
            MetricMetadata stringMetadata = MetricMetadata.builder()
                    .metric(metric)
                    .key("hostname")
                    .value("server-001")
                    .dataType("string")
                    .build();

            MetricMetadata numberMetadata = MetricMetadata.builder()
                    .metric(metric)
                    .key("port")
                    .value("8080")
                    .dataType("number")
                    .build();

            // Then
            assertThat(stringMetadata.getDataType()).isEqualTo("string");
            assertThat(numberMetadata.getDataType()).isEqualTo("number");
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("메타데이터 값 업데이트")
        void updateValue_WhenCalled_ShouldUpdateValue() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();
            MetricMetadata metadata = MetricMetadata.builder()
                    .metric(metric)
                    .key("hostname")
                    .value("server-001")
                    .dataType("string")
                    .build();

            // When
            metadata.updateValue("server-002", "string");

            // Then
            assertThat(metadata.getValue()).isEqualTo("server-002");
            assertThat(metadata.getDataType()).isEqualTo("string");
        }

        @Test
        @DisplayName("메타데이터 설명 업데이트")
        void updateDescription_WhenCalled_ShouldUpdateDescription() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();
            MetricMetadata metadata = MetricMetadata.builder()
                    .metric(metric)
                    .key("hostname")
                    .value("server-001")
                    .build();

            // When
            metadata.updateDescription("서버 호스트명");

            // Then
            assertThat(metadata.getDescription()).isEqualTo("서버 호스트명");
        }

        @Test
        @DisplayName("연결된 메트릭 설정")
        void setMetric_WhenCalled_ShouldSetMetric() {
            // Given
            Metric metric1 = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();
            Metric metric2 = Metric.builder()
                    .metricName("memory.usage")
                    .metricValue(67.8)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();
            MetricMetadata metadata = MetricMetadata.builder()
                    .metric(metric1)
                    .key("hostname")
                    .value("server-001")
                    .build();

            // When
            metadata.setMetric(metric2);

            // Then
            assertThat(metadata.getMetric()).isEqualTo(metric2);
        }
    }
}

