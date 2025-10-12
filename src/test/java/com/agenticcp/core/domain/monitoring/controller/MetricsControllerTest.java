package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.service.MetricsCollectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MetricsController 단위 테스트
 * API 엔드포인트의 핵심 비즈니스 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 */
@ExtendWith(MockitoExtension.class)
class MetricsControllerTest {

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private MetricsCollectionService metricsCollectionService;

    @InjectMocks
    private MetricsController metricsController;

    private Metric testMetric;

    @BeforeEach
    void setUp() {
        testMetric = Metric.builder()
                .metricName("cpu.usage")
                .metricValue(75.5)
                .unit("%")
                .metricType(Metric.MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .tenantId("test-tenant")
                .build();
    }

    /**
     * 메트릭 목록 조회 테스트 그룹
     * 시나리오 1: 전체 메트릭 목록 조회 성공
     * 시나리오 2: 메트릭 이름으로 필터링 조회 성공
     * 시나리오 3: 메트릭 타입으로 필터링 조회 성공
     * 시나리오 4: 빈 결과 조회 성공
     */
    @Nested
    @DisplayName("메트릭 목록 조회 테스트")
    class GetMetricsTest {

        @Test
        @DisplayName("전체 메트릭 목록 조회 성공")
        void getMetrics_WhenNoFilters_ShouldReturnAllMetrics() {
            // 테스트 케이스: 전체 메트릭 목록 조회 성공
            // 목적: 필터 없이 모든 메트릭을 정상적으로 조회하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 수가 예상과 일치하는지
            // 4. repository.findAll()이 호출되는지
            
            // Given
            TenantContextHolder.setTenantKey("test-tenant");
            
            try {
                Pageable pageable = PageRequest.of(0, 10);
                List<Metric> metrics = Arrays.asList(testMetric, createTestMetric("memory.usage"));
                Page<Metric> metricPage = new PageImpl<>(metrics, pageable, 2);
                when(metricRepository.findByTenantId(anyString(), any(Pageable.class))).thenReturn(metricPage);

                // When
                ResponseEntity<ApiResponse<Page<Metric>>> response = 
                    metricsController.getMetrics(null, null, pageable);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData().getContent()).hasSize(2);
                verify(metricRepository).findByTenantId(anyString(), any(Pageable.class));
            } finally {
                TenantContextHolder.clear();
            }
        }

        @Test
        @DisplayName("메트릭 이름으로 필터링 조회 성공")
        void getMetrics_WhenMetricNameFilter_ShouldReturnFilteredMetrics() {
            // 테스트 케이스: 메트릭 이름으로 필터링 조회 성공
            // 목적: 특정 메트릭 이름으로 필터링된 결과를 정상적으로 조회하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 수가 1개인지
            // 4. repository.findLatestByMetricName()이 호출되는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                String metricName = "cpu.usage";
                Pageable pageable = PageRequest.of(0, 10);
                List<Metric> metrics = Arrays.asList(testMetric);
                when(metricRepository.findLatestByMetricName(eq(metricName), anyString(), any(Pageable.class))).thenReturn(metrics);

                // When
                ResponseEntity<ApiResponse<Page<Metric>>> response = 
                    metricsController.getMetrics(metricName, null, pageable);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData().getContent()).hasSize(1);
                verify(metricRepository).findLatestByMetricName(eq(metricName), anyString(), any(Pageable.class));
            }
        }

        @Test
        @DisplayName("메트릭 타입으로 필터링 조회 성공")
        void getMetrics_WhenMetricTypeFilter_ShouldReturnFilteredMetrics() {
            // 테스트 케이스: 메트릭 타입으로 필터링 조회 성공
            // 목적: 특정 메트릭 타입으로 필터링된 결과를 정상적으로 조회하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 수가 예상과 일치하는지
            // 4. repository.findByMetricType()이 호출되는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                Metric.MetricType metricType = Metric.MetricType.SYSTEM;
                Pageable pageable = PageRequest.of(0, 10);
                List<Metric> metrics = Arrays.asList(testMetric);
                Page<Metric> metricPage = new PageImpl<>(metrics, pageable, 1);
                when(metricRepository.findByMetricType(eq(metricType), anyString(), eq(pageable))).thenReturn(metricPage);

                // When
                ResponseEntity<ApiResponse<Page<Metric>>> response = 
                    metricsController.getMetrics(null, metricType, pageable);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData().getContent()).hasSize(1);
                verify(metricRepository).findByMetricType(eq(metricType), anyString(), eq(pageable));
            }
        }

        @Test
        @DisplayName("빈 결과 조회 성공")
        void getMetrics_WhenNoResults_ShouldReturnEmptyList() {
            // 테스트 케이스: 빈 결과 조회 성공
            // 목적: 조회 결과가 없을 때 빈 목록을 정상적으로 반환하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 목록이 비어있는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                Pageable pageable = PageRequest.of(0, 10);
                when(metricRepository.findByTenantId(anyString(), any(Pageable.class))).thenReturn(Page.empty());

                // When
                ResponseEntity<ApiResponse<Page<Metric>>> response = 
                    metricsController.getMetrics(null, null, pageable);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData().getContent()).isEmpty();
            }
        }
    }

    /**
     * 특정 메트릭 조회 테스트 그룹
     * 시나리오 5: 메트릭 이름으로 조회 성공
     * 시나리오 6: 시간 범위로 조회 성공
     * 시나리오 7: 메트릭을 찾을 수 없는 경우
     * 시나리오 8: 잘못된 메트릭 이름으로 조회 실패
     */
    @Nested
    @DisplayName("특정 메트릭 조회 테스트")
    class GetMetricByNameTest {

        @Test
        @DisplayName("메트릭 이름으로 조회 성공")
        void getMetricByName_WhenValidName_ShouldReturnMetrics() {
            // 테스트 케이스: 메트릭 이름으로 조회 성공
            // 목적: 유효한 메트릭 이름으로 메트릭을 정상적으로 조회하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 수가 예상과 일치하는지
            // 4. 반환된 메트릭의 이름이 요청한 이름과 일치하는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                String metricName = "cpu.usage";
                List<Metric> metrics = Arrays.asList(testMetric);
                when(metricRepository.findLatestByMetricName(eq(metricName), anyString(), any(Pageable.class))).thenReturn(metrics);

                // When
                ResponseEntity<ApiResponse<List<Metric>>> response = 
                    metricsController.getMetricByName(metricName, null, null);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).hasSize(1);
                assertThat(response.getBody().getData().get(0).getMetricName()).isEqualTo(metricName);
            }
        }

        @Test
        @DisplayName("시간 범위로 조회 성공")
        void getMetricByName_WhenTimeRange_ShouldReturnMetrics() {
            // 테스트 케이스: 시간 범위로 조회 성공
            // 목적: 특정 시간 범위 내의 메트릭을 정상적으로 조회하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 수가 예상과 일치하는지
            // 4. repository.findByMetricNameAndTimeRange()가 호출되는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                String metricName = "cpu.usage";
                LocalDateTime startTime = LocalDateTime.now().minusHours(1);
                LocalDateTime endTime = LocalDateTime.now();
                List<Metric> metrics = Arrays.asList(testMetric);
                when(metricRepository.findByMetricNameAndTimeRange(eq(metricName), anyString(), eq(startTime), eq(endTime)))
                    .thenReturn(metrics);

                // When
                ResponseEntity<ApiResponse<List<Metric>>> response = 
                    metricsController.getMetricByName(metricName, startTime, endTime);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).hasSize(1);
                verify(metricRepository).findByMetricNameAndTimeRange(eq(metricName), anyString(), eq(startTime), eq(endTime));
            }
        }

        @Test
        @DisplayName("메트릭을 찾을 수 없는 경우 ResourceNotFoundException 발생")
        void getMetricByName_WhenMetricNotFound_ShouldThrowResourceNotFoundException() {
            // 테스트 케이스: 메트릭을 찾을 수 없는 경우 예외 발생
            // 목적: 존재하지 않는 메트릭을 조회할 때 적절한 예외가 발생하는지 확인
            // 검증 항목:
            // 1. ResourceNotFoundException이 발생하는지
            // 2. 예외가 발생한 후 서비스가 정상적으로 종료되는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                String metricName = "nonexistent.metric";
                when(metricRepository.findLatestByMetricName(eq(metricName), anyString(), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

                // When & Then
                assertThatThrownBy(() -> metricsController.getMetricByName(metricName, null, null))
                    .isInstanceOf(ResourceNotFoundException.class);
            }
        }

        @Test
        @DisplayName("잘못된 메트릭 이름으로 조회 실패")
        void getMetricByName_WhenInvalidName_ShouldThrowBusinessException() {
            // 테스트 케이스: 잘못된 메트릭 이름으로 조회 실패
            // 목적: 빈 문자열이나 null 메트릭 이름으로 조회할 때 적절한 예외가 발생하는지 확인
            // 검증 항목:
            // 1. BusinessException이 발생하는지
            // 2. 예외 메시지가 적절한지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                // When & Then
                assertThatThrownBy(() -> metricsController.getMetricByName("", null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("메트릭 이름이 유효하지 않습니다.");
            }
        }

        @Test
        @DisplayName("잘못된 시간 범위로 조회 실패")
        void getMetricByName_WhenInvalidTimeRange_ShouldThrowBusinessException() {
            // 테스트 케이스: 잘못된 시간 범위로 조회 실패
            // 목적: 시작 시간이 종료 시간보다 늦을 때 적절한 예외가 발생하는지 확인
            // 검증 항목:
            // 1. BusinessException이 발생하는지
            // 2. 예외 메시지가 적절한지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                String metricName = "cpu.usage";
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = LocalDateTime.now().minusHours(1);

                // When & Then
                assertThatThrownBy(() -> metricsController.getMetricByName(metricName, startTime, endTime))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("시작 시간이 종료 시간보다 늦을 수 없습니다.");
            }
        }
    }

    /**
     * 메트릭 트렌드 조회 테스트 그룹
     * 시나리오 9: 기본 시간 범위로 트렌드 조회 성공
     * 시나리오 10: 특정 시간부터 트렌드 조회 성공
     * 시나리오 11: 빈 트렌드 결과 조회 성공
     */
    @Nested
    @DisplayName("메트릭 트렌드 조회 테스트")
    class GetMetricsTrendTest {

        @Test
        @DisplayName("기본 시간 범위로 트렌드 조회 성공")
        void getMetricsTrend_WhenNoSinceTime_ShouldReturnTrendMetrics() {
            // 테스트 케이스: 기본 시간 범위로 트렌드 조회 성공
            // 목적: sinceTime 파라미터 없이 기본 시간 범위로 트렌드를 정상적으로 조회하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 수가 예상과 일치하는지
            // 4. repository.findSince()가 호출되는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                List<Metric> metrics = Arrays.asList(testMetric, createTestMetric("memory.usage"));
                when(metricRepository.findSince(anyString(), any(LocalDateTime.class))).thenReturn(metrics);

                // When
                ResponseEntity<ApiResponse<List<Metric>>> response = 
                    metricsController.getMetricsTrend(null);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).hasSize(2);
                verify(metricRepository).findSince(anyString(), any(LocalDateTime.class));
            }
        }

        @Test
        @DisplayName("특정 시간부터 트렌드 조회 성공")
        void getMetricsTrend_WhenSinceTimeProvided_ShouldReturnTrendMetrics() {
            // 테스트 케이스: 특정 시간부터 트렌드 조회 성공
            // 목적: 특정 시간부터 트렌드를 정상적으로 조회하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 수가 예상과 일치하는지
            // 4. repository.findSince()가 올바른 파라미터로 호출되는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                LocalDateTime sinceTime = LocalDateTime.now().minusHours(2);
                List<Metric> metrics = Arrays.asList(testMetric);
                when(metricRepository.findSince(anyString(), eq(sinceTime))).thenReturn(metrics);

                // When
                ResponseEntity<ApiResponse<List<Metric>>> response = 
                    metricsController.getMetricsTrend(sinceTime);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).hasSize(1);
                verify(metricRepository).findSince(anyString(), eq(sinceTime));
            }
        }

        @Test
        @DisplayName("빈 트렌드 결과 조회 성공")
        void getMetricsTrend_WhenNoResults_ShouldReturnEmptyList() {
            // 테스트 케이스: 빈 트렌드 결과 조회 성공
            // 목적: 트렌드 조회 결과가 없을 때 빈 목록을 정상적으로 반환하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 목록이 비어있는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                when(metricRepository.findSince(anyString(), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

                // When
                ResponseEntity<ApiResponse<List<Metric>>> response = 
                    metricsController.getMetricsTrend(null);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).isEmpty();
            }
        }
    }

    /**
     * 수동 메트릭 수집 테스트 그룹
     * 시나리오 12: 수동 메트릭 수집 성공
     * 시나리오 13: 수동 메트릭 수집 중 BusinessException 발생
     * 시나리오 14: 수동 메트릭 수집 중 예상치 못한 예외 발생
     */
    @Nested
    @DisplayName("수동 메트릭 수집 테스트")
    class CollectMetricsTest {

        @Test
        @DisplayName("수동 메트릭 수집 성공")
        void collectMetrics_WhenSuccessful_ShouldReturnSuccessMessage() {
            // 테스트 케이스: 수동 메트릭 수집 성공
            // 목적: 수동 메트릭 수집이 정상적으로 완료되는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 성공 메시지가 반환되는지
            // 4. metricsCollectionService.collectMetricsManually()가 호출되는지
            
            // Given
            doNothing().when(metricsCollectionService).collectMetricsManually();

            // When
            ResponseEntity<ApiResponse<String>> response = 
                metricsController.collectMetrics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo("메트릭 수집이 완료되었습니다.");
            verify(metricsCollectionService).collectMetricsManually();
        }

        @Test
        @DisplayName("수동 메트릭 수집 중 BusinessException 발생")
        void collectMetrics_WhenBusinessException_ShouldThrowBusinessException() {
            // 테스트 케이스: 수동 메트릭 수집 중 BusinessException 발생
            // 목적: 메트릭 수집 중 비즈니스 예외가 발생할 때 적절히 처리되는지 확인
            // 검증 항목:
            // 1. BusinessException이 발생하는지
            // 2. 예외가 올바르게 전파되는지
            
            // Given
            BusinessException businessException = new BusinessException(MonitoringErrorCode.METRICS_COLLECTION_FAILED);
            doThrow(businessException).when(metricsCollectionService).collectMetricsManually();

            // When & Then
            assertThatThrownBy(() -> metricsController.collectMetrics())
                .isInstanceOf(BusinessException.class)
                .isEqualTo(businessException);
        }

        @Test
        @DisplayName("수동 메트릭 수집 중 예상치 못한 예외 발생")
        void collectMetrics_WhenUnexpectedException_ShouldThrowBusinessException() {
            // 테스트 케이스: 수동 메트릭 수집 중 예상치 못한 예외 발생
            // 목적: 예상치 못한 예외가 발생할 때 적절히 처리되는지 확인
            // 검증 항목:
            // 1. BusinessException이 발생하는지
            // 2. 예외 메시지가 적절한지
            
            // Given
            RuntimeException runtimeException = new RuntimeException("Unexpected error");
            doThrow(runtimeException).when(metricsCollectionService).collectMetricsManually();

            // When & Then
            assertThatThrownBy(() -> metricsController.collectMetrics())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("메트릭 수집 중 예상치 못한 오류가 발생했습니다.");
        }
    }

    /**
     * 메트릭 이름 목록 조회 테스트 그룹
     * 시나리오 15: 메트릭 이름 목록 조회 성공
     * 시나리오 16: 빈 메트릭 이름 목록 조회 성공
     * 시나리오 17: 메트릭 이름 목록 조회 중 예외 발생
     */
    @Nested
    @DisplayName("메트릭 이름 목록 조회 테스트")
    class GetMetricNamesTest {

        @Test
        @DisplayName("메트릭 이름 목록 조회 성공")
        void getMetricNames_WhenSuccessful_ShouldReturnMetricNames() {
            // 테스트 케이스: 메트릭 이름 목록 조회 성공
            // 목적: 모든 메트릭 이름을 정상적으로 조회하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 이름 수가 예상과 일치하는지
            // 4. 반환된 메트릭 이름들이 예상과 일치하는지
            // 5. repository.findDistinctMetricNames()가 호출되는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                List<String> metricNames = Arrays.asList("cpu.usage", "memory.usage", "disk.usage");
                when(metricRepository.findDistinctMetricNames(anyString())).thenReturn(metricNames);

                // When
                ResponseEntity<ApiResponse<List<String>>> response = 
                    metricsController.getMetricNames();

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).hasSize(3);
                assertThat(response.getBody().getData()).containsExactlyInAnyOrder("cpu.usage", "memory.usage", "disk.usage");
                verify(metricRepository).findDistinctMetricNames(anyString());
            }
        }

        @Test
        @DisplayName("빈 메트릭 이름 목록 조회 성공")
        void getMetricNames_WhenNoResults_ShouldReturnEmptyList() {
            // 테스트 케이스: 빈 메트릭 이름 목록 조회 성공
            // 목적: 메트릭 이름이 없을 때 빈 목록을 정상적으로 반환하는지 확인
            // 검증 항목:
            // 1. HTTP 상태 코드가 200 OK인지
            // 2. 응답이 성공 상태인지
            // 3. 반환된 메트릭 이름 목록이 비어있는지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                when(metricRepository.findDistinctMetricNames(anyString())).thenReturn(Collections.emptyList());

                // When
                ResponseEntity<ApiResponse<List<String>>> response = 
                    metricsController.getMetricNames();

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).isEmpty();
            }
        }

        @Test
        @DisplayName("메트릭 이름 목록 조회 중 예외 발생")
        void getMetricNames_WhenException_ShouldThrowBusinessException() {
            // 테스트 케이스: 메트릭 이름 목록 조회 중 예외 발생
            // 목적: 데이터베이스 오류 등으로 예외가 발생할 때 적절히 처리되는지 확인
            // 검증 항목:
            // 1. BusinessException이 발생하는지
            // 2. 예외 메시지가 적절한지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                RuntimeException runtimeException = new RuntimeException("Database error");
                when(metricRepository.findDistinctMetricNames(anyString())).thenThrow(runtimeException);

                // When & Then
                assertThatThrownBy(() -> metricsController.getMetricNames())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("메트릭 이름 목록 조회 중 오류가 발생했습니다.");
            }
        }
    }

    /**
     * 예외 처리 테스트 그룹
     * 시나리오 18: Repository 예외 발생 시 BusinessException 변환
     * 시나리오 19: 잘못된 파라미터로 인한 BusinessException 발생
     */
    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("Repository 예외 발생 시 BusinessException 변환")
        void getMetrics_WhenRepositoryException_ShouldThrowBusinessException() {
            // 테스트 케이스: Repository 예외 발생 시 BusinessException 변환
            // 목적: 데이터베이스 연결 실패 등으로 예외가 발생할 때 적절히 처리되는지 확인
            // 검증 항목:
            // 1. BusinessException이 발생하는지
            // 2. 예외 메시지가 적절한지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                Pageable pageable = PageRequest.of(0, 10);
                RuntimeException runtimeException = new RuntimeException("Database connection failed");
                when(metricRepository.findByTenantId(anyString(), any(Pageable.class))).thenThrow(runtimeException);

                // When & Then
                assertThatThrownBy(() -> metricsController.getMetrics(null, null, pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("메트릭 목록 조회 중 오류가 발생했습니다.");
            }
        }

        @Test
        @DisplayName("잘못된 파라미터로 인한 BusinessException 발생")
        void getMetricByName_WhenNullMetricName_ShouldThrowBusinessException() {
            // 테스트 케이스: 잘못된 파라미터로 인한 BusinessException 발생
            // 목적: null 메트릭 이름으로 조회할 때 적절한 예외가 발생하는지 확인
            // 검증 항목:
            // 1. BusinessException이 발생하는지
            // 2. 예외 메시지가 적절한지
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn("test-tenant");
                
                // When & Then
                assertThatThrownBy(() -> metricsController.getMetricByName(null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("메트릭 이름이 유효하지 않습니다.");
            }
        }
    }

    // 테스트 헬퍼 메서드
    private Metric createTestMetric(String metricName) {
        return Metric.builder()
                .metricName(metricName)
                .metricValue(50.0)
                .unit("%")
                .metricType(Metric.MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .tenantId("test-tenant")
                .build();
    }
}