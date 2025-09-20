package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 리소스 조건 데이터 전송 객체
 * 
 * <p>정책의 리소스 관련 조건을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceConditions {
    
    /**
     * 허용 리소스 타입 목록
     */
    private List<String> allowedResourceTypes;
    
    /**
     * 금지 리소스 타입 목록
     */
    private List<String> deniedResourceTypes;
    
    /**
     * 허용 리소스 ID 목록
     */
    private List<String> allowedResourceIds;
    
    /**
     * 금지 리소스 ID 목록
     */
    private List<String> deniedResourceIds;
    
    /**
     * 허용 리소스 태그 목록
     */
    private List<ResourceTagCondition> allowedResourceTags;
    
    /**
     * 금지 리소스 태그 목록
     */
    private List<ResourceTagCondition> deniedResourceTags;
    
    /**
     * 허용 리소스 속성 조건
     */
    private List<ResourceAttributeCondition> allowedResourceAttributes;
    
    /**
     * 금지 리소스 속성 조건
     */
    private List<ResourceAttributeCondition> deniedResourceAttributes;
    
    /**
     * 리소스 태그 조건 데이터 전송 객체
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceTagCondition {
        private String tagKey;
        private String tagValue;
        private String operator; // EQ, NE, CONTAINS, NOT_CONTAINS, EXISTS, NOT_EXISTS
        
        /**
         * 조건이 유효한지 확인
         * 
         * @return 유효하면 true, 그렇지 않으면 false
         */
        public boolean isValid() {
            return tagKey != null && !tagKey.isEmpty() && 
                   operator != null && !operator.isEmpty();
        }
    }
    
    /**
     * 리소스 속성 조건 데이터 전송 객체
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceAttributeCondition {
        private String attributeName;
        private String operator; // EQ, NE, GT, LT, IN, NOT_IN, CONTAINS, NOT_CONTAINS
        private Object value;
        private String description;
        
        /**
         * 조건이 유효한지 확인
         * 
         * @return 유효하면 true, 그렇지 않으면 false
         */
        public boolean isValid() {
            return attributeName != null && !attributeName.isEmpty() && 
                   operator != null && !operator.isEmpty() && 
                   value != null;
        }
    }
    
    /**
     * 리소스 타입이 허용되는지 확인
     * 
     * @param resourceType 확인할 리소스 타입
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isResourceTypeAllowed(String resourceType) {
        if (resourceType == null || resourceType.isEmpty()) {
            return false;
        }
        
        // 금지 리소스 타입 확인
        if (deniedResourceTypes != null && deniedResourceTypes.contains(resourceType)) {
            return false;
        }
        
        // 허용 리소스 타입 확인
        if (allowedResourceTypes != null && !allowedResourceTypes.isEmpty()) {
            return allowedResourceTypes.contains(resourceType);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * 리소스 ID가 허용되는지 확인
     * 
     * @param resourceId 확인할 리소스 ID
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isResourceIdAllowed(String resourceId) {
        if (resourceId == null || resourceId.isEmpty()) {
            return false;
        }
        
        // 금지 리소스 ID 확인
        if (deniedResourceIds != null && deniedResourceIds.contains(resourceId)) {
            return false;
        }
        
        // 허용 리소스 ID 확인
        if (allowedResourceIds != null && !allowedResourceIds.isEmpty()) {
            return allowedResourceIds.contains(resourceId);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * 리소스 태그가 허용되는지 확인
     * 
     * @param tagKey 태그 키
     * @param tagValue 태그 값
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isResourceTagAllowed(String tagKey, String tagValue) {
        if (tagKey == null || tagKey.isEmpty()) {
            return false;
        }
        
        // 금지 리소스 태그 확인
        if (deniedResourceTags != null) {
            for (ResourceTagCondition condition : deniedResourceTags) {
                if (condition.isValid() && 
                    condition.getTagKey().equals(tagKey) && 
                    evaluateTagCondition(condition, tagValue)) {
                    return false;
                }
            }
        }
        
        // 허용 리소스 태그 확인
        if (allowedResourceTags != null && !allowedResourceTags.isEmpty()) {
            for (ResourceTagCondition condition : allowedResourceTags) {
                if (condition.isValid() && 
                    condition.getTagKey().equals(tagKey) && 
                    evaluateTagCondition(condition, tagValue)) {
                    return true;
                }
            }
            return false; // 허용 조건이 있는데 만족되지 않음
        }
        
        return true; // 허용 조건이 없으면 기본적으로 허용
    }
    
    /**
     * 리소스 속성이 허용되는지 확인
     * 
     * @param attributeName 속성 이름
     * @param attributeValue 속성 값
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isResourceAttributeAllowed(String attributeName, Object attributeValue) {
        if (attributeName == null || attributeValue == null) {
            return false;
        }
        
        // 금지 리소스 속성 확인
        if (deniedResourceAttributes != null) {
            for (ResourceAttributeCondition condition : deniedResourceAttributes) {
                if (condition.isValid() && 
                    condition.getAttributeName().equals(attributeName) && 
                    evaluateAttributeCondition(condition, attributeValue)) {
                    return false;
                }
            }
        }
        
        // 허용 리소스 속성 확인
        if (allowedResourceAttributes != null && !allowedResourceAttributes.isEmpty()) {
            for (ResourceAttributeCondition condition : allowedResourceAttributes) {
                if (condition.isValid() && 
                    condition.getAttributeName().equals(attributeName) && 
                    evaluateAttributeCondition(condition, attributeValue)) {
                    return true;
                }
            }
            return false; // 허용 조건이 있는데 만족되지 않음
        }
        
        return true; // 허용 조건이 없으면 기본적으로 허용
    }
    
    /**
     * 태그 조건 평가
     * 
     * @param condition 조건
     * @param value 비교할 값
     * @return 조건을 만족하면 true, 그렇지 않으면 false
     */
    private boolean evaluateTagCondition(ResourceTagCondition condition, String value) {
        // TODO: 실제 태그 조건 평가 로직 구현
        // 현재는 단순 문자열 비교로 대체
        return condition.getTagValue() != null && 
               condition.getTagValue().equals(value);
    }
    
    /**
     * 속성 조건 평가
     * 
     * @param condition 조건
     * @param value 비교할 값
     * @return 조건을 만족하면 true, 그렇지 않으면 false
     */
    private boolean evaluateAttributeCondition(ResourceAttributeCondition condition, Object value) {
        // TODO: 실제 속성 조건 평가 로직 구현
        // 현재는 단순 문자열 비교로 대체
        return condition.getValue().toString().equals(value.toString());
    }
}
