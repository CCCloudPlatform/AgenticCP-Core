package com.agenticcp.core.domain.monitoring.storage.impl;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.StorageType;
import com.agenticcp.core.domain.monitoring.storage.MetricsStorageFactory;
import com.agenticcp.core.domain.monitoring.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * InfluxDBStorage 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
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

    @Nested
    @DisplayName("저장소 타입 테스트")
    class StorageTypeTest {

        @Test
        @DisplayName("저장소 타입 확인")
        void getStorageType_WhenCalled_ReturnsInfluxDB() {
            // When
            StorageType result = influxDBStorage.getStorageType();

            // Then
            assertThat(result).isEqualTo(StorageType.INFLUXDB);
        }
    }

    @Nested
    @DisplayName("활성화 상태 테스트")
    class EnabledStateTest {

        @Test
        @DisplayName("저장소 활성화 상태 확인")
        void isEnabled_WhenCalled_ReturnsTrue() {
            // When
            boolean result = influxDBStorage.isEnabled();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("저장소 활성화 상태 설정")
        void setEnabled_WhenDisabled_ChangesEnabledState() {
            // When
            influxDBStorage.setEnabled(false);

            // Then
            assertThat(influxDBStorage.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("비활성화 시 연결 해제")
        void setEnabled_WhenDisabled_Disconnects() {
            // Given
            influxDBStorage.connect();
            assertThat(influxDBStorage.isConnected()).isTrue();

            // When
            influxDBStorage.setEnabled(false);

            // Then
            assertThat(influxDBStorage.isEnabled()).isFalse();
            assertThat(influxDBStorage.isConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("메트릭 저장 테스트")
    class SaveMetricsTest {

        @Test
        @DisplayName("메트릭 저장 성공")
        void saveMetrics_WhenCalled_DoesNotThrowException() {
            // Given
            List<Metric> metrics = createTestMetrics();

            // When & Then
            assertThatCode(() -> influxDBStorage.saveMetrics(metrics))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("비활성화된 저장소에서 메트릭 저장 시 무시")
        void saveMetrics_WhenDisabled_DoesNothing() {
            // Given
            influxDBStorage.setEnabled(false);
            List<Metric> metrics = createTestMetrics();

            // When & Then
            assertThatCode(() -> influxDBStorage.saveMetrics(metrics))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("메트릭 조회 테스트")
    class GetMetricsTest {

        @Test
        @DisplayName("메트릭 조회 성공")
        void getMetrics_WhenCalled_ReturnsEmptyList() {
            // Given
            String metricName = "cpu.usage";
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();

            // When
            List<Metric> result = influxDBStorage.getMetrics(metricName, startTime, endTime);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty(); // TODO: 실제 구현 후 수정
        }

        @Test
        @DisplayName("비활성화된 저장소에서 메트릭 조회 시 빈 리스트 반환")
        void getMetrics_WhenDisabled_ReturnsEmptyList() {
            // Given
            influxDBStorage.setEnabled(false);
            String metricName = "cpu.usage";
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();

            // When
            List<Metric> result = influxDBStorage.getMetrics(metricName, startTime, endTime);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("연결 관리 테스트")
    class ConnectionTest {

        @Test
        @DisplayName("연결 상태 확인")
        void isConnected_WhenNotConnected_ReturnsFalse() {
            // When
            boolean result = influxDBStorage.isConnected();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("연결 성공")
        void connect_WhenCalled_ConnectsSuccessfully() {
            // When & Then
            assertThatCode(() -> influxDBStorage.connect())
                    .doesNotThrowAnyException();
            
            // Then
            assertThat(influxDBStorage.isConnected()).isTrue();
        }

        @Test
        @DisplayName("연결 해제 성공")
        void disconnect_WhenConnected_DisconnectsSuccessfully() {
            // Given
            influxDBStorage.connect();
            assertThat(influxDBStorage.isConnected()).isTrue();

            // When
            influxDBStorage.disconnect();

            // Then
            assertThat(influxDBStorage.isConnected()).isFalse();
        }
    }

    /**
     * 테스트용 메트릭 데이터 생성
     */
    private List<Metric> createTestMetrics() {
        return TestDataBuilder.testMetrics();
    }
}
