package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.entity.MetricThreshold;
import com.agenticcp.core.domain.monitoring.entity.Alert;
import com.agenticcp.core.domain.monitoring.event.ThresholdExceededEvent;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.repository.MetricThresholdRepository;
import com.agenticcp.core.domain.monitoring.repository.AlertRepository;
import com.agenticcp.core.domain.notification.service.MonitoringNotificationService;
import com.agenticcp.core.domain.notification.service.NotificationDeDuplicationService;
import com.agenticcp.core.domain.monitoring.enums.AlertType;
import com.agenticcp.core.domain.monitoring.enums.AlertStatus;
import com.agenticcp.core.domain.monitoring.enums.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 모니터링 시스템과 알람 시스템 연동 테스트
 * 
 * <p>이 테스트는 다음을 검증합니다:</p>
 * <ul>
 *   <li>ThresholdExceededEvent 발행</li>
 *   <li>MonitoringAlertService에서 이벤트 수신</li>
 *   <li>Alert 엔티티 생성 및 저장</li>
 *   <li>MonitoringNotificationService 호출</li>
 * </ul>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-09
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
@DisplayName("모니터링-알람 시스템 연동 테스트")
class MonitoringAlarmIntegrationTest {

    @Mock
    private MetricRepository metricRepository;
    
    @Mock
    private MetricThresholdRepository metricThresholdRepository;
    
    @Mock
    private AlertRepository alertRepository;
    
    @Mock
    private MonitoringNotificationService monitoringNotificationService;
    
    @Mock
    private NotificationDeDuplicationService notificationDeDuplicationService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MonitoringAlertService monitoringAlertService;

    @BeforeEach
    void setUp() {
        // MonitoringAlertService 초기화 (@RequiredArgsConstructor 순서대로)
        monitoringAlertService = new MonitoringAlertService(
            monitoringNotificationService,
            alertRepository,
            notificationDeDuplicationService
        );
    }

    @Test
    @DisplayName("ThresholdExceededEvent 수신 시 Alert 생성 및 알림 발송")
    void testThresholdExceededEventHandling() {
        // Given: CPU 사용률 95% 메트릭과 임계값 90% 설정
        Metric cpuMetric = createCpuMetric(95.0);
        MetricThreshold cpuThreshold = createCpuThreshold(90.0, ">");
        ThresholdExceededEvent event = new ThresholdExceededEvent(this, cpuThreshold, cpuMetric);
        
        when(alertRepository.save(any(Alert.class))).thenReturn(createMockAlert());
        when(notificationDeDuplicationService.canSendNotification(any(String.class), any())).thenReturn(true);

        // When: MonitoringAlertService에서 이벤트 처리
        monitoringAlertService.handleThresholdExceeded(event);

        // Then: Alert 엔티티 생성 및 저장 확인
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertCaptor.capture());
        
        Alert savedAlert = alertCaptor.getValue();
        assertThat(savedAlert.getAlertName()).contains("메트릭 임계값 초과");
        assertThat(savedAlert.getAlertType()).isEqualTo(AlertType.THRESHOLD);
        assertThat(savedAlert.getSeverity()).isEqualTo(Severity.WARNING);
        
        // Then: MonitoringNotificationService 호출 확인
        verify(monitoringNotificationService, times(1)).sendThresholdViolationAlert(
            eq(cpuMetric), 
            eq(90.0), 
            eq(">")
        );
        
        // Then: 중복 방지 서비스 호출 확인
        verify(notificationDeDuplicationService, times(1)).recordNotificationSent(any(String.class));
    }

    @Test
    @DisplayName("중복 알림 방지 기능 테스트")
    void testDuplicateAlertPrevention() {
        // Given: CPU 사용률 95% 메트릭과 임계값 90% 설정, 중복 알림으로 설정
        Metric cpuMetric = createCpuMetric(95.0);
        MetricThreshold cpuThreshold = createCpuThreshold(90.0, ">");
        ThresholdExceededEvent event = new ThresholdExceededEvent(this, cpuThreshold, cpuMetric);
        
        when(notificationDeDuplicationService.canSendNotification(any(String.class), any())).thenReturn(false);

        // When: MonitoringAlertService에서 이벤트 처리
        monitoringAlertService.handleThresholdExceeded(event);

        // Then: 중복 알림으로 인해 Alert 저장하지 않음
        verify(alertRepository, never()).save(any(Alert.class));
        verify(monitoringNotificationService, never()).sendThresholdViolationAlert(any(), any(), any());
        verify(notificationDeDuplicationService, never()).recordNotificationSent(any());
    }

    @Test
    @DisplayName("메모리 사용률 임계값 위반 시 알림 처리")
    void testMemoryUsageThresholdViolation() {
        // Given: 메모리 사용률 90% 메트릭과 임계값 85% 설정
        Metric memoryMetric = createMemoryMetric(90.0);
        MetricThreshold memoryThreshold = createMemoryThreshold(85.0, ">");
        ThresholdExceededEvent event = new ThresholdExceededEvent(this, memoryThreshold, memoryMetric);
        
        when(alertRepository.save(any(Alert.class))).thenReturn(createMockAlert());
        when(notificationDeDuplicationService.canSendNotification(any(String.class), any())).thenReturn(true);

        // When: MonitoringAlertService에서 이벤트 처리
        monitoringAlertService.handleThresholdExceeded(event);

        // Then: Alert 엔티티 생성 및 저장 확인
        verify(alertRepository, times(1)).save(any(Alert.class));
        
        // Then: MonitoringNotificationService 호출 확인
        verify(monitoringNotificationService, times(1)).sendThresholdViolationAlert(
            eq(memoryMetric), 
            eq(85.0), 
            eq(">")
        );
    }

    @Test
    @DisplayName("임계값 위반 이벤트 데이터 검증")
    void testThresholdExceededEventData() {
        // Given: CPU 사용률 95% 메트릭과 임계값 90% 설정
        Metric cpuMetric = createCpuMetric(95.0);
        MetricThreshold cpuThreshold = createCpuThreshold(90.0, ">");
        ThresholdExceededEvent event = new ThresholdExceededEvent(this, cpuThreshold, cpuMetric);

        // When & Then: 이벤트 데이터 검증
        assertThat(event.getMetric()).isEqualTo(cpuMetric);
        assertThat(event.getThreshold()).isEqualTo(cpuThreshold);
        assertThat(event.getSource()).isEqualTo(this);
        
        // 메트릭 데이터 검증
        assertThat(event.getMetric().getMetricName()).isEqualTo("cpu.usage");
        assertThat(event.getMetric().getMetricValue()).isEqualTo(95.0);
        assertThat(event.getMetric().getUnit()).isEqualTo("%");
        
        // 임계값 데이터 검증
        assertThat(event.getThreshold().getMetricName()).isEqualTo("cpu.usage");
        assertThat(event.getThreshold().getThresholdValue()).isEqualTo(90.0);
        assertThat(event.getThreshold().getOperator()).isEqualTo(">");
    }

    @Test
    @DisplayName("Alert 엔티티 생성 데이터 검증")
    void testAlertEntityCreation() {
        // Given: CPU 사용률 95% 메트릭과 임계값 90% 설정
        Metric cpuMetric = createCpuMetric(95.0);
        MetricThreshold cpuThreshold = createCpuThreshold(90.0, ">");
        ThresholdExceededEvent event = new ThresholdExceededEvent(this, cpuThreshold, cpuMetric);
        
        when(alertRepository.save(any(Alert.class))).thenReturn(createMockAlert());
        when(notificationDeDuplicationService.canSendNotification(any(String.class), any())).thenReturn(true);

        // When: MonitoringAlertService에서 이벤트 처리
        monitoringAlertService.handleThresholdExceeded(event);

        // Then: Alert 엔티티 생성 데이터 검증
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertCaptor.capture());
        
        Alert savedAlert = alertCaptor.getValue();
        assertThat(savedAlert.getAlertName()).contains("메트릭 임계값 초과");
        assertThat(savedAlert.getAlertType()).isEqualTo(AlertType.THRESHOLD);
        assertThat(savedAlert.getSeverity()).isEqualTo(Severity.WARNING);
        assertThat(savedAlert.getStatus()).isEqualTo(AlertStatus.TRIGGERED);
        assertThat(savedAlert.getTenantId()).isEqualTo("test-tenant");
    }

    // Helper methods
    private Metric createCpuMetric(Double value) {
        return Metric.builder()
                .metricName("cpu.usage")
                .metricValue(value)
                .unit("%")
                .metricType(Metric.MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .tenantId("test-tenant")
                .build();
    }

    private Metric createMemoryMetric(Double value) {
        return Metric.builder()
                .metricName("memory.usage")
                .metricValue(value)
                .unit("%")
                .metricType(Metric.MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .tenantId("test-tenant")
                .build();
    }

    private MetricThreshold createCpuThreshold(Double thresholdValue, String operator) {
        return MetricThreshold.builder()
                .metricName("cpu.usage")
                .thresholdValue(thresholdValue)
                .operator(operator)
                .thresholdType(MetricThreshold.ThresholdType.WARNING)
                .severity(MetricThreshold.Severity.MEDIUM)
                .isActive(true)
                .alertEnabled(true)
                .build();
    }

    private MetricThreshold createMemoryThreshold(Double thresholdValue, String operator) {
        return MetricThreshold.builder()
                .metricName("memory.usage")
                .thresholdValue(thresholdValue)
                .operator(operator)
                .thresholdType(MetricThreshold.ThresholdType.WARNING)
                .severity(MetricThreshold.Severity.MEDIUM)
                .isActive(true)
                .alertEnabled(true)
                .build();
    }

    private Alert createMockAlert() {
        return Alert.builder()
                .alertName("Test Alert")
                .alertType(AlertType.THRESHOLD)
                .severity(Severity.WARNING)
                .status(AlertStatus.ACTIVE)
                .tenantId("test-tenant")
                .build();
    }
}