package com.agenticcp.core.domain.monitoring.health.service;

import com.agenticcp.core.domain.monitoring.health.dto.*;
import com.agenticcp.core.domain.monitoring.health.exception.ComponentNotFoundException;
import com.agenticcp.core.domain.monitoring.health.exception.HealthCheckException;
import com.agenticcp.core.domain.monitoring.health.indicator.HealthIndicator;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import com.agenticcp.core.domain.platform.repository.PlatformHealthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AdvancedHealthCheckService 테스트 클래스
 * 
 * 고급 헬스체크 서비스의 핵심 기능을 테스트합니다.
 * 
 * 테스트 시나리오:
 * - 전체 헬스체크 (모든 컴포넌트 상태 확인)
 * - 개별 컴포넌트 헬스체크 (특정 서비스 상태 확인)
 * - 헬스체크 요약 (전체 서비스 상태 통계)
 * - 예외 상황 처리 (헬스체크 실패 시)
 */
@ExtendWith(MockitoExtension.class)
class AdvancedHealthCheckServiceTest {

    @Mock
    private List<HealthIndicator> healthIndicators;

    @Mock
    private PlatformHealthRepository platformHealthRepository;

    @Mock
    private HealthIndicator databaseIndicator;

    @Mock
    private HealthIndicator systemIndicator;

    private AdvancedHealthCheckService advancedHealthCheckService;

    @BeforeEach
    void setUp() {
        advancedHealthCheckService = new AdvancedHealthCheckService(healthIndicators, platformHealthRepository);
    }

    /**
     * 전체 헬스체크 테스트 - 모든 컴포넌트가 정상일 때
     * 
     * 모든 헬스체크 인디케이터가 HEALTHY 상태를 반환할 때,
     * 전체 상태도 HEALTHY가 되는지 확인합니다.
     */
    @Test
    void getOverallHealth_WhenAllHealthy_ShouldReturnHealthyStatus() {
        // Given
        HealthIndicatorResult healthyResult = HealthIndicatorResult.healthy("Database is healthy");
        when(databaseIndicator.getName()).thenReturn("database");
        when(databaseIndicator.check()).thenReturn(healthyResult);
        when(healthIndicators.iterator()).thenReturn(Arrays.asList(databaseIndicator).iterator());

        // When
        HealthStatusResponse response = advancedHealthCheckService.getOverallHealth();

        // Then
        assertThat(response.getOverallStatus()).isEqualTo(PlatformHealth.HealthStatus.HEALTHY);
        assertThat(response.getComponents()).hasSize(1);
        assertThat(response.getComponents().get("database")).isEqualTo(healthyResult);
        assertThat(response.getResponseTime()).isGreaterThanOrEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("Health check completed");

        verify(platformHealthRepository).save(any(PlatformHealth.class));
    }

    /**
     * 전체 헬스체크 테스트 - 치명적 오류가 있을 때
     * 
     * 하나의 컴포넌트라도 CRITICAL 상태를 반환하면,
     * 전체 상태도 CRITICAL이 되는지 확인합니다.
     */
    @Test
    void getOverallHealth_WhenCritical_ShouldReturnCriticalStatus() {
        // Given
        HealthIndicatorResult criticalResult = HealthIndicatorResult.critical("Database connection failed");
        when(databaseIndicator.getName()).thenReturn("database");
        when(databaseIndicator.check()).thenReturn(criticalResult);
        when(healthIndicators.iterator()).thenReturn(Arrays.asList(databaseIndicator).iterator());

        // When
        HealthStatusResponse response = advancedHealthCheckService.getOverallHealth();

        // Then
        assertThat(response.getOverallStatus()).isEqualTo(PlatformHealth.HealthStatus.CRITICAL);
        assertThat(response.getComponents()).hasSize(1);
        assertThat(response.getComponents().get("database")).isEqualTo(criticalResult);

        verify(platformHealthRepository).save(any(PlatformHealth.class));
    }

    /**
     * 개별 컴포넌트 헬스체크 테스트 - 컴포넌트가 존재할 때
     * 
     * 특정 컴포넌트의 헬스체크를 요청했을 때,
     * 해당 컴포넌트의 상태가 올바르게 반환되는지 확인합니다.
     */
    @Test
    void getComponentHealth_WhenComponentExists_ShouldReturnComponentStatus() {
        // Given
        HealthIndicatorResult result = HealthIndicatorResult.healthy("Database is healthy");
        when(databaseIndicator.getName()).thenReturn("database");
        when(databaseIndicator.check()).thenReturn(result);
        when(healthIndicators.stream()).thenReturn(Arrays.<HealthIndicator>asList(databaseIndicator).stream());

        // When
        ComponentHealthStatus response = advancedHealthCheckService.getComponentHealth("database");

        // Then
        assertThat(response.getComponent()).isEqualTo("database");
        assertThat(response.getStatus()).isEqualTo(PlatformHealth.HealthStatus.HEALTHY);
        assertThat(response.getMessage()).isEqualTo("Database is healthy");
        assertThat(response.getResponseTime()).isGreaterThanOrEqualTo(0);

        verify(platformHealthRepository).save(any(PlatformHealth.class));
    }

    /**
     * 개별 컴포넌트 헬스체크 테스트 - 컴포넌트가 존재하지 않을 때
     * 
     * 존재하지 않는 컴포넌트의 헬스체크를 요청했을 때,
     * ComponentNotFoundException이 발생하는지 확인합니다.
     */
    @Test
    void getComponentHealth_WhenComponentNotFound_ShouldThrowComponentNotFoundException() {
        // Given
        when(healthIndicators.stream()).thenReturn(Stream.empty());

        // When & Then
        assertThatThrownBy(() -> advancedHealthCheckService.getComponentHealth("nonexistent"))
                .isInstanceOf(ComponentNotFoundException.class)
                .hasMessage("Component 'nonexistent' not found");
    }

    /**
     * 헬스체크 요약 테스트 - 데이터베이스에서 통계 조회
     * 
     * 데이터베이스에 저장된 헬스체크 상태들을 기반으로
     * 전체 서비스의 상태 통계가 올바르게 계산되는지 확인합니다.
     */
    @Test
    void getHealthSummary_ShouldReturnSummaryFromDatabase() {
        // Given
        PlatformHealth healthyHealth = PlatformHealth.builder()
                .serviceName("database")
                .status(PlatformHealth.HealthStatus.HEALTHY)
                .build();
        PlatformHealth warningHealth = PlatformHealth.builder()
                .serviceName("system")
                .status(PlatformHealth.HealthStatus.WARNING)
                .build();
        PlatformHealth criticalHealth = PlatformHealth.builder()
                .serviceName("application")
                .status(PlatformHealth.HealthStatus.CRITICAL)
                .build();

        List<PlatformHealth> healthStatuses = Arrays.asList(healthyHealth, warningHealth, criticalHealth);
        when(platformHealthRepository.findLatestHealthStatus()).thenReturn(healthStatuses);

        // When
        HealthCheckSummary summary = advancedHealthCheckService.getHealthSummary();

        // Then
        assertThat(summary.getTotalServices()).isEqualTo(3);
        assertThat(summary.getHealthyServices()).isEqualTo(1);
        assertThat(summary.getWarningServices()).isEqualTo(1);
        assertThat(summary.getCriticalServices()).isEqualTo(1);
        assertThat(summary.getUnknownServices()).isEqualTo(0);
        assertThat(summary.getLastUpdated()).isNotNull();
    }

    /**
     * 예외 처리 테스트 - 헬스체크 실패 시
     * 
     * 헬스체크 인디케이터에서 예외가 발생했을 때,
     * HealthCheckException이 발생하는지 확인합니다.
     */
    @Test
    void getOverallHealth_WhenException_ShouldThrowHealthCheckException() {
        // Given
        when(databaseIndicator.getName()).thenReturn("database");
        when(databaseIndicator.check()).thenThrow(new RuntimeException("Health check failed"));
        when(healthIndicators.iterator()).thenReturn(Arrays.asList(databaseIndicator).iterator());

        // When & Then
        assertThatThrownBy(() -> advancedHealthCheckService.getOverallHealth())
                .isInstanceOf(HealthCheckException.class)
                .hasMessageContaining("Health indicator error for database")
                .hasMessageContaining("Health check failed");
    }

    /**
     * 존재하지 않는 컴포넌트 헬스체크 테스트
     * 
     * 요청한 컴포넌트가 존재하지 않을 때 ComponentNotFoundException이 발생하는지 확인합니다.
     */
    @Test
    void getComponentHealth_WithNonExistentComponent_ShouldThrowComponentNotFoundException() {
        // Given
        String nonExistentComponent = "nonexistent";
        when(healthIndicators.stream()).thenReturn(Stream.empty());

        // When & Then
        assertThatThrownBy(() -> advancedHealthCheckService.getComponentHealth(nonExistentComponent))
                .isInstanceOf(ComponentNotFoundException.class)
                .hasMessage("Component 'nonexistent' not found");
    }

    /**
     * 헬스 인디케이터 오류 테스트
     * 
     * 헬스 인디케이터에서 예외가 발생했을 때 HealthCheckException이 발생하는지 확인합니다.
     */
    @Test
    void getOverallHealth_WhenHealthIndicatorThrowsException_ShouldThrowHealthCheckException() {
        // Given
        when(databaseIndicator.getName()).thenReturn("database");
        when(databaseIndicator.check()).thenThrow(new RuntimeException("Database connection failed"));
        when(healthIndicators.iterator()).thenReturn(Arrays.asList(databaseIndicator).iterator());

        // When & Then
        assertThatThrownBy(() -> advancedHealthCheckService.getOverallHealth())
                .isInstanceOf(HealthCheckException.class)
                .hasMessageContaining("Health indicator error for database")
                .hasMessageContaining("Database connection failed");
    }

    /**
     * 개별 컴포넌트 헬스체크에서 예외 발생 테스트
     * 
     * 개별 컴포넌트 헬스체크에서 예외가 발생했을 때 적절한 처리가 되는지 확인합니다.
     */
    @Test
    void getComponentHealth_WhenException_ShouldHandleGracefully() {
        // Given
        String componentName = "database";
        when(databaseIndicator.getName()).thenReturn(componentName);
        when(databaseIndicator.check()).thenThrow(new RuntimeException("Database connection failed"));
        when(healthIndicators.stream()).thenReturn(Arrays.asList(databaseIndicator).stream());

        // When
        ComponentHealthStatus response = advancedHealthCheckService.getComponentHealth(componentName);

        // Then
        assertThat(response.getComponent()).isEqualTo(componentName);
        assertThat(response.getStatus()).isEqualTo(PlatformHealth.HealthStatus.CRITICAL);
        assertThat(response.getMessage()).contains("Health check failed");
    }
}
