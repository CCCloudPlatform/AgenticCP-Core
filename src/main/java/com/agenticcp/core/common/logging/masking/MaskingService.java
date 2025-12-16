package com.agenticcp.core.common.logging.masking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 민감 정보 마스킹 처리 서비스입니다. @Masked 필드/Map/List를 재귀적으로 마스킹합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaskingService {

    private final MaskingStrategyProvider strategyProvider;

    /**
     * 객체의 마스킹이 필요한 필드들을 처리합니다.
     * 
     * ⚠️ 주의: 이 메서드는 실제 객체의 필드 값을 변경합니다.
     * 로깅 목적으로만 사용해야 하며, 비즈니스 로직에 사용되는 객체에는 사용하지 마세요.
     * 
     * @deprecated 실제 객체를 변경하는 것은 위험합니다. 
     *             대신 {@link #toMaskedJson(Object)} 또는 로깅 시점에 명시적으로 마스킹을 적용하세요.
     * @param object 마스킹 처리할 객체 (원본이 변경됨)
     */
    @Deprecated
    public void mask(Object object) {
        if (object == null) {
            return;
        }
        
        try {
            maskObjectRecursively(object);
        } catch (Exception e) {
            log.warn("마스킹 처리 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * Map 형태의 데이터를 재귀적으로 마스킹 처리합니다.
     * 
     * @param map 마스킹 처리할 Map
     */
    public void maskMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof String) {
                // 문자열 값에 대해서는 키 이름으로 마스킹 타입을 추정
                String maskedValue = maskByKeyName(entry.getKey(), (String) value);
                entry.setValue(maskedValue);
            } else if (value instanceof Map) {
                // 중첩된 Map 처리
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                maskMap(nestedMap);
            } else if (value instanceof List) {
                // List 처리
                maskList((List<?>) value);
            }
        }
    }
    
    /**
     * 객체를 재귀적으로 마스킹 처리합니다.
     */
    private void maskObjectRecursively(Object object) {
        if (object == null) {
            return;
        }
        
        Class<?> clazz = object.getClass();
        
        // Map 타입 처리
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            maskMap(map);
            return;
        }
        
        // List 타입 처리
        if (object instanceof List) {
            maskList((List<?>) object);
            return;
        }
        
        // 일반 객체 처리
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Masked.class)) {
                maskField(object, field);
            }
        }
    }
    
    /**
     * 특정 필드를 마스킹 처리합니다.
     */
    private void maskField(Object object, Field field) {
        try {
            field.setAccessible(true);
            Object value = field.get(object);
            
            if (value instanceof String) {
                Masked annotation = field.getAnnotation(Masked.class);
                MaskingType type = annotation.type();
                String maskedValue = applyMaskingStrategy((String) value, type);
                field.set(object, maskedValue);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                maskMap(map);
            } else if (value instanceof List) {
                maskList((List<?>) value);
            }
        } catch (IllegalAccessException e) {
            log.warn("필드 마스킹 처리 실패 [Field: {}]: {}", field.getName(), e.getMessage());
        }
    }
    
    /**
     * List의 각 요소를 마스킹 처리합니다.
     */
    @SuppressWarnings("unchecked")
    private void maskList(List<?> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof String) {
                // 문자열 리스트의 경우 기본 마스킹 적용
                ((List<Object>) list).set(i, strategyProvider.getStrategy(MaskingType.DEFAULT).mask((String) item));
            } else {
                // 객체 리스트의 경우 재귀 처리
                maskObjectRecursively(item);
            }
        }
    }
    
    /**
     * 키 이름을 기반으로 마스킹 타입을 추정하고 마스킹을 적용합니다.
     */
    private String maskByKeyName(String keyName, String value) {
        if (keyName == null || value == null) {
            return value;
        }
        
        String lowerKeyName = keyName.toLowerCase();
        MaskingType type = determineMaskingTypeByKeyName(lowerKeyName);
        
        if (type != null) {
            return strategyProvider.getStrategy(type).mask(value);
        }
        
        return value; // 마스킹하지 않음
    }
    
    /**
     * 키 이름을 기반으로 마스킹 타입을 결정합니다.
     */
    private MaskingType determineMaskingTypeByKeyName(String lowerKeyName) {
        // 키워드와 마스킹 타입 매핑
        Map<MaskingType, String[]> keywordMap = Map.of(
            MaskingType.PASSWORD, new String[]{"password", "pwd"},
            MaskingType.CREDIT_CARD, new String[]{"credit", "card"},
            MaskingType.IP_ADDRESS, new String[]{"ip"},
            MaskingType.EMAIL, new String[]{"email"},
            MaskingType.PHONE_NUMBER, new String[]{"phone", "tel"},
            MaskingType.SSN, new String[]{"ssn", "resident"},
            MaskingType.SECRET_KEY, new String[]{"secretkey", "secret_key", "secret-key"},
            MaskingType.TOKEN, new String[]{"token", "jwt"},
            MaskingType.ACCOUNT_SCOPE, new String[]{"accountscope", "account_scope", "account-scope"}
        );
        
        return keywordMap.entrySet().stream()
            .filter(entry -> Arrays.stream(entry.getValue())
                .anyMatch(lowerKeyName::contains))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 마스킹 전략을 적용합니다.
     */
    public String applyMaskingStrategy(String value, MaskingType type) {
        return strategyProvider.getStrategy(type).mask(value);
    }
    
    // ==================== 기존 LogMaskingUtils 기능 통합 ====================
    
    /**
     * 일반 문자열 마스킹. 앞/뒤 일부만 노출하고 가운데를 *로 마스킹합니다.
     * 예) abcdefg -> ab***fg (revealStart=2, revealEnd=2)
     * 
     * @param value 마스킹할 문자열
     * @param revealStart 앞에서 노출할 문자 수
     * @param revealEnd 뒤에서 노출할 문자 수
     * @return 마스킹된 문자열
     */
    public String mask(String value, int revealStart, int revealEnd) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int length = value.length();
        if (revealStart + revealEnd >= length) {
            return repeat('*', Math.max(3, length));
        }
        String start = value.substring(0, Math.max(0, revealStart));
        String end = value.substring(length - Math.max(0, revealEnd));
        int maskLen = Math.max(3, length - start.length() - end.length());
        return start + repeat('*', maskLen) + end;
    }
    
    /**
     * 테넌트 키 마스킹 (앞 2자리, 뒤 2자리 노출)
     */
    public String maskTenantKey(String tenantKey) {
        return mask(tenantKey, 2, 2);
    }

    /**
     * 계정 범위(Account Scope) 마스킹 (앞 4자리, 뒤 4자리 노출)
     * AWS Account ID, Azure Subscription ID, GCP Project ID 등을 마스킹합니다.
     * 
     * 내부적으로 AccountScopeMaskingStrategy를 사용합니다.
     */
    public String maskAccountScope(String accountScope) {
        return applyMaskingStrategy(accountScope, MaskingType.ACCOUNT_SCOPE);
    }

    public String maskIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        
        if (ip.contains(".")) { // IPv4
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
            }
        } else if (ip.contains(":")) { // IPv6
            String[] parts = ip.split(":");
            if (parts.length >= 4) {
                return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":****";
            }
        }
        
        return ip;
    }
    
    /**
     * User-Agent 마스킹
     * 길이가 maxPrefixLen 이하면 원문, 그 이상이면 prefix + "..."
     */
    public String previewUserAgent(String userAgent, int maxPrefixLen) {
        if (userAgent == null || userAgent.length() <= maxPrefixLen) {
            return userAgent;
        }
        return userAgent.substring(0, maxPrefixLen) + "...";
    }
    
    /**
     * 문자열 반복 유틸리티
     */
    private String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
    
    /**
     * 객체를 마스킹하여 JSON 문자열로 변환합니다.
     * 원본 객체는 변경하지 않고, 마스킹된 복사본을 JSON으로 직렬화합니다.
     * 
     * @param object 마스킹할 객체
     * @param objectMapper JSON 직렬화에 사용할 ObjectMapper
     * @return 마스킹된 JSON 문자열, 객체가 null이면 null
     */
    public String toMaskedJson(Object object, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        if (object == null) {
            return null;
        }
        
        try {
            // 객체를 복사한 후 마스킹 (원본 보호)
            Object maskedCopy = deepCopyAndMask(object, objectMapper);
            return objectMapper.writeValueAsString(maskedCopy);
        } catch (Exception e) {
            log.warn("마스킹된 JSON 변환 실패: {}", e.getMessage());
            return "{}"; // 오류 시 빈 JSON 반환
        }
    }
    
    /**
     * 객체를 깊은 복사한 후 마스킹합니다.
     * 원본 객체는 변경하지 않습니다.
     * 
     * @param object 복사 및 마스킹할 객체
     * @param objectMapper JSON 직렬화/역직렬화에 사용할 ObjectMapper
     * @return 마스킹된 복사본
     */
    private Object deepCopyAndMask(Object object, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        if (object == null) {
            return null;
        }
        
        try {
            // Jackson을 사용하여 깊은 복사
            String json = objectMapper.writeValueAsString(object);
            Object copy = objectMapper.readValue(json, object.getClass());
            
            // 복사본에만 마스킹 적용 (원본은 변경되지 않음)
            mask(copy);
            return copy;
        } catch (Exception e) {
            log.warn("객체 복사 및 마스킹 실패: {}", e.getMessage());
            return object; // 실패 시 원본 반환 (마스킹되지 않음)
        }
    }
}
