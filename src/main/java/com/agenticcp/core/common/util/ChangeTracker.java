package com.agenticcp.core.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 엔티티 변경 추적 유틸리티
 * 
 * 엔티티의 변경 전/후 값을 비교하여 변경 사항을 추적합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeTracker {

    private final ObjectMapper objectMapper;

    /**
     * 엔티티의 변경 전 값 추출
     * 
     * @param entity 엔티티 객체
     * @return 변경 전 값 Map
     */
    public Map<String, Object> extractOldValue(Object entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            // 엔티티를 Map으로 변환
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(entity, Map.class);
            return filterSensitiveFields(map);
        } catch (Exception e) {
            log.warn("변경 전 값 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 엔티티의 변경 후 값 추출
     * 
     * @param entity 엔티티 객체
     * @return 변경 후 값 Map
     */
    public Map<String, Object> extractNewValue(Object entity) {
        return extractOldValue(entity); // 동일한 로직
    }

    /**
     * 두 엔티티의 변경 사항 추적
     * 
     * @param oldEntity 변경 전 엔티티
     * @param newEntity 변경 후 엔티티
     * @return Map with "oldValue", "newValue", "changedFields"
     */
    public Map<String, Object> trackChanges(Object oldEntity, Object newEntity) {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, Object> oldValue = extractOldValue(oldEntity);
        Map<String, Object> newValue = extractNewValue(newEntity);
        
        result.put("oldValue", oldValue);
        result.put("newValue", newValue);
        result.put("changedFields", findChangedFields(oldValue, newValue));
        
        return result;
    }

    /**
     * 변경된 필드 목록 추출
     * 
     * @param oldValue 변경 전 값
     * @param newValue 변경 후 값
     * @return 변경된 필드 목록
     */
    public Map<String, Map<String, Object>> findChangedFields(
        Map<String, Object> oldValue, 
        Map<String, Object> newValue
    ) {
        if (oldValue == null || newValue == null) {
            return new HashMap<>();
        }
        
        Map<String, Map<String, Object>> changedFields = new HashMap<>();
        
        for (String key : newValue.keySet()) {
            Object oldVal = oldValue.get(key);
            Object newVal = newValue.get(key);
            
            if (!isEqual(oldVal, newVal)) {
                Map<String, Object> change = new HashMap<>();
                change.put("old", oldVal);
                change.put("new", newVal);
                changedFields.put(key, change);
            }
        }
        
        return changedFields;
    }

    /**
     * 민감 필드 필터링
     */
    private Map<String, Object> filterSensitiveFields(Map<String, Object> map) {
        Map<String, Object> filtered = new HashMap<>(map);
        
        // 민감 필드 마스킹
        String[] sensitiveFields = {"password", "passwordHash", "token", "apiKey", "secret"};
        for (String field : sensitiveFields) {
            if (filtered.containsKey(field)) {
                filtered.put(field, "***MASKED***");
            }
        }
        
        return filtered;
    }

    /**
     * 두 객체가 같은지 비교
     */
    private boolean isEqual(Object obj1, Object obj2) {
        if (obj1 == obj2) return true;
        if (obj1 == null || obj2 == null) return false;
        return obj1.equals(obj2);
    }
}

