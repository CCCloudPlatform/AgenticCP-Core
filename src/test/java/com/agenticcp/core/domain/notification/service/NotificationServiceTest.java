package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.enums.ChannelType;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import com.agenticcp.core.domain.notification.entity.NotificationChannelEntity;
import com.agenticcp.core.domain.notification.repository.NotificationChannelRepository;
import com.agenticcp.core.domain.notification.repository.NotificationRepository;
import com.agenticcp.core.domain.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 알림 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationChannelRepository channelRepository;

    @Mock
    private NotificationChannelFactory channelFactory;

    @Mock
    private NotificationChannel notificationChannel;

    @Mock
    private NotificationChannelEntity mockChannelEntity;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = NotificationRequest.builder()
                .notificationId("test-notification-001")
                .tenantId(1L)
                .userId(1L)
                .title("테스트 알림")
                .content("이것은 테스트 알림입니다.")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .recipient("test@example.com")
                .channelId("1")
                .data(Map.of("key", "value"))
                .metadata(Map.of("source", "test"))
                .build();

        // Mock NotificationRepository 설정 - save 메서드가 저장된 엔티티를 반환하도록 설정
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * 알림 발송 성공 테스트
     * 
     * Given: 유효한 알림 요청과 활성화된 이메일 채널
     * When: 알림 발송 요청
     * Then: 알림이 성공적으로 발송되고 SENT 상태로 반환됨
     */
    @Test
    void sendNotification_Success() {
        // Given
        NotificationResponse mockResponse = NotificationResponse.builder()
                .notificationId(testRequest.getNotificationId())
                .status(com.agenticcp.core.domain.notification.enums.NotificationStatus.SENT)
                .message("알림이 성공적으로 발송되었습니다.")
                .success(true)
                .build();

        // Mock NotificationChannelEntity 설정
        when(mockChannelEntity.getChannelType()).thenReturn(ChannelType.EMAIL);
        
        when(channelRepository.findById(any())).thenReturn(java.util.Optional.of(mockChannelEntity));
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.send(any())).thenReturn(mockResponse);

        // When
        NotificationResponse response = notificationService.sendNotification(testRequest);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(testRequest.getNotificationId(), response.getNotificationId());
        
               verify(notificationRepository, times(2)).save(any()); // 생성 + 상태 업데이트
               verify(notificationChannel, times(1)).send(any());
    }

    /**
     * 채널을 찾을 수 없는 경우 테스트
     * 
     * Given: 존재하지 않는 채널 ID로 알림 요청
     * When: 알림 발송 요청
     * Then: FAILED 상태와 "채널을 찾을 수 없습니다" 에러 메시지 반환
     */
    @Test
    void sendNotification_ChannelNotFound() {
        // Given
        when(channelRepository.findById(any())).thenReturn(java.util.Optional.empty());

        // When
        NotificationResponse response = notificationService.sendNotification(testRequest);

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(com.agenticcp.core.domain.notification.enums.NotificationStatus.FAILED, response.getStatus());
        assertTrue(response.getErrorMessage().contains("채널을 찾을 수 없습니다"));
    }

    /**
     * 채널 발송 실패 테스트
     * 
     * Given: 유효한 채널이지만 발송 중 예외 발생
     * When: 알림 발송 요청
     * Then: FAILED 상태와 채널 발송 실패 에러 메시지 반환
     */
    @Test
    void sendNotification_ChannelSendFailure() {
        // Given
        // Mock NotificationChannelEntity 설정
        when(mockChannelEntity.getChannelType()).thenReturn(ChannelType.EMAIL);
        
        when(channelRepository.findById(any())).thenReturn(java.util.Optional.of(mockChannelEntity));
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.send(any())).thenThrow(new RuntimeException("채널 발송 실패"));

        // When
        NotificationResponse response = notificationService.sendNotification(testRequest);

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(com.agenticcp.core.domain.notification.enums.NotificationStatus.FAILED, response.getStatus());
        assertTrue(response.getErrorMessage().contains("채널 발송 실패"));
    }
}
