package com.agenticcp.core.domain.notification.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.entity.NotificationChannelEntity;
import com.agenticcp.core.domain.notification.enums.ChannelType;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import com.agenticcp.core.domain.notification.service.MonitoringNotificationService;
import com.agenticcp.core.domain.notification.service.NotificationService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * NotificationController 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController 단위 테스트")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private MonitoringNotificationService monitoringNotificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    private NotificationRequest testRequest;
    private NotificationResponse testResponse;
    private NotificationChannelEntity testChannel;

    @BeforeEach
    void setUp() {
        // TenantContextHolder Mock 설정
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                .thenReturn("1"); // 테스트용 테넌트 키
        
        testRequest = NotificationRequest.builder()
                .notificationId("test-001")
                .title("테스트 알림")
                .content("테스트 내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .recipient("test@example.com")
                .build();

        testResponse = NotificationResponse.builder()
                .notificationId("test-001")
                .status(NotificationStatus.SENT)
                .success(true)
                .message("알림이 성공적으로 발송되었습니다.")
                .sentAt(LocalDateTime.now())
                .build();

        testChannel = NotificationChannelEntity.builder()
                .tenantId("1")
                .channelName("테스트 이메일 채널")
                .channelType(ChannelType.EMAIL)
                .isActive(true)
                .build();
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
        @DisplayName("알림 발송 성공")
        void sendNotification_WhenSuccess_ReturnsCreated() {
            // Given
            when(notificationService.sendNotification(any(NotificationRequest.class)))
                    .thenReturn(testResponse);

            // When
            ResponseEntity<ApiResponse<NotificationResponse>> response = 
                    notificationController.sendNotification(testRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().getNotificationId()).isEqualTo("test-001");
            assertThat(response.getBody().getData().getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(response.getBody().getData().isSuccess()).isTrue();
            assertThat(testRequest.getTenantId()).isEqualTo("1");
        }

        @Test
        @DisplayName("알림 발송 실패")
        void sendNotification_WhenFailure_ReturnsBadRequest() {
            // Given
            NotificationResponse failureResponse = NotificationResponse.builder()
                    .notificationId("test-001")
                    .status(NotificationStatus.FAILED)
                    .success(false)
                    .message("발송 실패")
                    .build();

            when(notificationService.sendNotification(any(NotificationRequest.class)))
                    .thenReturn(failureResponse);

            // When
            ResponseEntity<ApiResponse<NotificationResponse>> response = 
                    notificationController.sendNotification(testRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("알림 히스토리 조회 테스트")
    class GetNotificationHistoryTest {

        @Test
        @DisplayName("알림 히스토리 조회 성공")
        void getNotificationHistory_WhenCalled_ReturnsHistory() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<com.agenticcp.core.domain.notification.entity.Notification> historyPage = 
                    new PageImpl<>(List.of(), pageable, 0);
            
            when(notificationService.getNotificationHistory(anyString(), any(Pageable.class)))
                    .thenReturn(historyPage);

            // When
            ResponseEntity<ApiResponse<Page<com.agenticcp.core.domain.notification.entity.Notification>>> response = 
                    notificationController.getNotificationHistory(null, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
        }

        @Test
        @DisplayName("사용자별 알림 히스토리 조회 성공")
        void getNotificationHistory_WhenUserIdProvided_ReturnsUserHistory() {
            // Given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            Page<com.agenticcp.core.domain.notification.entity.Notification> historyPage = 
                    new PageImpl<>(List.of(), pageable, 0);
            
            when(notificationService.getNotificationHistoryByUser(anyString(), anyLong(), any(Pageable.class)))
                    .thenReturn(historyPage);

            // When
            ResponseEntity<ApiResponse<Page<com.agenticcp.core.domain.notification.entity.Notification>>> response = 
                    notificationController.getNotificationHistory(userId, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("알림 채널 관리 테스트")
    class NotificationChannelTest {

        @Test
        @DisplayName("알림 채널 목록 조회 성공")
        void getNotificationChannels_WhenCalled_ReturnsChannelList() {
            // Given
            List<NotificationChannelEntity> channels = Arrays.asList(testChannel);
            when(notificationService.getActiveChannels(anyString()))
                    .thenReturn(channels);

            // When
            ResponseEntity<ApiResponse<List<NotificationChannelEntity>>> response = 
                    notificationController.getNotificationChannels();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getChannelName()).isEqualTo("테스트 이메일 채널");
            assertThat(response.getBody().getData().get(0).getChannelType()).isEqualTo(ChannelType.EMAIL);
        }

        @Test
        @DisplayName("알림 채널 생성 성공")
        void createNotificationChannel_WhenCalled_ReturnsCreated() {
            // Given
            when(notificationService.createChannel(any(NotificationChannelEntity.class)))
                    .thenReturn(testChannel);

            // When
            ResponseEntity<ApiResponse<NotificationChannelEntity>> response = 
                    notificationController.createNotificationChannel(testChannel);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().getChannelName()).isEqualTo("테스트 이메일 채널");
            assertThat(response.getBody().getData().getChannelType()).isEqualTo(ChannelType.EMAIL);
        }

        @Test
        @DisplayName("알림 채널 수정 성공")
        void updateNotificationChannel_WhenCalled_ReturnsUpdated() {
            // Given
            Long channelId = 1L;
            when(notificationService.updateChannel(anyLong(), any(NotificationChannelEntity.class)))
                    .thenReturn(testChannel);

            // When
            ResponseEntity<ApiResponse<NotificationChannelEntity>> response = 
                    notificationController.updateNotificationChannel(channelId, testChannel);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isNotNull();
        }

        @Test
        @DisplayName("알림 채널 삭제 성공")
        void deleteNotificationChannel_WhenCalled_ReturnsNoContent() {
            // Given
            Long channelId = 1L;

            // When
            ResponseEntity<Void> response = 
                    notificationController.deleteNotificationChannel(channelId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();
        }
    }
}
