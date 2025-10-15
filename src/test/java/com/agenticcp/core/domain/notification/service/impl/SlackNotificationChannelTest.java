package com.agenticcp.core.domain.notification.service.impl;

import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SlackNotificationChannel 단위 테스트
 * 
 * <p>슬랙 알림 채널의 핵심 발송 로직을 테스트합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class SlackNotificationChannelTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SlackNotificationChannel slackChannel;

    private NotificationRequest testRequest;
    private static final String TEST_WEBHOOK_URL = "https://hooks.slack.com/services/TEST/WEBHOOK/URL";

    @BeforeEach
    void setUp() {
        // 웹훅 URL 설정 (ReflectionTestUtils 사용)
        ReflectionTestUtils.setField(slackChannel, "webhookUrl", TEST_WEBHOOK_URL);
        
        testRequest = NotificationRequest.builder()
                .notificationId("slack-test-001")
                .tenantId("test-tenant")
                .userId(1L)
                .title("테스트 알림")
                .content("슬랙 알림 테스트 내용입니다.")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.HIGH)
                .recipient("slack-channel")
                .channelId("1")
                .data(createTestData())
                .build();
    }

    /**
     * 슬랙 알림 발송 성공 테스트
     * 
     * Given: 유효한 알림 요청과 웹훅 URL
     * When: 슬랙 알림 발송 요청
     * Then: RestTemplate으로 웹훅 호출하고 SENT 상태 반환
     */
    @Test
    void send_Success() {
        // Given
        when(restTemplate.postForEntity(
                eq(TEST_WEBHOOK_URL),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("ok"));

        // When
        NotificationResponse response = slackChannel.send(testRequest);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(NotificationStatus.SENT, response.getStatus());
        assertEquals("slack-test-001", response.getNotificationId());
        assertEquals("슬랙 메시지가 성공적으로 발송되었습니다.", response.getMessage());
        assertNotNull(response.getSentAt());
        
        // RestTemplate 호출 검증
        verify(restTemplate, times(1)).postForEntity(
                eq(TEST_WEBHOOK_URL),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    /**
     * 슬랙 알림 발송 실패 - 네트워크 오류
     * 
     * Given: RestTemplate에서 예외 발생
     * When: 슬랙 알림 발송 요청
     * Then: FAILED 상태와 에러 메시지 반환
     */
    @Test
    void send_NetworkError_Failure() {
        // Given
        when(restTemplate.postForEntity(
                eq(TEST_WEBHOOK_URL),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RestClientException("Connection timeout"));

        // When
        NotificationResponse response = slackChannel.send(testRequest);

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(NotificationStatus.FAILED, response.getStatus());
        assertEquals("slack-test-001", response.getNotificationId());
        assertTrue(response.getErrorMessage().contains("Connection timeout"));
        assertNull(response.getSentAt());
    }

    /**
     * 슬랙 알림 발송 - URGENT 우선순위
     * 
     * Given: URGENT 우선순위의 알림 요청
     * When: 슬랙 알림 발송 요청
     * Then: 🔥 이모지가 포함된 메시지 발송
     */
    @Test
    void send_UrgentPriority_ContainsFireEmoji() {
        // Given
        testRequest = NotificationRequest.builder()
                .notificationId(testRequest.getNotificationId())
                .tenantId(testRequest.getTenantId())
                .userId(testRequest.getUserId())
                .title(testRequest.getTitle())
                .content(testRequest.getContent())
                .type(testRequest.getType())
                .priority(NotificationPriority.URGENT)
                .recipient(testRequest.getRecipient())
                .channelId(testRequest.getChannelId())
                .data(testRequest.getData())
                .build();
        
        when(restTemplate.postForEntity(
                eq(TEST_WEBHOOK_URL),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("ok"));

        // When
        NotificationResponse response = slackChannel.send(testRequest);

        // Then
        assertTrue(response.isSuccess());
        verify(restTemplate).postForEntity(
                eq(TEST_WEBHOOK_URL),
                argThat(entity -> {
                    @SuppressWarnings("unchecked")
                    HttpEntity<Map<String, Object>> httpEntity = (HttpEntity<Map<String, Object>>) entity;
                    Map<String, Object> body = httpEntity.getBody();
                    String text = (String) body.get("text");
                    return text.contains("🔥");
                }),
                eq(String.class)
        );
    }

    /**
     * 슬랙 알림 발송 - LOW 우선순위
     * 
     * Given: LOW 우선순위의 알림 요청
     * When: 슬랙 알림 발송 요청
     * Then: ℹ️ 이모지가 포함된 메시지 발송
     */
    @Test
    void send_LowPriority_ContainsInfoEmoji() {
        // Given
        testRequest = NotificationRequest.builder()
                .notificationId(testRequest.getNotificationId())
                .tenantId(testRequest.getTenantId())
                .userId(testRequest.getUserId())
                .title(testRequest.getTitle())
                .content(testRequest.getContent())
                .type(testRequest.getType())
                .priority(NotificationPriority.LOW)
                .recipient(testRequest.getRecipient())
                .channelId(testRequest.getChannelId())
                .data(testRequest.getData())
                .build();
        
        when(restTemplate.postForEntity(
                eq(TEST_WEBHOOK_URL),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("ok"));

        // When
        NotificationResponse response = slackChannel.send(testRequest);

        // Then
        assertTrue(response.isSuccess());
        verify(restTemplate).postForEntity(
                eq(TEST_WEBHOOK_URL),
                argThat(entity -> {
                    @SuppressWarnings("unchecked")
                    HttpEntity<Map<String, Object>> httpEntity = (HttpEntity<Map<String, Object>>) entity;
                    Map<String, Object> body = httpEntity.getBody();
                    String text = (String) body.get("text");
                    return text.contains("ℹ️");
                }),
                eq(String.class)
        );
    }

    /**
     * 슬랙 연결 테스트 성공
     * 
     * Given: 유효한 웹훅 URL
     * When: 연결 테스트 요청
     * Then: true 반환
     */
    @Test
    void testConnection_Success() {
        // Given
        when(restTemplate.postForEntity(
                eq(TEST_WEBHOOK_URL),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("ok"));

        // When
        boolean result = slackChannel.testConnection();

        // Then
        assertTrue(result);
        verify(restTemplate, times(1)).postForEntity(
                eq(TEST_WEBHOOK_URL),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    /**
     * 슬랙 연결 테스트 실패 - 웹훅 URL 미설정
     * 
     * Given: 웹훅 URL이 설정되지 않음
     * When: 연결 테스트 요청
     * Then: false 반환
     */
    @Test
    void testConnection_NoWebhookUrl_Failure() {
        // Given
        ReflectionTestUtils.setField(slackChannel, "webhookUrl", "");

        // When
        boolean result = slackChannel.testConnection();

        // Then
        assertFalse(result);
        verify(restTemplate, never()).postForEntity(any(), any(), any());
    }

    /**
     * 슬랙 연결 테스트 실패 - 네트워크 오류
     * 
     * Given: RestTemplate에서 예외 발생
     * When: 연결 테스트 요청
     * Then: false 반환
     */
    @Test
    void testConnection_NetworkError_Failure() {
        // Given
        when(restTemplate.postForEntity(
                eq(TEST_WEBHOOK_URL),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RestClientException("Network error"));

        // When
        boolean result = slackChannel.testConnection();

        // Then
        assertFalse(result);
    }

    /**
     * 채널 타입 확인
     * 
     * Given: SlackNotificationChannel
     * When: getChannelType() 호출
     * Then: "SLACK" 반환
     */
    @Test
    void getChannelType_ReturnsSlack() {
        // When
        String channelType = slackChannel.getChannelType();

        // Then
        assertEquals("SLACK", channelType);
    }

    /**
     * 채널 활성화 상태 확인 - 웹훅 URL 있음
     * 
     * Given: 웹훅 URL이 설정됨
     * When: isEnabled() 호출
     * Then: true 반환
     */
    @Test
    void isEnabled_WithWebhookUrl_ReturnsTrue() {
        // When
        boolean enabled = slackChannel.isEnabled();

        // Then
        assertTrue(enabled);
    }

    /**
     * 채널 활성화 상태 확인 - 웹훅 URL 없음
     * 
     * Given: 웹훅 URL이 설정되지 않음
     * When: isEnabled() 호출
     * Then: false 반환
     */
    @Test
    void isEnabled_WithoutWebhookUrl_ReturnsFalse() {
        // Given
        ReflectionTestUtils.setField(slackChannel, "webhookUrl", null);

        // When
        boolean enabled = slackChannel.isEnabled();

        // Then
        assertFalse(enabled);
    }

    /**
     * 채널 설정 검증 성공
     * 
     * Given: 웹훅 URL이 설정됨
     * When: validateConfiguration() 호출
     * Then: true 반환
     */
    @Test
    void validateConfiguration_WithWebhookUrl_ReturnsTrue() {
        // When
        boolean valid = slackChannel.validateConfiguration();

        // Then
        assertTrue(valid);
    }

    /**
     * 채널 설정 검증 실패
     * 
     * Given: 웹훅 URL이 설정되지 않음
     * When: validateConfiguration() 호출
     * Then: false 반환
     */
    @Test
    void validateConfiguration_WithoutWebhookUrl_ReturnsFalse() {
        // Given
        ReflectionTestUtils.setField(slackChannel, "webhookUrl", "");

        // When
        boolean valid = slackChannel.validateConfiguration();

        // Then
        assertFalse(valid);
    }

    /**
     * 슬랙 메시지에 데이터 포함 확인
     * 
     * Given: 추가 데이터가 포함된 알림 요청
     * When: 슬랙 알림 발송 요청
     * Then: 메시지에 데이터 필드 포함
     */
    @Test
    void send_WithData_ContainsDataFields() {
        // Given
        when(restTemplate.postForEntity(
                eq(TEST_WEBHOOK_URL),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("ok"));

        // When
        NotificationResponse response = slackChannel.send(testRequest);

        // Then
        assertTrue(response.isSuccess());
        verify(restTemplate).postForEntity(
                eq(TEST_WEBHOOK_URL),
                argThat(entity -> {
                    @SuppressWarnings("unchecked")
                    HttpEntity<Map<String, Object>> httpEntity = (HttpEntity<Map<String, Object>>) entity;
                    Map<String, Object> body = httpEntity.getBody();
                    assertNotNull(body.get("blocks"));
                    return true;
                }),
                eq(String.class)
        );
    }

    /**
     * 테스트용 데이터 생성
     */
    private Map<String, Object> createTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("metricName", "cpu.usage");
        data.put("metricValue", 95.5);
        data.put("thresholdValue", 90.0);
        data.put("severity", "HIGH");
        return data;
    }
}

