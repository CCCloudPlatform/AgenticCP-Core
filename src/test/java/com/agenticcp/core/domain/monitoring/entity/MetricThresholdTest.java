package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.domain.monitoring.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * MetricThreshold 엔티티 단위 테스트
 * 임계값 관리 핵심 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 */
class MetricThresholdTest {

    /**
     * 빌더 패턴 테스트 그룹
     * 시나리오 1: 정상적인 MetricThreshold 생성
     * 시나리오 2: 다양한 임계값 타입으로 생성
     * 시나리오 3: TestDataBuilder를 사용한 생성
     */
    @Nested
    @DisplayName("빌더 패턴 테스트")
    class BuilderPatternTest {

        @Test
        @DisplayName("정상적인 MetricThreshold 생성")
        void buildMetricThreshold_WhenValidData_ShouldCreateThreshold() {
            // Given
            String metricName = "cpu.usage";
            MetricThreshold.ThresholdType thresholdType = MetricThreshold.ThresholdType.WARNING;
            Double thresholdValue = 80.0;
            String operator = ">";
            String description = "CPU 사용률 경고 임계값";
            Boolean isActive = true;

            // When
            MetricThreshold threshold = MetricThreshold.builder()
                    .metricName(metricName)
                    .thresholdType(thresholdType)
                    .thresholdValue(thresholdValue)
                    .operator(operator)
                    .description(description)
                    .isActive(isActive)
                    .build();

            // Then
            assertThat(threshold.getMetricName()).isEqualTo(metricName);
            assertThat(threshold.getThresholdType()).isEqualTo(thresholdType);
            assertThat(threshold.getThresholdValue()).isEqualTo(thresholdValue);
            assertThat(threshold.getOperator()).isEqualTo(operator);
            assertThat(threshold.getDescription()).isEqualTo(description);
            assertThat(threshold.getIsActive()).isEqualTo(isActive);
        }

        @Test
        @DisplayName("다양한 임계값 타입으로 생성")
        void buildMetricThreshold_WithDifferentTypes_ShouldCreateThresholds() {
            // Given & When
            MetricThreshold warningThreshold = MetricThreshold.builder()
                    .metricName("cpu.usage")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(80.0)
                    .operator(">")
                    .description("CPU 경고 임계값")
                    .isActive(true)
                    .build();

            MetricThreshold criticalThreshold = MetricThreshold.builder()
                    .metricName("cpu.usage")
                    .thresholdType(MetricThreshold.ThresholdType.CRITICAL)
                    .thresholdValue(95.0)
                    .operator(">")
                    .description("CPU 위험 임계값")
                    .isActive(true)
                    .build();

            // Then
            assertThat(warningThreshold.getThresholdType()).isEqualTo(MetricThreshold.ThresholdType.WARNING);
            assertThat(criticalThreshold.getThresholdType()).isEqualTo(MetricThreshold.ThresholdType.CRITICAL);
        }

        @Test
        @DisplayName("TestDataBuilder를 사용한 MetricThreshold 생성")
        void buildMetricThreshold_UsingTestDataBuilder_ShouldCreateThreshold() {
            // When
            MetricThreshold threshold = TestDataBuilder.metricThresholdBuilder().build();

            // Then
            assertThat(threshold.getMetricName()).isEqualTo("cpu.usage");
            assertThat(threshold.getThresholdType()).isEqualTo(MetricThreshold.ThresholdType.WARNING);
            assertThat(threshold.getThresholdValue()).isEqualTo(80.0);
            assertThat(threshold.getOperator()).isEqualTo(">");
            assertThat(threshold.getIsActive()).isTrue();
        }
    }

    /**
     * 임계값 위반 검증 테스트 그룹
     * 시나리오 4: 임계값 위반 검증 (초과)
     * 시나리오 5: 임계값 위반 검증 (미만)
     * 시나리오 6: 임계값 위반 검증 (동일)
     * 시나리오 7: 임계값 위반 검증 (이하)
     */
    @Nested
    @DisplayName("임계값 위반 검증 테스트")
    class ThresholdViolationTest {

        @Test
        @DisplayName("임계값 위반 검증 - 초과 연산자")
        void checkViolation_WithGreaterThanOperator_ShouldDetectViolation() {
            // Given
            MetricThreshold threshold = MetricThreshold.builder()
                    .metricName("cpu.usage")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(80.0)
                    .operator(">")
                    .isActive(true)
                    .build();

            // When & Then
            assertThat(threshold.isThresholdViolated(85.0)).isTrue();  // 85 > 80
            assertThat(threshold.isThresholdViolated(75.0)).isFalse(); // 75 > 80
            assertThat(threshold.isThresholdViolated(80.0)).isFalse(); // 80 > 80
        }

        @Test
        @DisplayName("임계값 위반 검증 - 미만 연산자")
        void checkViolation_WithLessThanOperator_ShouldDetectViolation() {
            // Given
            MetricThreshold threshold = MetricThreshold.builder()
                    .metricName("disk.free")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(10.0)
                    .operator("<")
                    .isActive(true)
                    .build();

            // When & Then
            assertThat(threshold.isThresholdViolated(5.0)).isTrue();   // 5 < 10
            assertThat(threshold.isThresholdViolated(15.0)).isFalse(); // 15 < 10
            assertThat(threshold.isThresholdViolated(10.0)).isFalse(); // 10 < 10
        }

        @Test
        @DisplayName("임계값 위반 검증 - 동일 연산자")
        void checkViolation_WithEqualToOperator_ShouldDetectViolation() {
            // Given
            MetricThreshold threshold = MetricThreshold.builder()
                    .metricName("service.status")
                    .thresholdType(MetricThreshold.ThresholdType.CRITICAL)
                    .thresholdValue(0.0)
                    .operator("==")
                    .isActive(true)
                    .build();

            // When & Then
            assertThat(threshold.isThresholdViolated(0.0)).isTrue();   // 0 == 0
            assertThat(threshold.isThresholdViolated(1.0)).isFalse(); // 1 == 0
            assertThat(threshold.isThresholdViolated(-1.0)).isFalse(); // -1 == 0
        }

        @Test
        @DisplayName("임계값 위반 검증 - 이하 연산자")
        void checkViolation_WithLessThanOrEqualOperator_ShouldDetectViolation() {
            // Given
            MetricThreshold threshold = MetricThreshold.builder()
                    .metricName("memory.usage")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(90.0)
                    .operator("<=")
                    .isActive(true)
                    .build();

            // When & Then
            assertThat(threshold.isThresholdViolated(90.0)).isTrue();  // 90 <= 90
            assertThat(threshold.isThresholdViolated(85.0)).isTrue(); // 85 <= 90
            assertThat(threshold.isThresholdViolated(95.0)).isFalse(); // 95 <= 90
        }

        @Test
        @DisplayName("임계값 위반 검증 - 이상 연산자")
        void checkViolation_WithGreaterThanOrEqualOperator_ShouldDetectViolation() {
            // Given
            MetricThreshold threshold = MetricThreshold.builder()
                    .metricName("response.time")
                    .thresholdType(MetricThreshold.ThresholdType.CRITICAL)
                    .thresholdValue(1000.0)
                    .operator(">=")
                    .isActive(true)
                    .build();

            // When & Then
            assertThat(threshold.isThresholdViolated(1000.0)).isTrue(); // 1000 >= 1000
            assertThat(threshold.isThresholdViolated(1500.0)).isTrue(); // 1500 >= 1000
            assertThat(threshold.isThresholdViolated(500.0)).isFalse(); // 500 >= 1000
        }
    }

    /**
     * 활성화 상태 관리 테스트 그룹
     * 시나리오 8: 활성화된 임계값은 위반 검사 수행
     * 시나리오 9: 비활성화된 임계값은 위반 검사 수행 안함
     * 시나리오 10: 활성화 상태 변경
     */
    @Nested
    @DisplayName("활성화 상태 관리 테스트")
    class ActivationStatusTest {

        @Test
        @DisplayName("활성화된 임계값은 위반 검사 수행")
        void checkViolation_WhenActive_ShouldPerformCheck() {
            // Given
            MetricThreshold activeThreshold = MetricThreshold.builder()
                    .metricName("cpu.usage")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(80.0)
                    .operator(">")
                    .isActive(true)
                    .build();

            // When & Then
            assertThat(activeThreshold.isThresholdViolated(85.0)).isTrue();
            assertThat(activeThreshold.isThresholdViolated(75.0)).isFalse();
        }

        @Test
        @DisplayName("비활성화된 임계값은 위반 검사 수행 안함")
        void checkViolation_WhenInactive_ShouldNotPerformCheck() {
            // Given
            MetricThreshold inactiveThreshold = MetricThreshold.builder()
                    .metricName("cpu.usage")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(80.0)
                    .operator(">")
                    .isActive(false)
                    .build();

            // When & Then
            assertThat(inactiveThreshold.isThresholdViolated(85.0)).isFalse();
            assertThat(inactiveThreshold.isThresholdViolated(75.0)).isFalse();
        }

        @Test
        @DisplayName("활성화 상태 변경")
        void changeActivationStatus_ShouldUpdateStatus() {
            // Given
            MetricThreshold threshold = MetricThreshold.builder()
                    .metricName("cpu.usage")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(80.0)
                    .operator(">")
                    .isActive(false)  // 비활성화로 생성
                    .build();

            // When & Then
            assertThat(threshold.getIsActive()).isFalse();
            assertThat(threshold.isThresholdViolated(85.0)).isFalse();
        }
    }

    /**
     * equals/hashCode 테스트 그룹
     * 시나리오 11: 동일한 데이터로 생성된 MetricThreshold는 equals true
     * 시나리오 12: 다른 데이터로 생성된 MetricThreshold는 equals false
     * 시나리오 13: hashCode 일관성 검증
     */
    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("동일한 데이터로 생성된 MetricThreshold는 equals true")
        void equals_WhenSameData_ShouldReturnTrue() {
            // Given
            MetricThreshold threshold1 = TestDataBuilder.metricThresholdBuilder().build();
            MetricThreshold threshold2 = TestDataBuilder.metricThresholdBuilder().build();

            // When & Then
            assertThat(threshold1).isEqualTo(threshold2);
            assertThat(threshold1.hashCode()).isEqualTo(threshold2.hashCode());
        }

        @Test
        @DisplayName("다른 데이터로 생성된 MetricThreshold는 equals false")
        void equals_WhenDifferentData_ShouldReturnFalse() {
            // Given
            MetricThreshold threshold1 = MetricThreshold.builder()
                    .metricName("cpu.usage")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(80.0)
                    .operator(">")
                    .isActive(true)
                    .build();

            MetricThreshold threshold2 = MetricThreshold.builder()
                    .metricName("memory.usage")
                    .thresholdType(MetricThreshold.ThresholdType.CRITICAL)
                    .thresholdValue(90.0)
                    .operator(">")
                    .isActive(true)
                    .build();

            // When & Then
            assertThat(threshold1).isNotEqualTo(threshold2);
        }

        @Test
        @DisplayName("null 값과의 equals 비교")
        void equals_WhenComparedWithNull_ShouldReturnFalse() {
            // Given
            MetricThreshold threshold = TestDataBuilder.metricThresholdBuilder().build();

            // When & Then
            assertThat(threshold.equals(null)).isFalse();
        }
    }

    /**
     * toString 테스트 그룹
     * 시나리오 14: toString 메서드가 모든 필드를 포함하는지 검증
     * 시나리오 15: null 값이 포함된 경우 toString 동작 검증
     */
    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString 메서드가 모든 필드를 포함")
        void toString_ShouldContainAllFields() {
            // Given
            MetricThreshold threshold = TestDataBuilder.metricThresholdBuilder().build();

            // When
            String toString = threshold.toString();

            // Then
            assertThat(toString).contains("metricName=cpu.usage");
            assertThat(toString).contains("thresholdType=WARNING");
            assertThat(toString).contains("thresholdValue=80.0");
            assertThat(toString).contains("operator=>");
            assertThat(toString).contains("isActive=true");
        }

        @Test
        @DisplayName("null 값이 포함된 경우 toString 동작")
        void toString_WithNullValues_ShouldHandleGracefully() {
            // Given
            MetricThreshold threshold = MetricThreshold.builder()
                    .metricName("test.metric")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(50.0)
                    .operator(">")
                    .isActive(true)
                    .build();

            // When
            String toString = threshold.toString();

            // Then
            assertThat(toString).contains("metricName=test.metric");
            assertThat(toString).contains("thresholdValue=50.0");
            assertThat(toString).contains("description=null");
        }
    }

    /**
     * 비즈니스 로직 테스트 그룹
     * 시나리오 16: 임계값 업데이트
     * 시나리오 17: 연산자 변경
     * 시나리오 18: 설명 업데이트
     */
    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("임계값 업데이트")
        void updateThresholdValue_ShouldUpdateValue() {
            // Given
            MetricThreshold threshold = TestDataBuilder.metricThresholdBuilder()
                    .thresholdValue(90.0)
                    .build();

            // When & Then
            assertThat(threshold.getThresholdValue()).isEqualTo(90.0);
            assertThat(threshold.isThresholdViolated(95.0)).isTrue();  // 95 > 90
            assertThat(threshold.isThresholdViolated(85.0)).isFalse(); // 85 > 90
        }

        @Test
        @DisplayName("연산자 변경")
        void changeOperator_ShouldUpdateOperator() {
            // Given
            MetricThreshold threshold = MetricThreshold.builder()
                    .metricName("cpu.usage")
                    .thresholdType(MetricThreshold.ThresholdType.WARNING)
                    .thresholdValue(80.0)
                    .operator("<")
                    .isActive(true)
                    .build();

            // When & Then
            assertThat(threshold.getOperator()).isEqualTo("<");
            assertThat(threshold.isThresholdViolated(75.0)).isTrue();  // 75 < 80
            assertThat(threshold.isThresholdViolated(85.0)).isFalse(); // 85 < 80
        }

        @Test
        @DisplayName("설명 업데이트")
        void updateDescription_ShouldUpdateDescription() {
            // Given
            String newDescription = "Updated CPU usage threshold";
            MetricThreshold threshold = TestDataBuilder.metricThresholdBuilder()
                    .description(newDescription)
                    .build();

            // When & Then
            assertThat(threshold.getDescription()).isEqualTo(newDescription);
        }
    }
}
