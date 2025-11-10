package com.agenticcp.core.domain.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 권한 검증 어노테이션
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-10
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();
    String resource() default "";
    String action() default "";
}


