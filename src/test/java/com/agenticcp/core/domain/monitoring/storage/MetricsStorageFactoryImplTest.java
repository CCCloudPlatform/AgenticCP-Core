package com.agenticcp.core.domain.monitoring.storage;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.enums.StorageType;
import com.agenticcp.core.domain.monitoring.storage.impl.InfluxDBStorage;
import com.agenticcp.core.domain.monitoring.storage.impl.TimescaleDBStorage;
import com.agenticcp.core.domain.monitoring.storage.impl.PrometheusStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * MetricsStorageFactoryImpl 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@DisplayName("MetricsStorageFactoryImpl 단위 테스트")
class MetricsStorageFactoryImplTest {

    private MetricsStorageFactoryImpl storageFactory;

    @BeforeEach
    void setUp() {
        storageFactory = new MetricsStorageFactoryImpl();
        storageFactory.initializeDefaultConfigs(); // @PostConstruct 메서드 수동 호출
    }

    @Nested
    @DisplayName("저장소 생성 테스트")
    class CreateStorageTest {

        @Test
        @DisplayName("InfluxDB 저장소 생성 성공")
        void createStorage_WhenInfluxDB_ReturnsInfluxDBStorage() {
            // Given - InfluxDB는 기본 활성화됨

            // When
            MetricsStorage result = storageFactory.createStorage(StorageType.INFLUXDB);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(InfluxDBStorage.class);
        }

        @Test
        @DisplayName("TimescaleDB 저장소 생성 성공")
        void createStorage_WhenTimescaleDB_ReturnsTimescaleDBStorage() {
            // Given
            storageFactory.setStorageEnabled(StorageType.TIMESCALEDB, true);

            // When
            MetricsStorage result = storageFactory.createStorage(StorageType.TIMESCALEDB);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(TimescaleDBStorage.class);
        }

        @Test
        @DisplayName("Prometheus 저장소 생성 성공")
        void createStorage_WhenPrometheus_ReturnsPrometheusStorage() {
            // Given
            storageFactory.setStorageEnabled(StorageType.PROMETHEUS, true);

            // When
            MetricsStorage result = storageFactory.createStorage(StorageType.PROMETHEUS);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(PrometheusStorage.class);
        }

        @Test
        @DisplayName("null 저장소 타입으로 생성 시 예외 발생")
        void createStorage_WhenNullType_ThrowsBusinessException() {
            // When & Then
            assertThatThrownBy(() -> storageFactory.createStorage(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("저장소 타입이 null입니다.");
        }

        @Test
        @DisplayName("모든 활성화된 저장소 생성 성공")
        void createAllStorages_WhenAllEnabled_ReturnsAllStorages() {
            // Given - 모두 활성화
            storageFactory.setStorageEnabled(StorageType.INFLUXDB, true);
            storageFactory.setStorageEnabled(StorageType.TIMESCALEDB, true);
            storageFactory.setStorageEnabled(StorageType.PROMETHEUS, true);

            // When
            List<MetricsStorage> result = storageFactory.createAllStorages();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(MetricsStorage::getStorageType)
                    .containsExactlyInAnyOrder(StorageType.INFLUXDB, StorageType.TIMESCALEDB, StorageType.PROMETHEUS);
        }
    }

    @Nested
    @DisplayName("저장소 존재 여부 확인 테스트")
    class HasStorageTest {

        @Test
        @DisplayName("InfluxDB 저장소 존재 여부 확인")
        void hasStorage_WhenInfluxDB_ReturnsTrue() {
            // When
            boolean result = storageFactory.hasStorage(StorageType.INFLUXDB);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("TimescaleDB 저장소 존재 여부 확인")
        void hasStorage_WhenTimescaleDB_ReturnsTrue() {
            // When
            boolean result = storageFactory.hasStorage(StorageType.TIMESCALEDB);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Prometheus 저장소 존재 여부 확인")
        void hasStorage_WhenPrometheus_ReturnsTrue() {
            // When
            boolean result = storageFactory.hasStorage(StorageType.PROMETHEUS);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("null 저장소 타입 존재 여부 확인")
        void hasStorage_WhenNullType_ReturnsFalse() {
            // When
            boolean result = storageFactory.hasStorage(null);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("저장소 설정 관리 테스트")
    class StorageConfigTest {

        @Test
        @DisplayName("저장소 활성화 상태 설정")
        void setStorageEnabled_WhenCalled_UpdatesEnabledState() {
            // Given
            StorageType type = StorageType.INFLUXDB;
            boolean enabled = false;

            // When
            storageFactory.setStorageEnabled(type, enabled);

            // Then
            MetricsStorageFactory.StorageConfig config = storageFactory.getStorageConfig(type);
            assertThat(config).isNotNull();
            assertThat(config.isEnabled()).isEqualTo(enabled);
        }

        @Test
        @DisplayName("저장소 설정 정보 조회")
        void getStorageConfig_WhenCalled_ReturnsConfig() {
            // Given
            storageFactory.setStorageEnabled(StorageType.INFLUXDB, true); // 먼저 활성화

            // When
            MetricsStorageFactory.StorageConfig config = storageFactory.getStorageConfig(StorageType.INFLUXDB);

            // Then
            assertThat(config).isNotNull();
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getUrl()).isEqualTo("http://localhost:8086");
            assertThat(config.getDatabase()).isEqualTo("metrics");
        }

        @Test
        @DisplayName("저장소 설정 정보 업데이트")
        void updateStorageConfig_WhenCalled_UpdatesConfig() {
            // Given
            MetricsStorageFactory.StorageConfig newConfig = MetricsStorageFactory.StorageConfig.builder()
                    .enabled(false)
                    .url("http://new-influxdb:8086")
                    .database("new_metrics")
                    .build();

            // When
            storageFactory.updateStorageConfig(StorageType.INFLUXDB, newConfig);

            // Then
            MetricsStorageFactory.StorageConfig updatedConfig = storageFactory.getStorageConfig(StorageType.INFLUXDB);
            assertThat(updatedConfig.isEnabled()).isFalse();
            assertThat(updatedConfig.getUrl()).isEqualTo("http://new-influxdb:8086");
            assertThat(updatedConfig.getDatabase()).isEqualTo("new_metrics");
        }
    }
}
