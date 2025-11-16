package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 클래스 레벨 감사 로깅 애노테이션입니다.
 * 대상 HTTP 메서드/제외 메서드 등 기본 정책을 지정할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditController {
    AuditResourceType resourceType();
    AuditSeverity defaultSeverity() default AuditSeverity.MEDIUM;
    boolean defaultIncludeRequestData() default false;
    boolean defaultIncludeResponseData() default false;
    String[] targetHttpMethods() default {"POST", "PUT", "PATCH", "DELETE"};
    String[] excludeMethods() default {};
}
