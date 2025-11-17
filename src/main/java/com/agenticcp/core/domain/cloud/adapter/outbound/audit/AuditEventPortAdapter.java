package com.agenticcp.core.domain.cloud.adapter.outbound.audit;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * 감사 이벤트 포트 어댑터
 * AuditEventPort 인터페이스를 구현하여 감사 로그를 기록합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPortAdapter implements AuditEventPort {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 감사 이벤트를 기록합니다.
     * 
     * @param action 수행된 작업
     * @param subject 대상 엔티티/리소스
     * @param outcome 결과 (SUCCESS, FAILURE 등)
     * @param attributes 추가 속성 정보
     */
    @Override
    public void record(String action, String subject, String outcome, Map<String, Object> attributes) {
        log.debug("[AuditEventPortAdapter] record - action={}, subject={}, outcome={}", 
                  action, subject, outcome);
        
        try {
            // 현재 테넌트 정보 조회
            String tenantKey = TenantContextHolder.getCurrentTenantKey();
            
            // 감사 로그 엔티티 생성
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .resourceType(parseResourceType(subject))
                    .success(parseOutcome(outcome))
                    .severity(AuditSeverity.INFO)
                    .tenantId(tenantKey != null ? tenantKey : "SYSTEM")
                    .userId(getCurrentUserId())
                    .timestamp(Instant.now())
                    .metadata(serializeAttributes(attributes))
                    .build();
            
            // 감사 로그 저장
            auditLogRepository.save(auditLog);
            
            log.debug("[AuditEventPortAdapter] record - success");
            
        } catch (Exception e) {
            // 감사 로그 저장 실패 시 로깅만 하고 예외는 던지지 않음
            // (비즈니스 로직에 영향을 주지 않도록)
            log.error("[AuditEventPortAdapter] record - failed to save audit log", e);
        }
    }

    /**
     * 현재 사용자 ID를 조회합니다.
     * 
     * @return 사용자 ID (조회 실패 시 "SYSTEM")
     */
    private String getCurrentUserId() {
        try {
            // Spring Security Context에서 사용자 정보 추출
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("[AuditEventPortAdapter] getCurrentUserId - failed to get user from security context", e);
        }
        
        return "SYSTEM";
    }

    /**
     * outcome 문자열을 success boolean으로 변환합니다.
     * 
     * @param outcome 결과 문자열 (SUCCESS, FAILURE 등)
     * @return 성공 여부
     */
    private Boolean parseOutcome(String outcome) {
        if (outcome == null) {
            return false;
        }
        return "SUCCESS".equalsIgnoreCase(outcome) || "success".equals(outcome);
    }

    /**
     * 리소스 타입 문자열을 AuditResourceType enum으로 변환합니다.
     * 
     * @param resourceTypeStr 리소스 타입 문자열
     * @return AuditResourceType
     */
    private AuditResourceType parseResourceType(String resourceTypeStr) {
        try {
            // "CloudAccount" -> "CLOUD_ACCOUNT"로 변환
            String enumName = resourceTypeStr.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
            return AuditResourceType.valueOf(enumName);
        } catch (Exception e) {
            log.debug("[AuditEventPortAdapter] parseResourceType - unknown resource type: {}, using CONFIG", 
                      resourceTypeStr);
            return AuditResourceType.CONFIG;
        }
    }

    /**
     * 속성 맵을 JSON 문자열로 직렬화합니다.
     * 
     * @param attributes 속성 맵
     * @return JSON 문자열
     */
    private String serializeAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            log.warn("[AuditEventPortAdapter] serializeAttributes - JSON serialization failed", e);
            return "{}";
        }
    }
}

