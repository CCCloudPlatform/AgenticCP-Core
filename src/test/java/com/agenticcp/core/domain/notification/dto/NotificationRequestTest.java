package com.agenticcp.core.domain.notification.dto;

import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 알림 요청 DTO 테스트
 */
class NotificationRequestTest {

    /**
     * NotificationRequest 빌더 패턴 성공 테스트
     * 
     * Given: 모든 필수 필드가 포함된 알림 요청 데이터
     * When: Builder 패턴으로 NotificationRequest 생성
     * Then: 모든 필드가 올바르게 설정된 NotificationRequest 객체 생성
     */
    @Test
    void testBuilder_Success() {
        // Given
        String notificationId = "test-001";
        Long tenantId = 1L;
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
        assertEquals(notificationId, request.getNotificationId());
        assertEquals(tenantId, request.getTenantId());
        assertEquals(userId, request.getUserId());
        assertEquals(title, request.getTitle());
        assertEquals(content, request.getContent());
        assertEquals(type, request.getType());
        assertEquals(priority, request.getPriority());
        assertEquals(recipient, request.getRecipient());
        assertEquals(data, request.getData());
        assertEquals(metadata, request.getMetadata());
        assertEquals(scheduledAt, request.getScheduledAt());
    }

    /**
     * NotificationRequest 빌더 패턴 null 값 테스트
     * 
     * Given: 일부 필드에 null 값을 포함한 알림 요청 데이터
     * When: Builder 패턴으로 NotificationRequest 생성
     * Then: null 값들이 허용되고 객체가 정상적으로 생성됨
     */
    @Test
    void testBuilder_WithNullValues() {
        // When
        NotificationRequest request = NotificationRequest.builder()
                .notificationId("test-002")
                .tenantId(1L)
                .title("테스트")
                .content("내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .build();

        // Then
        assertNotNull(request);
        assertEquals("test-002", request.getNotificationId());
        assertEquals(1L, request.getTenantId());
        assertNull(request.getUserId());
        assertNull(request.getRecipient());
        assertNull(request.getData());
        assertNull(request.getMetadata());
        assertNull(request.getScheduledAt());
    }

    @Test
    void testToString() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .notificationId("test-003")
                .tenantId(1L)
                .title("테스트 알림")
                .content("테스트 내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.HIGH)
                .build();

        // When
        String toString = request.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("test-003"));
        assertTrue(toString.contains("테스트 알림"));
        assertTrue(toString.contains("ALERT"));
        assertTrue(toString.contains("HIGH"));
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        NotificationRequest request1 = NotificationRequest.builder()
                .notificationId("test-004")
                .tenantId(1L)
                .title("테스트")
                .content("내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .build();

        NotificationRequest request2 = NotificationRequest.builder()
                .notificationId("test-004")
                .tenantId(1L)
                .title("테스트")
                .content("내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .build();

        NotificationRequest request3 = NotificationRequest.builder()
                .notificationId("test-005")
                .tenantId(1L)
                .title("테스트")
                .content("내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .build();

        // Then
        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }
}
