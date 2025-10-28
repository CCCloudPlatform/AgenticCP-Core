package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * 감사 이벤트 어댑터
 * 
 * AuditEventPort 인터페이스를 구현하여 클라우드 리소스 관련 감사 이벤트를 기록합니다.
 * 기존 감사 시스템(AuditLogger)과 연동하여 일관된 감사 로깅을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventAdapter implements AuditEventPort {

    private final AuditLogger auditLogger;

    @Override
    public void record(String action, String subject, String outcome, Map<String, Object> attributes) {
        try {
            log.debug("[AuditEventAdapter] Recording audit event: action={}, subject={}, outcome={}", 
                    action, subject, outcome);
            
            // AuditEventDto 생성
            AuditEventDto auditEvent = createAuditEventDto(action, subject, outcome, attributes);
            
            // 기존 감사 시스템을 통해 로깅
            auditLogger.log(auditEvent);
            
            log.debug("[AuditEventAdapter] Audit event recorded successfully: action={}", action);
            
        } catch (Exception e) {
            log.error("[AuditEventAdapter] Failed to record audit event: action={}, subject={}, error={}", 
                    action, subject, e.getMessage(), e);
        }
    }
    
    /**
     * AuditEventDto 생성
     * 
     * @param action 수행된 작업
     * @param subject 대상 주체
     * @param outcome 결과
     * @param attributes 추가 속성
     * @return AuditEventDto
     */
    private AuditEventDto createAuditEventDto(String action, String subject, String outcome, Map<String, Object> attributes) {
        return new AuditEventDto(
            action,
            AuditResourceType.CLOUD_PROVIDER, // 클라우드 제공업체 관련 감사
            null, // HTTP 메서드는 클라우드 작업에서는 해당 없음
            null, // 요청 경로는 클라우드 작업에서는 해당 없음
            String.format("Cloud resource operation: %s on %s", action, subject),
            "CloudAdapter", // 컨트롤러명
            "record", // 메서드명
            determineSeverity(outcome),
            Instant.now(),
            extractRequestId(attributes),
            extractTenantId(attributes),
            extractUserId(attributes),
            extractClientIp(attributes),
            "SUCCESS".equalsIgnoreCase(outcome),
            "SUCCESS".equalsIgnoreCase(outcome) ? null : outcome,
            attributes, // 요청 데이터로 사용
            Map.of("outcome", outcome), // 응답 데이터
            Map.of("subject", subject), // 메타데이터
            null, // oldValue
            null, // newValue
            subject // targetResourceId
        );
    }
    
    /**
     * 결과에 따른 심각도 결정
     */
    private AuditSeverity determineSeverity(String outcome) {
        if ("SUCCESS".equalsIgnoreCase(outcome)) {
            return AuditSeverity.INFO;
        } else if ("FAILURE".equalsIgnoreCase(outcome)) {
            return AuditSeverity.HIGH;
        } else {
            return AuditSeverity.MEDIUM;
        }
    }
    
    /**
     * 속성에서 RequestId 추출
     */
    private String extractRequestId(Map<String, Object> attributes) {
        return (String) attributes.getOrDefault("requestId", "cloud-operation");
    }
    
    /**
     * 속성에서 TenantId 추출
     */
    private String extractTenantId(Map<String, Object> attributes) {
        return (String) attributes.getOrDefault("tenantId", "unknown");
    }
    
    /**
     * 속성에서 UserId 추출
     */
    private String extractUserId(Map<String, Object> attributes) {
        return (String) attributes.getOrDefault("userId", "system");
    }
    
    /**
     * 속성에서 ClientIp 추출
     */
    private String extractClientIp(Map<String, Object> attributes) {
        return (String) attributes.getOrDefault("clientIp", "internal");
    }
}
