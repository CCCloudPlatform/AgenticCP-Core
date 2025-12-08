package com.agenticcp.core.domain.monitoring.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MicrometerConfig 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025.11.13
 */
@DisplayName("MicrometerConfig 단위 테스트")
class MicrometerConfigTest {
    
    private MicrometerConfig micrometerConfig;
    private SimpleMeterRegistry meterRegistry;
    
    @BeforeEach
    void setUp() {
        micrometerConfig = new MicrometerConfig();
        meterRegistry = new SimpleMeterRegistry();
    }
    
    @Test
    @DisplayName("정상 설정 시 공통 태그가 추가된다")
    void metricsCommonTags_WhenConfigured_AddsCommonTags() {
        // Given
        MeterRegistryCustomizer<MeterRegistry> customizer = micrometerConfig.metricsCommonTags();
        
        // When
        customizer.customize(meterRegistry);
        
        // Then
        assertThat(customizer).isNotNull();
        
        // 공통 태그가 설정되었는지 확인 - 메트릭을 등록하여 공통 태그가 적용되는지 검증
        Counter testCounter = meterRegistry.counter("test.counter");
        assertThat(testCounter).isNotNull();
        
        // 메트릭의 태그에서 공통 태그 확인
        assertThat(testCounter.getId().getTag("application")).isEqualTo("agenticcp-core");
        assertThat(testCounter.getId().getTag("environment")).isEqualTo("monitoring");
    }
}

