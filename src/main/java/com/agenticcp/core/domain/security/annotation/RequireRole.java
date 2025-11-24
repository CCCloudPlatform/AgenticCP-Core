package com.agenticcp.core.domain.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 특정 역할 보유 여부를 검증하는 애노테이션입니다.
 *
 * @author AgenticCP Team
 * @version 1.0
 * @since 2025-11-08
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
    boolean requireAll() default false;
}


