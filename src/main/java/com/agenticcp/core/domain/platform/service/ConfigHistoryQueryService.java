package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.domain.platform.dto.ConfigHistoryResponse;
import com.agenticcp.core.domain.security.entity.AuditLog;
import com.agenticcp.core.domain.security.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigHistoryQueryService {

    private static final Logger log = LoggerFactory.getLogger(ConfigHistoryQueryService.class);
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public Page<ConfigHistoryResponse> getHistory(String configKey, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
        
        log.info("[ConfigHistoryQueryService] Searching for configKey={} with resourceType={}", 
                configKey, AuditResourceType.PLATFORM_CONFIG.name());
        
        // 먼저 정확한 조건으로 검색
        Page<AuditLog> logs = auditLogRepository
                .findByResourceTypeAndResourceIdAndEventTypeOrderByEventTimestampDesc(
                        AuditResourceType.PLATFORM_CONFIG.name(),
                        configKey,
                        AuditLog.EventType.CONFIGURATION_CHANGE,
                        pageable
                );
        
        log.info("[ConfigHistoryQueryService] Found {} logs for exact match", logs.getTotalElements());
        
        // 결과가 없으면 다른 가능한 resourceType들도 시도해보기
        if (logs.getTotalElements() == 0) {
            log.warn("[ConfigHistoryQueryService] No exact match found. Trying alternative resourceTypes...");
            
            // "PlatformConfig" 문자열로도 시도해보기
            Page<AuditLog> alternativeLogs = auditLogRepository
                    .findByResourceTypeAndResourceIdAndEventTypeOrderByEventTimestampDesc(
                            "PlatformConfig",  // ConfigAuditService에서 metadata에 저장하는 값
                            configKey,
                            AuditLog.EventType.CONFIGURATION_CHANGE,
                            pageable
                    );
            
            log.info("[ConfigHistoryQueryService] Found {} logs with 'PlatformConfig' resourceType", 
                    alternativeLogs.getTotalElements());
            
            if (alternativeLogs.getTotalElements() > 0) {
                logs = alternativeLogs;
            }
        }

        return logs.map(auditLog -> {
            try {
                // 디버깅: 원본 details 로그 출력
                log.info("[ConfigHistoryQueryService] Processing auditLog ID={}, details={}", 
                        auditLog.getId(), auditLog.getDetails());
                
                // details JSON 파싱
                JsonNode detailsNode = parseDetails(auditLog.getDetails());
                
                // 디버깅: 파싱된 JSON 구조 출력
                log.info("[ConfigHistoryQueryService] Parsed detailsNode: {}", detailsNode.toString());
                
                // 각 필드 추출 (여러 가능한 필드명 시도)
                String reason = extractStringWithFallback(detailsNode, "reason", "changeReason", "description");
                String valueType = extractStringWithFallback(detailsNode, "valueType", "configType", "type");
                String prevValue = extractAndMaskValueWithFallback(detailsNode, valueType, "oldValue", "prevValue", "previousValue");
                String newValue = extractAndMaskValueWithFallback(detailsNode, valueType, "newValue", "currentValue", "updatedValue");
                
                // 디버깅: 추출된 값들 출력
                log.info("[ConfigHistoryQueryService] Extracted values - reason={}, valueType={}, prevValue={}, newValue={}", 
                        reason, valueType, prevValue, newValue);
                
                // ENCRYPTED 타입의 경우 더 명확한 표시
                if ("ENCRYPTED".equalsIgnoreCase(valueType)) {
                    if (prevValue != null && !prevValue.isEmpty()) {
                        prevValue = "[ENCRYPTED_VALUE]";
                    }
                    if (newValue != null && !newValue.isEmpty()) {
                        newValue = "[ENCRYPTED_VALUE]";
                    }
                }
                
                return new ConfigHistoryResponse(
                        auditLog.getAction(),
                        auditLog.getUser() != null ? String.valueOf(auditLog.getUser().getId()) : null,
                        reason,
                        valueType,
                        prevValue,
                        newValue,
                        auditLog.getEventTimestamp()
                );
            } catch (Exception e) {
                log.warn("Failed to parse audit log details for logId={}: {}", auditLog.getId(), e.getMessage());
                // 파싱 실패 시 기본값으로 응답 생성
                return new ConfigHistoryResponse(
                        auditLog.getAction(),
                        auditLog.getUser() != null ? String.valueOf(auditLog.getUser().getId()) : null,
                        null,
                        null,
                        null,
                        null,
                        auditLog.getEventTimestamp()
                );
            }
        });
    }
    
    /**
     * details JSON 문자열을 JsonNode로 파싱
     */
    private JsonNode parseDetails(String details) throws JsonProcessingException {
        if (details == null || details.trim().isEmpty()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(details);
    }
    
    /**
     * JsonNode에서 문자열 필드 추출
     */
    private String extractString(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode.isNull() ? null : fieldNode.asText();
    }
    
    /**
     * JsonNode에서 문자열 필드 추출 (여러 필드명 시도)
     */
    private String extractStringWithFallback(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = extractString(node, fieldName);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
    
    /**
     * JsonNode에서 값을 추출하고 민감한 값은 마스킹 처리
     */
    private String extractAndMaskValue(JsonNode node, String fieldName, String valueType) {
        String value = extractString(node, fieldName);
        if (value == null) {
            return null;
        }
        
        // 민감한 값 타입인 경우 마스킹 처리
        if (isSensitiveValueType(valueType)) {
            return maskSensitiveValue(value);
        }
        
        return value;
    }
    
    /**
     * JsonNode에서 값을 추출하고 민감한 값은 마스킹 처리 (여러 필드명 시도)
     */
    private String extractAndMaskValueWithFallback(JsonNode node, String valueType, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = extractString(node, fieldName);
            if (value != null && !value.trim().isEmpty()) {
                // 민감한 값 타입인 경우 마스킹 처리
                if (isSensitiveValueType(valueType)) {
                    return maskSensitiveValue(value);
                }
                return value;
            }
        }
        return null;
    }
    
    /**
     * 민감한 값 타입인지 확인
     */
    private boolean isSensitiveValueType(String valueType) {
        if (valueType == null) {
            return false;
        }
        
        String lowerType = valueType.toLowerCase();
        return lowerType.contains("secret") || 
               lowerType.contains("password") || 
               lowerType.contains("key") || 
               lowerType.contains("token") ||
               lowerType.contains("credential");
    }
    
    /**
     * 민감한 값 마스킹 처리
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        
        // ENCRYPTED 타입은 완전 마스킹 (보안상 더 안전)
        if ("ENCRYPTED".equalsIgnoreCase(value)) {
            return "***";
        }
        
        // 다른 민감한 값들은 부분 마스킹
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}



