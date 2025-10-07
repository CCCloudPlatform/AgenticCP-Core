package com.agenticcp.core.domain.monitoring.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer 설정 클래스
 * 
 * @author AgenticCP
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class MicrometerConfig {
    /**
     * MeterRegistry 커스터마이저
     * 
     * @return MeterRegistryCustomizer
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config().commonTags(
                "application", "agenticcp-core",
                "environment", "monitoring"
            );
            log.info("Micrometer 공통 태그 설정 완료");
        };
    }
}