package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 감사 이벤트를 JSON 형태로 기록하는 로거입니다.
 * 심각도에 따라 로그 레벨을 다르게 적용합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    private static final Map<AuditSeverity, BiConsumer<Logger, String>> logActions
            = new EnumMap<>(AuditSeverity.class);

    static {
        logActions.put(AuditSeverity.CRITICAL, Logger::error);
        logActions.put(AuditSeverity.HIGH, Logger::warn);
        logActions.put(AuditSeverity.MEDIUM, Logger::info);
        logActions.put(AuditSeverity.LOW, Logger::info);
        logActions.put(AuditSeverity.INFO, Logger::info);
    }

    private final ObjectMapper objectMapper;

    public void log(AuditEventDto auditEvent) {    
        try {
            String jsonLog = objectMapper.writeValueAsString(auditEvent);

            logActions.getOrDefault(auditEvent.severity(), Logger::info)
                    .accept(auditLog, jsonLog);

        } catch (JsonProcessingException e) {
            log.error("감사 이벤트 JSON 직렬화에 실패했습니다: {}", e.getMessage(), e);
        }
    }
}
