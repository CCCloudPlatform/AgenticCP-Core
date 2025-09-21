package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

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
        if (condition == null || value == null) {
            return false;
        }
        
        String expectedValue = condition.getTagValue();
        if (expectedValue == null) {
            return false;
        }
        
        // 연산자에 따른 평가
        String operator = condition.getOperator();
        if ("EQ".equals(operator) || "EQUALS".equals(operator)) {
            return expectedValue.equals(value);
        } else if ("NE".equals(operator) || "NOT_EQUALS".equals(operator)) {
            return !expectedValue.equals(value);
        } else if ("CONTAINS".equals(operator)) {
            return value.contains(expectedValue);
        } else if ("NOT_CONTAINS".equals(operator)) {
            return !value.contains(expectedValue);
        } else if ("STARTS_WITH".equals(operator)) {
            return value.startsWith(expectedValue);
        } else if ("ENDS_WITH".equals(operator)) {
            return value.endsWith(expectedValue);
        } else if ("REGEX".equals(operator)) {
            try {
                return value.matches(expectedValue);
            } catch (Exception e) {
                return false; // 잘못된 정규식
            }
        } else if ("IN".equals(operator)) {
            // 쉼표로 구분된 값들 중 하나와 일치하는지 확인
            String[] values = expectedValue.split(",");
            for (String v : values) {
                if (v.trim().equals(value)) {
                    return true;
                }
            }
            return false;
        } else if ("NOT_IN".equals(operator)) {
            // 쉼표로 구분된 값들 중 어느 것과도 일치하지 않는지 확인
            String[] notInValues = expectedValue.split(",");
            for (String v : notInValues) {
                if (v.trim().equals(value)) {
                    return false;
                }
            }
            return true;
        } else if ("GT".equals(operator) || "GREATER_THAN".equals(operator)) {
            try {
                return Double.parseDouble(value) > Double.parseDouble(expectedValue);
            } catch (NumberFormatException e) {
                return value.compareTo(expectedValue) > 0; // 문자열 비교로 대체
            }
        } else if ("LT".equals(operator) || "LESS_THAN".equals(operator)) {
            try {
                return Double.parseDouble(value) < Double.parseDouble(expectedValue);
            } catch (NumberFormatException e) {
                return value.compareTo(expectedValue) < 0; // 문자열 비교로 대체
            }
        } else if ("GTE".equals(operator) || "GREATER_THAN_OR_EQUAL".equals(operator)) {
            try {
                return Double.parseDouble(value) >= Double.parseDouble(expectedValue);
            } catch (NumberFormatException e) {
                return value.compareTo(expectedValue) >= 0; // 문자열 비교로 대체
            }
        } else if ("LTE".equals(operator) || "LESS_THAN_OR_EQUAL".equals(operator)) {
            try {
                return Double.parseDouble(value) <= Double.parseDouble(expectedValue);
            } catch (NumberFormatException e) {
                return value.compareTo(expectedValue) <= 0; // 문자열 비교로 대체
            }
        } else {
            return false;
        }
    }
    
    /**
     * 속성 조건 평가
     * 
     * @param condition 조건
     * @param value 비교할 값
     * @return 조건을 만족하면 true, 그렇지 않으면 false
     */
    private boolean evaluateAttributeCondition(ResourceAttributeCondition condition, Object value) {
        if (condition == null || value == null) {
            return false;
        }
        
        Object expectedValue = condition.getValue();
        if (expectedValue == null) {
            return false;
        }
        
        // 연산자에 따른 평가
        String operator = condition.getOperator();
        if ("EQ".equals(operator) || "EQUALS".equals(operator)) {
            return compareValues(expectedValue, value) == 0;
        } else if ("NE".equals(operator) || "NOT_EQUALS".equals(operator)) {
            return compareValues(expectedValue, value) != 0;
        } else if ("CONTAINS".equals(operator)) {
            return value.toString().contains(expectedValue.toString());
        } else if ("NOT_CONTAINS".equals(operator)) {
            return !value.toString().contains(expectedValue.toString());
        } else if ("STARTS_WITH".equals(operator)) {
            return value.toString().startsWith(expectedValue.toString());
        } else if ("ENDS_WITH".equals(operator)) {
            return value.toString().endsWith(expectedValue.toString());
        } else if ("REGEX".equals(operator)) {
            try {
                return value.toString().matches(expectedValue.toString());
            } catch (Exception e) {
                return false; // 잘못된 정규식
            }
        } else if ("IN".equals(operator)) {
            // 배열이나 리스트에서 값이 포함되는지 확인
            return isValueInCollection(expectedValue, value);
        } else if ("NOT_IN".equals(operator)) {
            // 배열이나 리스트에서 값이 포함되지 않는지 확인
            return !isValueInCollection(expectedValue, value);
        } else if ("GT".equals(operator) || "GREATER_THAN".equals(operator)) {
            return compareValues(expectedValue, value) < 0;
        } else if ("LT".equals(operator) || "LESS_THAN".equals(operator)) {
            return compareValues(expectedValue, value) > 0;
        } else if ("GTE".equals(operator) || "GREATER_THAN_OR_EQUAL".equals(operator)) {
            return compareValues(expectedValue, value) <= 0;
        } else if ("LTE".equals(operator) || "LESS_THAN_OR_EQUAL".equals(operator)) {
            return compareValues(expectedValue, value) >= 0;
        } else if ("IS_NULL".equals(operator)) {
            return value == null;
        } else if ("IS_NOT_NULL".equals(operator)) {
            return value != null;
        } else if ("IS_EMPTY".equals(operator)) {
            return isEmpty(value);
        } else if ("IS_NOT_EMPTY".equals(operator)) {
            return !isEmpty(value);
        } else {
            return false;
        }
    }
    
    /**
     * 값 비교 (숫자, 날짜, 문자열 지원)
     */
    private int compareValues(Object expected, Object actual) {
        try {
            // 숫자 비교
            if (expected instanceof Number && actual instanceof Number) {
                double expectedNum = ((Number) expected).doubleValue();
                double actualNum = ((Number) actual).doubleValue();
                return Double.compare(expectedNum, actualNum);
            }
            
            // 날짜 비교
            if (expected instanceof LocalDateTime && actual instanceof LocalDateTime) {
                return ((LocalDateTime) expected).compareTo((LocalDateTime) actual);
            }
            
            // 문자열 비교
            return expected.toString().compareTo(actual.toString());
            
        } catch (Exception e) {
            return expected.toString().compareTo(actual.toString());
        }
    }
    
    /**
     * 컬렉션에서 값이 포함되는지 확인
     */
    private boolean isValueInCollection(Object collection, Object value) {
        try {
            if (collection instanceof java.util.Collection) {
                return ((java.util.Collection<?>) collection).contains(value);
            }
            
            if (collection instanceof Object[]) {
                Object[] array = (Object[]) collection;
                for (Object item : array) {
                    if (Objects.equals(item, value)) {
                        return true;
                    }
                }
                return false;
            }
            
            if (collection instanceof String) {
                // 쉼표로 구분된 문자열 처리
                String[] values = collection.toString().split(",");
                for (String v : values) {
                    if (v.trim().equals(value.toString())) {
                        return true;
                    }
                }
                return false;
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 값이 비어있는지 확인
     */
    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        
        if (value instanceof java.util.Collection) {
            return ((java.util.Collection<?>) value).isEmpty();
        }
        
        if (value instanceof Object[]) {
            return ((Object[]) value).length == 0;
        }
        
        return false;
    }
}
