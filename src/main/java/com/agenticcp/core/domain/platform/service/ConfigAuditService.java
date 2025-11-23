package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.audit.AuditPublishEvent;
import com.agenticcp.core.common.audit.AuditEventBuilder;
import com.agenticcp.core.common.dto.audit.AuditContextDto;
import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.util.EncryptedValueMasker;
import com.agenticcp.core.common.context.TenantContextHolder;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
 

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
    private final ApplicationEventPublisher eventPublisher;

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
        // 액션 정규화 (CREATE/UPDATE/DELETE 등으로 제한)
        String normalizedAction = normalizeAction(action);

        // 운영 로그 - INFO 레벨로 변경하여 항상 출력
        log.info("[ConfigAuditService] logConfigChange called - action={} key={} userId={} reason={} type={}",
                normalizedAction, configKey, userId, reason, valueType);

        String tenantKey = TenantContextHolder.getCurrentTenantKey();

        Map<String, Object> details = new HashMap<>();
        details.put("configKey", configKey);
        if (tenantKey != null) {
            details.put("tenantKey", tenantKey);
        }
        boolean encryptedType = EncryptedValueMasker.isEncryptedType(valueType);
        details.put("oldValue", EncryptedValueMasker.maskForAudit(oldValue, encryptedType));
        details.put("newValue", EncryptedValueMasker.maskForAudit(newValue, encryptedType));
        details.put("action", normalizedAction);
        details.put("reason", safeString(reason));
        details.put("valueType", safeString(valueType));
        // AuditLog 스키마 정렬: eventType/eventCategory를 details에 명시 (로거 파이프라인과 호환)
        details.put("eventType", "CONFIGURATION_CHANGE");
        details.put("eventCategory", "CONFIGURE");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("eventType", "CONFIGURATION_CHANGE");
        metadata.put("eventCategory", "CONFIGURE");
        metadata.put("resourceType", "PlatformConfig");
        metadata.put("resourceId", configKey);
        if (tenantKey != null) {
            metadata.put("tenantKey", tenantKey);
        }

        Map<String, Object> oldValueMap = new HashMap<>();
        oldValueMap.put("value", EncryptedValueMasker.maskForAudit(oldValue, encryptedType));
        Map<String, Object> newValueMap = new HashMap<>();
        newValueMap.put("value", EncryptedValueMasker.maskForAudit(newValue, encryptedType));

        AuditContextDto context = AuditContextDto.builder()
                .userId(userId)
                .action(normalizedAction)
                .resourceType(AuditResourceType.PLATFORM_CONFIG)
                .httpMethod("SYSTEM")
                .requestPath("/internal/platform-config")
                .operationSummary("Platform Config " + normalizedAction)
                .controllerName("PlatformConfigService")
                .methodName("logConfigChange")
                .severity(AuditSeverity.INFO)
                .includeRequestData(true)
                .includeResponseData(false)
                .build();

        // metadata는 현재 빌더에 별도 세터가 없으므로 requestData에 핵심 키만 유지하거나, 필요 시 로거 파이프라인에서 병합
        AuditEventDto event = AuditEventBuilder.builder(context)
                .requestData(details)
                .responseData(null)
                .oldValue(oldValueMap)
                .newValue(newValueMap)
                .targetResourceId(configKey)
                .success(true)
                .build();

        // 파일 로깅 (AUDIT 로거)
        auditLogger.log(event);

        // 이벤트 퍼블리시 → 파일/DB 리스너가 처리
        eventPublisher.publishEvent(new AuditPublishEvent(this, event));
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

    private String normalizeAction(String action) {
        if (action == null) {
            return "UPDATE"; // 기본값
        }
        String upper = action.trim().toUpperCase();
        return switch (upper) {
            case "CREATE", "CREATED", "ADD", "ADDED" -> "CREATE";
            case "UPDATE", "UPDATED", "MODIFY", "MODIFIED", "CHANGE", "CHANGED" -> "UPDATE";
            case "DELETE", "DELETED", "REMOVE", "REMOVED" -> "DELETE";
            default -> upper;
        };
    }

    // DB 저장 로직은 리스너(AuditDatabaseListener)가 담당합니다.
}


