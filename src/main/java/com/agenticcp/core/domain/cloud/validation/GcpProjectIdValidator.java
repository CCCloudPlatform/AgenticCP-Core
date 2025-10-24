package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * GCP Project ID Validator 구현체
 * 
 * GCP Project ID는 다음 조건을 만족해야 합니다:
 * - 6-30자
 * - 소문자로 시작
 * - 소문자, 숫자, 하이픈만 사용 가능
 * - 하이픈으로 끝날 수 없음
 * 예: my-gcp-project, test-project-123
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public class GcpProjectIdValidator implements ConstraintValidator<ValidGcpProjectId, String> {
    
    private int minLength;
    private int maxLength;
    
    @Override
    public void initialize(ValidGcpProjectId constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
    }
    
    @Override
    public boolean isValid(String projectId, ConstraintValidatorContext context) {
        // null 또는 빈 문자열은 @NotBlank로 처리하므로 여기서는 통과
        if (projectId == null || projectId.isEmpty()) {
            return true;
        }
        
        // 길이 검증
        int length = projectId.length();
        if (length < minLength || length > maxLength) {
            return false;
        }
        
        // 기본 패턴 검증 (소문자로 시작, 소문자/숫자/하이픈만 사용, 하이픈으로 끝나지 않음)
        return isValidGcpProjectIdFormat(projectId);
    }
    
    /**
     * GCP Project ID 형식을 검증합니다.
     * 
     * @param projectId 검증할 Project ID
     * @return 형식이 올바르면 true, 그렇지 않으면 false
     */
    private boolean isValidGcpProjectIdFormat(String projectId) {
        // 소문자로 시작해야 함
        if (!Character.isLowerCase(projectId.charAt(0))) {
            return false;
        }
        
        // 하이픈으로 끝날 수 없음
        if (projectId.endsWith("-")) {
            return false;
        }
        
        // 소문자, 숫자, 하이픈만 사용 가능
        for (char c : projectId.toCharArray()) {
            if (!Character.isLowerCase(c) && !Character.isDigit(c) && c != '-') {
                return false;
            }
        }
        
        return true;
    }
}

