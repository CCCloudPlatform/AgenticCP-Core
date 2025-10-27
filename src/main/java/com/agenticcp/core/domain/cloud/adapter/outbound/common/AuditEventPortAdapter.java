package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 감사 이벤트 포트 어댑터
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AuditEventPortAdapter implements AuditEventPort {

    @Override
    public void record(String action, String subject, String outcome, Map<String, Object> attributes) {
        // TODO: 실제 감사 이벤트 기록 로직 구현
        log.debug("AuditEventPortAdapter.record - action={}, subject={}, outcome={}", action, subject, outcome);
    }
}
