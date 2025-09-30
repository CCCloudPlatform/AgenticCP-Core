package com.agenticcp.core.domain.monitoring.storage.impl;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.StorageType;
import com.agenticcp.core.domain.monitoring.storage.MetricsStorageFactory;
import com.agenticcp.core.domain.monitoring.storage.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * InfluxDBStorage 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("InfluxDB 저장소 테스트")
class InfluxDBStorageTest {

    private InfluxDBStorage influxDBStorage;
    private MetricsStorageFactory.StorageConfig config;

    @BeforeEach
    void setUp() {
        config = TestDataBuilder.influxDBConfig();
        influxDBStorage = new InfluxDBStorage(config);
    }

    @Test
    @DisplayName("저장소 타입 확인")
    void getStorageType_ReturnsInfluxDB() {
        // when
        StorageType result = influxDBStorage.getStorageType();

        // then
        assertThat(result).isEqualTo(StorageType.INFLUXDB);
    }

    @Test
    @DisplayName("저장소 활성화 상태 확인")
    void isEnabled_ReturnsTrue() {
        // when
        boolean result = influxDBStorage.isEnabled();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("저장소 활성화 상태 설정")
    void setEnabled_ChangesEnabledState() {
        // when
        influxDBStorage.setEnabled(false);

        // then
        assertThat(influxDBStorage.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("메트릭 저장 성공")
    void saveMetrics_Success() {
        // given
        List<Metric> metrics = createTestMetrics();

        // when & then
        assertThatCode(() -> influxDBStorage.saveMetrics(metrics))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비활성화된 저장소에서 메트릭 저장 시 무시")
    void saveMetrics_WhenDisabled_DoesNothing() {
        // given
        influxDBStorage.setEnabled(false);
        List<Metric> metrics = createTestMetrics();

        // when & then
        assertThatCode(() -> influxDBStorage.saveMetrics(metrics))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("메트릭 조회 성공")
    void getMetrics_Success() {
        // given
        String metricName = "cpu.usage";
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        // when
        List<Metric> result = influxDBStorage.getMetrics(metricName, startTime, endTime);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty(); // TODO: 실제 구현 후 수정
    }

    @Test
    @DisplayName("비활성화된 저장소에서 메트릭 조회 시 빈 리스트 반환")
    void getMetrics_WhenDisabled_ReturnsEmptyList() {
        // given
        influxDBStorage.setEnabled(false);
        String metricName = "cpu.usage";
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        // when
        List<Metric> result = influxDBStorage.getMetrics(metricName, startTime, endTime);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("연결 상태 확인")
    void isConnected_ReturnsFalse() {
        // when
        boolean result = influxDBStorage.isConnected();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("연결 성공")
    void connect_Success() {
        // when & then
        assertThatCode(() -> influxDBStorage.connect())
                .doesNotThrowAnyException();
        
        // 연결 후 상태 확인
        assertThat(influxDBStorage.isConnected()).isTrue();
    }

    @Test
    @DisplayName("연결 해제 성공")
    void disconnect_Success() {
        // given
        influxDBStorage.connect();
        assertThat(influxDBStorage.isConnected()).isTrue();

        // when
        influxDBStorage.disconnect();

        // then
        assertThat(influxDBStorage.isConnected()).isFalse();
    }

    @Test
    @DisplayName("비활성화 시 연결 해제")
    void setEnabled_False_Disconnects() {
        // given
        influxDBStorage.connect();
        assertThat(influxDBStorage.isConnected()).isTrue();

        // when
        influxDBStorage.setEnabled(false);

        // then
        assertThat(influxDBStorage.isEnabled()).isFalse();
        assertThat(influxDBStorage.isConnected()).isFalse();
    }

    /**
     * 테스트용 메트릭 데이터 생성
     */
    private List<Metric> createTestMetrics() {
        return TestDataBuilder.testMetrics();
    }
}
