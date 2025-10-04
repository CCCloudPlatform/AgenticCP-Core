package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 레벨 감사 로깅 애노테이션
 * 
 * 개별 메서드에 감사 로깅을 적용할 때 사용합니다.
 * 
 * @author AgenticCP Team
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
