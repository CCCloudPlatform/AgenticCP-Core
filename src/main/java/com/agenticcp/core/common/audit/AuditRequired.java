package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 레벨 감사 로깅 애노테이션입니다.
 * 액션/리소스/심각도 및 데이터 포함 여부를 설정합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditRequired {
    String action();
    AuditResourceType resourceType();
    String description() default "";
    boolean includeRequestData() default false;
    boolean includeResponseData() default false;
    AuditSeverity severity() default AuditSeverity.INFO;
}
