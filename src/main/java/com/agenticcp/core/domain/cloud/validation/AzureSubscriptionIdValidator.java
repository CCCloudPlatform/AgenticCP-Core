package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Azure Subscription ID Validator 구현체
 * 
 * Azure Subscription ID는 UUID 형식(8-4-4-4-12)이어야 합니다.
 * 예: 12345678-1234-1234-1234-123456789012
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public class AzureSubscriptionIdValidator implements ConstraintValidator<ValidAzureSubscriptionId, String> {
    
    private static final Pattern AZURE_SUBSCRIPTION_ID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    private static final Pattern AZURE_SUBSCRIPTION_ID_NO_HYPHEN_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{32}$"
    );
    
    private static final Pattern AZURE_SUBSCRIPTION_ID_LOWERCASE_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );
    
    private static final Pattern AZURE_SUBSCRIPTION_ID_LOWERCASE_NO_HYPHEN_PATTERN = Pattern.compile(
        "^[0-9a-f]{32}$"
    );
    
    private boolean caseSensitive;
    private boolean strictFormat;
    
    @Override
    public void initialize(ValidAzureSubscriptionId constraintAnnotation) {
        this.caseSensitive = constraintAnnotation.caseSensitive();
        this.strictFormat = constraintAnnotation.strictFormat();
    }
    
    @Override
    public boolean isValid(String subscriptionId, ConstraintValidatorContext context) {
        // null 또는 빈 문자열은 @NotBlank로 처리하므로 여기서는 통과
        if (subscriptionId == null || subscriptionId.isEmpty()) {
            return true;
        }
        
        // 대소문자 구분 설정에 따라 검증할 문자열 준비
        String validationString = caseSensitive ? subscriptionId : subscriptionId.toLowerCase();
        
        // 대소문자 구분이 활성화된 경우 원본 문자열로 검증
        if (caseSensitive) {
            return validateSubscriptionId(subscriptionId);
        }
        
        // 대소문자 구분이 비활성화된 경우 소문자로 변환하여 검증
        return validateSubscriptionId(validationString);
    }
    
    /**
     * Subscription ID 형식을 검증합니다.
     * 
     * @param subscriptionId 검증할 Subscription ID
     * @return 형식이 올바르면 true, 그렇지 않으면 false
     */
    private boolean validateSubscriptionId(String subscriptionId) {
        // 대소문자 구분 설정에 따라 적절한 패턴 선택
        Pattern hyphenPattern = caseSensitive ? AZURE_SUBSCRIPTION_ID_LOWERCASE_PATTERN : AZURE_SUBSCRIPTION_ID_PATTERN;
        Pattern noHyphenPattern = caseSensitive ? AZURE_SUBSCRIPTION_ID_LOWERCASE_NO_HYPHEN_PATTERN : AZURE_SUBSCRIPTION_ID_NO_HYPHEN_PATTERN;
        
        // 엄격한 형식 검증
        if (strictFormat) {
            return hyphenPattern.matcher(subscriptionId).matches();
        }
        
        // 유연한 형식 검증 (하이픈 있음/없음 모두 허용)
        return hyphenPattern.matcher(subscriptionId).matches() ||
               noHyphenPattern.matcher(subscriptionId).matches();
    }
}

