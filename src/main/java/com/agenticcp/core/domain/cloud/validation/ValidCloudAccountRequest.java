package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 클라우드 계정 등록 요청 클래스 레벨 검증 어노테이션
 * 
 * CSP별 필수 필드를 검증합니다:
 * - AWS: roleArn (IAM_ROLE 인증 방식 사용 시)
 * - GCP: serviceAccountEmail
 * - Azure: azureTenantId, azureClientId
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CloudAccountRequestValidator.class)
@Documented
public @interface ValidCloudAccountRequest {
    
    String message() default "CSP별 필수 필드가 누락되었습니다";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}

