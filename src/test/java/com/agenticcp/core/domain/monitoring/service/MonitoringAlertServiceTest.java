package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.entity.Alert;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.entity.MetricThreshold;
import com.agenticcp.core.domain.monitoring.entity.MetricThreshold.ThresholdType;
import com.agenticcp.core.domain.monitoring.enums.AlertStatus;
import com.agenticcp.core.domain.monitoring.enums.AlertType;
import com.agenticcp.core.domain.monitoring.enums.Severity;
import com.agenticcp.core.domain.monitoring.event.HealthStatusChangedEvent;
import com.agenticcp.core.domain.monitoring.event.ThresholdExceededEvent;
import com.agenticcp.core.domain.monitoring.repository.AlertRepository;
import com.agenticcp.core.domain.notification.service.MonitoringNotificationService;
import com.agenticcp.core.domain.notification.service.NotificationDeDuplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MonitoringAlertService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringAlertService 테스트")
class MonitoringAlertServiceTest {
    
    @Mock
    private MonitoringNotificationService monitoringNotificationService;
    
    @Mock
    private AlertRepository alertRepository;
    
    @Mock
    private NotificationDeDuplicationService deDuplicationService;
    
    @InjectMocks
    private MonitoringAlertService monitoringAlertService;
    
    private MetricThreshold testThreshold;
    private Metric testMetric;
    
    @BeforeEach
    void setUp() {
        // 테스트용 임계값 설정
        testThreshold = MetricThreshold.builder()
                .metricName("cpu_usage")
                .thresholdValue(80.0)
                .operator(">")
                .thresholdType(ThresholdType.CRITICAL)
                .isActive(true)
                .alertEnabled(true)
                .build();
        setId(testThreshold, 1L);
        
        // 테스트용 메트릭
        testMetric = Metric.builder()
                .tenantId("tenant-a")
                .metricName("cpu_usage")
                .metricValue(95.0)
                .collectedAt(LocalDateTime.now())
                .build();
        setId(testMetric, 100L);
    }
    
    @Test
    @DisplayName("임계값 초과 이벤트 처리 - 최초 알림 발송")
    void handleThresholdExceeded_FirstAlert_ShouldSendNotification() {
        // Given
        ThresholdExceededEvent event = new ThresholdExceededEvent(this, testThreshold, testMetric);
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deDuplicationService.canSendNotification(anyString(), any())).thenReturn(true);
        
        // When
        monitoringAlertService.handleThresholdExceeded(event);
        
        // Then
        // Alert 저장 확인
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        Alert savedAlert = alertCaptor.getValue();
        
        assertThat(savedAlert.getTenantId()).isEqualTo("tenant-a");
        assertThat(savedAlert.getAlertType()).isEqualTo(AlertType.THRESHOLD);
        assertThat(savedAlert.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(savedAlert.getStatus()).isEqualTo(AlertStatus.TRIGGERED);
        
        // 알림 발송 확인
        verify(monitoringNotificationService).sendThresholdViolationAlert(
            eq(testMetric),
            eq(80.0),
            eq(">")
        );
    }
    
    @Test
    @DisplayName("임계값 초과 이벤트 처리 - 중복 알림 방지 (5분 내)")
    void handleThresholdExceeded_DuplicateWithin5Minutes_ShouldNotSendNotification() {
        // Given
        ThresholdExceededEvent event1 = new ThresholdExceededEvent(this, testThreshold, testMetric);
        ThresholdExceededEvent event2 = new ThresholdExceededEvent(this, testThreshold, testMetric);
        
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deDuplicationService.canSendNotification(anyString(), any())).thenReturn(true).thenReturn(false);
        
        // When - 첫 번째 알림 발송
        monitoringAlertService.handleThresholdExceeded(event1);
        
        // When - 5분 내 두 번째 알림 시도
        monitoringAlertService.handleThresholdExceeded(event2);
        
        // Then - Alert는 1번만 저장
        verify(alertRepository, times(1)).save(any(Alert.class));
        
        // Then - 알림은 1번만 발송
        verify(monitoringNotificationService, times(1)).sendThresholdViolationAlert(
            any(Metric.class),
            anyDouble(),
            anyString()
        );
    }
    
    @Test
    @DisplayName("헬스 상태 변화 이벤트 처리 - CRITICAL 상태로 변경")
    void handleHealthStatusChanged_ToCritical_ShouldSendNotification() {
        // Given
        HealthStatusChangedEvent event = new HealthStatusChangedEvent(
            this,
            "database",
            "HEALTHY",
            "CRITICAL",
            "tenant-a"
        );
        
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        monitoringAlertService.handleHealthStatusChanged(event);
        
        // Then
        // Alert 저장 확인
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        Alert savedAlert = alertCaptor.getValue();
        
        assertThat(savedAlert.getTenantId()).isEqualTo("tenant-a");
        assertThat(savedAlert.getAlertType()).isEqualTo(AlertType.AVAILABILITY);
        assertThat(savedAlert.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(savedAlert.getStatus()).isEqualTo(AlertStatus.TRIGGERED);
        
        // 알림 발송 확인
        verify(monitoringNotificationService).sendSystemStatusChangeAlert(
            eq("database"),
            eq("HEALTHY"),
            eq("CRITICAL"),
            eq("tenant-a")
        );
    }
    
    @Test
    @DisplayName("헬스 상태 변화 이벤트 처리 - WARNING 상태로 변경")
    void handleHealthStatusChanged_ToWarning_ShouldSendNotification() {
        // Given
        HealthStatusChangedEvent event = new HealthStatusChangedEvent(
            this,
            "database",
            "HEALTHY",
            "WARNING",
            "tenant-a"
        );
        
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        monitoringAlertService.handleHealthStatusChanged(event);
        
        // Then
        // Alert 저장 확인
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        Alert savedAlert = alertCaptor.getValue();
        
        assertThat(savedAlert.getSeverity()).isEqualTo(Severity.WARNING);
        
        // 알림 발송 확인
        verify(monitoringNotificationService).sendSystemStatusChangeAlert(
            eq("database"),
            eq("HEALTHY"),
            eq("WARNING"),
            eq("tenant-a")
        );
    }
    
    @Test
    @DisplayName("헬스 상태 변화 이벤트 처리 - HEALTHY로 복구")
    void handleHealthStatusChanged_ToHealthy_ShouldNotSendNotification() {
        // Given
        HealthStatusChangedEvent event = new HealthStatusChangedEvent(
            this,
            "database",
            "CRITICAL",
            "HEALTHY",
            "tenant-a"
        );
        
        // When
        monitoringAlertService.handleHealthStatusChanged(event);
        
        // Then - HEALTHY로 복구 시에는 알림 발송 안 함
        verify(alertRepository, never()).save(any(Alert.class));
        verify(monitoringNotificationService, never()).sendSystemStatusChangeAlert(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        );
    }
    
    @Test
    @DisplayName("다른 테넌트의 동일 메트릭 - 별도 알림 발송")
    void handleThresholdExceeded_DifferentTenants_ShouldSendSeparateNotifications() {
        // Given
        Metric metric1 = Metric.builder()
                .tenantId("tenant-a")
                .metricName("cpu_usage")
                .metricValue(95.0)
                .collectedAt(LocalDateTime.now())
                .build();
        setId(metric1, 100L);
        
        Metric metric2 = Metric.builder()
                .tenantId("tenant-b")
                .metricName("cpu_usage")
                .metricValue(95.0)
                .collectedAt(LocalDateTime.now())
                .build();
        setId(metric2, 101L);
        
        ThresholdExceededEvent event1 = new ThresholdExceededEvent(this, testThreshold, metric1);
        ThresholdExceededEvent event2 = new ThresholdExceededEvent(this, testThreshold, metric2);
        
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deDuplicationService.canSendNotification(anyString(), any())).thenReturn(true);
        
        // When
        monitoringAlertService.handleThresholdExceeded(event1);
        monitoringAlertService.handleThresholdExceeded(event2);
        
        // Then - 2개의 Alert 저장
        verify(alertRepository, times(2)).save(any(Alert.class));
        
        // Then - 2개의 알림 발송
        verify(monitoringNotificationService, times(2)).sendThresholdViolationAlert(
            any(Metric.class),
            anyDouble(),
            anyString()
        );
    }
    
    /**
     * Reflection을 사용하여 BaseEntity의 id 설정
     */
    private void setId(Object entity, Long id) {
        try {
            Class<?> currentClass = entity.getClass();
            java.lang.reflect.Field idField = null;
            
            // BaseEntity 또는 TenantAwareEntity에서 id 필드 찾기
            while (currentClass != null && !currentClass.equals(Object.class)) {
                try {
                    idField = currentClass.getDeclaredField("id");
                    break;
                } catch (NoSuchFieldException e) {
                    currentClass = currentClass.getSuperclass();
                }
            }
            
            if (idField != null) {
                idField.setAccessible(true);
                idField.set(entity, id);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id", e);
        }
    }
}

