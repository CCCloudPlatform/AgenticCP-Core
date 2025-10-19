package com.agenticcp.core.domain.ui.annotation;

import com.agenticcp.core.domain.ui.entity.MenuPermission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메뉴 접근 권한 검증 어노테이션
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MenuAccess {
    
    /**
     * 메뉴 키
     * 
     * @return 메뉴 키
     */
    String menuKey();
    
    /**
     * 접근 타입
     * 
     * @return 접근 타입
     */
    MenuPermission.AccessType accessType() default MenuPermission.AccessType.READ;
    
    /**
     * 권한 검증 실패 시 예외 발생 여부
     * false인 경우 권한이 없어도 메서드 실행을 허용 (로깅만 수행)
     * 
     * @return 예외 발생 여부
     */
    boolean throwOnAccessDenied() default true;
}
