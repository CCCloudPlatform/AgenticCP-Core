package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.dto.DashboardData;
import com.agenticcp.core.domain.monitoring.dto.LogEntryResponse;
import com.agenticcp.core.domain.monitoring.entity.Alert;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.Severity;
import com.agenticcp.core.domain.monitoring.enums.AlertType;
import com.agenticcp.core.domain.monitoring.enums.AlertStatus;
import com.agenticcp.core.domain.monitoring.repository.AlertRepository;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import com.agenticcp.core.domain.platform.repository.PlatformHealthRepository;
import com.agenticcp.core.domain.platform.service.MaintenanceModeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MonitoringDashboardService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-22
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringDashboardService 테스트")
class MonitoringDashboardServiceTest {

    @Mock
    private HealthCheckService healthCheckService;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private PlatformHealthRepository platformHealthRepository;

    @InjectMocks
    private MonitoringDashboardService monitoringDashboardService;

    private String testTenantId;
    private List<PlatformHealth> sampleHealthData;
    private List<Alert> sampleAlerts;
    private List<Metric> sampleMetrics;

    @BeforeEach
    void setUp() {
        testTenantId = "tenant-001";
        sampleHealthData = createSampleHealthData();
        sampleAlerts = createSampleAlerts();
        sampleMetrics = createSampleMetrics();
    }

    @Nested
    @DisplayName("대시보드 데이터 조회 테스트")
    class GetDashboardDataTest {

        @Test
        @DisplayName("대시보드 데이터 조회 성공")
        void getDashboardData_Success() {
            // Given
            doNothing().when(healthCheckService).checkAllSystemComponents();
            when(platformHealthRepository.findAll()).thenReturn(sampleHealthData);
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(sampleAlerts);
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(sampleMetrics));

            // When
            DashboardData result = monitoringDashboardService.getDashboardData(testTenantId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(testTenantId);
            assertThat(result.getHealthSummary()).isNotNull();
            assertThat(result.getServiceStatuses()).isNotEmpty();
            assertThat(result.getRecentAlerts()).isNotEmpty();
            assertThat(result.getMetricSummary()).isNotNull();
            assertThat(result.getMaintenanceStatus()).isNotNull();
            assertThat(result.getLogSummary()).isNotNull();

            verify(platformHealthRepository, atLeastOnce()).findAll();
            verify(alertRepository, atLeastOnce()).findByTenantId(testTenantId);
            verify(metricRepository).findByTenantId(eq(testTenantId), any(Pageable.class));
        }

        @Test
        @DisplayName("대시보드 데이터 조회 - 헬스 데이터 없음")
        void getDashboardData_NoHealthData() {
            // Given
            doNothing().when(healthCheckService).checkAllSystemComponents();
            when(platformHealthRepository.findAll()).thenReturn(Arrays.asList());
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(Arrays.asList());
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Arrays.asList()));

            // When
            DashboardData result = monitoringDashboardService.getDashboardData(testTenantId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getHealthSummary().getTotalServices()).isEqualTo(0);
            assertThat(result.getServiceStatuses()).isEmpty();
            assertThat(result.getRecentAlerts()).isEmpty();
        }

        @Test
        @DisplayName("대시보드 데이터 조회 - 예외 발생")
        void getDashboardData_Exception() {
            // Given
            when(platformHealthRepository.findAll()).thenThrow(new RuntimeException("데이터베이스 연결 실패"));

            // When & Then
            assertThatThrownBy(() -> monitoringDashboardService.getDashboardData(testTenantId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("대시보드 데이터 조회 실패");
        }
    }

    @Nested
    @DisplayName("알림 목록 조회 테스트")
    class GetAlertsTest {

        @Test
        @DisplayName("알림 목록 조회 성공")
        void getAlerts_Success() {
            // Given
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(sampleAlerts);

            // When
            List<DashboardData.AlertSummary> result = monitoringDashboardService.getAlerts(
                    testTenantId, 0, 10, null, null, null, null);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLevel()).isEqualTo("WARNING");
            assertThat(result.get(0).getMessage()).isEqualTo("CPU 사용률이 높습니다");

            verify(alertRepository).findByTenantId(testTenantId);
        }

        @Test
        @DisplayName("알림 목록 조회 - 페이지네이션")
        void getAlerts_WithPagination() {
            // Given
            List<Alert> manyAlerts = createManyAlerts(25);
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(manyAlerts);

            // When
            List<DashboardData.AlertSummary> result = monitoringDashboardService.getAlerts(
                    testTenantId, 1, 10, null, null, null, null);

            // Then
            assertThat(result).hasSize(10);
            verify(alertRepository).findByTenantId(testTenantId);
        }

        @Test
        @DisplayName("알림 목록 조회 - 빈 결과")
        void getAlerts_EmptyResult() {
            // Given
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(Arrays.asList());

            // When
            List<DashboardData.AlertSummary> result = monitoringDashboardService.getAlerts(
                    testTenantId, 0, 10, null, null, null, null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("메트릭 데이터 조회 테스트")
    class GetMetricsTest {

        @Test
        @DisplayName("메트릭 데이터 조회 성공")
        void getMetrics_Success() {
            // Given
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(sampleMetrics));

            // When
            List<Metric> result = monitoringDashboardService.getMetrics(
                    testTenantId, 0, 10, null, null);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMetricName()).isEqualTo("cpu.usage");

            verify(metricRepository).findByTenantId(eq(testTenantId), any(Pageable.class));
        }

        @Test
        @DisplayName("메트릭 데이터 조회 - 빈 결과")
        void getMetrics_EmptyResult() {
            // Given
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Arrays.asList()));

            // When
            List<Metric> result = monitoringDashboardService.getMetrics(
                    testTenantId, 0, 10, null, null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("로그 엔트리 조회 테스트")
    class GetLogsTest {

        @Test
        @DisplayName("로그 엔트리 조회 성공")
        void getLogs_Success() {
            // Given
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(sampleAlerts);
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(sampleMetrics));

            // When
            List<LogEntryResponse> result = monitoringDashboardService.getLogs(
                    testTenantId, 0, 10, null, null, null, null, null);

            // Then
            assertThat(result).isNotEmpty();
            verify(alertRepository).findByTenantId(testTenantId);
            verify(metricRepository).findByTenantId(eq(testTenantId), any(Pageable.class));
        }

        @Test
        @DisplayName("로그 엔트리 조회 - 빈 결과")
        void getLogs_EmptyResult() {
            // Given
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(Arrays.asList());
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Arrays.asList()));

            // When
            List<LogEntryResponse> result = monitoringDashboardService.getLogs(
                    testTenantId, 0, 10, null, null, null, null, null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("헬스 체크 데이터 빌드 테스트")
    class HealthDataBuildTest {

        @Test
        @DisplayName("헬스 요약 생성 - 정상 상태")
        void buildHealthSummary_HealthyStatus() {
            // Given
            List<PlatformHealth> healthyData = Arrays.asList(
                    createPlatformHealth("service1", "HEALTHY"),
                    createPlatformHealth("service2", "HEALTHY")
            );
            when(platformHealthRepository.findAll()).thenReturn(healthyData);
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(Arrays.asList());
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Arrays.asList()));

            // When
            DashboardData result = monitoringDashboardService.getDashboardData(testTenantId);

            // Then
            assertThat(result.getHealthSummary().getOverallStatus()).isEqualTo("HEALTHY");
            assertThat(result.getHealthSummary().getTotalServices()).isEqualTo(2);
            assertThat(result.getHealthSummary().getHealthyServices()).isEqualTo(2);
            assertThat(result.getHealthSummary().getCriticalServices()).isEqualTo(0);
        }

        @Test
        @DisplayName("헬스 요약 생성 - 경고 상태")
        void buildHealthSummary_WarningStatus() {
            // Given
            List<PlatformHealth> warningData = Arrays.asList(
                    createPlatformHealth("service1", "HEALTHY"),
                    createPlatformHealth("service2", "WARNING")
            );
            when(platformHealthRepository.findAll()).thenReturn(warningData);
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(Arrays.asList());
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Arrays.asList()));

            // When
            DashboardData result = monitoringDashboardService.getDashboardData(testTenantId);

            // Then
            assertThat(result.getHealthSummary().getOverallStatus()).isEqualTo("WARNING");
            assertThat(result.getHealthSummary().getWarningServices()).isEqualTo(1);
        }

        @Test
        @DisplayName("헬스 요약 생성 - 위험 상태")
        void buildHealthSummary_CriticalStatus() {
            // Given
            List<PlatformHealth> criticalData = Arrays.asList(
                    createPlatformHealth("service1", "CRITICAL"),
                    createPlatformHealth("service2", "HEALTHY")
            );
            when(platformHealthRepository.findAll()).thenReturn(criticalData);
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(Arrays.asList());
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Arrays.asList()));

            // When
            DashboardData result = monitoringDashboardService.getDashboardData(testTenantId);

            // Then
            assertThat(result.getHealthSummary().getOverallStatus()).isEqualTo("CRITICAL");
            assertThat(result.getHealthSummary().getCriticalServices()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("메트릭 요약 생성 테스트")
    class MetricSummaryBuildTest {

        @Test
        @DisplayName("메트릭 요약 생성 - 정상 데이터")
        void buildMetricSummary_WithData() {
            // Given
            when(platformHealthRepository.findAll()).thenReturn(sampleHealthData);
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(sampleAlerts);
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(sampleMetrics));

            // When
            DashboardData result = monitoringDashboardService.getDashboardData(testTenantId);

            // Then
            assertThat(result.getMetricSummary()).isNotNull();
            assertThat(result.getMetricSummary().getTotalRequests()).isEqualTo(2);
            assertThat(result.getMetricSummary().getSuccessRate()).isGreaterThan(0);
        }

        @Test
        @DisplayName("메트릭 요약 생성 - 빈 데이터")
        void buildMetricSummary_EmptyData() {
            // Given
            when(platformHealthRepository.findAll()).thenReturn(sampleHealthData);
            when(alertRepository.findByTenantId(testTenantId)).thenReturn(Arrays.asList());
            when(metricRepository.findByTenantId(eq(testTenantId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Arrays.asList()));

            // When
            DashboardData result = monitoringDashboardService.getDashboardData(testTenantId);

            // Then
            assertThat(result.getMetricSummary()).isNotNull();
            assertThat(result.getMetricSummary().getTotalRequests()).isEqualTo(0);
            assertThat(result.getMetricSummary().getSuccessRate()).isEqualTo(0.0);
        }
    }

    // === Helper Methods ===

    private List<PlatformHealth> createSampleHealthData() {
        PlatformHealth health1 = new PlatformHealth();
        health1.setServiceName("api-service");
        health1.setStatus(PlatformHealth.HealthStatus.HEALTHY);
        health1.setErrorMessage(null);
        health1.setLastCheckTime(LocalDateTime.now());
        health1.setMetadata("{\"responseTime\": 150}");

        PlatformHealth health2 = new PlatformHealth();
        health2.setServiceName("database-service");
        health2.setStatus(PlatformHealth.HealthStatus.WARNING);
        health2.setErrorMessage("Connection pool high");
        health2.setLastCheckTime(LocalDateTime.now().minusMinutes(5));
        health2.setMetadata("{\"connectionCount\": 80}");

        return Arrays.asList(health1, health2);
    }

    private PlatformHealth createPlatformHealth(String serviceName, String status) {
        PlatformHealth health = new PlatformHealth();
        health.setServiceName(serviceName);
        health.setStatus(PlatformHealth.HealthStatus.valueOf(status));
        health.setLastCheckTime(LocalDateTime.now());
        return health;
    }

    private List<Alert> createSampleAlerts() {
        Alert alert1 = Alert.builder()
                .alertName("cpu-high")
                .description("CPU 사용률이 높습니다")
                .severity(Severity.WARNING)
                .alertType(AlertType.THRESHOLD)
                .status(AlertStatus.ACTIVE)
                .tenantId(testTenantId)
                .build();
        alert1.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        Alert alert2 = Alert.builder()
                .alertName("memory-low")
                .description("메모리 부족 경고")
                .severity(Severity.ERROR)
                .alertType(AlertType.PERFORMANCE)
                .status(AlertStatus.RESOLVED)
                .tenantId(testTenantId)
                .build();
        alert2.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        return Arrays.asList(alert1, alert2);
    }

    private List<Alert> createManyAlerts(int count) {
        List<Alert> alerts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Alert alert = Alert.builder()
                    .alertName("alert-" + i)
                    .description("Alert " + i)
                    .severity(Severity.INFO)
                    .alertType(AlertType.THRESHOLD)
                    .status(AlertStatus.ACTIVE)
                    .tenantId(testTenantId)
                    .build();
            alert.setCreatedAt(LocalDateTime.now().minusMinutes(i));
            alerts.add(alert);
        }
        return alerts;
    }

    private List<Metric> createSampleMetrics() {
        Metric metric1 = Metric.builder()
                .metricName("cpu.usage")
                .metricValue(75.5)
                .unit("percent")
                .metricType(Metric.MetricType.SYSTEM)
                .status(Metric.Status.ACTIVE)
                .collectedAt(LocalDateTime.now().minusMinutes(5))
                .source("system")
                .tenantId(testTenantId)
                .build();

        Metric metric2 = Metric.builder()
                .metricName("memory.usage")
                .metricValue(60.2)
                .unit("percent")
                .metricType(Metric.MetricType.SYSTEM)
                .status(Metric.Status.ACTIVE)
                .collectedAt(LocalDateTime.now().minusMinutes(3))
                .source("system")
                .tenantId(testTenantId)
                .build();

        return Arrays.asList(metric1, metric2);
    }
}
