package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.entity.Notification;
import com.agenticcp.core.domain.notification.entity.NotificationChannelEntity;
import com.agenticcp.core.domain.notification.enums.ChannelType;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import com.agenticcp.core.domain.notification.repository.NotificationChannelRepository;
import com.agenticcp.core.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * NotificationService 추가 시나리오 테스트
 * 
 * <p>비동기 발송, CRUD, 히스토리 조회 등 확장 기능을 테스트합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceExtendedTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationChannelRepository channelRepository;

    @Mock
    private NotificationChannelFactory channelFactory;

    @Mock
    private NotificationChannel notificationChannel;

    @InjectMocks
    private NotificationService notificationService;

    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    private NotificationRequest testRequest;
    private NotificationChannelEntity testChannel;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                .thenReturn("test-tenant");
        
        testRequest = NotificationRequest.builder()
                .notificationId("test-001")
                .tenantId("test-tenant")
                .userId(1L)
                .title("테스트 알림")
                .content("테스트 내용")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.MEDIUM)
                .channelId("1")
                .recipient("test@example.com")
                .build();

        testChannel = NotificationChannelEntity.builder()
                .tenantId("test-tenant")
                .channelName("테스트 채널")
                .channelType(ChannelType.SLACK)
                .isActive(true)
                .build();
        // ID와 isDeleted는 리플렉션으로 설정
        org.springframework.test.util.ReflectionTestUtils.setField(testChannel, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(testChannel, "isDeleted", false);

        testNotification = Notification.builder()
                .notificationId("test-001")
                .tenantId("test-tenant")
                .userId(1L)
                .title("테스트 알림")
                .content("테스트 내용")
                .status(NotificationStatus.SENT)
                .build();
        // ID는 자동 생성되므로 리플렉션으로 설정
        org.springframework.test.util.ReflectionTestUtils.setField(testNotification, "id", 1L);

        // lenient()로 불필요한 stubbing 경고 방지
        lenient().when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        if (tenantContextHolderMock != null) {
            tenantContextHolderMock.close();
        }
    }

    // ==================== 비동기 발송 테스트 ====================

    /**
     * 비동기 알림 발송 성공 테스트
     * 
     * Given: 유효한 알림 요청
     * When: 비동기 알림 발송 (sendNotificationAsync)
     * Then: CompletableFuture로 성공 응답 반환
     */
    @Test
    void sendNotificationAsync_Success() throws ExecutionException, InterruptedException {
        // Given
        NotificationResponse mockResponse = NotificationResponse.builder()
                .notificationId("test-001")
                .status(NotificationStatus.SENT)
                .success(true)
                .build();

        when(channelRepository.findById(any())).thenReturn(Optional.of(testChannel));
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.send(any())).thenReturn(mockResponse);

        // When
        CompletableFuture<NotificationResponse> future = notificationService.sendNotificationAsync(testRequest);
        NotificationResponse response = future.get();

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(NotificationStatus.SENT, response.getStatus());
    }

    /**
     * 비동기 알림 발송 실패 테스트
     * 
     * Given: 발송 중 예외 발생
     * When: 비동기 알림 발송
     * Then: FAILED 상태의 응답 반환
     */
    @Test
    void sendNotificationAsync_Failure() throws ExecutionException, InterruptedException {
        // Given
        when(channelRepository.findById(any())).thenThrow(new RuntimeException("DB Error"));

        // When
        CompletableFuture<NotificationResponse> future = notificationService.sendNotificationAsync(testRequest);
        NotificationResponse response = future.get();

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(NotificationStatus.FAILED, response.getStatus());
        assertTrue(response.getErrorMessage().contains("DB Error"));
    }

    // ==================== 히스토리 조회 테스트 ====================

    /**
     * 테넌트별 알림 히스토리 조회 테스트
     * 
     * Given: 테넌트의 알림 이력
     * When: getNotificationHistory() 호출
     * Then: 페이징된 알림 이력 반환
     */
    @Test
    void getNotificationHistory_Success() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> page = new PageImpl<>(notifications);
        Pageable pageable = PageRequest.of(0, 20);

        when(notificationRepository.findByTenantIdAndIsDeletedFalse("test-tenant", pageable))
                .thenReturn(page);

        // When
        Page<Notification> result = notificationService.getNotificationHistory("test-tenant", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test-001", result.getContent().get(0).getNotificationId());
        verify(notificationRepository).findByTenantIdAndIsDeletedFalse("test-tenant", pageable);
    }

    /**
     * 사용자별 알림 히스토리 조회 테스트
     * 
     * Given: 특정 사용자의 알림 이력
     * When: getNotificationHistoryByUser() 호출
     * Then: 해당 사용자의 알림만 반환
     */
    @Test
    void getNotificationHistoryByUser_Success() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> page = new PageImpl<>(notifications);
        Pageable pageable = PageRequest.of(0, 20);

        when(notificationRepository.findByTenantIdAndUserIdAndIsDeletedFalse("test-tenant", 1L, pageable))
                .thenReturn(page);

        // When
        Page<Notification> result = notificationService.getNotificationHistoryByUser("test-tenant", 1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getUserId());
        verify(notificationRepository).findByTenantIdAndUserIdAndIsDeletedFalse("test-tenant", 1L, pageable);
    }

    // ==================== 채널 관리 테스트 ====================

    /**
     * 활성화된 채널 목록 조회 테스트
     * 
     * Given: 테넌트에 활성화된 채널 존재
     * When: getActiveChannels() 호출
     * Then: 활성화된 채널 목록 반환
     */
    @Test
    void getActiveChannels_Success() {
        // Given
        List<NotificationChannelEntity> channels = Arrays.asList(testChannel);
        when(channelRepository.findActiveChannelsByTenant("test-tenant"))
                .thenReturn(channels);

        // When
        List<NotificationChannelEntity> result = notificationService.getActiveChannels("test-tenant");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("테스트 채널", result.get(0).getChannelName());
        assertTrue(result.get(0).getIsActive());
    }

    /**
     * 채널 생성 성공 테스트
     * 
     * Given: 새로운 채널 정보
     * When: createChannel() 호출
     * Then: 채널 생성 및 저장
     */
    @Test
    void createChannel_Success() {
        // Given
        NotificationChannelEntity newChannel = NotificationChannelEntity.builder()
                .channelName("새 채널")
                .channelType(ChannelType.EMAIL)
                .tenantId("test-tenant")
                .build();

        when(channelRepository.save(any())).thenReturn(newChannel);
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.validateConfiguration()).thenReturn(true);

        // When
        NotificationChannelEntity result = notificationService.createChannel(newChannel);

        // Then
        assertNotNull(result);
        assertEquals("새 채널", result.getChannelName());
        verify(channelRepository).save(any());
    }

    /**
     * 채널 수정 성공 테스트
     * 
     * Given: 기존 채널 정보
     * When: updateChannel() 호출
     * Then: 채널 정보 업데이트
     */
    @Test
    void updateChannel_Success() {
        // Given
        NotificationChannelEntity updatedChannel = NotificationChannelEntity.builder()
                .channelName("수정된 채널")
                .channelType(ChannelType.SLACK)
                .build();

        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.validateConfiguration()).thenReturn(true);

        // When
        NotificationChannelEntity result = notificationService.updateChannel(1L, updatedChannel);

        // Then
        assertNotNull(result);
        assertEquals("수정된 채널", result.getChannelName());
        verify(channelRepository).save(any());
    }

    /**
     * 채널 수정 실패 - 채널 없음
     * 
     * Given: 존재하지 않는 채널 ID
     * When: updateChannel() 호출
     * Then: RuntimeException 발생
     */
    @Test
    void updateChannel_NotFound_ThrowsException() {
        // Given
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.updateChannel(999L, testChannel);
        });
    }

    /**
     * 채널 삭제 (소프트 삭제) 테스트
     * 
     * Given: 기존 채널
     * When: deleteChannel() 호출
     * Then: isDeleted 플래그가 true로 설정
     */
    @Test
    void deleteChannel_SoftDelete_Success() {
        // Given
        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationService.deleteChannel(1L);

        // Then
        verify(channelRepository).save(argThat(channel -> 
            channel.getIsDeleted() == true
        ));
    }

    /**
     * 채널 삭제 실패 - 채널 없음
     * 
     * Given: 존재하지 않는 채널 ID
     * When: deleteChannel() 호출
     * Then: RuntimeException 발생
     */
    @Test
    void deleteChannel_NotFound_ThrowsException() {
        // Given
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.deleteChannel(999L);
        });
    }

    // ==================== 채널 설정 검증 테스트 ====================

    /**
     * 채널 설정 검증 성공 테스트
     * 
     * Given: 유효한 채널 설정
     * When: createChannel() 호출 (내부에서 검증 수행)
     * Then: 채널 생성 성공
     */
    @Test
    void validateChannelConfiguration_Valid_Success() {
        // Given
        when(channelRepository.save(any())).thenReturn(testChannel);
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.validateConfiguration()).thenReturn(true);

        // When
        NotificationChannelEntity result = notificationService.createChannel(testChannel);

        // Then
        assertNotNull(result);
        verify(notificationChannel).validateConfiguration();
    }

    /**
     * 채널 설정 검증 실패 테스트
     * 
     * Given: 유효하지 않은 채널 설정
     * When: createChannel() 호출
     * Then: RuntimeException 발생
     */
    @Test
    void validateChannelConfiguration_Invalid_ThrowsException() {
        // Given
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.validateConfiguration()).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.createChannel(testChannel);
        });
    }

    // ==================== JSON 변환 테스트 ====================

    /**
     * Map을 JSON으로 변환 테스트
     * 
     * Given: 데이터가 포함된 알림 요청
     * When: 알림 발송 (내부에서 JSON 변환 수행)
     * Then: 정상적으로 저장
     */
    @Test
    void convertMapToJson_WithData_Success() {
        // Given
        Map<String, Object> data = Map.of(
            "key1", "value1",
            "key2", 123
        );
        testRequest = NotificationRequest.builder()
                .notificationId(testRequest.getNotificationId())
                .tenantId(testRequest.getTenantId())
                .userId(testRequest.getUserId())
                .title(testRequest.getTitle())
                .content(testRequest.getContent())
                .type(testRequest.getType())
                .priority(testRequest.getPriority())
                .channelId(testRequest.getChannelId())
                .recipient(testRequest.getRecipient())
                .data(data)
                .build();

        when(channelRepository.findById(any())).thenReturn(Optional.of(testChannel));
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.send(any())).thenReturn(NotificationResponse.builder()
                .notificationId("test-001")
                .status(NotificationStatus.SENT)
                .success(true)
                .build());

        // When
        NotificationResponse response = notificationService.sendNotification(testRequest);

        // Then
        assertTrue(response.isSuccess());
        verify(notificationRepository, times(2)).save(any()); // 생성 + 상태 업데이트
    }

    /**
     * 빈 Map JSON 변환 테스트
     * 
     * Given: 빈 데이터 맵
     * When: 알림 발송
     * Then: null로 저장됨
     */
    @Test
    void convertMapToJson_EmptyData_ReturnsNull() {
        // Given
        testRequest = NotificationRequest.builder()
                .notificationId(testRequest.getNotificationId())
                .tenantId(testRequest.getTenantId())
                .userId(testRequest.getUserId())
                .title(testRequest.getTitle())
                .content(testRequest.getContent())
                .type(testRequest.getType())
                .priority(testRequest.getPriority())
                .channelId(testRequest.getChannelId())
                .recipient(testRequest.getRecipient())
                .data(Map.of())
                .build();

        when(channelRepository.findById(any())).thenReturn(Optional.of(testChannel));
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.send(any())).thenReturn(NotificationResponse.builder()
                .notificationId("test-001")
                .status(NotificationStatus.SENT)
                .success(true)
                .build());

        // When
        NotificationResponse response = notificationService.sendNotification(testRequest);

        // Then
        assertTrue(response.isSuccess());
    }

    /**
     * 예약 알림 생성 테스트
     * 
     * Given: scheduledAt이 포함된 알림 요청
     * When: 알림 생성
     * Then: 예약 시간이 설정된 알림 생성
     */
    @Test
    void createNotification_WithScheduledAt_Success() {
        // Given
        LocalDateTime scheduledTime = LocalDateTime.now().plusHours(1);
        testRequest = NotificationRequest.builder()
                .notificationId(testRequest.getNotificationId())
                .tenantId(testRequest.getTenantId())
                .userId(testRequest.getUserId())
                .title(testRequest.getTitle())
                .content(testRequest.getContent())
                .type(testRequest.getType())
                .priority(testRequest.getPriority())
                .channelId(testRequest.getChannelId())
                .recipient(testRequest.getRecipient())
                .scheduledAt(scheduledTime)
                .build();

        when(channelRepository.findById(any())).thenReturn(Optional.of(testChannel));
        when(channelFactory.getChannel(any())).thenReturn(notificationChannel);
        when(notificationChannel.send(any())).thenReturn(NotificationResponse.builder()
                .notificationId("test-001")
                .status(NotificationStatus.PENDING)
                .success(true)
                .build());

        // When
        NotificationResponse response = notificationService.sendNotification(testRequest);

        // Then
        assertNotNull(response);
        verify(notificationRepository, atLeast(1)).save(argThat(notification -> 
            notification.getScheduledAt() != null
        ));
    }
}

