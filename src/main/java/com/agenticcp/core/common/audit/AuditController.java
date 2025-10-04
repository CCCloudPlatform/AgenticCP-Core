package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 클래스 레벨 감사 로깅 애노테이션
 * 
 * 이 애노테이션을 컨트롤러 클래스에 적용하면 해당 컨트롤러의 모든 메서드에 대해
 * 자동으로 감사 로깅이 적용됩니다. 메서드명을 기반으로 AuditAction을 자동 매핑합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
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
