package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * AWS IAM Role ARN 검증 어노테이션
 * 
 * AWS IAM Role ARN은 다음 형식을 따라야 합니다:
 * arn:aws:iam::{account-id}:role/{role-name}
 * 예: arn:aws:iam::123456789012:role/MyRole
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IamRoleArnValidator.class)
@Documented
public @interface ValidIamRoleArn {
    
    String message() default "올바른 AWS IAM Role ARN 형식이 아닙니다 (예: arn:aws:iam::123456789012:role/RoleName)";
    
    /**
     * Role 이름의 최소 길이 (기본값: 1)
     */
    int minRoleNameLength() default 1;
    
    /**
     * Role 이름의 최대 길이 (기본값: 64)
     */
    int maxRoleNameLength() default 64;
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}

