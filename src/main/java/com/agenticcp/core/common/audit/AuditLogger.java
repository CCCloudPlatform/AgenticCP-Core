package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.enums.AuditErrorCode;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.exception.BusinessException;
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
 * @version 1.0.0
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

    /**
     * 감사 이벤트를 JSON 형태로 직렬화하여 감사 로거에 출력합니다.
     *
     * @param auditEvent 기록할 감사 이벤트
     * @throws BusinessException 직렬화 실패 시
     */
    public void log(AuditEventDto auditEvent) {
        try {
            String jsonLog = objectMapper.writeValueAsString(auditEvent);
            BiConsumer<Logger, String> logAction = resolveLogAction(auditEvent);
            logAction.accept(auditLog, jsonLog);
        } catch (JsonProcessingException e) {
            log.error("감사 이벤트 JSON 직렬화 실패 [Action: {}, RequestId: {}, TenantId: {}]: {}",
                    auditEvent.action(), auditEvent.requestId(), auditEvent.tenantId(), e.getMessage(), e);
            throw new BusinessException(AuditErrorCode.AUDIT_LOG_CONVERSION_FAILED);
        }
    }

    private BiConsumer<Logger, String> resolveLogAction(AuditEventDto auditEvent) {
        AuditSeverity severity = auditEvent.severity();
        if (severity == null) {
            log.warn("감사 이벤트 Severity 누락 [Action: {}, RequestId: {}], 기본 INFO 사용",
                    auditEvent.action(), auditEvent.requestId());
            return Logger::info;
        }

        BiConsumer<Logger, String> logAction = logActions.get(severity);
        if (logAction == null) {
            log.warn("정의되지 않은 감사 Severity [{}] 감지 [Action: {}, RequestId: {}], 기본 INFO 사용",
                    severity, auditEvent.action(), auditEvent.requestId());
            return Logger::info;
        }
        return logAction;
    }
}
