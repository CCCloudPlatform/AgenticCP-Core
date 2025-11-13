package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.domain.monitoring.enums.AlertStatus;
import com.agenticcp.core.domain.monitoring.enums.AlertType;
import com.agenticcp.core.domain.monitoring.enums.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Alert 엔티티 단위 테스트
 * 알림 엔티티의 비즈니스 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@DisplayName("Alert 엔티티 단위 테스트")
class AlertTest {

    @Nested
    @DisplayName("빌더 패턴 테스트")
    class BuilderPatternTest {

        @Test
        @DisplayName("정상적인 Alert 생성")
        void buildAlert_WhenValidData_ShouldCreateAlert() {
            // Given
            String tenantId = "tenant-001";
            String alertName = "CPU 사용률 경고";
            AlertType alertType = AlertType.THRESHOLD;
            Severity severity = Severity.ERROR;
            String condition = "{\"metric\":\"cpu.usage\",\"threshold\":80}";

            // When
            Alert alert = Alert.builder()
                    .tenantId(tenantId)
                    .alertName(alertName)
                    .alertType(alertType)
                    .severity(severity)
                    .condition(condition)
                    .build();

            // Then
            assertThat(alert.getTenantId()).isEqualTo(tenantId);
            assertThat(alert.getAlertName()).isEqualTo(alertName);
            assertThat(alert.getAlertType()).isEqualTo(alertType);
            assertThat(alert.getSeverity()).isEqualTo(severity);
            assertThat(alert.getCondition()).isEqualTo(condition);
            assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
            assertThat(alert.getIsEnabled()).isTrue();
            assertThat(alert.getTriggerCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("기본값이 올바르게 설정됨")
        void buildAlert_WhenDefaultValues_ShouldSetDefaults() {
            // When
            Alert alert = Alert.builder()
                    .tenantId("tenant-001")
                    .alertName("Test Alert")
                    .alertType(AlertType.THRESHOLD)
                    .severity(Severity.WARNING)
                    .condition("{}")
                    .build();

            // Then
            assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
            assertThat(alert.getIsEnabled()).isTrue();
            assertThat(alert.getTriggerCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("알림 활성화")
        void activate_WhenCalled_ShouldSetActiveStatus() {
            // Given
            Alert alert = Alert.builder()
                    .tenantId("tenant-001")
                    .alertName("Test Alert")
                    .alertType(AlertType.THRESHOLD)
                    .severity(Severity.WARNING)
                    .condition("{}")
                    .status(AlertStatus.DISABLED)
                    .isEnabled(false)
                    .build();

            // When
            alert.activate();

            // Then
            assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
            assertThat(alert.getIsEnabled()).isTrue();
        }

        @Test
        @DisplayName("알림 비활성화")
        void deactivate_WhenCalled_ShouldSetDisabledStatus() {
            // Given
            Alert alert = Alert.builder()
                    .tenantId("tenant-001")
                    .alertName("Test Alert")
                    .alertType(AlertType.THRESHOLD)
                    .severity(Severity.WARNING)
                    .condition("{}")
                    .status(AlertStatus.ACTIVE)
                    .isEnabled(true)
                    .build();

            // When
            alert.deactivate();

            // Then
            assertThat(alert.getStatus()).isEqualTo(AlertStatus.DISABLED);
            assertThat(alert.getIsEnabled()).isFalse();
        }

        @Test
        @DisplayName("알림 활성 상태 확인 - 활성 상태")
        void isActive_WhenActiveAndEnabled_ReturnsTrue() {
            // Given
            Alert alert = Alert.builder()
                    .tenantId("tenant-001")
                    .alertName("Test Alert")
                    .alertType(AlertType.THRESHOLD)
                    .severity(Severity.WARNING)
                    .condition("{}")
                    .status(AlertStatus.ACTIVE)
                    .isEnabled(true)
                    .build();

            // When
            boolean result = alert.isActive();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("알림 활성 상태 확인 - 비활성 상태")
        void isActive_WhenDisabled_ReturnsFalse() {
            // Given
            Alert alert = Alert.builder()
                    .tenantId("tenant-001")
                    .alertName("Test Alert")
                    .alertType(AlertType.THRESHOLD)
                    .severity(Severity.WARNING)
                    .condition("{}")
                    .status(AlertStatus.DISABLED)
                    .isEnabled(false)
                    .build();

            // When
            boolean result = alert.isActive();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("트리거 횟수 증가")
        void incrementTriggerCount_WhenCalled_ShouldIncrementCount() {
            // Given
            Alert alert = Alert.builder()
                    .tenantId("tenant-001")
                    .alertName("Test Alert")
                    .alertType(AlertType.THRESHOLD)
                    .severity(Severity.WARNING)
                    .condition("{}")
                    .triggerCount(5)
                    .build();

            // When
            alert.incrementTriggerCount();

            // Then
            assertThat(alert.getTriggerCount()).isEqualTo(6);
            assertThat(alert.getLastTriggered()).isNotNull();
        }

        @Test
        @DisplayName("트리거 횟수 증가 - 초기값이 null인 경우")
        void incrementTriggerCount_WhenCountIsNull_ShouldSetToOne() {
            // Given
            Alert alert = Alert.builder()
                    .tenantId("tenant-001")
                    .alertName("Test Alert")
                    .alertType(AlertType.THRESHOLD)
                    .severity(Severity.WARNING)
                    .condition("{}")
                    .triggerCount(null)
                    .build();

            // When
            alert.incrementTriggerCount();

            // Then
            assertThat(alert.getTriggerCount()).isEqualTo(1);
            assertThat(alert.getLastTriggered()).isNotNull();
        }

        @Test
        @DisplayName("트리거 횟수 리셋")
        void resetTriggerCount_WhenCalled_ShouldResetCount() {
            // Given
            LocalDateTime lastTriggered = LocalDateTime.now().minusHours(1);
            Alert alert = Alert.builder()
                    .tenantId("tenant-001")
                    .alertName("Test Alert")
                    .alertType(AlertType.THRESHOLD)
                    .severity(Severity.WARNING)
                    .condition("{}")
                    .triggerCount(10)
                    .lastTriggered(lastTriggered)
                    .build();

            // When
            alert.resetTriggerCount();

            // Then
            assertThat(alert.getTriggerCount()).isEqualTo(0);
            assertThat(alert.getLastTriggered()).isNull();
        }
    }
}

