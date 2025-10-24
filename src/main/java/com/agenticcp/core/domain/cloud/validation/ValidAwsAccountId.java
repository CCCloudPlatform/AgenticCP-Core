package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * AWS Account ID 검증 어노테이션
 * 
 * AWS Account ID는 정확히 12자리 숫자여야 합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AwsAccountIdValidator.class)
@Documented
public @interface ValidAwsAccountId {
    
    String message() default "올바른 AWS Account ID 형식이 아닙니다 (12자리 숫자)";
    
    /**
     * Account ID의 길이 (기본값: 12)
     */
    int length() default 12;
    
    /**
     * Account ID가 숫자로만 구성되어야 하는지 여부 (기본값: true)
     */
    boolean numericOnly() default true;
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}

