package com.agenticcp.core.domain.monitoring.storage.impl;

import com.agenticcp.core.common.exception.BusinessException;
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
 * TimescaleDBStorage 단위 테스트
 * <p>
 * TimescaleDBStorage의 핵심 기능인 메트릭 저장, 조회, 연결 관리, 활성화/비활성화 기능을 테스트합니다.
 * 실제 TimescaleDB와의 연동 없이 Mock 객체나 가상 로직을 통해 동작을 검증합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@DisplayName("TimescaleDB 저장소 테스트")
class TimescaleDBStorageTest {

    private TimescaleDBStorage timescaleDBStorage;
    private MetricsStorageFactory.StorageConfig config;

    @BeforeEach
    void setUp() {
        config = TestDataBuilder.timescaleDBConfig();
        timescaleDBStorage = new TimescaleDBStorage(config);
    }

    @Nested
    @DisplayName("저장소 타입 테스트")
    class StorageTypeTest {

        @Test
        @DisplayName("저장소 타입 확인")
        void getStorageType_WhenCalled_ReturnsTimescaleDB() {
            // When
            StorageType type = timescaleDBStorage.getStorageType();

            // Then
            assertThat(type).isEqualTo(StorageType.TIMESCALEDB);
        }
    }

    @Nested
    @DisplayName("활성화 상태 테스트")
    class EnabledStateTest {

        @Test
        @DisplayName("초기 활성화 상태 확인")
        void isEnabled_WhenCalled_ReturnsTrue() {
            // When
            boolean result = timescaleDBStorage.isEnabled();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("활성화 상태 변경")
        void setEnabled_WhenDisabled_ChangesEnabledState() {
            // When
            timescaleDBStorage.setEnabled(false);

            // Then
            assertThat(timescaleDBStorage.isEnabled()).isFalse();

            // When
            timescaleDBStorage.setEnabled(true);

            // Then
            assertThat(timescaleDBStorage.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("연결 관리 테스트")
    class ConnectionTest {

        @Test
        @DisplayName("초기 연결 상태 확인")
        void isConnected_WhenNotConnected_ReturnsFalse() {
            // When
            boolean result = timescaleDBStorage.isConnected();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("연결 성공")
        void connect_WhenCalled_ConnectsSuccessfully() {
            // When
            timescaleDBStorage.connect();

            // Then
            assertThat(timescaleDBStorage.isConnected()).isTrue();
        }

        @Test
        @DisplayName("이미 연결된 상태에서 연결 시도")
        void connect_WhenAlreadyConnected_DoesNothing() {
            // Given
            timescaleDBStorage.connect();

            // When
            timescaleDBStorage.connect();

            // Then
            assertThat(timescaleDBStorage.isConnected()).isTrue();
        }

        @Test
        @DisplayName("연결 해제 성공")
        void disconnect_WhenConnected_DisconnectsSuccessfully() {
            // Given
            timescaleDBStorage.connect();

            // When
            timescaleDBStorage.disconnect();

            // Then
            assertThat(timescaleDBStorage.isConnected()).isFalse();
        }

        @Test
        @DisplayName("이미 연결 해제된 상태에서 해제 시도")
        void disconnect_WhenAlreadyDisconnected_DoesNothing() {
            // Given
            timescaleDBStorage.disconnect();

            // When
            timescaleDBStorage.disconnect();

            // Then
            assertThat(timescaleDBStorage.isConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("메트릭 저장 테스트")
    class SaveMetricsTest {

        @Test
        @DisplayName("메트릭 저장 시도 (활성화, 연결됨)")
        void saveMetrics_WhenEnabledAndConnected_DoesNotThrowException() {
            // Given
            timescaleDBStorage.connect();
            List<Metric> metrics = createTestMetrics();

            // When & Then
            assertThatCode(() -> timescaleDBStorage.saveMetrics(metrics))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("메트릭 저장 시도 (비활성화)")
        void saveMetrics_WhenDisabled_ThrowsBusinessException() {
            // Given
            timescaleDBStorage.setEnabled(false);
            List<Metric> metrics = createTestMetrics();

            // When & Then
            assertThatThrownBy(() -> timescaleDBStorage.saveMetrics(metrics))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("비활성화");
        }

        @Test
        @DisplayName("메트릭 저장 시도 (연결 안됨)")
        void saveMetrics_WhenNotConnected_ThrowsBusinessException() {
            // Given
            timescaleDBStorage.setEnabled(true);
            timescaleDBStorage.disconnect();
            List<Metric> metrics = createTestMetrics();

            // When & Then
            assertThatThrownBy(() -> timescaleDBStorage.saveMetrics(metrics))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("연결되지 않았습니다");
        }
    }

    @Nested
    @DisplayName("메트릭 조회 테스트")
    class GetMetricsTest {

        @Test
        @DisplayName("메트릭 조회 시도 (활성화, 연결됨)")
        void getMetrics_WhenEnabledAndConnected_ReturnsEmptyList() {
            // Given
            timescaleDBStorage.connect();
            String metricName = "cpu.usage";
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();

            // When
            List<Metric> result = timescaleDBStorage.getMetrics(metricName, startTime, endTime);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("메트릭 조회 시도 (비활성화)")
        void getMetrics_WhenDisabled_ThrowsBusinessException() {
            // Given
            timescaleDBStorage.setEnabled(false);
            String metricName = "cpu.usage";
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();

            // When & Then
            assertThatThrownBy(() -> timescaleDBStorage.getMetrics(metricName, startTime, endTime))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("비활성화");
        }

        @Test
        @DisplayName("메트릭 조회 시도 (연결 안됨)")
        void getMetrics_WhenNotConnected_ThrowsBusinessException() {
            // Given
            timescaleDBStorage.setEnabled(true);
            timescaleDBStorage.disconnect();
            String metricName = "cpu.usage";
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();

            // When & Then
            assertThatThrownBy(() -> timescaleDBStorage.getMetrics(metricName, startTime, endTime))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("연결되지 않았습니다");
        }
    }

    /**
     * 테스트용 메트릭 데이터 생성
     */
    private List<Metric> createTestMetrics() {
        return TestDataBuilder.testMetrics();
    }
}
