package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.enums.ChannelType;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import com.agenticcp.core.domain.notification.entity.NotificationChannelEntity;
import com.agenticcp.core.domain.notification.repository.NotificationChannelRepository;
import com.agenticcp.core.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationService 단위 테스트
 * 
 * <p>알림 발송 및 채널 관리 로직을 테스트합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

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

    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    private NotificationRequest testRequest;

    @BeforeEach
    void setUp() {
        // TenantContextHolder Mock 설정
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                .thenReturn("1"); // 테스트용 테넌트 키
        
        testRequest = NotificationRequest.builder()
                .notificationId("test-notification-001")
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

    @AfterEach
    void tearDown() {
        if (tenantContextHolderMock != null) {
            tenantContextHolderMock.close();
        }
    }

    @Nested
    @DisplayName("알림 발송 테스트")
    class SendNotificationTest {

        @Test
        @DisplayName("정상 발송 시 SENT 상태 반환")
        void sendNotification_WhenValidRequest_ReturnsSentStatus() {
            // Given
            NotificationResponse mockResponse = NotificationResponse.builder()
                    .notificationId(testRequest.getNotificationId())
                    .status(NotificationStatus.SENT)
                    .message("알림이 성공적으로 발송되었습니다.")
                    .success(true)
                    .build();

            when(mockChannelEntity.getChannelType()).thenReturn(ChannelType.EMAIL);
            when(channelRepository.findById(any())).thenReturn(java.util.Optional.of(mockChannelEntity));
            when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
            when(notificationChannel.send(any())).thenReturn(mockResponse);

            // When
            NotificationResponse response = notificationService.sendNotification(testRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getNotificationId()).isEqualTo(testRequest.getNotificationId());
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.SENT);
            
            verify(notificationRepository, times(2)).save(any());
            verify(notificationChannel, times(1)).send(any());
        }

        @Test
        @DisplayName("채널 없음 시 FAILED 상태 반환")
        void sendNotification_WhenChannelNotFound_ReturnsFailedStatus() {
            // Given
            when(channelRepository.findById(any())).thenReturn(java.util.Optional.empty());

            // When
            NotificationResponse response = notificationService.sendNotification(testRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(response.getErrorMessage()).contains("채널을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("채널 발송 실패 시 FAILED 상태 반환")
        void sendNotification_WhenChannelSendFailure_ReturnsFailedStatus() {
            // Given
            when(mockChannelEntity.getChannelType()).thenReturn(ChannelType.EMAIL);
            when(channelRepository.findById(any())).thenReturn(java.util.Optional.of(mockChannelEntity));
            when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
            when(notificationChannel.send(any())).thenThrow(new RuntimeException("채널 발송 실패"));

            // When
            NotificationResponse response = notificationService.sendNotification(testRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(response.getErrorMessage()).contains("채널 발송 실패");
        }
    }
}
