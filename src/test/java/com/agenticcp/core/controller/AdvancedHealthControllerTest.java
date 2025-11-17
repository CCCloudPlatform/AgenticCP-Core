package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.monitoring.health.dto.*;
import com.agenticcp.core.domain.monitoring.health.service.AdvancedHealthCheckService;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import com.agenticcp.core.domain.platform.service.MaintenanceModeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * AdvancedHealthController 단위 테스트
 * 
 * <p>스프링 컨텍스트 없이 Mock을 사용한 순수 단위 테스트입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdvancedHealthController 단위 테스트")
class AdvancedHealthControllerTest {

    @Mock
    private AdvancedHealthCheckService advancedHealthCheckService;

    @Mock
    private MaintenanceModeService maintenanceModeService;

    @InjectMocks
    private AdvancedHealthController advancedHealthController;


    private HealthStatusResponse healthStatusResponse;
    private ComponentHealthStatus componentHealthStatus;
    private HealthCheckSummary healthCheckSummary;

    @BeforeEach
    void setUp() {
        // 전체 헬스체크 응답 설정
        Map<String, HealthIndicatorResult> components = new HashMap<>();
        components.put("database", HealthIndicatorResult.healthy("Database is healthy"));
        components.put("system", HealthIndicatorResult.healthy("System resources are normal"));
        
        healthStatusResponse = HealthStatusResponse.builder()
                .overallStatus(PlatformHealth.HealthStatus.HEALTHY)
                .timestamp(LocalDateTime.now())
                .components(components)
                .responseTime(50L)
                .message("Health check completed")
                .build();

        // 개별 컴포넌트 헬스체크 응답 설정
        componentHealthStatus = ComponentHealthStatus.builder()
                .component("database")
                .status(PlatformHealth.HealthStatus.HEALTHY)
                .message("Database is healthy")
                .timestamp(LocalDateTime.now())
                .responseTime(25L)
                .build();

        // 헬스체크 요약 설정
        healthCheckSummary = HealthCheckSummary.builder()
                .totalServices(3L)
                .healthyServices(2L)
                .warningServices(1L)
                .criticalServices(0L)
                .unknownServices(0L)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("전체 헬스체크 테스트")
    class GetOverallHealthTest {

        @Test
        @DisplayName("정상 조회 시 헬스 상태 반환")
        void getOverallHealth_WhenValid_ReturnsHealthStatus() {
            // Given
            when(advancedHealthCheckService.getOverallHealth()).thenReturn(healthStatusResponse);

            // When
            ResponseEntity<ApiResponse<HealthStatusResponse>> response = 
                    advancedHealthController.getOverallHealth();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getOverallStatus()).isEqualTo(PlatformHealth.HealthStatus.HEALTHY);
        }
    }

    @Nested
    @DisplayName("컴포넌트 헬스체크 테스트")
    class GetComponentHealthTest {

        @Test
        @DisplayName("정상 조회 시 컴포넌트 상태 반환")
        void getComponentHealth_WhenValidComponent_ReturnsComponentStatus() {
            // Given
            when(advancedHealthCheckService.getComponentHealth("database")).thenReturn(componentHealthStatus);

            // When
            ResponseEntity<ApiResponse<ComponentHealthStatus>> response = 
                    advancedHealthController.getComponentHealth("database");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getComponent()).isEqualTo("database");
            assertThat(response.getBody().getData().getStatus()).isEqualTo(PlatformHealth.HealthStatus.HEALTHY);
        }

        @Test
        @DisplayName("존재하지 않는 컴포넌트 조회 시 UNKNOWN 상태 반환")
        void getComponentHealth_WhenNonExistentComponent_ReturnsUnknown() {
            // Given
            ComponentHealthStatus unknownStatus = ComponentHealthStatus.builder()
                    .component("nonexistent")
                    .status(PlatformHealth.HealthStatus.UNKNOWN)
                    .message("Component not found")
                    .timestamp(LocalDateTime.now())
                    .responseTime(1L)
                    .build();
            when(advancedHealthCheckService.getComponentHealth("nonexistent")).thenReturn(unknownStatus);

            // When
            ResponseEntity<ApiResponse<ComponentHealthStatus>> response = 
                    advancedHealthController.getComponentHealth("nonexistent");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getStatus()).isEqualTo(PlatformHealth.HealthStatus.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("헬스체크 요약 테스트")
    class GetHealthSummaryTest {

        @Test
        @DisplayName("정상 조회 시 요약 정보 반환")
        void getHealthSummary_WhenValid_ReturnsSummary() {
            // Given
            when(advancedHealthCheckService.getHealthSummary()).thenReturn(healthCheckSummary);

            // When
            ResponseEntity<ApiResponse<HealthCheckSummary>> response = 
                    advancedHealthController.getHealthSummary();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getTotalServices()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("사용 가능한 컴포넌트 목록 테스트")
    class GetAvailableComponentsTest {

        @Test
        @DisplayName("정상 조회 시 컴포넌트 목록 반환")
        void getAvailableComponents_WhenValid_ReturnsComponentsList() {
            // When
            ResponseEntity<ApiResponse<Map<String, String>>> response = 
                    advancedHealthController.getAvailableComponents();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).containsKey("database");
            assertThat(response.getBody().getData()).containsKey("system");
            assertThat(response.getBody().getData()).containsKey("application");
        }
    }

}
