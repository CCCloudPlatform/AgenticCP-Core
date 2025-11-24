package com.agenticcp.core.domain.monitoring.health.service;

import com.agenticcp.core.domain.monitoring.health.dto.*;
import com.agenticcp.core.domain.monitoring.health.exception.ComponentNotFoundException;
import com.agenticcp.core.domain.monitoring.health.exception.HealthCheckException;
import com.agenticcp.core.domain.monitoring.health.indicator.HealthIndicator;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import com.agenticcp.core.domain.platform.repository.PlatformHealthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdvancedHealthCheckService 단위 테스트
 * 
 * <p>고급 헬스체크 서비스의 핵심 기능을 테스트합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdvancedHealthCheckService 단위 테스트")
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

    @Nested
    @DisplayName("전체 헬스체크 테스트")
    class GetOverallHealthTest {

        @Test
        @DisplayName("모든 컴포넌트가 정상일 때 HEALTHY 상태 반환")
        void getOverallHealth_WhenAllHealthy_ReturnsHealthyStatus() {
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

        @Test
        @DisplayName("치명적 오류가 있을 때 CRITICAL 상태 반환")
        void getOverallHealth_WhenCritical_ReturnsCriticalStatus() {
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

        @Test
        @DisplayName("헬스 인디케이터 예외 발생 시 HealthCheckException 발생")
        void getOverallHealth_WhenException_ThrowsHealthCheckException() {
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

        @Test
        @DisplayName("헬스 인디케이터 예외 발생 시 HealthCheckException 발생 (다른 메시지)")
        void getOverallHealth_WhenHealthIndicatorThrowsException_ThrowsHealthCheckException() {
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
    }

    @Nested
    @DisplayName("컴포넌트 헬스체크 테스트")
    class GetComponentHealthTest {

        @Test
        @DisplayName("컴포넌트가 존재할 때 컴포넌트 상태 반환")
        void getComponentHealth_WhenComponentExists_ReturnsComponentStatus() {
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

        @Test
        @DisplayName("컴포넌트가 존재하지 않을 때 ComponentNotFoundException 발생")
        void getComponentHealth_WhenComponentNotFound_ThrowsComponentNotFoundException() {
            // Given
            when(healthIndicators.stream()).thenReturn(Stream.empty());

            // When & Then
            assertThatThrownBy(() -> advancedHealthCheckService.getComponentHealth("nonexistent"))
                    .isInstanceOf(ComponentNotFoundException.class)
                    .hasMessage("Component 'nonexistent' not found");
        }

        @Test
        @DisplayName("헬스 인디케이터 예외 발생 시 HealthCheckException 발생")
        void getComponentHealth_WhenException_ThrowsHealthCheckException() {
            // Given
            String componentName = "database";
            when(databaseIndicator.getName()).thenReturn(componentName);
            when(databaseIndicator.check()).thenThrow(new RuntimeException("Database connection failed"));
            when(healthIndicators.stream()).thenReturn(Arrays.asList(databaseIndicator).stream());

            // When & Then
            assertThatThrownBy(() -> advancedHealthCheckService.getComponentHealth(componentName))
                    .isInstanceOf(HealthCheckException.class)
                    .hasMessageContaining("Health indicator error for database")
                    .hasMessageContaining("Database connection failed");
        }
    }

    @Nested
    @DisplayName("헬스체크 요약 테스트")
    class GetHealthSummaryTest {

        @Test
        @DisplayName("데이터베이스에서 통계 조회 시 요약 정보 반환")
        void getHealthSummary_WhenValid_ReturnsSummaryFromDatabase() {
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
    }
}
