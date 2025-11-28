package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.audit.AuditPublishEvent;
import com.agenticcp.core.common.audit.AuditEventBuilder;
import com.agenticcp.core.common.context.AuditContextProvider;
import com.agenticcp.core.common.dto.audit.AuditContextDto;
import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.enums.FeatureFlagSeverity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 기능 플래그 감사 로깅 서비스
 * 
 * 기능 플래그 변경에 대한 감사 로그를 자동으로 기록합니다.
 * FeatureFlagSeverity에 따라 적절한 AuditSeverity로 변환하여 로깅합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeatureFlagAuditService {

    private final AuditLogger auditLogger;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditContextProvider auditContextProvider;

    /**
     * 기능 플래그 변경 감사 로깅
     * 
     * @param oldFlag 변경 전 플래그 (null인 경우 CREATE)
     * @param newFlag 변경 후 플래그
     * @param action 액션 (CREATE, UPDATE, DELETE, TOGGLE 등)
     * @param userId 사용자 ID
     */
    public void logFlagChange(FeatureFlag oldFlag, FeatureFlag newFlag, String action, String userId) {
        FeatureFlagSeverity severity = newFlag != null ? newFlag.getSeverity() : 
                                       (oldFlag != null ? oldFlag.getSeverity() : FeatureFlagSeverity.LOW);
        
        logFlagChange(oldFlag, newFlag, action, userId, severity);
    }

    /**
     * 기능 플래그 변경 감사 로깅 (심각도 지정)
     * 
     * @param oldFlag 변경 전 플래그
     * @param newFlag 변경 후 플래그
     * @param action 액션
     * @param userId 사용자 ID
     * @param severity 플래그 심각도
     */
    public void logFlagChange(FeatureFlag oldFlag, FeatureFlag newFlag, String action, 
                             String userId, FeatureFlagSeverity severity) {
        String normalizedAction = normalizeAction(action);
        String flagKey = newFlag != null ? newFlag.getFlagKey() : 
                        (oldFlag != null ? oldFlag.getFlagKey() : "unknown");

        log.info("[FeatureFlagAuditService] logFlagChange - action={} flagKey={} severity={} userId={}",
                normalizedAction, flagKey, severity, userId);

        // 변경 상세 정보 생성
        Map<String, Object> changeDetails = createChangeDetails(oldFlag, newFlag, normalizedAction);
        
        // 변경 전/후 값 생성
        Map<String, Object> oldValue = oldFlag != null ? convertFlagToMap(oldFlag) : null;
        Map<String, Object> newValue = newFlag != null ? convertFlagToMap(newFlag) : null;

        // AuditSeverity 변환
        AuditSeverity auditSeverity = convertToAuditSeverity(severity);

        // 컨텍스트 정보 가져오기
        AuditContextDto mdcContext = auditContextProvider.getCurrentContext();
        
        // AuditContextDto 생성
        AuditContextDto context = AuditContextDto.builder()
                .requestId(mdcContext.requestId())
                .tenantId(mdcContext.tenantId())
                .clientIp(mdcContext.clientIp())
                .userId(userId != null ? userId : mdcContext.userId())
                .action(normalizedAction)
                .resourceType(AuditResourceType.FEATURE_FLAG)
                .httpMethod(mdcContext.httpMethod())
                .requestPath(mdcContext.requestPath() != null && !mdcContext.requestPath().isBlank() 
                        ? mdcContext.requestPath() 
                        : "/api/v1/feature-flags")
                .operationSummary("Feature Flag " + normalizedAction)
                .controllerName("FeatureFlagController")
                .methodName("")
                .severity(auditSeverity)
                .includeRequestData(true)
                .includeResponseData(false)
                .build();

        // 감사 이벤트 생성
        AuditEventDto event = AuditEventBuilder.builder(context)
                .requestData(changeDetails)
                .responseData(null)
                .oldValue(oldValue)
                .newValue(newValue)
                .targetResourceId(flagKey)
                .success(true)
                .build();

        // 파일 로깅
        auditLogger.log(event);

        // 이벤트 발행 (DB 저장은 리스너가 처리)
        publishEvent(event);
    }


    /**
     * FeatureFlagSeverity를 AuditSeverity로 변환
     * 
     * @param severity FeatureFlagSeverity
     * @return AuditSeverity
     */
    public AuditSeverity convertToAuditSeverity(FeatureFlagSeverity severity) {
        if (severity == null) {
            return AuditSeverity.INFO;
        }
        return severity.toAuditSeverity();
    }

    /**
     * 변경 상세 정보 생성
     * 
     * @param oldFlag 변경 전 플래그
     * @param newFlag 변경 후 플래그
     * @param action 액션
     * @return 변경 상세 정보 Map
     */
    public Map<String, Object> createChangeDetails(FeatureFlag oldFlag, FeatureFlag newFlag, String action) {
        Map<String, Object> details = new HashMap<>();
        
        String flagKey = newFlag != null ? newFlag.getFlagKey() : 
                        (oldFlag != null ? oldFlag.getFlagKey() : "unknown");
        
        details.put("flagKey", flagKey);
        details.put("action", action);
        details.put("eventType", "FEATURE_FLAG_CHANGE");
        details.put("eventCategory", "FEATURE_FLAG");
        
        if (oldFlag != null) {
            details.put("oldFlagName", oldFlag.getFlagName());
            details.put("oldIsEnabled", oldFlag.getIsEnabled());
            details.put("oldSeverity", oldFlag.getSeverity() != null ? oldFlag.getSeverity().name() : null);
        }
        
        if (newFlag != null) {
            details.put("newFlagName", newFlag.getFlagName());
            details.put("newIsEnabled", newFlag.getIsEnabled());
            details.put("newSeverity", newFlag.getSeverity() != null ? newFlag.getSeverity().name() : null);
        }
        
        // 변경된 필드 추적
        if (oldFlag != null && newFlag != null) {
            Map<String, Object> changedFields = new HashMap<>();
            
            if (!java.util.Objects.equals(oldFlag.getFlagName(), newFlag.getFlagName())) {
                changedFields.put("flagName", Map.of("old", oldFlag.getFlagName(), "new", newFlag.getFlagName()));
            }
            if (!java.util.Objects.equals(oldFlag.getIsEnabled(), newFlag.getIsEnabled())) {
                changedFields.put("isEnabled", Map.of("old", oldFlag.getIsEnabled(), "new", newFlag.getIsEnabled()));
            }
            if (!java.util.Objects.equals(oldFlag.getSeverity(), newFlag.getSeverity())) {
                changedFields.put("severity", Map.of(
                    "old", oldFlag.getSeverity() != null ? oldFlag.getSeverity().name() : null,
                    "new", newFlag.getSeverity() != null ? newFlag.getSeverity().name() : null
                ));
            }
            if (!java.util.Objects.equals(oldFlag.getDescription(), newFlag.getDescription())) {
                changedFields.put("description", Map.of("old", oldFlag.getDescription(), "new", newFlag.getDescription()));
            }
            if (!java.util.Objects.equals(oldFlag.getRolloutPercentage(), newFlag.getRolloutPercentage())) {
                changedFields.put("rolloutPercentage", Map.of("old", oldFlag.getRolloutPercentage(), "new", newFlag.getRolloutPercentage()));
            }
            
            if (!changedFields.isEmpty()) {
                details.put("changedFields", changedFields);
            }
        }
        
        return details;
    }

    /**
     * 이벤트 발행
     * 
     * @param event 감사 이벤트
     */
    public void publishEvent(AuditEventDto event) {
        try {
            AuditPublishEvent publishEvent = new AuditPublishEvent(this, event);
            eventPublisher.publishEvent(publishEvent);
            log.debug("[FeatureFlagAuditService] 이벤트 발행 완료 - action={} flagKey={}", 
                     event.action(), event.targetResourceId());
        } catch (Exception e) {
            log.error("[FeatureFlagAuditService] 이벤트 발행 실패 - action={}: {}", 
                     event.action(), e.getMessage(), e);
        }
    }

    /**
     * FeatureFlag를 Map으로 변환
     * 
     * @param flag FeatureFlag 엔티티
     * @return Map
     */
    private Map<String, Object> convertFlagToMap(FeatureFlag flag) {
        if (flag == null) {
            return null;
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", flag.getId());
        map.put("flagKey", flag.getFlagKey());
        map.put("flagName", flag.getFlagName());
        map.put("description", flag.getDescription());
        map.put("isEnabled", flag.getIsEnabled());
        map.put("status", flag.getStatus() != null ? flag.getStatus().name() : null);
        map.put("severity", flag.getSeverity() != null ? flag.getSeverity().name() : null);
        map.put("rolloutPercentage", flag.getRolloutPercentage());
        map.put("targetTenants", flag.getTargetTenants());
        map.put("targetUsers", flag.getTargetUsers());
        map.put("startDate", flag.getStartDate());
        map.put("endDate", flag.getEndDate());
        map.put("metadata", flag.getMetadata());
        map.put("cacheTtlSeconds", flag.getCacheTtlSeconds());
        
        return map;
    }

    /**
     * 액션 정규화
     * 
     * @param action 원본 액션
     * @return 정규화된 액션
     */
    private String normalizeAction(String action) {
        if (action == null) {
            return "UPDATE";
        }
        String upper = action.trim().toUpperCase();
        return switch (upper) {
            case "CREATE", "CREATED", "ADD", "ADDED" -> "CREATE";
            case "UPDATE", "UPDATED", "MODIFY", "MODIFIED", "CHANGE", "CHANGED" -> "UPDATE";
            case "DELETE", "DELETED", "REMOVE", "REMOVED" -> "DELETE";
            case "TOGGLE", "TOGGLED", "ENABLE", "DISABLE" -> "TOGGLE";
            default -> upper;
        };
    }
}

