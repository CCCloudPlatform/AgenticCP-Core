package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * AWS IAM Role ARN Validator 구현체
 * 
 * AWS IAM Role ARN은 다음 형식을 따라야 합니다:
 * arn:aws:iam::{account-id}:role/{role-name}
 * 예: arn:aws:iam::123456789012:role/MyRole
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public class IamRoleArnValidator implements ConstraintValidator<ValidIamRoleArn, String> {
    
    private static final Pattern IAM_ROLE_ARN_PATTERN = Pattern.compile(
        "^arn:aws:iam::\\d{12}:role/.+$"
    );
    
    private int minRoleNameLength;
    private int maxRoleNameLength;
    
    @Override
    public void initialize(ValidIamRoleArn constraintAnnotation) {
        this.minRoleNameLength = constraintAnnotation.minRoleNameLength();
        this.maxRoleNameLength = constraintAnnotation.maxRoleNameLength();
    }
    
    @Override
    public boolean isValid(String roleArn, ConstraintValidatorContext context) {
        // null 또는 빈 문자열은 통과 (선택적 필드로 처리)
        if (roleArn == null || roleArn.isEmpty()) {
            return true;
        }
        
        // 기본 ARN 형식 검증
        if (!IAM_ROLE_ARN_PATTERN.matcher(roleArn).matches()) {
            return false;
        }
        
        // Role 이름 길이 검증
        String roleName = extractRoleName(roleArn);
        if (roleName != null) {
            int roleNameLength = roleName.length();
            return roleNameLength >= minRoleNameLength && roleNameLength <= maxRoleNameLength;
        }
        
        return false;
    }
    
    /**
     * ARN에서 Role 이름을 추출합니다.
     * 
     * @param roleArn ARN 문자열
     * @return Role 이름 또는 null
     */
    private String extractRoleName(String roleArn) {
        try {
            // arn:aws:iam::123456789012:role/RoleName 형식에서 RoleName 부분 추출
            String[] parts = roleArn.split(":role/");
            if (parts.length == 2) {
                return parts[1];
            }
        } catch (Exception e) {
            // 파싱 실패 시 null 반환
        }
        return null;
    }
}

