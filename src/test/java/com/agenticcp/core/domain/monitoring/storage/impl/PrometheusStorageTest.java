package com.agenticcp.core.domain.monitoring.storage.impl;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.StorageType;
import com.agenticcp.core.domain.monitoring.storage.MetricsStorageFactory;
import com.agenticcp.core.domain.monitoring.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PrometheusStorage 단위 테스트
 * <p>
 * PrometheusStorage의 핵심 기능인 메트릭 저장, 조회, 연결 관리, 활성화/비활성화 기능을 테스트합니다.
 * 실제 Prometheus와의 연동 없이 Mock 객체나 가상 로직을 통해 동작을 검증합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("Prometheus 저장소 테스트")
class PrometheusStorageTest {

    private PrometheusStorage prometheusStorage;
    private MetricsStorageFactory.StorageConfig config;

    @BeforeEach
    void setUp() {
        config = TestDataBuilder.prometheusConfig();
        prometheusStorage = new PrometheusStorage(config);
    }

    @Test
    @DisplayName("저장소 타입 확인")
    void getStorageType_ReturnsPrometheus() {
        // when
        StorageType type = prometheusStorage.getStorageType();

        // then
        assertThat(type).isEqualTo(StorageType.PROMETHEUS);
    }

    @Test
    @DisplayName("초기 활성화 상태 확인")
    void isEnabled_InitiallyTrue() {
        // then
        assertThat(prometheusStorage.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("활성화 상태 변경")
    void setEnabled_ChangesState() {
        // when
        prometheusStorage.setEnabled(false);

        // then
        assertThat(prometheusStorage.isEnabled()).isFalse();

        // when
        prometheusStorage.setEnabled(true);

        // then
        assertThat(prometheusStorage.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("초기 연결 상태 확인")
    void isConnected_InitiallyFalse() {
        // then
        assertThat(prometheusStorage.isConnected()).isFalse();
    }

    @Test
    @DisplayName("연결 성공")
    void connect_Success() {
        // when
        prometheusStorage.connect();

        // then
        assertThat(prometheusStorage.isConnected()).isTrue();
    }

    @Test
    @DisplayName("이미 연결된 상태에서 연결 시도")
    void connect_AlreadyConnected() {
        // given
        prometheusStorage.connect(); // 먼저 연결

        // when
        prometheusStorage.connect(); // 다시 연결 시도

        // then
        assertThat(prometheusStorage.isConnected()).isTrue(); // 여전히 연결 상태
    }

    @Test
    @DisplayName("연결 해제 성공")
    void disconnect_Success() {
        // given
        prometheusStorage.connect(); // 먼저 연결

        // when
        prometheusStorage.disconnect();

        // then
        assertThat(prometheusStorage.isConnected()).isFalse();
    }

    @Test
    @DisplayName("이미 연결 해제된 상태에서 해제 시도")
    void disconnect_AlreadyDisconnected() {
        // given
        prometheusStorage.disconnect(); // 먼저 해제 (초기 상태)

        // when
        prometheusStorage.disconnect(); // 다시 해제 시도

        // then
        assertThat(prometheusStorage.isConnected()).isFalse(); // 여전히 해제 상태
    }

    @Test
    @DisplayName("메트릭 저장 시도 (활성화, 연결됨)")
    void saveMetrics_EnabledAndConnected_Success() {
        // given
        prometheusStorage.connect(); // 연결
        List<Metric> metrics = createTestMetrics();

        // when
        assertThatCode(() -> prometheusStorage.saveMetrics(metrics))
                .doesNotThrowAnyException(); // 예외가 발생하지 않아야 함
    }

    @Test
    @DisplayName("메트릭 저장 시도 (비활성화)")
    void saveMetrics_Disabled_ThrowsException() {
        // given
        prometheusStorage.setEnabled(false); // 비활성화
        List<Metric> metrics = createTestMetrics();

        // when & then
        assertThatThrownBy(() -> prometheusStorage.saveMetrics(metrics))
                .isInstanceOf(com.agenticcp.core.common.exception.BusinessException.class)
                .hasMessageContaining("비활성화");
    }

    @Test
    @DisplayName("메트릭 저장 시도 (연결 안됨)")
    void saveMetrics_NotConnected_ThrowsException() {
        // given
        prometheusStorage.setEnabled(true); // 활성화
        prometheusStorage.disconnect(); // 연결 해제
        List<Metric> metrics = createTestMetrics();

        // when & then
        assertThatThrownBy(() -> prometheusStorage.saveMetrics(metrics))
                .isInstanceOf(com.agenticcp.core.common.exception.BusinessException.class)
                .hasMessageContaining("연결되지 않았습니다");
    }

    @Test
    @DisplayName("메트릭 조회 시도 (활성화, 연결됨)")
    void getMetrics_EnabledAndConnected_ReturnsEmptyList() {
        // given
        prometheusStorage.connect(); // 연결
        String metricName = "cpu.usage";
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        // when
        List<Metric> result = prometheusStorage.getMetrics(metricName, startTime, endTime);

        // then
        assertThat(result).isEmpty(); // 실제 조회 로직은 TODO이므로 빈 리스트 반환 예상
    }

    @Test
    @DisplayName("메트릭 조회 시도 (비활성화)")
    void getMetrics_Disabled_ThrowsException() {
        // given
        prometheusStorage.setEnabled(false); // 비활성화
        String metricName = "cpu.usage";
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        // when & then
        assertThatThrownBy(() -> prometheusStorage.getMetrics(metricName, startTime, endTime))
                .isInstanceOf(com.agenticcp.core.common.exception.BusinessException.class)
                .hasMessageContaining("비활성화");
    }

    @Test
    @DisplayName("메트릭 조회 시도 (연결 안됨)")
    void getMetrics_NotConnected_ThrowsException() {
        // given
        prometheusStorage.setEnabled(true); // 활성화
        prometheusStorage.disconnect(); // 연결 해제
        String metricName = "cpu.usage";
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        // when & then
        assertThatThrownBy(() -> prometheusStorage.getMetrics(metricName, startTime, endTime))
                .isInstanceOf(com.agenticcp.core.common.exception.BusinessException.class)
                .hasMessageContaining("연결되지 않았습니다");
    }

    /**
     * 테스트용 메트릭 데이터 생성
     */
    private List<Metric> createTestMetrics() {
        return TestDataBuilder.testMetrics();
    }
}
