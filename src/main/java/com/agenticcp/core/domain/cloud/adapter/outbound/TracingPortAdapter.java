package com.agenticcp.core.domain.cloud.adapter.outbound;

import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tracing 포트의 기본 구현체
 * 현재는 로그 기반으로 동작하며, 향후 OpenTelemetry 등의 분산 추적 시스템과 연동 가능합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class TracingPortAdapter implements TracingPort {

    @Override
    public AutoCloseable startSpan(String name, Map<String, String> tags) {
        log.debug("[TracingPortAdapter] Starting span: name={}, tags={}", name, tags);
        long startTime = System.currentTimeMillis();
        
        return () -> {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("[TracingPortAdapter] Span completed: name={}, duration={}ms", name, duration);
        };
    }

    @Override
    public void tag(String key, String value) {
        log.debug("[TracingPortAdapter] Adding tag: {}={}", key, value);
    }

    @Override
    public void event(String name, Map<String, String> attrs) {
        log.debug("[TracingPortAdapter] Recording event: name={}, attrs={}", name, attrs);
    }
}

