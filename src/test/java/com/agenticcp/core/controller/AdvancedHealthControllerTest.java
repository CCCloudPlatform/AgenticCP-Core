package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.monitoring.health.dto.*;
import com.agenticcp.core.domain.monitoring.health.service.AdvancedHealthCheckService;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import org.junit.jupiter.api.BeforeEach;
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
 * AdvancedHealthController 순수 단위 테스트 클래스
 * 
 * 스프링 컨텍스트 없이 Mock을 사용한 순수 단위 테스트입니다.
 * 
 * 테스트 시나리오:
 * - 전체 헬스체크 API (정상/오류)
 * - 개별 컴포넌트 헬스체크 API (정상/오류)
 * - 헬스체크 요약 API
 * - 사용 가능한 컴포넌트 목록 API
 */
@ExtendWith(MockitoExtension.class)
class AdvancedHealthControllerTest {

    @Mock
    private AdvancedHealthCheckService advancedHealthCheckService;

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

    /**
     * 전체 헬스체크 API 테스트 - 정상 응답
     */
    @Test
    void getOverallHealth_ShouldReturnHealthStatus() {
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

    /**
     * 전체 헬스체크 API 테스트 - 서비스 예외 발생
     */
    @Test
    void getOverallHealth_WhenServiceThrowsException_ShouldReturnError() {
        // Given
        when(advancedHealthCheckService.getOverallHealth())
                .thenThrow(new RuntimeException("Health check service error"));

        // When
        ResponseEntity<ApiResponse<HealthStatusResponse>> response = 
                advancedHealthController.getOverallHealth();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Health check failed");
    }

    /**
     * 개별 컴포넌트 헬스체크 API 테스트 - 정상 응답
     */
    @Test
    void getComponentHealth_ShouldReturnComponentStatus() {
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

    /**
     * 개별 컴포넌트 헬스체크 API 테스트 - 존재하지 않는 컴포넌트
     */
    @Test
    void getComponentHealth_WithNonExistentComponent_ShouldReturnUnknown() {
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

    /**
     * 헬스체크 요약 API 테스트 - 정상 응답
     */
    @Test
    void getHealthSummary_ShouldReturnSummary() {
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

    /**
     * 사용 가능한 컴포넌트 목록 API 테스트
     */
    @Test
    void getAvailableComponents_ShouldReturnComponentsList() {
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
