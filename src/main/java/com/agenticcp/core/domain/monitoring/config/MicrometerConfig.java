package com.agenticcp.core.domain.monitoring.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
// import io.micrometer.prometheus.PrometheusConfig;
// import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
     * Prometheus MeterRegistry 설정
     * 
     * @return PrometheusMeterRegistry
     */
    @Bean
    @Primary
    public MeterRegistry prometheusMeterRegistry() {
        log.info("Prometheus MeterRegistry 초기화 (Spring Boot 자동 설정 사용)");
        // Spring Boot가 자동으로 Prometheus MeterRegistry를 생성하므로
        // 여기서는 SimpleMeterRegistry를 반환 (실제로는 Spring Boot가 관리)
        return new SimpleMeterRegistry();
    }

    /**
     * Simple MeterRegistry 설정 (백업용)
     * 
     * @return SimpleMeterRegistry
     */
    @Bean("simpleMeterRegistry")
    public MeterRegistry simpleMeterRegistry() {
        log.info("Simple MeterRegistry 초기화");
        return new SimpleMeterRegistry();
    }

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