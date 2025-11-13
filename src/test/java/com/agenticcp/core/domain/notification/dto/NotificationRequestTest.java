package com.agenticcp.core.domain.notification.dto;

import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NotificationRequest 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@DisplayName("NotificationRequest 단위 테스트")
class NotificationRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("빌더 패턴 테스트")
    class BuilderTest {

    @Test
        @DisplayName("빌더 패턴으로 모든 필드 설정 성공")
        void builder_WhenAllFieldsSet_ReturnsRequestWithAllFields() {
        // Given
        String notificationId = "test-001";
        String tenantId = "1";
        Long userId = 100L;
        String title = "테스트 알림";
        String content = "테스트 내용";
        NotificationType type = NotificationType.ALERT;
        NotificationPriority priority = NotificationPriority.MEDIUM;
        String recipient = "test@example.com";
        Map<String, Object> data = Map.of("key", "value");
        Map<String, Object> metadata = Map.of("source", "test");
        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(1);

        // When
        NotificationRequest request = NotificationRequest.builder()
                .notificationId(notificationId)
                .tenantId(tenantId)
                .userId(userId)
                .title(title)
                .content(content)
                .type(type)
                .priority(priority)
                .recipient(recipient)
                .data(data)
                .metadata(metadata)
                .scheduledAt(scheduledAt)
                .build();

        // Then
            assertThat(request.getNotificationId()).isEqualTo(notificationId);
            assertThat(request.getTenantId()).isEqualTo(tenantId);
            assertThat(request.getUserId()).isEqualTo(userId);
            assertThat(request.getTitle()).isEqualTo(title);
            assertThat(request.getContent()).isEqualTo(content);
            assertThat(request.getType()).isEqualTo(type);
            assertThat(request.getPriority()).isEqualTo(priority);
            assertThat(request.getRecipient()).isEqualTo(recipient);
            assertThat(request.getData()).isEqualTo(data);
            assertThat(request.getMetadata()).isEqualTo(metadata);
            assertThat(request.getScheduledAt()).isEqualTo(scheduledAt);
        }

    @Test
        @DisplayName("빌더 패턴으로 선택적 필드 null 허용")
        void builder_WhenOptionalFieldsNull_ReturnsRequestWithNullFields() {
        // When
        NotificationRequest request = NotificationRequest.builder()
                .notificationId("test-002")
                .tenantId("1")
                .title("테스트")
                .content("내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .build();

        // Then
            assertThat(request).isNotNull();
            assertThat(request.getNotificationId()).isEqualTo("test-002");
            assertThat(request.getTenantId()).isEqualTo("1");
            assertThat(request.getUserId()).isNull();
            assertThat(request.getRecipient()).isNull();
            assertThat(request.getData()).isNull();
            assertThat(request.getMetadata()).isNull();
            assertThat(request.getScheduledAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation 테스트")
    class ValidationTest {

        @Test
        @DisplayName("유효한 데이터로 검증 성공")
        void validate_WhenValidData_ShouldPass() {
            // Given
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId("1")
                    .title("테스트 알림")
                    .content("테스트 내용")
                    .type(NotificationType.ALERT)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("테넌트 ID가 null일 때 검증 실패")
        void validate_WhenTenantIdIsNull_ShouldFail() {
            // Given
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId(null)
                    .title("테스트 알림")
                    .content("테스트 내용")
                    .type(NotificationType.ALERT)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("테넌트 ID는 필수입니다");
        }

        @Test
        @DisplayName("제목이 null일 때 검증 실패")
        void validate_WhenTitleIsNull_ShouldFail() {
            // Given
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId("1")
                    .title(null)
                    .content("테스트 내용")
                    .type(NotificationType.ALERT)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("알림 제목은 필수입니다");
        }

        @Test
        @DisplayName("내용이 null일 때 검증 실패")
        void validate_WhenContentIsNull_ShouldFail() {
            // Given
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId("1")
                    .title("테스트 알림")
                    .content(null)
                    .type(NotificationType.ALERT)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("알림 내용은 필수입니다");
        }

        @Test
        @DisplayName("타입이 null일 때 검증 실패")
        void validate_WhenTypeIsNull_ShouldFail() {
            // Given
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId("1")
                    .title("테스트 알림")
                    .content("테스트 내용")
                    .type(null)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("알림 타입은 필수입니다");
        }

        @Test
        @DisplayName("우선순위가 null일 때 검증 실패")
        void validate_WhenPriorityIsNull_ShouldFail() {
            // Given
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId("1")
                    .title("테스트 알림")
                    .content("테스트 내용")
                    .type(NotificationType.ALERT)
                    .priority(null)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("알림 우선순위는 필수입니다");
        }

        @Test
        @DisplayName("사용자 ID가 음수일 때 검증 실패")
        void validate_WhenUserIdIsNegative_ShouldFail() {
            // Given
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId("1")
                    .title("테스트 알림")
                    .content("테스트 내용")
                    .type(NotificationType.ALERT)
                    .priority(NotificationPriority.MEDIUM)
                    .userId(-1L)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("사용자 ID는 양수여야 합니다");
        }

        @Test
        @DisplayName("재시도 횟수가 음수일 때 검증 실패")
        void validate_WhenRetryCountIsNegative_ShouldFail() {
            // Given
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId("1")
                    .title("테스트 알림")
                    .content("테스트 내용")
                    .type(NotificationType.ALERT)
                    .priority(NotificationPriority.MEDIUM)
                    .retryCount(-1)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("재시도 횟수는 0 이상이어야 합니다");
        }

        @Test
        @DisplayName("제목이 최대 길이 초과 시 검증 실패")
        void validate_WhenTitleExceedsMaxLength_ShouldFail() {
            // Given
            String longTitle = "a".repeat(256);
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId("1")
                    .title(longTitle)
                    .content("테스트 내용")
                    .type(NotificationType.ALERT)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("알림 제목은 255자를 초과할 수 없습니다");
        }

        @Test
        @DisplayName("내용이 최대 길이 초과 시 검증 실패")
        void validate_WhenContentExceedsMaxLength_ShouldFail() {
            // Given
            String longContent = "a".repeat(5001);
            NotificationRequest request = NotificationRequest.builder()
                    .tenantId("1")
                    .title("테스트 알림")
                    .content(longContent)
                    .type(NotificationType.ALERT)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            // When
            Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("알림 내용은 5000자를 초과할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("Object 메서드 테스트")
    class ObjectMethodTest {

    @Test
        @DisplayName("toString 메서드 테스트")
        void toString_WhenCalled_ShouldContainFields() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .notificationId("test-003")
                .tenantId("1")
                .title("테스트 알림")
                .content("테스트 내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.HIGH)
                .build();

        // When
        String toString = request.toString();

        // Then
            assertThat(toString).isNotNull();
            assertThat(toString).contains("test-003");
            assertThat(toString).contains("테스트 알림");
            assertThat(toString).contains("ALERT");
            assertThat(toString).contains("HIGH");
    }

    @Test
        @DisplayName("equals와 hashCode 테스트")
        void equalsAndHashCode_WhenSameFields_ShouldBeEqual() {
        // Given
        NotificationRequest request1 = NotificationRequest.builder()
                .notificationId("test-004")
                .tenantId("1")
                .title("테스트")
                .content("내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .build();

        NotificationRequest request2 = NotificationRequest.builder()
                .notificationId("test-004")
                .tenantId("1")
                .title("테스트")
                .content("내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .build();

        NotificationRequest request3 = NotificationRequest.builder()
                .notificationId("test-005")
                .tenantId("1")
                .title("테스트")
                .content("내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .build();

        // Then
            assertThat(request1).isEqualTo(request2);
            assertThat(request1).isNotEqualTo(request3);
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
            assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
        }
    }
}
