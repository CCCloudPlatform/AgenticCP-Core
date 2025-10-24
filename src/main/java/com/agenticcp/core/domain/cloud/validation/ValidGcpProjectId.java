package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * GCP Project ID 검증 어노테이션
 * 
 * GCP Project ID는 다음 조건을 만족해야 합니다:
 * - 6-30자
 * - 소문자로 시작
 * - 소문자, 숫자, 하이픈만 사용 가능
 * - 하이픈으로 끝날 수 없음
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GcpProjectIdValidator.class)
@Documented
public @interface ValidGcpProjectId {
    
    String message() default "올바른 GCP Project ID 형식이 아닙니다 (6-30자, 소문자/숫자/하이픈만 가능)";
    
    /**
     * Project ID의 최소 길이 (기본값: 6)
     */
    int minLength() default 6;
    
    /**
     * Project ID의 최대 길이 (기본값: 30)
     */
    int maxLength() default 30;
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}

