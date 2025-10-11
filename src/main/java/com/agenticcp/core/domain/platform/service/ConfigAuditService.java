package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.dto.AuditEventDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.util.EncryptedValueMasker;
import com.agenticcp.core.domain.security.entity.AuditLog;
import com.agenticcp.core.domain.security.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

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

        Map<String, Object> details = new HashMap<>();
        details.put("configKey", configKey);
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

        AuditEventDto event = new AuditEventDto(
                normalizedAction,                    // action
                AuditResourceType.PLATFORM_CONFIG,   // resourceType
                null,                                // httpMethod
                null,                                // requestPath
                "Platform Config " + normalizedAction, // operationSummary
                "PlatformConfigController",          // controllerName
                "",                                  // methodName
                AuditSeverity.INFO,                  // severity
                Instant.now(),                       // timestamp
                null,                                // requestId
                null,                                // tenantId
                userId,                              // userId
                null,                                // clientIp
                true,                                // success
                null,                                // error
                details,                            // requestData (상세 정보)
                null,                               // responseData (응답 데이터 없음)
                metadata                            // metadata (메타 정보)
        );

        // 1. 파일 기반 감사 로그 기록
        log.info("[ConfigAuditService] Calling auditLogger.log() for configKey={}", configKey);
        auditLogger.log(event);
        log.info("[ConfigAuditService] auditLogger.log() completed for configKey={}", configKey);
        
        // 2. RDBMS 기반 설정 이력 기록 (조회용)
        saveToDatabase(configKey, normalizedAction, userId, reason, valueType, 
                      EncryptedValueMasker.maskForAudit(oldValue, encryptedType),
                      EncryptedValueMasker.maskForAudit(newValue, encryptedType));
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

    /**
     * 설정 변경 이력을 RDBMS에 저장 (조회용)
     */
    @Transactional
    private void saveToDatabase(String configKey, String action, String userId, String reason, 
                               String valueType, String oldValue, String newValue) {
        try {
            // 변경 상세 정보를 JSON으로 저장
            Map<String, Object> changeDetails = new HashMap<>();
            changeDetails.put("configKey", configKey);
            changeDetails.put("oldValue", oldValue);
            changeDetails.put("newValue", newValue);
            changeDetails.put("action", action);
            changeDetails.put("reason", safeString(reason));
            changeDetails.put("valueType", safeString(valueType));
            
            String detailsJson = objectMapper.writeValueAsString(changeDetails);
            
            // AuditLog 엔티티 생성
            AuditLog auditLog = AuditLog.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(AuditLog.EventType.CONFIGURATION_CHANGE)
                    .eventCategory(AuditLog.EventCategory.CONFIGURE)
                    .eventName("Platform Config " + action)
                    .description(String.format("Config '%s' %s", configKey, action))
                    .resourceType("PlatformConfig")
                    .resourceId(configKey)
                    .action(action)
                    .result(AuditLog.Result.SUCCESS)
                    .eventTimestamp(LocalDateTime.now())
                    .details(detailsJson)
                    .build();
            
            auditLogRepository.save(auditLog);
            
            log.info("[ConfigAuditService] Config history saved to database: configKey={}, action={}", 
                    configKey, action);
                    
        } catch (JsonProcessingException e) {
            log.error("[ConfigAuditService] Failed to save config history to database: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[ConfigAuditService] Unexpected error saving config history: {}", e.getMessage(), e);
        }
    }
}


