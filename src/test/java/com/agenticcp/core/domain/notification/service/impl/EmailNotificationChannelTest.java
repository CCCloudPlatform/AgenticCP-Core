package com.agenticcp.core.domain.notification.service.impl;

import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 이메일 알림 채널 테스트
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationChannel emailChannel;

    private NotificationRequest testRequest;

    @BeforeEach
    void setUp() {
        emailChannel = new EmailNotificationChannel(mailSender);
        
        testRequest = NotificationRequest.builder()
                .notificationId("test-email-001")
                .tenantId("1")
                .userId(1L)
                .title("테스트 이메일")
                .content("이것은 테스트 이메일입니다.")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .recipient("test@example.com")
                .data(Map.of("metric", "cpu.usage", "value", "85.5"))
                .metadata(Map.of("source", "monitoring"))
                .build();
    }

    /**
     * 채널 타입 반환 테스트
     * 
     * Given: EmailNotificationChannel 인스턴스
     * When: 채널 타입 조회
     * Then: "EMAIL" 문자열 반환
     */
    @Test
    void getChannelType_ReturnsEmail() {
        // When
        String channelType = emailChannel.getChannelType();

        // Then
        assertEquals("EMAIL", channelType);
    }

    /**
     * 이메일 채널 활성화 상태 테스트
     * 
     * Given: JavaMailSender가 설정된 이메일 채널
     * When: 채널 활성화 상태 조회
     * Then: true 반환
     */
    @Test
    void isEnabled_WithMailSender_ReturnsTrue() {
        // When
        boolean enabled = emailChannel.isEnabled();

        // Then
        assertTrue(enabled);
    }

    /**
     * 이메일 발송 성공 테스트
     * 
     * Given: 유효한 알림 요청과 정상 작동하는 MailSender
     * When: 이메일 발송 요청
     * Then: 이메일이 성공적으로 발송되고 SENT 상태 반환
     */
    @Test
    void send_Success() {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        NotificationResponse response = emailChannel.send(testRequest);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(testRequest.getNotificationId(), response.getNotificationId());
        assertNotNull(response.getSentAt());
        
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    /**
     * 이메일 발송 실패 테스트
     * 
     * Given: MailSender에서 SMTP 서버 연결 실패 예외 발생
     * When: 이메일 발송 요청
     * Then: FAILED 상태와 에러 메시지가 포함된 응답 반환
     */
    @Test
    void send_MailSenderThrowsException_ReturnsFailureResponse() {
        // Given
        doThrow(new RuntimeException("SMTP 서버 연결 실패")).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        NotificationResponse response = emailChannel.send(testRequest);

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(testRequest.getNotificationId(), response.getNotificationId());
        assertTrue(response.getErrorMessage().contains("SMTP 서버 연결 실패"));
    }

    @Test
    void testConnection_Success() {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        boolean result = emailChannel.testConnection();

        // Then
        assertTrue(result);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testConnection_Failure() {
        // Given
        doThrow(new RuntimeException("연결 테스트 실패")).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        boolean result = emailChannel.testConnection();

        // Then
        assertFalse(result);
    }

    @Test
    void validateConfiguration_WithMailSender_ReturnsTrue() {
        // When
        boolean valid = emailChannel.validateConfiguration();

        // Then
        assertTrue(valid);
    }
}
