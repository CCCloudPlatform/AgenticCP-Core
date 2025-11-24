package com.agenticcp.core.domain.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 권한 검증이 필요한 클래스나 메서드에 부여하는 애노테이션입니다.
 *
 * @author AgenticCP Team
 * @version 1.0
 * @since 2025-11-08
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();
    String resource() default "";
    String action() default "";
}


