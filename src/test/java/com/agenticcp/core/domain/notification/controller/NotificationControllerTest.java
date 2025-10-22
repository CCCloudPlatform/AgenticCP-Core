package com.agenticcp.core.domain.notification.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Disabled;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * 알림 컨트롤러 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@Disabled("Controller test disabled")
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

    /**
     * 알림 발송 API 성공 테스트
     * 
     * Given: 유효한 알림 요청 데이터
     * When: POST /api/notifications/send 요청
     * Then: 200 OK와 함께 성공 응답 반환, 자동으로 테넌트 ID 설정됨
     */
    @Test
    void testSendNotification_Success() {
        // Given
        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenReturn(testResponse);

        // When
        ResponseEntity<NotificationResponse> response = notificationController.sendNotification(testRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test-001", response.getBody().getNotificationId());
        assertEquals(NotificationStatus.SENT, response.getBody().getStatus());
        assertTrue(response.getBody().isSuccess());
        
        // 자동 테넌트 ID 설정 검증
        assertEquals("1", testRequest.getTenantId());
    }

    /**
     * 알림 발송 API 실패 테스트
     * 
     * Given: 알림 서비스에서 발송 실패 응답
     * When: POST /api/notifications/send 요청
     * Then: 400 Bad Request와 함께 실패 응답 반환
     */
    @Test
    void testSendNotification_Failure() {
        // Given
        NotificationResponse failureResponse = NotificationResponse.builder()
                .notificationId("test-001")
                .status(NotificationStatus.FAILED)
                .success(false)
                .errorMessage("발송 실패")
                .build();

        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenReturn(failureResponse);

        // When
        ResponseEntity<NotificationResponse> response = notificationController.sendNotification(testRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("발송 실패", response.getBody().getErrorMessage());
    }

    /**
     * 알림 채널 목록 조회 API 성공 테스트
     * 
     * Given: 활성화된 이메일 채널이 존재
     * When: GET /api/notifications/channels 요청
     * Then: 200 OK와 함께 채널 목록 반환
     */
    @Test
    void testGetNotificationChannels_Success() {
        // Given
        List<NotificationChannelEntity> channels = Arrays.asList(testChannel);
        when(notificationService.getActiveChannels(any(String.class)))
                .thenReturn(channels);

        // When
        ResponseEntity<List<NotificationChannelEntity>> response = notificationController.getNotificationChannels();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("테스트 이메일 채널", response.getBody().get(0).getChannelName());
        assertEquals(ChannelType.EMAIL, response.getBody().get(0).getChannelType());
    }

    /**
     * 알림 채널 생성 API 성공 테스트
     * 
     * Given: 새로운 이메일 채널 생성 요청 데이터
     * When: POST /api/notifications/channels 요청
     * Then: 200 OK와 함께 생성된 채널 정보 반환
     */
    @Test
    void testCreateNotificationChannel_Success() {
        // Given
        when(notificationService.createChannel(any(NotificationChannelEntity.class)))
                .thenReturn(testChannel);

        // When
        ResponseEntity<NotificationChannelEntity> response = notificationController.createNotificationChannel(testChannel);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("테스트 이메일 채널", response.getBody().getChannelName());
        assertEquals(ChannelType.EMAIL, response.getBody().getChannelType());
    }
}
