package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 트레이싱 포트 어댑터
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class TracingPortAdapter implements TracingPort {

    @Override
    public AutoCloseable startSpan(String name, Map<String, String> tags) {
        // TODO: 실제 트레이싱 스팬 시작 로직 구현
        log.debug("TracingPortAdapter.startSpan - name={}, tags={}", name, tags);
        
        return new AutoCloseable() {
            @Override
            public void close() {
                log.debug("TracingPortAdapter.close - name={}", name);
            }
        };
    }

    @Override
    public void tag(String key, String value) {
        // TODO: 실제 태그 추가 로직 구현
        log.debug("TracingPortAdapter.tag - {}={}", key, value);
    }

    @Override
    public void event(String name, Map<String, String> attrs) {
        // TODO: 실제 이벤트 기록 로직 구현
        log.debug("TracingPortAdapter.event - name={}, attrs={}", name, attrs);
    }
}
