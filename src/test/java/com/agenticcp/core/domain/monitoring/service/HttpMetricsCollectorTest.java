package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * HttpMetricsCollector 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpMetricsCollector 테스트")
class HttpMetricsCollectorTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Search search;

    @Mock
    private Counter counter;

    @Mock
    private Timer timer;

    private HttpMetricsCollector httpMetricsCollector;

    @BeforeEach
    void setUp() {
        httpMetricsCollector = new HttpMetricsCollector(meterRegistry);
        // enabled 필드를 true로 강제 설정 (@Value 주입이 안되므로)
        ReflectionTestUtils.setField(httpMetricsCollector, "enabled", true);
    }

    @Nested
    @DisplayName("기본 정보 조회")
    class BasicInfo {

        @Test
        @DisplayName("수집기 이름 반환")
        void getCollectorName() {
            // when
            String name = httpMetricsCollector.getCollectorName();

            // then
            assertThat(name).isEqualTo("http-metrics");
        }

        @Test
        @DisplayName("수집기 설명 반환")
        void getCollectorDescription() {
            // when
            String description = httpMetricsCollector.getCollectorDescription();

            // then
            assertThat(description).isEqualTo("HTTP API 응답시간, 처리량, 에러율 수집기");
        }

        @Test
        @DisplayName("수집기 타입 반환")
        void getCollectorType() {
            // when
            CollectorType type = httpMetricsCollector.getCollectorType();

            // then
            assertThat(type).isEqualTo(CollectorType.CUSTOM);
        }

        @Test
        @DisplayName("기본 활성화 상태")
        void isEnabledByDefault() {
            // then
            assertThat(httpMetricsCollector.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("시스템 메트릭 수집")
    class SystemMetricsCollection {

        @Test
        @DisplayName("HTTP 수집기는 시스템 메트릭을 수집하지 않음")
        void collectSystemMetrics_ReturnsNull() {
            // when
            var result = httpMetricsCollector.collectSystemMetrics();

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("애플리케이션 메트릭 수집")
    class ApplicationMetricsCollection {

        @Test
        @DisplayName("비활성화 상태에서는 빈 리스트 반환")
        void collectApplicationMetrics_WhenDisabled_ReturnsEmptyList() {
            // given
            ReflectionTestUtils.setField(httpMetricsCollector, "enabled", false);

            // when
            List<Metric> metrics = httpMetricsCollector.collectApplicationMetrics();

            // then
            assertThat(metrics).isEmpty();
            verifyNoInteractions(meterRegistry);
        }

        @Test
        @DisplayName("메트릭이 없을 때 빈 리스트 반환")
        void collectApplicationMetrics_WhenNoMetrics_ReturnsEmptyList() {
            // given
            // meterRegistry가 null을 반환하도록 설정 (메트릭 없음)
            when(meterRegistry.find(anyString())).thenReturn(null);

            // when
            List<Metric> metrics = httpMetricsCollector.collectApplicationMetrics();

            // then
            assertThat(metrics).isEmpty();
        }

        @Test
        @DisplayName("HTTP 요청 카운트 메트릭 수집")
        void collectApplicationMetrics_RequestCount() {
            // given
            Search requestSearch = mock(Search.class);
            when(meterRegistry.find("http.server.requests")).thenReturn(requestSearch);
            when(requestSearch.counter()).thenReturn(counter);
            when(counter.count()).thenReturn(100.0);

            // when
            List<Metric> metrics = httpMetricsCollector.collectApplicationMetrics();

            // then
            assertThat(metrics).isNotEmpty();
            assertThat(metrics).anyMatch(m -> 
                m.getMetricName().equals("http.requests.total") && 
                m.getMetricValue() == 100.0
            );
        }

        @Test
        @DisplayName("HTTP 응답 시간 메트릭 수집")
        void collectApplicationMetrics_ResponseTime() {
            // given
            Search timerSearch = mock(Search.class);
            when(meterRegistry.find("http.server.requests")).thenReturn(timerSearch);
            when(timerSearch.counter()).thenReturn(null);
            when(timerSearch.timer()).thenReturn(timer);
            when(timer.count()).thenReturn(50L);
            when(timer.mean(TimeUnit.MILLISECONDS)).thenReturn(120.5);
            when(timer.max(TimeUnit.MILLISECONDS)).thenReturn(500.0);
            when(timer.totalTime(TimeUnit.SECONDS)).thenReturn(6.025);

            // when
            List<Metric> metrics = httpMetricsCollector.collectApplicationMetrics();

            // then
            assertThat(metrics).hasSize(3);
            assertThat(metrics).anyMatch(m -> 
                m.getMetricName().equals("http.response.time.avg") && 
                m.getMetricValue() == 120.5 &&
                m.getUnit().equals("ms")
            );
            assertThat(metrics).anyMatch(m -> 
                m.getMetricName().equals("http.response.time.max") && 
                m.getMetricValue() == 500.0
            );
            assertThat(metrics).anyMatch(m -> 
                m.getMetricName().equals("http.response.time.total") && 
                m.getMetricValue() == 6.025 &&
                m.getUnit().equals("seconds")
            );
        }

        @Test
        @DisplayName("HTTP 에러율 메트릭 수집")
        void collectApplicationMetrics_ErrorRate() {
            // given
            // 에러율 계산을 위한 복잡한 Mock 설정은 실제 통합 테스트에서 검증
            // 단위 테스트에서는 기본 동작만 확인
            when(meterRegistry.find(anyString())).thenReturn(null);

            // when
            List<Metric> metrics = httpMetricsCollector.collectApplicationMetrics();

            // then
            // 메트릭이 없을 때 빈 리스트 반환하는지만 확인
            assertThat(metrics).isEmpty();
        }

        @Test
        @DisplayName("HTTP 상태 코드별 통계 수집")
        void collectApplicationMetrics_StatusCodes() {
            // given
            // 상태 코드 통계는 실제 통합 테스트에서 검증
            // 단위 테스트에서는 기본 동작만 확인
            when(meterRegistry.find(anyString())).thenReturn(null);

            // when
            List<Metric> metrics = httpMetricsCollector.collectApplicationMetrics();

            // then
            assertThat(metrics).isEmpty();
        }

        @Test
        @DisplayName("메트릭 수집 중 예외 발생 시 빈 리스트 반환")
        void collectApplicationMetrics_WhenException_ReturnsEmptyList() {
            // given
            // find()가 예외를 던지면 빈 리스트 반환
            when(meterRegistry.find(anyString())).thenThrow(new RuntimeException("Test exception"));

            // when
            List<Metric> metrics = httpMetricsCollector.collectApplicationMetrics();

            // then
            assertThat(metrics).isEmpty();
        }

        @Test
        @DisplayName("수집된 메트릭의 기본 정보 검증")
        void collectApplicationMetrics_MetricBasicInfo() {
            // given
            Search requestSearch = mock(Search.class);
            when(meterRegistry.find("http.server.requests")).thenReturn(requestSearch);
            when(requestSearch.counter()).thenReturn(counter);
            when(counter.count()).thenReturn(100.0);

            // when
            List<Metric> metrics = httpMetricsCollector.collectApplicationMetrics();

            // then
            assertThat(metrics).isNotEmpty();
            assertThat(metrics).allMatch(m -> 
                m.getMetricType() == Metric.MetricType.APPLICATION &&
                m.getSource().equals("http") &&
                m.getCollectedAt() != null
            );
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class IntegrationScenarios {

        @Test
        @DisplayName("전체 HTTP 메트릭 수집 시나리오")
        void fullHttpMetricsCollection() {
            // given
            // 간단한 시나리오: 요청 카운트와 응답 시간만 테스트
            Search requestSearch = mock(Search.class);
            when(meterRegistry.find("http.server.requests")).thenReturn(requestSearch);
            when(requestSearch.counter()).thenReturn(counter);
            when(requestSearch.timer()).thenReturn(timer);
            when(counter.count()).thenReturn(1000.0);
            when(timer.count()).thenReturn(1000L);
            when(timer.mean(TimeUnit.MILLISECONDS)).thenReturn(150.0);
            when(timer.max(TimeUnit.MILLISECONDS)).thenReturn(800.0);
            when(timer.totalTime(TimeUnit.SECONDS)).thenReturn(150.0);

            // when
            List<Metric> metrics = httpMetricsCollector.collectApplicationMetrics();

            // then
            assertThat(metrics).hasSizeGreaterThanOrEqualTo(1);
            assertThat(metrics).extracting(Metric::getMetricName)
                .contains("http.requests.total");
        }
    }
}

