package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Azure Subscription ID 검증 어노테이션
 * 
 * Azure Subscription ID는 UUID 형식(8-4-4-4-12)이어야 합니다.
 * 예: 12345678-1234-1234-1234-123456789012
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AzureSubscriptionIdValidator.class)
@Documented
public @interface ValidAzureSubscriptionId {
    
    String message() default "올바른 Azure Subscription ID 형식이 아닙니다 (UUID 형식)";
    
    /**
     * 대소문자를 구분할지 여부 (기본값: false - 대소문자 구분 안함)
     */
    boolean caseSensitive() default false;
    
    /**
     * 엄격한 UUID 형식을 요구할지 여부 (기본값: true)
     * false인 경우 하이픈 없이도 허용
     */
    boolean strictFormat() default true;
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}

