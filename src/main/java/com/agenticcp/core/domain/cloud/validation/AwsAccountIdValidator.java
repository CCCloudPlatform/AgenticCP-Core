package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * AWS Account ID Validator 구현체
 * 
 * AWS Account ID는 정확히 12자리 숫자여야 합니다.
 * 예: 123456789012
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public class AwsAccountIdValidator implements ConstraintValidator<ValidAwsAccountId, String> {
    
    private static final Pattern AWS_ACCOUNT_ID_PATTERN = Pattern.compile("^\\d{12}$");
    
    private int length;
    private boolean numericOnly;
    
    @Override
    public void initialize(ValidAwsAccountId constraintAnnotation) {
        this.length = constraintAnnotation.length();
        this.numericOnly = constraintAnnotation.numericOnly();
    }
    
    @Override
    public boolean isValid(String accountId, ConstraintValidatorContext context) {
        // null 또는 빈 문자열은 @NotBlank로 처리하므로 여기서는 통과
        if (accountId == null || accountId.isEmpty()) {
            return true;
        }
        
        // 길이 검증
        if (accountId.length() != length) {
            return false;
        }
        
        // 숫자만 허용하는 경우 숫자 검증
        if (numericOnly) {
            return isValidNumericAccountId(accountId);
        }
        
        // 비숫자 허용인 경우 길이만 맞으면 통과 (기본 AWS Account ID 형식은 무시)
        return true;
    }
    
    /**
     * 숫자로만 구성된 Account ID인지 검증합니다.
     * 
     * @param accountId 검증할 Account ID
     * @return 숫자로만 구성되어 있으면 true, 그렇지 않으면 false
     */
    private boolean isValidNumericAccountId(String accountId) {
        for (char c : accountId.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}

