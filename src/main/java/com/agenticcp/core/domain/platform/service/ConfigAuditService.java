package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.dto.AuditEventDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 플랫폼 설정 변경에 대한 감사(이력) 기록 서비스 - 스켈레톤
 * 
 * 주의: 본 스켈레톤은 AuditLog 저장소 직접 의존 없이 AuditLogger 기반의 최소 구현만 포함합니다.
 * 이후 커밋에서 AuditLog 엔티티/조회 API와 연계하여 세부 구현을 보강합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigAuditService {

    private final AuditLogger auditLogger;

    /**
     * 설정 변경 감사 기록 (공통 엔트리 포인트)
     * @param configKey 설정 키 
     * @param oldValue 이전 값 (ENCRYPTED는 마스킹/암호문만 전달)
     * @param newValue 이후 값 (ENCRYPTED는 마스킹/암호문만 전달)
     * @param action CREATE/UPDATE/DELETE 등
     * @param userId 수행자 식별자(가능하면)
     * @param reason 변경 사유(옵션)
     * @param valueType 설정 타입(예: STRING/NUMBER/BOOLEAN/JSON/ENCRYPTED)
     */
    public void logConfigChange(String configKey,
                                String oldValue,
                                String newValue,
                                String action,
                                String userId,
                                String reason,
                                String valueType) {
        // 스켈레톤: 운영 로그 남기기 (자세한 저장/조회 로직은 후속 커밋에서 구현)
        if (log.isDebugEnabled()) {
            log.debug("[ConfigAuditService] action={} key={} userId={} reason={}", action, configKey, userId, reason);
        }

        Map<String, Object> details = new HashMap<>();
        details.put("configKey", configKey);
        details.put("oldValue", safeString(oldValue));
        details.put("newValue", safeString(newValue));
        details.put("action", action);
        details.put("reason", safeString(reason));
        details.put("valueType", safeString(valueType));

        AuditEventDto event = new AuditEventDto(
                action,
                AuditResourceType.PLATFORM_CONFIG,
                null,                 // httpMethod - 후속 단계에서 채움
                null,                 // requestPath - 후속 단계에서 채움
                "Platform Config " + action,
                "PlatformConfigController", // 기본값(후속 단계에서 정확히 채움)
                "",                  // methodName - 후속 단계에서 채움
                AuditSeverity.INFO,
                Instant.now(),
                null,                 // requestId - 컨텍스트 연계 예정
                null,                 // tenantId - 컨텍스트 연계 예정
                userId,
                null,                 // clientIp - 컨텍스트 연계 예정
                true,
                null,
                Map.of("configKey", configKey, "valueType", valueType),
                null,
                details
        );

        auditLogger.log(event);
    }

    public void logCreate(String configKey, String newValue, String userId, String reason, String valueType) {
        logConfigChange(configKey, null, newValue, "CREATE", userId, reason, valueType);
    }

    public void logUpdate(String configKey, String oldValue, String newValue, String userId, String reason, String valueType) {
        logConfigChange(configKey, oldValue, newValue, "UPDATE", userId, reason, valueType);
    }

    public void logDelete(String configKey, String oldValue, String userId, String reason, String valueType) {
        logConfigChange(configKey, oldValue, null, "DELETE", userId, reason, valueType);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}


