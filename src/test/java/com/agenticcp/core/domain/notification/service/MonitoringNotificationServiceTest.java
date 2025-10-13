package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.notification.config.NotificationChannelConfig;
import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.entity.NotificationChannelEntity;
import com.agenticcp.core.domain.notification.enums.ChannelType;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import com.agenticcp.core.domain.notification.repository.NotificationChannelRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 모니터링 알림 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class MonitoringNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationChannelRepository channelRepository;

    @Mock
    private NotificationChannelConfig channelConfig;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MonitoringNotificationService monitoringNotificationService;

    private Metric testMetric;
    private Tenant testTenant;
    private User testAdminUser;
    private NotificationChannelEntity testChannel;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() throws Exception {
        // TenantContextHolder Mock 설정
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                .thenReturn("123"); // 테스트용 테넌트 키
        
        // 테스트 테넌트 설정
        testTenant = Tenant.builder()
                .tenantKey("123")
                .tenantName("테스트 테넌트")
                .status(com.agenticcp.core.common.enums.Status.ACTIVE)
                .build();
        // Reflection으로 ID 설정 (BaseEntity의 id 필드)
        setId(testTenant, 1L);
        
        // 테스트 관리자 사용자 설정
        testAdminUser = User.builder()
                .username("admin")
                .email("admin@test.com")
                .name("테스트 관리자")
                .tenant(testTenant)
                .role(UserRole.TENANT_ADMIN)
                .status(Status.ACTIVE)
                .build();
        // Reflection으로 ID 설정
        setId(testAdminUser, 10L);
        
        // 테스트 슬랙 채널 설정
        testChannel = NotificationChannelEntity.builder()
                .tenantId("123")
                .channelName("테스트 슬랙 채널")
                .channelType(ChannelType.SLACK)
                .isActive(true)
                .build();
        // Reflection으로 ID 설정
        setId(testChannel, 1L);
        
        testMetric = Metric.builder()
                .metricName("cpu.usage")
                .metricValue(85.5)
                .unit("%")
                .metricType(Metric.MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .source("system-monitor")
                .status(Metric.Status.ACTIVE)
                .tenantId("123")
                .build();
        
        // Mock 기본 동작 설정 (lenient로 변경하여 불필요한 stubbing 경고 방지)
        lenient().when(tenantRepository.findByTenantKey("123")).thenReturn(Optional.of(testTenant));
        lenient().when(userRepository.findActiveUsersByTenant(testTenant, Status.ACTIVE))
                .thenReturn(List.of(testAdminUser));
        lenient().when(channelConfig.getChannelTypeForNotification(any())).thenReturn(ChannelType.SLACK);
        lenient().when(channelConfig.getDefaultAdminUserId()).thenReturn(1L);
        lenient().when(channelConfig.getFallbackOrder()).thenReturn(List.of(ChannelType.SLACK));
        lenient().when(channelRepository.findByTenantIdAndChannelTypeAndIsActiveTrueAndIsDeletedFalse("123", ChannelType.SLACK))
                .thenReturn(List.of(testChannel));
    }
    
    /**
     * Reflection을 사용하여 BaseEntity의 id 필드 설정
     */
    private void setId(Object entity, Long id) throws Exception {
        java.lang.reflect.Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @AfterEach
    void tearDown() {
        if (tenantContextHolderMock != null) {
            tenantContextHolderMock.close();
        }
    }

    /**
     * 임계값 초과 알림 발송 성공 테스트
     * 
     * Given: CPU 사용률이 임계값을 초과한 메트릭 데이터
     * When: 임계값 초과 알림 발송 요청
     * Then: 알림이 성공적으로 발송되고 적절한 우선순위로 설정됨
     */
    @Test
    void testSendThresholdViolationAlert_Success() {
        // Given
        Double thresholdValue = 80.0;
        String operator = ">";
        
        NotificationResponse mockResponse = NotificationResponse.builder()
                .notificationId("threshold_cpu.usage_123456789")
                .status(NotificationStatus.SENT)
                .success(true)
                .message("알림이 성공적으로 발송되었습니다.")
                .build();

        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenReturn(mockResponse);

        // When
        monitoringNotificationService.sendThresholdViolationAlert(testMetric, thresholdValue, operator);

        // Then
        verify(notificationService, times(1)).sendNotification(argThat(request ->
            request.getTenantId().equals("123") &&
            request.getUserId().equals(10L) &&  // 테넌트 관리자 ID 확인
            request.getType() == NotificationType.ALERT
        ));
        verify(tenantRepository).findByTenantKey("123");
        verify(userRepository).findActiveUsersByTenant(testTenant, Status.ACTIVE);
    }

    /**
     * 긴급 우선순위 임계값 초과 알림 테스트
     * 
     * Given: CPU 사용률이 90% 이상인 메트릭 데이터
     * When: 임계값 초과 알림 발송 요청
     * Then: URGENT 우선순위로 알림이 발송됨
     */
    @Test
    void testSendThresholdViolationAlert_UrgentPriority() {
        // Given
        Metric urgentMetric = Metric.builder()
                .metricName("cpu.usage")
                .metricValue(95.0) // 90% 이상으로 설정하여 URGENT 우선순위 테스트
                .unit("%")
                .metricType(Metric.MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .source("system-monitor")
                .status(Metric.Status.ACTIVE)
                .tenantId("123")
                .build();
        Double thresholdValue = 90.0;
        String operator = ">";

        NotificationResponse mockResponse = NotificationResponse.builder()
                .notificationId("threshold_cpu.usage_123456789")
                .status(NotificationStatus.SENT)
                .success(true)
                .build();

        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenReturn(mockResponse);

        // When
        monitoringNotificationService.sendThresholdViolationAlert(urgentMetric, thresholdValue, operator);

        // Then
        verify(notificationService, times(1)).sendNotification(argThat(request -> 
            request.getPriority() == NotificationPriority.URGENT
        ));
    }

    /**
     * 시스템 상태 변경 알림 발송 성공 테스트
     * 
     * Given: 데이터베이스 상태가 HEALTHY에서 CRITICAL로 변경
     * When: 시스템 상태 변경 알림 발송 요청
     * Then: 상태 변경 알림이 성공적으로 발송됨
     */
    @Test
    void testSendSystemStatusChangeAlert_Success() {
        // Given
        String serviceName = "database";
        String prevStatus = "HEALTHY";
        String currStatus = "CRITICAL";
        String tenantId = "123";

        NotificationResponse mockResponse = NotificationResponse.builder()
                .notificationId("status_change_database_123456789")
                .status(NotificationStatus.SENT)
                .success(true)
                .build();

        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenReturn(mockResponse);

        // When
        monitoringNotificationService.sendSystemStatusChangeAlert(serviceName, prevStatus, currStatus, tenantId);

        // Then
        verify(notificationService, times(1)).sendNotification(any(NotificationRequest.class));
    }

    /**
     * 메트릭 수집 실패 알림 발송 성공 테스트
     * 
     * Given: 시스템 메트릭 수집기가 연결 시간 초과로 실패
     * When: 수집 실패 알림 발송 요청
     * Then: 수집 실패 알림이 성공적으로 발송됨
     */
    @Test
    void testSendCollectionFailureAlert_Success() {
        // Given
        String collectorType = "SYSTEM";
        String errorMessage = "Connection timeout";
        String tenantId = "123";

        NotificationResponse mockResponse = NotificationResponse.builder()
                .notificationId("collection_failure_SYSTEM_123456789")
                .status(NotificationStatus.SENT)
                .success(true)
                .build();

        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenReturn(mockResponse);

        // When
        monitoringNotificationService.sendCollectionFailureAlert(collectorType, errorMessage, tenantId);

        // Then
        verify(notificationService, times(1)).sendNotification(any(NotificationRequest.class));
    }

    @Test
    void testSendThresholdViolationAlert_Exception() {
        // Given
        Double thresholdValue = 80.0;
        String operator = ">";

        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenThrow(new RuntimeException("알림 발송 실패"));

        // When & Then
        assertDoesNotThrow(() -> {
            monitoringNotificationService.sendThresholdViolationAlert(testMetric, thresholdValue, operator);
        });
    }

    @Test
    void testSendThresholdViolationAlert_WithMetric() {
        // Given
        Metric memoryMetric = Metric.builder()
                .metricName("memory.usage")
                .metricValue(75.0)
                .unit("%")
                .metricType(Metric.MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .source("system-monitor")
                .status(Metric.Status.ACTIVE)
                .tenantId("123")
                .build();

        Double thresholdValue = 80.0;
        String operator = ">";

        NotificationResponse mockResponse = NotificationResponse.builder()
                .notificationId("threshold_memory.usage_123456789")
                .status(NotificationStatus.SENT)
                .success(true)
                .build();

        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenReturn(mockResponse);

        // When
        monitoringNotificationService.sendThresholdViolationAlert(memoryMetric, thresholdValue, operator);

        // Then
        verify(notificationService, times(1)).sendNotification(any(NotificationRequest.class));
    }
}
