package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.monitoring.dto.DashboardData;
import com.agenticcp.core.domain.monitoring.dto.LogEntryResponse;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.service.MetricTrendService;
import com.agenticcp.core.domain.monitoring.service.MonitoringDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MonitoringDashboardController 컨트롤러 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringDashboardController 테스트")
class MonitoringDashboardControllerTest {

    @Mock
    private MonitoringDashboardService dashboardService;

    @Mock
    private MetricTrendService metricTrendService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MonitoringDashboardController controller;

    private MockMvc mockMvc;
    private String testTenantId;
    private DashboardData sampleDashboardData;
    private List<DashboardData.AlertSummary> sampleAlerts;
    private List<Metric> sampleMetrics;
    private List<LogEntryResponse> sampleLogs;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        testTenantId = "tenant-001";
        
        // 샘플 대시보드 데이터 생성
        sampleDashboardData = createSampleDashboardData();
        sampleAlerts = createSampleAlerts();
        sampleMetrics = createSampleMetrics();
        sampleLogs = createSampleLogs();
    }

    @Nested
    @DisplayName("대시보드 데이터 조회 테스트")
    class GetDashboardDataTest {

        @Test
        @DisplayName("대시보드 데이터 조회 성공 - 200 OK")
        void getDashboardData_WithValidTenant_ReturnsOkResponse() {
            // Given - 유효한 테넌트 컨텍스트가 설정된 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getDashboardData(testTenantId))
                        .thenReturn(sampleDashboardData);

                // When - 대시보드 데이터를 조회하는 API를 호출하는 경우
                ResponseEntity<ApiResponse<DashboardData>> response = 
                        controller.getDashboardData(request, authentication);

                // Then - 200 OK 상태코드와 함께 대시보드 데이터가 반환되어야 함
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).isEqualTo(sampleDashboardData);
                assertThat(response.getBody().getData().getTenantId()).isEqualTo(testTenantId);

                verify(dashboardService).getDashboardData(testTenantId);
            }
        }

        @Test
        @DisplayName("대시보드 데이터 조회 - 테넌트 컨텍스트 없음")
        void getDashboardData_NoTenantContext_ReturnsDefaultTenant() {
            // Given - 테넌트 컨텍스트가 없는 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(null);
                
                when(dashboardService.getDashboardData("default-tenant"))
                        .thenReturn(sampleDashboardData);

                // When & Then - 기본 테넌트로 대시보드 데이터를 조회하는 경우
                ResponseEntity<ApiResponse<DashboardData>> response = 
                        controller.getDashboardData(request, authentication);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();

                verify(dashboardService).getDashboardData("default-tenant");
            }
        }

        @Test
        @DisplayName("대시보드 데이터 조회 - 서비스 예외 발생")
        void getDashboardData_ServiceException_ReturnsInternalServerError() {
            // Given - 서비스에서 예외가 발생하는 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getDashboardData(testTenantId))
                        .thenThrow(new RuntimeException("데이터베이스 연결 실패"));

                // When & Then - 서비스 예외가 발생하는 경우
                ResponseEntity<ApiResponse<DashboardData>> response = 
                        controller.getDashboardData(request, authentication);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();
                assertThat(response.getBody().getErrorCode()).isEqualTo("DASHBOARD_001");
                assertThat(response.getBody().getMessage()).contains("대시보드 데이터 조회 실패");

                verify(dashboardService).getDashboardData(testTenantId);
            }
        }
    }

    @Nested
    @DisplayName("알림 목록 조회 테스트")
    class GetAlertsTest {

        @Test
        @DisplayName("알림 목록 조회 성공 - 기본 파라미터")
        void getAlerts_WithDefaultParams_Success() throws Exception {
            // Given - 기본 파라미터로 알림 목록 조회를 위한 Mock 설정
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getAlerts(eq(testTenantId), eq(0), eq(20), 
                        isNull(), isNull(), isNull(), isNull()))
                        .thenReturn(sampleAlerts);

                // When & Then - 기본 파라미터로 알림 목록 조회 API를 호출하는 경우
                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isArray())
                        .andExpect(jsonPath("$.data[0].level").value("WARNING"))
                        .andExpect(jsonPath("$.data[0].message").value("CPU 사용률이 높습니다"));

                verify(dashboardService).getAlerts(eq(testTenantId), eq(0), eq(20), 
                        isNull(), isNull(), isNull(), isNull());
            }
        }

        @Test
        @DisplayName("알림 목록 조회 성공 - 필터 파라미터 포함")
        void getAlerts_WithFilterParams_Success() throws Exception {
            // Given - 필터 파라미터가 포함된 알림 목록 조회를 위한 Mock 설정
            String level = "ERROR";
            String source = "system";
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getAlerts(eq(testTenantId), eq(1), eq(10), 
                        eq(level), eq(source), eq(startTime), eq(endTime)))
                        .thenReturn(sampleAlerts);

                // When & Then - 필터 파라미터로 알림 목록 조회 API를 호출하는 경우
                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", testTenantId)
                                .param("page", "1")
                                .param("size", "10")
                                .param("level", level)
                                .param("source", source)
                                .param("startTime", startTime.toString())
                                .param("endTime", endTime.toString()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isArray());

                verify(dashboardService).getAlerts(eq(testTenantId), eq(1), eq(10), 
                        eq(level), eq(source), eq(startTime), eq(endTime));
            }
        }

        @Test
        @DisplayName("알림 목록 조회 - 서비스 예외 발생")
        void getAlerts_ServiceException_ReturnsInternalServerError() throws Exception {
            // Given - 서비스에서 예외가 발생하는 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getAlerts(any(), anyInt(), anyInt(), any(), any(), any(), any()))
                        .thenThrow(new RuntimeException("알림 조회 실패"));

                // When & Then - 서비스 예외가 발생하는 경우
                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isInternalServerError())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.errorCode").value("ALERT_001"))
                        .andExpect(jsonPath("$.message").value(containsString("알림 목록 조회 실패")));
            }
        }
    }

    @Nested
    @DisplayName("메트릭 데이터 조회 테스트")
    class GetMetricsTest {

        @Test
        @DisplayName("메트릭 데이터 조회 성공 - 일반 조회 모드")
        void getMetrics_GeneralMode_Success() throws Exception {
            // Given - 일반 메트릭 조회를 위한 Mock 설정
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getMetrics(eq(testTenantId), eq(0), eq(20), 
                        isNull(), isNull()))
                        .thenReturn(sampleMetrics);

                // When & Then - 일반 메트릭 조회 API를 호출하는 경우
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isArray())
                        .andExpect(jsonPath("$.data[0].metricName").value("cpu.usage"))
                        .andExpect(jsonPath("$.data[0].metricValue").value(75.5));

                verify(dashboardService).getMetrics(eq(testTenantId), eq(0), eq(20), 
                        isNull(), isNull());
                verify(metricTrendService, never()).getMetricsWithTrend(any(), any(), any(), any(), any());
            }
        }

        @Test
        @DisplayName("메트릭 데이터 조회 성공 - 트렌드 분석 모드")
        void getMetrics_TrendAnalysisMode_Success() throws Exception {
            // Given - 트렌드 분석 모드로 메트릭 조회를 위한 Mock 설정
            String metricName = "cpu.usage";
            String interval = "1h";
            LocalDateTime startTime = LocalDateTime.now().minusHours(24);
            LocalDateTime endTime = LocalDateTime.now();
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(metricTrendService.getMetricsWithTrend(eq(testTenantId), eq(metricName), 
                        eq(startTime), eq(endTime), eq(interval)))
                        .thenReturn(sampleMetrics);

                // When & Then - 트렌드 분석 모드로 메트릭 조회 API를 호출하는 경우
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId)
                                .param("metricName", metricName)
                                .param("interval", interval)
                                .param("startTime", startTime.toString())
                                .param("endTime", endTime.toString()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isArray());

                verify(metricTrendService).getMetricsWithTrend(eq(testTenantId), eq(metricName), 
                        eq(startTime), eq(endTime), eq(interval));
                verify(dashboardService, never()).getMetrics(any(), anyInt(), anyInt(), any(), any());
            }
        }

        @Test
        @DisplayName("메트릭 데이터 조회 - 서비스 예외 발생")
        void getMetrics_ServiceException_ReturnsInternalServerError() throws Exception {
            // Given - 서비스에서 예외가 발생하는 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getMetrics(any(), anyInt(), anyInt(), any(), any()))
                        .thenThrow(new RuntimeException("메트릭 조회 실패"));

                // When & Then - 서비스 예외가 발생하는 경우
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isInternalServerError())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.errorCode").value("METRIC_001"))
                        .andExpect(jsonPath("$.message").value(containsString("메트릭 데이터 조회 실패")));
            }
        }
    }

    @Nested
    @DisplayName("로그 엔트리 조회 테스트")
    class GetLogsTest {

        @Test
        @DisplayName("로그 엔트리 조회 성공 - 기본 파라미터")
        void getLogs_WithDefaultParams_Success() throws Exception {
            // Given - 기본 파라미터로 로그 조회를 위한 Mock 설정
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getLogs(eq(testTenantId), eq(0), eq(20), 
                        isNull(), isNull(), isNull(), isNull(), isNull()))
                        .thenReturn(sampleLogs);

                // When & Then - 기본 파라미터로 로그 조회 API를 호출하는 경우
                mockMvc.perform(get("/api/monitoring/logs")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isArray())
                        .andExpect(jsonPath("$.data[0].level").value("INFO"))
                        .andExpect(jsonPath("$.data[0].type").value("alert"));

                verify(dashboardService).getLogs(eq(testTenantId), eq(0), eq(20), 
                        isNull(), isNull(), isNull(), isNull(), isNull());
            }
        }

        @Test
        @DisplayName("로그 엔트리 조회 성공 - 필터 파라미터 포함")
        void getLogs_WithFilterParams_Success() throws Exception {
            // Given - 필터 파라미터가 포함된 로그 조회를 위한 Mock 설정
            String level = "ERROR";
            String type = "failure";
            String source = "monitoring";
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getLogs(eq(testTenantId), eq(1), eq(10), 
                        eq(level), eq(type), eq(source), eq(startTime), eq(endTime)))
                        .thenReturn(sampleLogs);

                // When & Then - 필터 파라미터로 로그 조회 API를 호출하는 경우
                mockMvc.perform(get("/api/monitoring/logs")
                                .header("X-Tenant-Id", testTenantId)
                                .param("page", "1")
                                .param("size", "10")
                                .param("level", level)
                                .param("type", type)
                                .param("source", source)
                                .param("startTime", startTime.toString())
                                .param("endTime", endTime.toString()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isArray());

                verify(dashboardService).getLogs(eq(testTenantId), eq(1), eq(10), 
                        eq(level), eq(type), eq(source), eq(startTime), eq(endTime));
            }
        }

        @Test
        @DisplayName("로그 엔트리 조회 - 서비스 예외 발생")
        void getLogs_ServiceException_ReturnsInternalServerError() throws Exception {
            // Given - 서비스에서 예외가 발생하는 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getLogs(any(), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                        .thenThrow(new RuntimeException("로그 조회 실패"));

                // When & Then - 서비스 예외가 발생하는 경우
                mockMvc.perform(get("/api/monitoring/logs")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isInternalServerError())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.errorCode").value("LOG_001"))
                        .andExpect(jsonPath("$.message").value(containsString("로그 엔트리 조회 실패")));
            }
        }
    }

    @Nested
    @DisplayName("파라미터 검증 테스트")
    class ParameterValidationTest {

        @Test
        @DisplayName("알림 조회 - 음수 페이지 파라미터 (현재 구현에서는 허용)")
        void getAlerts_InvalidPageParam_ReturnsOk() throws Exception {
            // Given - 음수 페이지 번호로 요청하는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getAlerts(any(), anyInt(), anyInt(), any(), any(), any(), any()))
                        .thenReturn(sampleAlerts);

                // When & Then - 음수 페이지 번호도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", testTenantId)
                                .param("page", "-1")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("알림 조회 - 0 이하 페이지 크기 (현재 구현에서는 허용)")
        void getAlerts_InvalidSizeParam_ReturnsOk() throws Exception {
            // Given - 0 이하 페이지 크기로 요청하는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getAlerts(any(), anyInt(), anyInt(), any(), any(), any(), any()))
                        .thenReturn(sampleAlerts);

                // When & Then - 0 이하 페이지 크기도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", testTenantId)
                                .param("page", "0")
                                .param("size", "0"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("메트릭 조회 - 음수 페이지 파라미터 (현재 구현에서는 허용)")
        void getMetrics_InvalidPageParam_ReturnsOk() throws Exception {
            // Given - 음수 페이지 번호로 요청하는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getMetrics(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(sampleMetrics);

                // When & Then - 음수 페이지 번호도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId)
                                .param("page", "-1")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("로그 조회 - 음수 페이지 파라미터 (현재 구현에서는 허용)")
        void getLogs_InvalidPageParam_ReturnsOk() throws Exception {
            // Given - 음수 페이지 번호로 요청하는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getLogs(any(), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                        .thenReturn(sampleLogs);

                // When & Then - 음수 페이지 번호도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/logs")
                                .header("X-Tenant-Id", testTenantId)
                                .param("page", "-1")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }
    }

    @Nested
    @DisplayName("날짜 형식 검증 테스트")
    class DateFormatValidationTest {

        @Test
        @DisplayName("알림 조회 - 잘못된 날짜 형식 (Spring 자동 검증)")
        void getAlerts_InvalidDateFormat_ReturnsBadRequest() throws Exception {
            // Given & When & Then - 잘못된 날짜 형식은 Spring이 자동으로 400 에러 반환
            mockMvc.perform(get("/api/monitoring/alerts")
                            .header("X-Tenant-Id", testTenantId)
                            .param("startTime", "invalid-date")
                            .param("endTime", "2023-13-45T25:70:90"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("메트릭 조회 - 잘못된 날짜 형식 (Spring 자동 검증)")
        void getMetrics_InvalidDateFormat_ReturnsBadRequest() throws Exception {
            // Given & When & Then - 잘못된 날짜 형식은 Spring이 자동으로 400 에러 반환
            mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                            .header("X-Tenant-Id", testTenantId)
                            .param("startTime", "invalid-date")
                            .param("endTime", "2023-13-45T25:70:90"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("로그 조회 - 잘못된 날짜 형식 (Spring 자동 검증)")
        void getLogs_InvalidDateFormat_ReturnsBadRequest() throws Exception {
            // Given & When & Then - 잘못된 날짜 형식은 Spring이 자동으로 400 에러 반환
            mockMvc.perform(get("/api/monitoring/logs")
                            .header("X-Tenant-Id", testTenantId)
                            .param("startTime", "invalid-date")
                            .param("endTime", "2023-13-45T25:70:90"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("메트릭 조회 - 잘못된 날짜 범위 (현재 구현에서는 허용)")
        void getMetrics_InvalidDateRange_ReturnsOk() throws Exception {
            // Given - startTime이 endTime보다 늦은 시간으로 설정
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime.minusHours(1);
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getMetrics(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(sampleMetrics);

                // When & Then - 잘못된 날짜 범위도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId)
                                .param("startTime", startTime.toString())
                                .param("endTime", endTime.toString()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }
    }

    @Nested
    @DisplayName("메트릭 트렌드 분석 테스트")
    class MetricTrendAnalysisTest {

        @Test
        @DisplayName("메트릭 조회 - 잘못된 interval 값 (현재 구현에서는 허용)")
        void getMetrics_InvalidInterval_ReturnsOk() throws Exception {
            // Given - 지원하지 않는 interval 값으로 요청하는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);

                // When & Then - 잘못된 interval 값도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId)
                                .param("metricName", "cpu.usage")
                                .param("interval", "invalid-interval"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("메트릭 조회 - metricName만 있고 interval이 없는 경우 (현재 구현에서는 허용)")
        void getMetrics_MetricNameWithoutInterval_ReturnsOk() throws Exception {
            // Given - metricName만 있고 interval이 없는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getMetrics(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(sampleMetrics);

                // When & Then - metricName만 있어도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId)
                                .param("metricName", "cpu.usage"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("메트릭 조회 - interval만 있고 metricName이 없는 경우 (현재 구현에서는 허용)")
        void getMetrics_IntervalWithoutMetricName_ReturnsOk() throws Exception {
            // Given - interval만 있고 metricName이 없는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getMetrics(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(sampleMetrics);

                // When & Then - interval만 있어도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId)
                                .param("interval", "1h"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("메트릭 조회 - 트렌드 분석 모드 성공")
        void getMetrics_TrendAnalysisMode_Success() throws Exception {
            // Given - 트렌드 분석을 위한 유효한 파라미터
            String metricName = "cpu.usage";
            String interval = "1h";
            LocalDateTime startTime = LocalDateTime.now().minusHours(24);
            LocalDateTime endTime = LocalDateTime.now();
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(metricTrendService.getMetricsWithTrend(eq(testTenantId), eq(metricName), 
                        eq(startTime), eq(endTime), eq(interval)))
                        .thenReturn(sampleMetrics);

                // When & Then - 트렌드 분석 모드로 메트릭 조회 API를 호출하는 경우
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId)
                                .param("metricName", metricName)
                                .param("interval", interval)
                                .param("startTime", startTime.toString())
                                .param("endTime", endTime.toString()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isArray());

                verify(metricTrendService).getMetricsWithTrend(eq(testTenantId), eq(metricName), 
                        eq(startTime), eq(endTime), eq(interval));
            }
        }
    }

    @Nested
    @DisplayName("빈 결과 처리 테스트")
    class EmptyResultTest {

        @Test
        @DisplayName("알림 조회 - 조건에 맞는 데이터가 없는 경우")
        void getAlerts_NoMatchingData_ReturnsEmptyList() throws Exception {
            // Given - 조건에 맞는 알림이 없는 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getAlerts(any(), anyInt(), anyInt(), any(), any(), any(), any()))
                        .thenReturn(Arrays.asList());

                // When & Then - 조건에 맞는 알림이 없는 경우
                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isEmpty());
            }
        }

        @Test
        @DisplayName("메트릭 조회 - 조건에 맞는 데이터가 없는 경우")
        void getMetrics_NoMatchingData_ReturnsEmptyList() throws Exception {
            // Given - 조건에 맞는 메트릭이 없는 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getMetrics(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(Arrays.asList());

                // When & Then - 조건에 맞는 메트릭이 없는 경우
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isEmpty());
            }
        }

        @Test
        @DisplayName("로그 조회 - 조건에 맞는 데이터가 없는 경우")
        void getLogs_NoMatchingData_ReturnsEmptyList() throws Exception {
            // Given - 조건에 맞는 로그가 없는 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getLogs(any(), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                        .thenReturn(Arrays.asList());

                // When & Then - 조건에 맞는 로그가 없는 경우
                mockMvc.perform(get("/api/monitoring/logs")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("HTTP 메서드 검증 테스트")
    class HttpMethodValidationTest {

        @Test
        @DisplayName("대시보드 조회 - 잘못된 HTTP 메서드 (POST)")
        void getDashboardData_InvalidHttpMethod_ReturnsMethodNotAllowed() throws Exception {
            // Given & When & Then - POST 메서드로 GET 엔드포인트를 호출하는 경우
            mockMvc.perform(post("/api/monitoring/dashboard")
                            .header("X-Tenant-Id", testTenantId))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("알림 조회 - 잘못된 HTTP 메서드 (PUT)")
        void getAlerts_InvalidHttpMethod_ReturnsMethodNotAllowed() throws Exception {
            // Given & When & Then - PUT 메서드로 GET 엔드포인트를 호출하는 경우
            mockMvc.perform(put("/api/monitoring/alerts")
                            .header("X-Tenant-Id", testTenantId))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("메트릭 조회 - 잘못된 HTTP 메서드 (DELETE)")
        void getMetrics_InvalidHttpMethod_ReturnsMethodNotAllowed() throws Exception {
            // Given & When & Then - DELETE 메서드로 GET 엔드포인트를 호출하는 경우
            mockMvc.perform(delete("/api/monitoring/dashboard/metrics")
                            .header("X-Tenant-Id", testTenantId))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("로그 조회 - 잘못된 HTTP 메서드 (PATCH)")
        void getLogs_InvalidHttpMethod_ReturnsMethodNotAllowed() throws Exception {
            // Given & When & Then - PATCH 메서드로 GET 엔드포인트를 호출하는 경우
            mockMvc.perform(patch("/api/monitoring/logs")
                            .header("X-Tenant-Id", testTenantId))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("성능 및 응답 시간 테스트")
    class PerformanceTest {

        @Test
        @DisplayName("대시보드 조회 - 응답 시간 측정")
        void getDashboardData_PerformanceTest() throws Exception {
            // Given - 성능 테스트를 위한 Mock 설정
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getDashboardData(testTenantId))
                        .thenReturn(sampleDashboardData);

                // When - 응답 시간 측정
                long startTime = System.currentTimeMillis();
                
                mockMvc.perform(get("/api/monitoring/dashboard")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));

                long responseTime = System.currentTimeMillis() - startTime;
                
                // Then - 응답 시간이 1초 이내여야 함
                assertThat(responseTime).isLessThan(1000);
            }
        }

        @Test
        @DisplayName("알림 조회 - 응답 시간 측정")
        void getAlerts_PerformanceTest() throws Exception {
            // Given - 성능 테스트를 위한 Mock 설정
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getAlerts(any(), anyInt(), anyInt(), any(), any(), any(), any()))
                        .thenReturn(sampleAlerts);

                // When - 응답 시간 측정
                long startTime = System.currentTimeMillis();
                
                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));

                long responseTime = System.currentTimeMillis() - startTime;
                
                // Then - 응답 시간이 1초 이내여야 함
                assertThat(responseTime).isLessThan(1000);
            }
        }
    }

    @Nested
    @DisplayName("Content-Type 검증 테스트")
    class ContentTypeValidationTest {

        @Test
        @DisplayName("대시보드 조회 - 잘못된 Content-Type (현재 구현에서는 허용)")
        void getDashboardData_InvalidContentType_ReturnsOk() throws Exception {
            // Given - 잘못된 Content-Type으로 요청하는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getDashboardData(testTenantId))
                        .thenReturn(sampleDashboardData);

                // When & Then - 잘못된 Content-Type도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/dashboard")
                                .header("X-Tenant-Id", testTenantId)
                                .contentType(MediaType.APPLICATION_XML))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("알림 조회 - 잘못된 Content-Type (현재 구현에서는 허용)")
        void getAlerts_InvalidContentType_ReturnsOk() throws Exception {
            // Given - 잘못된 Content-Type으로 요청하는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getAlerts(any(), anyInt(), anyInt(), any(), any(), any(), any()))
                        .thenReturn(sampleAlerts);

                // When & Then - 잘못된 Content-Type도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", testTenantId)
                                .contentType(MediaType.APPLICATION_XML))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }
    }

    @Nested
    @DisplayName("요청 크기 제한 테스트")
    class RequestSizeLimitTest {

        @Test
        @DisplayName("알림 조회 - 너무 큰 페이지 크기 (현재 구현에서는 허용)")
        void getAlerts_TooLargePageSize_ReturnsOk() throws Exception {
            // Given - 최대 허용 크기를 초과하는 페이지 크기로 요청하는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getAlerts(any(), anyInt(), anyInt(), any(), any(), any(), any()))
                        .thenReturn(sampleAlerts);

                // When & Then - 너무 큰 페이지 크기도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", testTenantId)
                                .param("page", "0")
                                .param("size", "10000")) // 너무 큰 페이지 크기
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("메트릭 조회 - 너무 큰 페이지 크기 (현재 구현에서는 허용)")
        void getMetrics_TooLargePageSize_ReturnsOk() throws Exception {
            // Given - 최대 허용 크기를 초과하는 페이지 크기로 요청하는 경우
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(testTenantId);
                
                when(dashboardService.getMetrics(any(), anyInt(), anyInt(), any(), any()))
                        .thenReturn(sampleMetrics);

                // When & Then - 너무 큰 페이지 크기도 현재 구현에서는 200 OK로 처리됨
                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", testTenantId)
                                .param("page", "0")
                                .param("size", "10000")) // 너무 큰 페이지 크기
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }
    }

    @Nested
    @DisplayName("테넌트 검증 및 격리 테스트")
    class TenantValidationTest {

        @Test
        @DisplayName("대시보드 조회 - 테넌트 ID 누락 시 기본값 사용")
        void getDashboardData_MissingTenantId_UsesDefaultTenant() throws Exception {
            // Given - 테넌트 ID가 없는 경우
            when(dashboardService.getDashboardData("default-tenant"))
                    .thenReturn(sampleDashboardData);

            // When & Then - 기본 테넌트로 처리됨
            mockMvc.perform(get("/api/monitoring/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("알림 조회 - 다른 테넌트 간 데이터 격리")
        void getAlerts_DifferentTenants_DataIsolation() throws Exception {
            // Given - 두 개의 다른 테넌트
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";
            
            List<DashboardData.AlertSummary> tenant1Alerts = List.of(
                    DashboardData.AlertSummary.builder()
                            .id(1L)
                            .level("ERROR")
                            .message("Tenant 1 Alert")
                            .source("system")
                            .timestamp(LocalDateTime.now())
                            .build()
            );
            
            List<DashboardData.AlertSummary> tenant2Alerts = List.of(
                    DashboardData.AlertSummary.builder()
                            .id(2L)
                            .level("WARNING")
                            .message("Tenant 2 Alert")
                            .source("application")
                            .timestamp(LocalDateTime.now())
                            .build()
            );

            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // When - 첫 번째 테넌트로 요청
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(tenant1);
                when(dashboardService.getAlerts(eq(tenant1), anyInt(), anyInt(), any(), any(), any(), any()))
                        .thenReturn(tenant1Alerts);

                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", tenant1))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].message").value("Tenant 1 Alert"));

                // When - 두 번째 테넌트로 요청
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(tenant2);
                when(dashboardService.getAlerts(eq(tenant2), anyInt(), anyInt(), any(), any(), any(), any()))
                        .thenReturn(tenant2Alerts);

                mockMvc.perform(get("/api/monitoring/alerts")
                                .header("X-Tenant-Id", tenant2))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].message").value("Tenant 2 Alert"));
            }
        }

        @Test
        @DisplayName("메트릭 조회 - 테넌트별 메트릭 데이터 격리")
        void getMetrics_TenantSpecificData_Isolation() throws Exception {
            // Given - 테넌트별 메트릭 데이터
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";
            
            List<Metric> tenant1Metrics = List.of(
                    Metric.builder()
                            .metricName("cpu.usage")
                            .metricValue(75.5)
                            .unit("%")
                            .tenantId(tenant1)
                            .build()
            );
            
            List<Metric> tenant2Metrics = List.of(
                    Metric.builder()
                            .metricName("memory.usage")
                            .metricValue(60.2)
                            .unit("%")
                            .tenantId(tenant2)
                            .build()
            );

            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // When - 첫 번째 테넌트로 요청
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(tenant1);
                when(dashboardService.getMetrics(eq(tenant1), anyInt(), anyInt(), any(), any()))
                        .thenReturn(tenant1Metrics);

                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", tenant1))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].metricName").value("cpu.usage"));

                // When - 두 번째 테넌트로 요청
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(tenant2);
                when(dashboardService.getMetrics(eq(tenant2), anyInt(), anyInt(), any(), any()))
                        .thenReturn(tenant2Metrics);

                mockMvc.perform(get("/api/monitoring/dashboard/metrics")
                                .header("X-Tenant-Id", tenant2))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].metricName").value("memory.usage"));
            }
        }

        @Test
        @DisplayName("로그 조회 - 테넌트별 로그 데이터 격리")
        void getLogs_TenantSpecificData_Isolation() throws Exception {
            // Given - 테넌트별 로그 데이터
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";
            
            List<LogEntryResponse> tenant1Logs = List.of(
                    LogEntryResponse.builder()
                            .id(1L)
                            .level("ERROR")
                            .message("Tenant 1 Error")
                            .tenantId(tenant1)
                            .build()
            );
            
            List<LogEntryResponse> tenant2Logs = List.of(
                    LogEntryResponse.builder()
                            .id(2L)
                            .level("INFO")
                            .message("Tenant 2 Info")
                            .tenantId(tenant2)
                            .build()
            );

            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // When - 첫 번째 테넌트로 요청
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(tenant1);
                when(dashboardService.getLogs(eq(tenant1), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                        .thenReturn(tenant1Logs);

                mockMvc.perform(get("/api/monitoring/logs")
                                .header("X-Tenant-Id", tenant1))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].message").value("Tenant 1 Error"));

                // When - 두 번째 테넌트로 요청
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(tenant2);
                when(dashboardService.getLogs(eq(tenant2), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                        .thenReturn(tenant2Logs);

                mockMvc.perform(get("/api/monitoring/logs")
                                .header("X-Tenant-Id", tenant2))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].message").value("Tenant 2 Info"));
            }
        }
    }

    // === Helper Methods ===

    private DashboardData createSampleDashboardData() {
        DashboardData.HealthSummary healthSummary = DashboardData.HealthSummary.builder()
                .overallStatus("HEALTHY")
                .totalServices(5)
                .healthyServices(4)
                .warningServices(1)
                .criticalServices(0)
                .maintenanceServices(0)
                .lastCheckTime(LocalDateTime.now())
                .build();

        DashboardData.ServiceStatus serviceStatus = DashboardData.ServiceStatus.builder()
                .serviceName("api-service")
                .status("HEALTHY")
                .message("정상 동작 중")
                .lastCheckTime(LocalDateTime.now())
                .details(Map.of("responseTime", 150))
                .build();

        DashboardData.AlertSummary alertSummary = DashboardData.AlertSummary.builder()
                .id(1L)
                .level("WARNING")
                .message("CPU 사용률이 높습니다")
                .source("system")
                .timestamp(LocalDateTime.now().minusMinutes(10))
                .resolved(false)
                .build();

        DashboardData.MetricSummary metricSummary = DashboardData.MetricSummary.builder()
                .averageResponseTime(120.5)
                .successRate(99.2)
                .totalRequests(1000L)
                .errorCount(8L)
                .lastUpdated(LocalDateTime.now())
                .build();

        DashboardData.MaintenanceStatus maintenanceStatus = DashboardData.MaintenanceStatus.builder()
                .isActive(false)
                .build();

        DashboardData.LogSummary logSummary = DashboardData.LogSummary.builder()
                .totalLogs(500L)
                .successLogs(450L)
                .failureLogs(30L)
                .warningLogs(15L)
                .errorLogs(5L)
                .lastLogTime(LocalDateTime.now().minusMinutes(5))
                .build();

        return DashboardData.builder()
                .healthSummary(healthSummary)
                .serviceStatuses(Arrays.asList(serviceStatus))
                .recentAlerts(Arrays.asList(alertSummary))
                .metricSummary(metricSummary)
                .maintenanceStatus(maintenanceStatus)
                .logSummary(logSummary)
                .generatedAt(LocalDateTime.now())
                .tenantId(testTenantId)
                .build();
    }

    private List<DashboardData.AlertSummary> createSampleAlerts() {
        DashboardData.AlertSummary alert1 = DashboardData.AlertSummary.builder()
                .id(1L)
                .level("WARNING")
                .message("CPU 사용률이 높습니다")
                .source("system")
                .timestamp(LocalDateTime.now().minusMinutes(10))
                .resolved(false)
                .build();

        DashboardData.AlertSummary alert2 = DashboardData.AlertSummary.builder()
                .id(2L)
                .level("ERROR")
                .message("메모리 부족 경고")
                .source("monitoring")
                .timestamp(LocalDateTime.now().minusMinutes(5))
                .resolved(true)
                .build();

        return Arrays.asList(alert1, alert2);
    }

    private List<Metric> createSampleMetrics() {
        Metric metric1 = Metric.builder()
                .tenantId(testTenantId)
                .metricName("cpu.usage")
                .metricType(Metric.MetricType.SYSTEM)
                .metricValue(75.5)
                .unit("percent")
                .status(Metric.Status.ACTIVE)
                .collectedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        Metric metric2 = Metric.builder()
                .tenantId(testTenantId)
                .metricName("memory.usage")
                .metricType(Metric.MetricType.SYSTEM)
                .metricValue(60.2)
                .unit("percent")
                .status(Metric.Status.ACTIVE)
                .collectedAt(LocalDateTime.now().minusMinutes(3))
                .build();

        return Arrays.asList(metric1, metric2);
    }

    private List<LogEntryResponse> createSampleLogs() {
        LogEntryResponse log1 = LogEntryResponse.builder()
                .id(1L)
                .level("INFO")
                .type("alert")
                .source("monitoring")
                .message("시스템 상태 정상")
                .service("monitoring")
                .component("alert")
                .timestamp(LocalDateTime.now().minusMinutes(10))
                .metadata(Map.of("alertName", "system-health"))
                .tenantId(testTenantId)
                .build();

        LogEntryResponse log2 = LogEntryResponse.builder()
                .id(2L)
                .level("ERROR")
                .type("failure")
                .source("monitoring")
                .message("메트릭 수집 실패: database connection timeout")
                .service("monitoring")
                .component("metric")
                .timestamp(LocalDateTime.now().minusMinutes(5))
                .metadata(Map.of("metricName", "db.connection", "errorCode", "TIMEOUT"))
                .tenantId(testTenantId)
                .build();

        return Arrays.asList(log1, log2);
    }
}
