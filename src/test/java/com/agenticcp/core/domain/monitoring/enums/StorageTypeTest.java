package com.agenticcp.core.domain.monitoring.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * StorageType 열거형 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("저장소 타입 테스트")
class StorageTypeTest {

    @Test
    @DisplayName("InfluxDB 타입 정보 확인")
    void influxDB_Properties() {
        // when
        StorageType type = StorageType.INFLUXDB;

        // then
        assertThat(type.getDisplayName()).isEqualTo("InfluxDB");
        assertThat(type.getDescription()).isEqualTo("고성능 시계열 데이터베이스");
        assertThat(type.isSupported()).isTrue();
    }

    @Test
    @DisplayName("TimescaleDB 타입 정보 확인")
    void timescaleDB_Properties() {
        // when
        StorageType type = StorageType.TIMESCALEDB;

        // then
        assertThat(type.getDisplayName()).isEqualTo("TimescaleDB");
        assertThat(type.getDescription()).isEqualTo("PostgreSQL 기반 시계열 데이터베이스");
        assertThat(type.isSupported()).isTrue();
    }

    @Test
    @DisplayName("Prometheus 타입 정보 확인")
    void prometheus_Properties() {
        // when
        StorageType type = StorageType.PROMETHEUS;

        // then
        assertThat(type.getDisplayName()).isEqualTo("Prometheus");
        assertThat(type.getDescription()).isEqualTo("메트릭 모니터링 시스템");
        assertThat(type.isSupported()).isTrue();
    }

    @Test
    @DisplayName("모든 저장소 타입이 지원됨")
    void allTypes_AreSupported() {
        // when & then
        for (StorageType type : StorageType.values()) {
            assertThat(type.isSupported()).isTrue();
        }
    }

    @Test
    @DisplayName("저장소 타입 개수 확인")
    void values_Count() {
        // when
        StorageType[] types = StorageType.values();

        // then
        assertThat(types).hasSize(3);
        assertThat(types).containsExactly(
                StorageType.INFLUXDB,
                StorageType.TIMESCALEDB,
                StorageType.PROMETHEUS
        );
    }
}
