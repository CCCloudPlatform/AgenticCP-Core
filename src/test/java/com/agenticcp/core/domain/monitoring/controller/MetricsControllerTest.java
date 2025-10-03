package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.monitoring.TestDataBuilder;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.service.MetricsCollectionService;
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
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Metric> metrics = Arrays.asList(
                TestDataBuilder.metricBuilder().build(),
                TestDataBuilder.metricBuilder().metricName("cpu.usage").build()
            );
            Page<Metric> metricPage = new PageImpl<>(metrics, pageable, 2);
            when(metricRepository.findAll(pageable)).thenReturn(metricPage);

            // When
            ResponseEntity<ApiResponse<Page<Metric>>> response = 
                metricsController.getMetrics(null, null, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getContent()).hasSize(2);
            verify(metricRepository).findAll(pageable);
        }

        @Test
        @DisplayName("메트릭 이름으로 필터링 조회 성공")
        void getMetrics_WhenMetricNameFilter_ShouldReturnFilteredMetrics() {
            // Given
            String metricName = "cpu.usage";
            Pageable pageable = PageRequest.of(0, 10);
            List<Metric> metrics = Arrays.asList(
                TestDataBuilder.metricBuilder().metricName(metricName).build()
            );
            when(metricRepository.findLatestByMetricName(metricName, pageable)).thenReturn(metrics);

            // When
            ResponseEntity<ApiResponse<Page<Metric>>> response = 
                metricsController.getMetrics(metricName, null, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getContent()).hasSize(1);
            verify(metricRepository).findLatestByMetricName(metricName, pageable);
        }

        @Test
        @DisplayName("메트릭 타입으로 필터링 조회 성공")
        void getMetrics_WhenMetricTypeFilter_ShouldReturnFilteredMetrics() {
            // Given
            Metric.MetricType metricType = Metric.MetricType.SYSTEM;
            Pageable pageable = PageRequest.of(0, 10);
            List<Metric> metrics = Arrays.asList(
                TestDataBuilder.metricBuilder().metricType(metricType).build()
            );
            Page<Metric> metricPage = new PageImpl<>(metrics, pageable, 1);
            when(metricRepository.findByMetricType(metricType, pageable)).thenReturn(metricPage);

            // When
            ResponseEntity<ApiResponse<Page<Metric>>> response = 
                metricsController.getMetrics(null, metricType, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getContent()).hasSize(1);
            verify(metricRepository).findByMetricType(metricType, pageable);
        }

        @Test
        @DisplayName("빈 결과 조회 성공")
        void getMetrics_WhenNoResults_ShouldReturnEmptyList() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Metric> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(metricRepository.findAll(pageable)).thenReturn(emptyPage);

            // When
            ResponseEntity<ApiResponse<Page<Metric>>> response = 
                metricsController.getMetrics(null, null, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getContent()).isEmpty();
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
            // Given
            String metricName = "cpu.usage";
            List<Metric> metrics = Arrays.asList(
                TestDataBuilder.metricBuilder().metricName(metricName).build()
            );
            when(metricRepository.findLatestByMetricName(eq(metricName), any(Pageable.class)))
                .thenReturn(metrics);

            // When
            ResponseEntity<ApiResponse<List<Metric>>> response = 
                metricsController.getMetricByName(metricName, null, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getMetricName()).isEqualTo(metricName);
        }

        @Test
        @DisplayName("시간 범위로 조회 성공")
        void getMetricByName_WhenTimeRange_ShouldReturnMetrics() {
            // Given
            String metricName = "cpu.usage";
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();
            List<Metric> metrics = Arrays.asList(
                TestDataBuilder.metricBuilder().metricName(metricName).build()
            );
            when(metricRepository.findByMetricNameAndTimeRange(metricName, startTime, endTime))
                .thenReturn(metrics);

            // When
            ResponseEntity<ApiResponse<List<Metric>>> response = 
                metricsController.getMetricByName(metricName, startTime, endTime);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).hasSize(1);
            verify(metricRepository).findByMetricNameAndTimeRange(metricName, startTime, endTime);
        }

        @Test
        @DisplayName("메트릭을 찾을 수 없는 경우 ResourceNotFoundException 발생")
        void getMetricByName_WhenMetricNotFound_ShouldThrowResourceNotFoundException() {
            // Given
            String metricName = "nonexistent.metric";
            when(metricRepository.findLatestByMetricName(eq(metricName), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> metricsController.getMetricByName(metricName, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("잘못된 메트릭 이름으로 조회 실패")
        void getMetricByName_WhenInvalidName_ShouldThrowBusinessException() {
            // When & Then
            assertThatThrownBy(() -> metricsController.getMetricByName("", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("메트릭 이름이 유효하지 않습니다.");
        }

        @Test
        @DisplayName("잘못된 시간 범위로 조회 실패")
        void getMetricByName_WhenInvalidTimeRange_ShouldThrowBusinessException() {
            // Given
            String metricName = "cpu.usage";
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = LocalDateTime.now().minusHours(1);

            // When & Then
            assertThatThrownBy(() -> metricsController.getMetricByName(metricName, startTime, endTime))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("시작 시간이 종료 시간보다 늦을 수 없습니다.");
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
            // Given
            List<Metric> metrics = Arrays.asList(
                TestDataBuilder.metricBuilder().build(),
                TestDataBuilder.metricBuilder().metricName("memory.usage").build()
            );
            when(metricRepository.findSince(any(LocalDateTime.class))).thenReturn(metrics);

            // When
            ResponseEntity<ApiResponse<List<Metric>>> response = 
                metricsController.getMetricsTrend(null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).hasSize(2);
            verify(metricRepository).findSince(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("특정 시간부터 트렌드 조회 성공")
        void getMetricsTrend_WhenSinceTimeProvided_ShouldReturnTrendMetrics() {
            // Given
            LocalDateTime sinceTime = LocalDateTime.now().minusHours(2);
            List<Metric> metrics = Arrays.asList(
                TestDataBuilder.metricBuilder().build()
            );
            when(metricRepository.findSince(sinceTime)).thenReturn(metrics);

            // When
            ResponseEntity<ApiResponse<List<Metric>>> response = 
                metricsController.getMetricsTrend(sinceTime);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).hasSize(1);
            verify(metricRepository).findSince(sinceTime);
        }

        @Test
        @DisplayName("빈 트렌드 결과 조회 성공")
        void getMetricsTrend_WhenNoResults_ShouldReturnEmptyList() {
            // Given
            when(metricRepository.findSince(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

            // When
            ResponseEntity<ApiResponse<List<Metric>>> response = 
                metricsController.getMetricsTrend(null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEmpty();
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
            // Given
            List<String> metricNames = Arrays.asList("cpu.usage", "memory.usage", "disk.usage");
            when(metricRepository.findDistinctMetricNames()).thenReturn(metricNames);

            // When
            ResponseEntity<ApiResponse<List<String>>> response = 
                metricsController.getMetricNames();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).hasSize(3);
            assertThat(response.getBody().getData()).containsExactlyInAnyOrder("cpu.usage", "memory.usage", "disk.usage");
            verify(metricRepository).findDistinctMetricNames();
        }

        @Test
        @DisplayName("빈 메트릭 이름 목록 조회 성공")
        void getMetricNames_WhenNoResults_ShouldReturnEmptyList() {
            // Given
            when(metricRepository.findDistinctMetricNames()).thenReturn(Collections.emptyList());

            // When
            ResponseEntity<ApiResponse<List<String>>> response = 
                metricsController.getMetricNames();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEmpty();
        }

        @Test
        @DisplayName("메트릭 이름 목록 조회 중 예외 발생")
        void getMetricNames_WhenException_ShouldThrowBusinessException() {
            // Given
            RuntimeException runtimeException = new RuntimeException("Database error");
            when(metricRepository.findDistinctMetricNames()).thenThrow(runtimeException);

            // When & Then
            assertThatThrownBy(() -> metricsController.getMetricNames())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("메트릭 이름 목록 조회 중 오류가 발생했습니다.");
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
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            RuntimeException runtimeException = new RuntimeException("Database connection failed");
            when(metricRepository.findAll(pageable)).thenThrow(runtimeException);

            // When & Then
            assertThatThrownBy(() -> metricsController.getMetrics(null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("메트릭 목록 조회 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("잘못된 파라미터로 인한 BusinessException 발생")
        void getMetricByName_WhenNullMetricName_ShouldThrowBusinessException() {
            // When & Then
            assertThatThrownBy(() -> metricsController.getMetricByName(null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("메트릭 이름이 유효하지 않습니다.");
        }
    }
}
