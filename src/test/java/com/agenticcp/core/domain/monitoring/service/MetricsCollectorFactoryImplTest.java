package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * MetricsCollectorFactoryImpl 단위 테스트
 * 메트릭 수집기 팩토리 구현체의 핵심 비즈니스 로직을 검증
 */
@ExtendWith(MockitoExtension.class)
class MetricsCollectorFactoryImplTest {

    @Mock
    private MetricsEndpoint metricsEndpoint;
    
    @Mock
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    private SystemMetricsCollector systemMetricsCollector;
    private MicrometerMetricsCollector micrometerMetricsCollector;
    private MetricsCollectorFactoryImpl metricsCollectorFactory;

    @BeforeEach
    void setUp() {
        systemMetricsCollector = new SystemMetricsCollector();
        micrometerMetricsCollector = new MicrometerMetricsCollector(meterRegistry);
        metricsCollectorFactory = new MetricsCollectorFactoryImpl(systemMetricsCollector, micrometerMetricsCollector);
        
        // @PostConstruct 메서드 수동 호출 (테스트 환경에서는 자동 실행되지 않음)
        metricsCollectorFactory.initializeDefaultConfigs();
    }

    /**
     * 정상적인 수집기 생성 테스트 그룹
     */
    @Nested
    @DisplayName("정상적인 수집기 생성 테스트")
    class SuccessfulCollectorCreationTest {

        @Test
        @DisplayName("시스템 수집기 생성 성공")
        void createSystemCollector_Success() {
            // Given
            systemMetricsCollector.setEnabled(true);
            metricsCollectorFactory.setCollectorEnabled(CollectorType.SYSTEM, true);

            // When
            MetricsCollector collector = metricsCollectorFactory.createCollector(CollectorType.SYSTEM);

            // Then
            assertThat(collector).isNotNull();
            assertThat(collector.getCollectorType()).isEqualTo(CollectorType.SYSTEM);
            assertThat(collector.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("애플리케이션 수집기 생성 성공")
        void createApplicationCollector_Success() {
            // Given
            micrometerMetricsCollector.setEnabled(true);
            metricsCollectorFactory.setCollectorEnabled(CollectorType.APPLICATION, true);

            // When
            MetricsCollector collector = metricsCollectorFactory.createCollector(CollectorType.APPLICATION);

            // Then
            assertThat(collector).isNotNull();
            assertThat(collector.getCollectorType()).isEqualTo(CollectorType.APPLICATION);
            assertThat(collector.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("모든 활성화된 수집기 생성 성공")
        void createAllCollectors_Success() {
            // Given
            systemMetricsCollector.setEnabled(true);
            metricsCollectorFactory.setCollectorEnabled(CollectorType.SYSTEM, true);
            metricsCollectorFactory.setCollectorEnabled(CollectorType.APPLICATION, true);

            // When
            List<MetricsCollector> collectors = metricsCollectorFactory.createAllCollectors();

            // Then
            assertThat(collectors).isNotNull();
            assertThat(collectors).hasSize(2);
            assertThat(collectors).extracting(MetricsCollector::getCollectorType)
                    .containsExactlyInAnyOrder(CollectorType.SYSTEM, CollectorType.APPLICATION);
        }
    }

    /**
     * 예외 상황 테스트 그룹
     */
    @Nested
    @DisplayName("예외 상황 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("null 수집기 타입으로 생성 시 예외 발생")
        void createCollector_NullType_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> metricsCollectorFactory.createCollector(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("수집기 타입이 null입니다");
        }

        @Test
        @DisplayName("비활성화된 수집기 생성 시 예외 발생")
        void createCollector_DisabledCollector_ThrowsException() {
            // Given
            metricsCollectorFactory.setCollectorEnabled(CollectorType.SYSTEM, false);

            // When & Then
            assertThatThrownBy(() -> metricsCollectorFactory.createCollector(CollectorType.SYSTEM))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("수집기가 비활성화되어 있습니다");
        }

        @Test
        @DisplayName("존재하지 않는 수집기 타입으로 생성 시 예외 발생")
        void createCollector_UnsupportedType_ThrowsException() {
            // When & Then
            // CollectorType enum에 없는 값은 컴파일 타임에 방지되지만, 
            // 런타임에 null이나 잘못된 값이 전달될 수 있음
            assertThatThrownBy(() -> metricsCollectorFactory.createCollector(null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    /**
     * 수집기 관리 테스트 그룹
     */
    @Nested
    @DisplayName("수집기 관리 테스트")
    class CollectorManagementTest {

        @Test
        @DisplayName("수집기 활성화/비활성화 설정")
        void setCollectorEnabled_Success() {
            // Given
            CollectorType type = CollectorType.SYSTEM;

            // When
            metricsCollectorFactory.setCollectorEnabled(type, false);

            // Then
            assertThatThrownBy(() -> metricsCollectorFactory.createCollector(type))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("수집기가 비활성화되어 있습니다");

            // 다시 활성화
            metricsCollectorFactory.setCollectorEnabled(type, true);
            MetricsCollector collector = metricsCollectorFactory.createCollector(type);
            assertThat(collector).isNotNull();
        }

        @Test
        @DisplayName("수집기 존재 여부 확인")
        void hasCollector_Success() {
            // When & Then
            assertThat(metricsCollectorFactory.hasCollector(CollectorType.SYSTEM)).isTrue();
            assertThat(metricsCollectorFactory.hasCollector(CollectorType.APPLICATION)).isTrue();
            assertThat(metricsCollectorFactory.hasCollector(null)).isFalse();
        }

        @Test
        @DisplayName("수집기 설정 정보 조회")
        void getCollectorConfig_Success() {
            // When
            var systemConfig = metricsCollectorFactory.getCollectorConfig(CollectorType.SYSTEM);
            var applicationConfig = metricsCollectorFactory.getCollectorConfig(CollectorType.APPLICATION);

            // Then
            assertThat(systemConfig).isNotNull();
            assertThat(applicationConfig).isNotNull();
            assertThat(systemConfig.isEnabled()).isTrue();
            assertThat(applicationConfig.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("수집기 설정 정보 업데이트")
        void updateCollectorConfig_Success() {
            // Given
            var newConfig = MetricsCollectorFactoryImpl.CollectorConfig.builder()
                    .enabled(false)
                    .collectionInterval(30000L)
                    .retryCount(5)
                    .timeout(15000L)
                    .build();

            // When
            metricsCollectorFactory.updateCollectorConfig(CollectorType.SYSTEM, newConfig);

            // Then
            var updatedConfig = metricsCollectorFactory.getCollectorConfig(CollectorType.SYSTEM);
            assertThat(updatedConfig.isEnabled()).isFalse();
            assertThat(updatedConfig.getCollectionInterval()).isEqualTo(30000L);
            assertThat(updatedConfig.getRetryCount()).isEqualTo(5);
            assertThat(updatedConfig.getTimeout()).isEqualTo(15000L);
        }
    }

    /**
     * 설정 정보 테스트 그룹
     */
    @Nested
    @DisplayName("설정 정보 테스트")
    class ConfigurationTest {

        @Test
        @DisplayName("CollectorConfig 빌더 패턴 테스트")
        void collectorConfigBuilder_Success() {
            // When
            var config = MetricsCollectorFactoryImpl.CollectorConfig.builder()
                    .enabled(true)
                    .collectionInterval(60000L)
                    .retryCount(3)
                    .timeout(30000L)
                    .build();

            // Then
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getCollectionInterval()).isEqualTo(60000L);
            assertThat(config.getRetryCount()).isEqualTo(3);
            assertThat(config.getTimeout()).isEqualTo(30000L);
        }

        @Test
        @DisplayName("CollectorConfig toBuilder 테스트")
        void collectorConfigToBuilder_Success() {
            // Given
            var originalConfig = MetricsCollectorFactoryImpl.CollectorConfig.builder()
                    .enabled(true)
                    .collectionInterval(60000L)
                    .retryCount(3)
                    .timeout(30000L)
                    .build();

            // When
            var updatedConfig = originalConfig.toBuilder()
                    .enabled(false)
                    .collectionInterval(30000L)
                    .build();

            // Then
            assertThat(updatedConfig.isEnabled()).isFalse();
            assertThat(updatedConfig.getCollectionInterval()).isEqualTo(30000L);
            assertThat(updatedConfig.getRetryCount()).isEqualTo(3); // 원본 값 유지
            assertThat(updatedConfig.getTimeout()).isEqualTo(30000L); // 원본 값 유지
        }
    }

    /**
     * 에러 코드 테스트 그룹
     */
    @Nested
    @DisplayName("에러 코드 테스트")
    class ErrorCodeTest {

        @Test
        @DisplayName("MonitoringErrorCode 값 확인")
        void monitoringErrorCode_Values() {
            // When & Then
            assertThat(MonitoringErrorCode.COLLECTOR_NOT_FOUND.getHttpStatus().value()).isEqualTo(404);
            assertThat(MonitoringErrorCode.COLLECTOR_DISABLED.getHttpStatus().value()).isEqualTo(503);
            assertThat(MonitoringErrorCode.COLLECTOR_CREATION_FAILED.getHttpStatus().value()).isEqualTo(500);
            assertThat(MonitoringErrorCode.INVALID_COLLECTOR_TYPE.getHttpStatus().value()).isEqualTo(400);
            assertThat(MonitoringErrorCode.METRICS_COLLECTION_FAILED.getHttpStatus().value()).isEqualTo(500);
        }

        @Test
        @DisplayName("에러 코드 형식 확인")
        void errorCodeFormat_Validation() {
            // When & Then
            assertThat(MonitoringErrorCode.COLLECTOR_NOT_FOUND.getCode()).isEqualTo("MONITORING_8051");
            assertThat(MonitoringErrorCode.COLLECTOR_DISABLED.getCode()).isEqualTo("MONITORING_8052");
            assertThat(MonitoringErrorCode.COLLECTOR_CREATION_FAILED.getCode()).isEqualTo("MONITORING_8053");
            assertThat(MonitoringErrorCode.INVALID_COLLECTOR_TYPE.getCode()).isEqualTo("MONITORING_8054");
            assertThat(MonitoringErrorCode.METRICS_COLLECTION_FAILED.getCode()).isEqualTo("MONITORING_8055");
        }
    }
}
