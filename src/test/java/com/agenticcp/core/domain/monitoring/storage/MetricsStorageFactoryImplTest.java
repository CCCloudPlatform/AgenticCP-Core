package com.agenticcp.core.domain.monitoring.storage;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.enums.StorageType;
import com.agenticcp.core.domain.monitoring.storage.impl.InfluxDBStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MetricsStorageFactoryImpl 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("메트릭 저장소 팩토리 테스트")
class MetricsStorageFactoryImplTest {

    @Mock
    private InfluxDBStorage influxDBStorage;

    private MetricsStorageFactoryImpl storageFactory;

    @BeforeEach
    void setUp() {
        storageFactory = new MetricsStorageFactoryImpl(influxDBStorage);
        storageFactory.initializeDefaultConfigs(); // @PostConstruct 메서드 수동 호출
    }

    @Nested
    @DisplayName("저장소 생성 테스트")
    class CreateStorageTest {

    @Test
    @DisplayName("InfluxDB 저장소 생성 성공")
    void createStorage_InfluxDB_Success() {
        // given
        when(influxDBStorage.isEnabled()).thenReturn(true);
        storageFactory.setStorageEnabled(StorageType.INFLUXDB, true); // 팩토리에서 활성화

        // when
        MetricsStorage result = storageFactory.createStorage(StorageType.INFLUXDB);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(InfluxDBStorage.class);
        verify(influxDBStorage, never()).setEnabled(anyBoolean());
    }

    @Test
    @DisplayName("TimescaleDB 저장소 생성 시 예외 발생")
    void createStorage_TimescaleDB_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> storageFactory.createStorage(StorageType.TIMESCALEDB))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원되지 않는 저장소 타입입니다: TIMESCALEDB");
    }

    @Test
    @DisplayName("Prometheus 저장소 생성 시 예외 발생")
    void createStorage_Prometheus_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> storageFactory.createStorage(StorageType.PROMETHEUS))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원되지 않는 저장소 타입입니다: PROMETHEUS");
    }

    @Test
    @DisplayName("null 저장소 타입으로 생성 시 예외 발생")
    void createStorage_NullType_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> storageFactory.createStorage(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("저장소 타입이 null입니다.");
    }

    @Test
    @DisplayName("모든 활성화된 저장소 생성 성공")
    void createAllStorages_Success() {
        // given
        when(influxDBStorage.isEnabled()).thenReturn(true);
        storageFactory.setStorageEnabled(StorageType.INFLUXDB, true); // 팩토리에서 활성화

        // when
        List<MetricsStorage> result = storageFactory.createAllStorages();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(InfluxDBStorage.class);
    }

    @Test
    @DisplayName("InfluxDB 저장소 존재 여부 확인")
    void hasStorage_InfluxDB_ReturnsTrue() {
        // when
        boolean result = storageFactory.hasStorage(StorageType.INFLUXDB);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("TimescaleDB 저장소 존재 여부 확인")
    void hasStorage_TimescaleDB_ReturnsFalse() {
        // when
        boolean result = storageFactory.hasStorage(StorageType.TIMESCALEDB);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Prometheus 저장소 존재 여부 확인")
    void hasStorage_Prometheus_ReturnsFalse() {
        // when
        boolean result = storageFactory.hasStorage(StorageType.PROMETHEUS);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("null 저장소 타입 존재 여부 확인")
    void hasStorage_NullType_ReturnsFalse() {
        // when
        boolean result = storageFactory.hasStorage(null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("저장소 활성화 상태 설정")
    void setStorageEnabled_Success() {
        // when
        storageFactory.setStorageEnabled(StorageType.INFLUXDB, false);

        // then
        // 내부 상태 변경 확인 (실제 구현에서는 상태를 확인할 수 있는 방법이 필요)
        assertThatCode(() -> storageFactory.setStorageEnabled(StorageType.INFLUXDB, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("저장소 설정 정보 조회")
    void getStorageConfig_Success() {
        // given
        storageFactory.setStorageEnabled(StorageType.INFLUXDB, true); // 먼저 활성화

        // when
        MetricsStorageFactory.StorageConfig config = storageFactory.getStorageConfig(StorageType.INFLUXDB);

        // then
        assertThat(config).isNotNull();
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getUrl()).isEqualTo("http://localhost:8086");
        assertThat(config.getDatabase()).isEqualTo("metrics");
    }

    @Test
    @DisplayName("저장소 설정 정보 업데이트")
    void updateStorageConfig_Success() {
        // given
        MetricsStorageFactory.StorageConfig newConfig = MetricsStorageFactory.StorageConfig.builder()
                .enabled(false)
                .url("http://new-influxdb:8086")
                .database("new_metrics")
                .build();

        // when
        storageFactory.updateStorageConfig(StorageType.INFLUXDB, newConfig);

        // then
        MetricsStorageFactory.StorageConfig updatedConfig = storageFactory.getStorageConfig(StorageType.INFLUXDB);
        assertThat(updatedConfig.isEnabled()).isFalse();
        assertThat(updatedConfig.getUrl()).isEqualTo("http://new-influxdb:8086");
        assertThat(updatedConfig.getDatabase()).isEqualTo("new_metrics");
    }
    }
}
