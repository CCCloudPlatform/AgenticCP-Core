package com.agenticcp.core.domain.notification.service.impl;

import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SlackNotificationChannel 단위 테스트
 * 
 * <p>슬랙 알림 채널의 핵심 발송 로직을 테스트합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SlackNotificationChannel 단위 테스트")
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

    @Nested
    @DisplayName("알림 발송 테스트")
    class SendNotificationTest {

        @Test
        @DisplayName("정상 발송 시 SENT 상태 반환")
        void send_WhenValidRequest_ReturnsSentStatus() {
            // Given
            when(restTemplate.postForEntity(
                    eq(TEST_WEBHOOK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok("ok"));

            // When
            NotificationResponse response = slackChannel.send(testRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(response.getNotificationId()).isEqualTo("slack-test-001");
            assertThat(response.getMessage()).isEqualTo("슬랙 메시지가 성공적으로 발송되었습니다.");
            assertThat(response.getSentAt()).isNotNull();
            
            // RestTemplate 호출 검증
            verify(restTemplate, times(1)).postForEntity(
                    eq(TEST_WEBHOOK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("네트워크 오류 시 FAILED 상태 반환")
        void send_WhenNetworkError_ReturnsFailedStatus() {
            // Given
            when(restTemplate.postForEntity(
                    eq(TEST_WEBHOOK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("Connection timeout"));

            // When
            NotificationResponse response = slackChannel.send(testRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(response.getNotificationId()).isEqualTo("slack-test-001");
            assertThat(response.getErrorMessage()).contains("Connection timeout");
            assertThat(response.getSentAt()).isNull();
        }

        @Test
        @DisplayName("URGENT 우선순위 시 🔥 이모지 포함")
        void send_WhenUrgentPriority_ContainsFireEmoji() {
            // Given
            NotificationRequest urgentRequest = NotificationRequest.builder()
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
            NotificationResponse response = slackChannel.send(urgentRequest);

            // Then
            assertThat(response.isSuccess()).isTrue();
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

        @Test
        @DisplayName("LOW 우선순위 시 ℹ️ 이모지 포함")
        void send_WhenLowPriority_ContainsInfoEmoji() {
            // Given
            NotificationRequest lowPriorityRequest = NotificationRequest.builder()
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
            NotificationResponse response = slackChannel.send(lowPriorityRequest);

            // Then
            assertThat(response.isSuccess()).isTrue();
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

        @Test
        @DisplayName("데이터 포함 시 메시지에 블록 포함")
        void send_WhenDataProvided_ContainsBlocks() {
            // Given
            when(restTemplate.postForEntity(
                    eq(TEST_WEBHOOK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok("ok"));

            // When
            NotificationResponse response = slackChannel.send(testRequest);

            // Then
            assertThat(response.isSuccess()).isTrue();
            verify(restTemplate).postForEntity(
                    eq(TEST_WEBHOOK_URL),
                    argThat(entity -> {
                        @SuppressWarnings("unchecked")
                        HttpEntity<Map<String, Object>> httpEntity = (HttpEntity<Map<String, Object>>) entity;
                        Map<String, Object> body = httpEntity.getBody();
                        assertThat(body.get("blocks")).isNotNull();
                        return true;
                    }),
                    eq(String.class)
            );
        }
    }

    @Nested
    @DisplayName("연결 테스트")
    class ConnectionTest {

        @Test
        @DisplayName("정상 연결 시 true 반환")
        void testConnection_WhenValidWebhook_ReturnsTrue() {
            // Given
            when(restTemplate.postForEntity(
                    eq(TEST_WEBHOOK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok("ok"));

            // When
            boolean result = slackChannel.testConnection();

            // Then
            assertThat(result).isTrue();
            verify(restTemplate, times(1)).postForEntity(
                    eq(TEST_WEBHOOK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("웹훅 URL 미설정 시 false 반환")
        void testConnection_WhenNoWebhookUrl_ReturnsFalse() {
            // Given
            ReflectionTestUtils.setField(slackChannel, "webhookUrl", "");

            // When
            boolean result = slackChannel.testConnection();

            // Then
            assertThat(result).isFalse();
            verify(restTemplate, never()).postForEntity(any(), any(), any());
        }

        @Test
        @DisplayName("네트워크 오류 시 false 반환")
        void testConnection_WhenNetworkError_ReturnsFalse() {
            // Given
            when(restTemplate.postForEntity(
                    eq(TEST_WEBHOOK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("Network error"));

            // When
            boolean result = slackChannel.testConnection();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("채널 정보 테스트")
    class ChannelInfoTest {

        @Test
        @DisplayName("채널 타입 반환")
        void getChannelType_WhenCalled_ReturnsSlack() {
            // When
            String channelType = slackChannel.getChannelType();

            // Then
            assertThat(channelType).isEqualTo("SLACK");
        }

        @Test
        @DisplayName("웹훅 URL 설정 시 활성화 상태 true")
        void isEnabled_WhenWebhookUrlSet_ReturnsTrue() {
            // When
            boolean enabled = slackChannel.isEnabled();

            // Then
            assertThat(enabled).isTrue();
        }

        @Test
        @DisplayName("웹훅 URL 미설정 시 활성화 상태 false")
        void isEnabled_WhenWebhookUrlNotSet_ReturnsFalse() {
            // Given
            ReflectionTestUtils.setField(slackChannel, "webhookUrl", null);

            // When
            boolean enabled = slackChannel.isEnabled();

            // Then
            assertThat(enabled).isFalse();
        }

        @Test
        @DisplayName("웹훅 URL 설정 시 설정 검증 true")
        void validateConfiguration_WhenWebhookUrlSet_ReturnsTrue() {
            // When
            boolean valid = slackChannel.validateConfiguration();

            // Then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("웹훅 URL 미설정 시 설정 검증 false")
        void validateConfiguration_WhenWebhookUrlNotSet_ReturnsFalse() {
            // Given
            ReflectionTestUtils.setField(slackChannel, "webhookUrl", "");

            // When
            boolean valid = slackChannel.validateConfiguration();

            // Then
            assertThat(valid).isFalse();
        }
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

