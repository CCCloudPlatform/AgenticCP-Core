package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.agenticcp.core.domain.platform.dto.ConfigHistoryResponse;
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
import java.time.LocalDateTime;
import java.time.ZoneId;

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
        
        // PlatformConfig 리소스 타입과 대상 리소스 ID 기준으로 검색
        Page<AuditLog> logs = auditLogRepository
                .findByResourceTypeAndTargetResourceId(
                        AuditResourceType.PLATFORM_CONFIG,
                        configKey,
                        pageable
                );
        
        log.info("[ConfigHistoryQueryService] Found {} logs for PLATFORM_CONFIG and targetResourceId={}",
                logs.getTotalElements(), configKey);

        return logs.map(auditLog -> {
            try {
                // 메타데이터(JSON) 파싱
                JsonNode metadataNode = parseDetails(auditLog.getMetadata());
                
                // 각 필드 추출 (여러 가능한 필드명 시도)
                String reason = extractStringWithFallback(metadataNode, "reason", "changeReason", "description");
                String valueType = extractStringWithFallback(metadataNode, "valueType", "configType", "type");
                
                // old/new 값은 엔티티 컬럼에서 직접 사용
                String prevValue = auditLog.getOldValue();
                String newValue = auditLog.getNewValue();
                if (isSensitiveValueType(valueType)) {
                    prevValue = prevValue == null ? null : maskSensitiveValue(prevValue);
                    newValue = newValue == null ? null : maskSensitiveValue(newValue);
                }
                
                // 디버깅: 추출된 값들 출력
                log.info("[ConfigHistoryQueryService] Extracted values - reason={}, valueType={}, prevValue={}, newValue={}", 
                        reason, valueType, prevValue, newValue);
                
                // ENCRYPTED 타입의 경우 고정 표기
                if ("ENCRYPTED".equalsIgnoreCase(valueType)) {
                    if (prevValue != null && !prevValue.isEmpty()) {
                        prevValue = "Encrypted";
                    }
                    if (newValue != null && !newValue.isEmpty()) {
                        newValue = "Encrypted";
                    }
                }
                
                return new ConfigHistoryResponse(
                        auditLog.getAction(),
                        auditLog.getUserId(),
                        reason,
                        valueType,
                        prevValue,
                        newValue,
                        LocalDateTime.ofInstant(auditLog.getTimestamp(), ZoneId.systemDefault())
                );
            } catch (Exception e) {
                log.warn("Failed to parse audit log details for logId={}: {}", auditLog.getId(), e.getMessage());
                // 파싱 실패 시 기본값으로 응답 생성
                return new ConfigHistoryResponse(
                        auditLog.getAction(),
                        auditLog.getUserId(),
                        null,
                        null,
                        null,
                        null,
                        LocalDateTime.ofInstant(auditLog.getTimestamp(), ZoneId.systemDefault())
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



