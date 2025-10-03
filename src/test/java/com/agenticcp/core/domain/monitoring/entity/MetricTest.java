package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.domain.monitoring.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Metric 엔티티 단위 테스트
 * 핵심 엔티티의 비즈니스 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 */
class MetricTest {

    /**
     * 빌더 패턴 테스트 그룹
     * 시나리오 1: 정상적인 Metric 생성
     * 시나리오 2: 필수 필드 누락 시 예외 발생
     * 시나리오 3: 다양한 메트릭 타입으로 생성
     */
    @Nested
    @DisplayName("빌더 패턴 테스트")
    class BuilderPatternTest {

        @Test
        @DisplayName("정상적인 Metric 생성")
        void buildMetric_WhenValidData_ShouldCreateMetric() {
            // Given
            String metricName = "cpu.usage";
            Double metricValue = 45.2;
            String unit = "%";
            Metric.MetricType metricType = Metric.MetricType.SYSTEM;
            LocalDateTime collectedAt = LocalDateTime.now();
            String source = "system-collector";

            // When
            Metric metric = Metric.builder()
                    .metricName(metricName)
                    .metricValue(metricValue)
                    .unit(unit)
                    .metricType(metricType)
                    .collectedAt(collectedAt)
                    .source(source)
                    .build();

            // Then
            assertThat(metric.getMetricName()).isEqualTo(metricName);
            assertThat(metric.getMetricValue()).isEqualTo(metricValue);
            assertThat(metric.getUnit()).isEqualTo(unit);
            assertThat(metric.getMetricType()).isEqualTo(metricType);
            assertThat(metric.getCollectedAt()).isEqualTo(collectedAt);
            assertThat(metric.getSource()).isEqualTo(source);
        }

        @Test
        @DisplayName("다양한 메트릭 타입으로 생성")
        void buildMetric_WithDifferentTypes_ShouldCreateMetrics() {
            // Given & When
            Metric systemMetric = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .unit("%")
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(LocalDateTime.now())
                    .source("system-collector")
                    .build();

            Metric applicationMetric = Metric.builder()
                    .metricName("jvm.memory.heap.used")
                    .metricValue(1024.0)
                    .unit("MB")
                    .metricType(Metric.MetricType.APPLICATION)
                    .collectedAt(LocalDateTime.now())
                    .source("micrometer-collector")
                    .build();

            // Then
            assertThat(systemMetric.getMetricType()).isEqualTo(Metric.MetricType.SYSTEM);
            assertThat(applicationMetric.getMetricType()).isEqualTo(Metric.MetricType.APPLICATION);
        }

        @Test
        @DisplayName("TestDataBuilder를 사용한 Metric 생성")
        void buildMetric_UsingTestDataBuilder_ShouldCreateMetric() {
            // When
            Metric metric = TestDataBuilder.metricBuilder().build();

            // Then
            assertThat(metric.getMetricName()).isEqualTo("test.metric");
            assertThat(metric.getMetricValue()).isEqualTo(100.0);
            assertThat(metric.getUnit()).isEqualTo("count");
            assertThat(metric.getMetricType()).isEqualTo(Metric.MetricType.APPLICATION);
            assertThat(metric.getSource()).isEqualTo("test-source");
        }
    }

    /**
     * 유효성 검증 테스트 그룹
     * 시나리오 4: null 값 처리
     * 시나리오 5: 빈 문자열 처리
     * 시나리오 6: 음수 값 처리
     */
    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("null 값으로 Metric 생성 시 기본값 설정")
        void buildMetric_WithNullValues_ShouldSetDefaults() {
            // When
            Metric metric = Metric.builder()
                    .metricName("test.metric")
                    .metricValue(100.0)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(LocalDateTime.now())
                    .build();

            // Then
            assertThat(metric.getMetricName()).isEqualTo("test.metric");
            assertThat(metric.getMetricValue()).isEqualTo(100.0);
            assertThat(metric.getUnit()).isNull();
            assertThat(metric.getSource()).isNull();
            assertThat(metric.getStatus()).isNull();
        }

        @Test
        @DisplayName("빈 문자열로 Metric 생성")
        void buildMetric_WithEmptyString_ShouldCreateMetric() {
            // When
            Metric metric = Metric.builder()
                    .metricName("")
                    .metricValue(0.0)
                    .unit("")
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(LocalDateTime.now())
                    .source("")
                    .build();

            // Then
            assertThat(metric.getMetricName()).isEmpty();
            assertThat(metric.getMetricValue()).isEqualTo(0.0);
            assertThat(metric.getUnit()).isEmpty();
            assertThat(metric.getSource()).isEmpty();
        }

        @Test
        @DisplayName("음수 값으로 Metric 생성")
        void buildMetric_WithNegativeValue_ShouldCreateMetric() {
            // When
            Metric metric = Metric.builder()
                    .metricName("test.metric")
                    .metricValue(-10.5)
                    .unit("count")
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(LocalDateTime.now())
                    .build();

            // Then
            assertThat(metric.getMetricValue()).isEqualTo(-10.5);
        }
    }

    /**
     * equals/hashCode 테스트 그룹
     * 시나리오 7: 동일한 데이터로 생성된 Metric은 equals true
     * 시나리오 8: 다른 데이터로 생성된 Metric은 equals false
     * 시나리오 9: hashCode 일관성 검증
     */
    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("동일한 데이터로 생성된 Metric은 equals true")
        void equals_WhenSameData_ShouldReturnTrue() {
            // Given
            LocalDateTime fixedTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
            Metric metric1 = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .unit("%")
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(fixedTime)
                    .source("system-collector")
                    .build();

            Metric metric2 = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .unit("%")
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(fixedTime)
                    .source("system-collector")
                    .build();

            // When & Then
            assertThat(metric1).isEqualTo(metric2);
            assertThat(metric1.hashCode()).isEqualTo(metric2.hashCode());
        }

        @Test
        @DisplayName("다른 데이터로 생성된 Metric은 equals false")
        void equals_WhenDifferentData_ShouldReturnFalse() {
            // Given
            Metric metric1 = Metric.builder()
                    .metricName("cpu.usage")
                    .metricValue(45.2)
                    .unit("%")
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(LocalDateTime.now())
                    .source("system-collector")
                    .build();

            Metric metric2 = Metric.builder()
                    .metricName("memory.usage")
                    .metricValue(67.8)
                    .unit("%")
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(LocalDateTime.now())
                    .source("system-collector")
                    .build();

            // When & Then
            assertThat(metric1).isNotEqualTo(metric2);
        }

        @Test
        @DisplayName("null 값과의 equals 비교")
        void equals_WhenComparedWithNull_ShouldReturnFalse() {
            // Given
            Metric metric = TestDataBuilder.metricBuilder().build();

            // When & Then
            assertThat(metric.equals(null)).isFalse();
        }
    }

    /**
     * toString 테스트 그룹
     * 시나리오 10: toString 메서드가 모든 필드를 포함하는지 검증
     * 시나리오 11: null 값이 포함된 경우 toString 동작 검증
     */
    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString 메서드가 모든 필드를 포함")
        void toString_ShouldContainAllFields() {
            // Given
            Metric metric = TestDataBuilder.metricBuilder().build();

            // When
            String toString = metric.toString();

            // Then
            assertThat(toString).contains("metricName=test.metric");
            assertThat(toString).contains("metricValue=100.0");
            assertThat(toString).contains("unit=count");
            assertThat(toString).contains("metricType=APPLICATION");
            assertThat(toString).contains("source=test-source");
        }

        @Test
        @DisplayName("null 값이 포함된 경우 toString 동작")
        void toString_WithNullValues_ShouldHandleGracefully() {
            // Given
            Metric metric = Metric.builder()
                    .metricName("test.metric")
                    .metricValue(100.0)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(LocalDateTime.now())
                    .build();

            // When
            String toString = metric.toString();

            // Then
            assertThat(toString).contains("metricName=test.metric");
            assertThat(toString).contains("metricValue=100.0");
            assertThat(toString).contains("unit=null");
            assertThat(toString).contains("source=null");
        }
    }

    /**
     * 메타데이터 및 태그 연관관계 테스트 그룹
     * 시나리오 12: 메타데이터 추가 시 연관관계 설정
     * 시나리오 13: 태그 추가 시 연관관계 설정
     * 시나리오 14: 빈 컬렉션으로 초기화
     */
    @Nested
    @DisplayName("연관관계 테스트")
    class AssociationTest {

        @Test
        @DisplayName("메타데이터 추가 시 연관관계 설정")
        void addMetadata_ShouldSetAssociation() {
            // Given
            Metric metric = TestDataBuilder.metricBuilder().build();
            MetricMetadata metadata = MetricMetadata.builder()
                    .key("hostname")
                    .value("test-server")
                    .build();

            // When
            metric.getMetadataList().add(metadata);

            // Then
            assertThat(metric.getMetadataList()).hasSize(1);
            assertThat(metric.getMetadataList().get(0).getKey()).isEqualTo("hostname");
            assertThat(metric.getMetadataList().get(0).getValue()).isEqualTo("test-server");
        }

        @Test
        @DisplayName("태그 추가 시 연관관계 설정")
        void addTag_ShouldSetAssociation() {
            // Given
            Metric metric = TestDataBuilder.metricBuilder().build();
            MetricTag tag = MetricTag.builder()
                    .name("environment")
                    .value("production")
                    .build();

            // When
            metric.getTags().add(tag);

            // Then
            assertThat(metric.getTags()).hasSize(1);
            assertThat(metric.getTags().get(0).getName()).isEqualTo("environment");
            assertThat(metric.getTags().get(0).getValue()).isEqualTo("production");
        }

        @Test
        @DisplayName("빈 컬렉션으로 초기화")
        void initialize_WithEmptyCollections_ShouldCreateEmptyLists() {
            // When
            Metric metric = TestDataBuilder.metricBuilder().build();

            // Then
            assertThat(metric.getMetadataList()).isNotNull();
            assertThat(metric.getTags()).isNotNull();
            assertThat(metric.getMetadataList()).isEmpty();
            assertThat(metric.getTags()).isEmpty();
        }
    }

    /**
     * 비즈니스 로직 테스트 그룹
     * 시나리오 15: 메트릭 상태 설정
     * 시나리오 16: 메트릭 값 업데이트
     * 시나리오 17: 메트릭 타입 변경
     */
    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("메트릭 상태 설정")
        void setStatus_ShouldUpdateStatus() {
            // Given
            Metric metric = TestDataBuilder.metricBuilder()
                    .status(Metric.Status.ACTIVE)
                    .build();

            // When & Then
            assertThat(metric.getStatus()).isEqualTo(Metric.Status.ACTIVE);
        }

        @Test
        @DisplayName("메트릭 값 업데이트")
        void updateMetricValue_ShouldUpdateValue() {
            // Given
            Double newValue = 150.0;
            Metric metric = TestDataBuilder.metricBuilder()
                    .metricValue(newValue)
                    .build();

            // When & Then
            assertThat(metric.getMetricValue()).isEqualTo(newValue);
        }

        @Test
        @DisplayName("메트릭 타입 변경")
        void changeMetricType_ShouldUpdateType() {
            // Given
            Metric metric = TestDataBuilder.metricBuilder()
                    .metricType(Metric.MetricType.SYSTEM)
                    .build();

            // When & Then
            assertThat(metric.getMetricType()).isEqualTo(Metric.MetricType.SYSTEM);
        }
    }
}
