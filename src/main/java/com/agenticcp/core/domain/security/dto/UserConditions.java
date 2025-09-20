package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용자 조건 데이터 전송 객체
 * 
 * <p>정책의 사용자 관련 조건을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConditions {
    
    /**
     * 허용 사용자 ID 목록
     */
    private List<String> allowedUserIds;
    
    /**
     * 금지 사용자 ID 목록
     */
    private List<String> deniedUserIds;
    
    /**
     * 허용 사용자 그룹 목록
     */
    private List<String> allowedUserGroups;
    
    /**
     * 금지 사용자 그룹 목록
     */
    private List<String> deniedUserGroups;
    
    /**
     * 허용 역할 목록
     */
    private List<String> allowedRoles;
    
    /**
     * 금지 역할 목록
     */
    private List<String> deniedRoles;
    
    /**
     * 허용 권한 목록
     */
    private List<String> allowedPermissions;
    
    /**
     * 금지 권한 목록
     */
    private List<String> deniedPermissions;
    
    /**
     * 허용 사용자 속성 조건
     */
    private List<UserAttributeCondition> allowedUserAttributes;
    
    /**
     * 금지 사용자 속성 조건
     */
    private List<UserAttributeCondition> deniedUserAttributes;
    
    /**
     * 사용자 속성 조건 데이터 전송 객체
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAttributeCondition {
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
     * 사용자 ID가 허용되는지 확인
     * 
     * @param userId 확인할 사용자 ID
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isUserIdAllowed(String userId) {
        if (userId == null || userId.isEmpty()) {
            return false;
        }
        
        // 금지 사용자 ID 확인
        if (deniedUserIds != null && deniedUserIds.contains(userId)) {
            return false;
        }
        
        // 허용 사용자 ID 확인
        if (allowedUserIds != null && !allowedUserIds.isEmpty()) {
            return allowedUserIds.contains(userId);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * 사용자 그룹이 허용되는지 확인
     * 
     * @param userGroup 확인할 사용자 그룹
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isUserGroupAllowed(String userGroup) {
        if (userGroup == null || userGroup.isEmpty()) {
            return false;
        }
        
        // 금지 사용자 그룹 확인
        if (deniedUserGroups != null && deniedUserGroups.contains(userGroup)) {
            return false;
        }
        
        // 허용 사용자 그룹 확인
        if (allowedUserGroups != null && !allowedUserGroups.isEmpty()) {
            return allowedUserGroups.contains(userGroup);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * 역할이 허용되는지 확인
     * 
     * @param role 확인할 역할
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isRoleAllowed(String role) {
        if (role == null || role.isEmpty()) {
            return false;
        }
        
        // 금지 역할 확인
        if (deniedRoles != null && deniedRoles.contains(role)) {
            return false;
        }
        
        // 허용 역할 확인
        if (allowedRoles != null && !allowedRoles.isEmpty()) {
            return allowedRoles.contains(role);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * 권한이 허용되는지 확인
     * 
     * @param permission 확인할 권한
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isPermissionAllowed(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }
        
        // 금지 권한 확인
        if (deniedPermissions != null && deniedPermissions.contains(permission)) {
            return false;
        }
        
        // 허용 권한 확인
        if (allowedPermissions != null && !allowedPermissions.isEmpty()) {
            return allowedPermissions.contains(permission);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * 사용자 속성이 허용되는지 확인
     * 
     * @param attributeName 속성 이름
     * @param attributeValue 속성 값
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isUserAttributeAllowed(String attributeName, Object attributeValue) {
        if (attributeName == null || attributeValue == null) {
            return false;
        }
        
        // 금지 사용자 속성 확인
        if (deniedUserAttributes != null) {
            for (UserAttributeCondition condition : deniedUserAttributes) {
                if (condition.isValid() && 
                    condition.getAttributeName().equals(attributeName) && 
                    evaluateAttributeCondition(condition, attributeValue)) {
                    return false;
                }
            }
        }
        
        // 허용 사용자 속성 확인
        if (allowedUserAttributes != null && !allowedUserAttributes.isEmpty()) {
            for (UserAttributeCondition condition : allowedUserAttributes) {
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
     * 속성 조건 평가
     * 
     * @param condition 조건
     * @param value 비교할 값
     * @return 조건을 만족하면 true, 그렇지 않으면 false
     */
    private boolean evaluateAttributeCondition(UserAttributeCondition condition, Object value) {
        // TODO: 실제 속성 조건 평가 로직 구현
        // 현재는 단순 문자열 비교로 대체
        return condition.getValue().toString().equals(value.toString());
    }
}
