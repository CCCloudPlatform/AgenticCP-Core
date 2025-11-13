package com.agenticcp.core.domain.monitoring.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MetricTag 엔티티 단위 테스트
 * 메트릭 태그 엔티티의 비즈니스 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@DisplayName("MetricTag 엔티티 단위 테스트")
class MetricTagTest {

    @Nested
    @DisplayName("빌더 패턴 테스트")
    class BuilderPatternTest {

        @Test
        @DisplayName("정상적인 MetricTag 생성")
        void buildMetricTag_WhenValidData_ShouldCreateTag() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();
            String name = "environment";
            String value = "production";

            // When
            MetricTag tag = MetricTag.builder()
                    .metric(metric)
                    .name(name)
                    .value(value)
                    .build();

            // Then
            assertThat(tag.getMetric()).isEqualTo(metric);
            assertThat(tag.getName()).isEqualTo(name);
            assertThat(tag.getValue()).isEqualTo(value);
            assertThat(tag.getPriority()).isEqualTo(0); // 기본값
        }

        @Test
        @DisplayName("기본값이 올바르게 설정됨")
        void buildMetricTag_WhenDefaultValues_ShouldSetDefaults() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();

            // When
            MetricTag tag = MetricTag.builder()
                    .metric(metric)
                    .name("service")
                    .value("api-service")
                    .build();

            // Then
            assertThat(tag.getPriority()).isEqualTo(0);
        }

        @Test
        @DisplayName("모든 필드로 MetricTag 생성")
        void buildMetricTag_WithAllFields_ShouldCreateTag() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();

            // When
            MetricTag tag = MetricTag.builder()
                    .metric(metric)
                    .name("environment")
                    .value("production")
                    .category("infrastructure")
                    .priority(1)
                    .description("프로덕션 환경 태그")
                    .build();

            // Then
            assertThat(tag.getMetric()).isEqualTo(metric);
            assertThat(tag.getName()).isEqualTo("environment");
            assertThat(tag.getValue()).isEqualTo("production");
            assertThat(tag.getCategory()).isEqualTo("infrastructure");
            assertThat(tag.getPriority()).isEqualTo(1);
            assertThat(tag.getDescription()).isEqualTo("프로덕션 환경 태그");
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("태그 값 업데이트")
        void updateValue_WhenCalled_ShouldUpdateValue() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();
            MetricTag tag = MetricTag.builder()
                    .metric(metric)
                    .name("environment")
                    .value("staging")
                    .build();

            // When
            tag.updateValue("production");

            // Then
            assertThat(tag.getValue()).isEqualTo("production");
        }

        @Test
        @DisplayName("태그 우선순위 업데이트")
        void updatePriority_WhenCalled_ShouldUpdatePriority() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();
            MetricTag tag = MetricTag.builder()
                    .metric(metric)
                    .name("environment")
                    .value("production")
                    .priority(0)
                    .build();

            // When
            tag.updatePriority(5);

            // Then
            assertThat(tag.getPriority()).isEqualTo(5);
        }

        @Test
        @DisplayName("태그 설명 업데이트")
        void updateDescription_WhenCalled_ShouldUpdateDescription() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(java.time.LocalDateTime.now())
                    .build();
            MetricTag tag = MetricTag.builder()
                    .metric(metric)
                    .name("environment")
                    .value("production")
                    .build();

            // When
            tag.updateDescription("프로덕션 환경 태그");

            // Then
            assertThat(tag.getDescription()).isEqualTo("프로덕션 환경 태그");
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
            MetricTag tag = MetricTag.builder()
                    .metric(metric1)
                    .name("environment")
                    .value("production")
                    .build();

            // When
            tag.setMetric(metric2);

            // Then
            assertThat(tag.getMetric()).isEqualTo(metric2);
        }
    }
}

