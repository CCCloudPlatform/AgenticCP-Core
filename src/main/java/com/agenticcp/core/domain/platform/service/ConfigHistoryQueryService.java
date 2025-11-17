package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.agenticcp.core.domain.platform.dto.ConfigHistoryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 플랫폼 설정 변경 이력 조회 서비스
 * <p>
 * AuditLog 테이블에서 플랫폼 설정의 변경 이력을 조회하고 변환하는 서비스입니다.
 * 민감 정보(ENCRYPTED 타입, secret, password, key, token, credential 등)는 자동으로 마스킹 처리됩니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigHistoryQueryService {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 플랫폼 설정 변경 이력 조회
     * <p>
     * 지정된 설정 키의 변경 이력을 페이지네이션으로 조회합니다.
     * AuditLog 테이블에서 PLATFORM_CONFIG 리소스 타입의 로그를 조회하며,
     * 메타데이터에서 reason, valueType 등을 추출하고 민감 정보는 마스킹 처리합니다.
     * </p>
     *
     * @param configKey 조회할 설정 키
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (1~100 사이로 제한)
     * @return 설정 변경 이력 페이지
     */
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
                log.warn("[ConfigHistoryQueryService] Failed to parse audit log details for logId={}: {}", auditLog.getId(), e.getMessage(), e);
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
     * 메타데이터 JSON 문자열을 JsonNode로 파싱
     * <p>
     * AuditLog의 메타데이터 필드(JSON 문자열)를 JsonNode로 변환합니다.
     * null이거나 빈 문자열인 경우 빈 ObjectNode를 반환합니다.
     * </p>
     *
     * @param details 파싱할 JSON 문자열 (메타데이터)
     * @return 파싱된 JsonNode (null이거나 빈 문자열이면 빈 ObjectNode)
     * @throws JsonProcessingException JSON 파싱 실패 시
     */
    private JsonNode parseDetails(String details) throws JsonProcessingException {
        if (details == null || details.trim().isEmpty()) {
            log.debug("[ConfigHistoryQueryService] parseDetails - details is null or empty, returning empty ObjectNode");
            return objectMapper.createObjectNode();
        }
        log.debug("[ConfigHistoryQueryService] parseDetails - parsing JSON details");
        return objectMapper.readTree(details);
    }
    
    /**
     * JsonNode에서 문자열 필드 추출
     * <p>
     * JsonNode에서 지정된 필드명의 문자열 값을 추출합니다.
     * 필드가 존재하지 않거나 null인 경우 null을 반환합니다.
     * </p>
     *
     * @param node JSON 노드
     * @param fieldName 추출할 필드명
     * @return 필드 값 (존재하지 않거나 null이면 null)
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
     * <p>
     * 여러 필드명을 순차적으로 시도하여 첫 번째로 찾은 값을 반환합니다.
     * 모든 필드명을 시도했지만 값을 찾지 못한 경우 null을 반환합니다.
     * </p>
     *
     * @param node JSON 노드
     * @param fieldNames 시도할 필드명 목록 (순서대로 시도)
     * @return 첫 번째로 찾은 필드 값 (모두 없으면 null)
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
     * <p>
     * JsonNode에서 지정된 필드의 값을 추출하며, valueType이 민감한 타입인 경우 마스킹 처리합니다.
     * </p>
     *
     * @param node JSON 노드
     * @param fieldName 추출할 필드명
     * @param valueType 값의 타입 (민감 여부 판단에 사용)
     * @return 추출된 값 (민감한 타입이면 마스킹 처리됨)
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
     * <p>
     * 여러 필드명을 순차적으로 시도하여 첫 번째로 찾은 값을 반환하며,
     * valueType이 민감한 타입인 경우 마스킹 처리합니다.
     * </p>
     *
     * @param node JSON 노드
     * @param valueType 값의 타입 (민감 여부 판단에 사용)
     * @param fieldNames 시도할 필드명 목록 (순서대로 시도)
     * @return 첫 번째로 찾은 필드 값 (민감한 타입이면 마스킹 처리됨, 모두 없으면 null)
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
     * <p>
     * valueType 문자열에 다음 키워드가 포함되어 있는지 확인합니다:
     * secret, password, key, token, credential
     * 대소문자를 구분하지 않습니다.
     * </p>
     *
     * @param valueType 확인할 값 타입
     * @return 민감한 타입이면 true, 그렇지 않으면 false
     */
    private boolean isSensitiveValueType(String valueType) {
        if (valueType == null) {
            return false;
        }
        
        String lowerType = valueType.toLowerCase();
        boolean isSensitive = lowerType.contains("secret") || 
                             lowerType.contains("password") || 
                             lowerType.contains("key") || 
                             lowerType.contains("token") ||
                             lowerType.contains("credential");
        
        if (isSensitive) {
            log.debug("[ConfigHistoryQueryService] isSensitiveValueType - valueType={} is sensitive", valueType);
        }
        
        return isSensitive;
    }
    
    /**
     * 민감한 값 마스킹 처리
     * <p>
     * 민감한 값을 마스킹하여 반환합니다.
     * <ul>
     *   <li>값이 null이거나 길이가 4 이하인 경우: "****" 반환</li>
     *   <li>값이 "ENCRYPTED"인 경우: "***" 반환</li>
     *   <li>그 외의 경우: 앞 2자리 + "****" + 뒤 2자리 형태로 부분 마스킹</li>
     * </ul>
     * </p>
     *
     * @param value 마스킹할 값
     * @return 마스킹된 값
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 4) {
            log.debug("[ConfigHistoryQueryService] maskSensitiveValue - value is null or too short, returning full mask");
            return "****";
        }
        
        // ENCRYPTED 타입은 완전 마스킹 (보안상 더 안전)
        if ("ENCRYPTED".equalsIgnoreCase(value)) {
            log.debug("[ConfigHistoryQueryService] maskSensitiveValue - ENCRYPTED type, returning full mask");
            return "***";
        }
        
        // 다른 민감한 값들은 부분 마스킹
        String masked = value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        log.debug("[ConfigHistoryQueryService] maskSensitiveValue - partial masking applied");
        return masked;
    }
}



