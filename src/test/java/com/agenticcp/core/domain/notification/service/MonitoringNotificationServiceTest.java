package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.notification.dto.MetricDto;
import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

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

    @InjectMocks
    private MonitoringNotificationService monitoringNotificationService;

    private MetricDto testMetric;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() {
        // TenantContextHolder Mock 설정
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                .thenReturn("123"); // 테스트용 테넌트 키
        
        testMetric = MetricDto.builder()
                .id(1L)
                .metricName("cpu.usage")
                .metricValue(85.5)
                .unit("%")
                .metricType(MetricDto.MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .source("system-monitor")
                .status(MetricDto.Status.ACTIVE)
                .tenantId("123")
                .build();
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
        verify(notificationService, times(1)).sendNotification(any(NotificationRequest.class));
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
        testMetric.setMetricValue(95.0); // 90% 이상으로 설정하여 URGENT 우선순위 테스트
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
        monitoringNotificationService.sendThresholdViolationAlert(testMetric, thresholdValue, operator);

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
    void testSendThresholdViolationAlert_WithMetricDto() {
        // Given
        MetricDto memoryMetric = MetricDto.builder()
                .id(2L)
                .metricName("memory.usage")
                .metricValue(75.0)
                .unit("%")
                .metricType(MetricDto.MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .source("system-monitor")
                .status(MetricDto.Status.ACTIVE)
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
