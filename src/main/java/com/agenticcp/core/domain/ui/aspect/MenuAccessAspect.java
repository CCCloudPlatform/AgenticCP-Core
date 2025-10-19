package com.agenticcp.core.domain.ui.aspect;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.AuthorizationException;
import com.agenticcp.core.common.logging.LogMaskingUtils;
import com.agenticcp.core.domain.ui.annotation.MenuAccess;
import com.agenticcp.core.domain.ui.entity.MenuPermission;
import com.agenticcp.core.domain.ui.service.MenuAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 메뉴 접근 권한 검증 AOP
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MenuAccessAspect {

    private final MenuAuthorizationService menuAuthorizationService;

    /**
     * MenuAccess 어노테이션이 적용된 메서드에 대한 권한 검증
     * 
     * @param joinPoint 조인 포인트
     * @return 메서드 실행 결과
     * @throws Throwable 예외
     */
    @Around("@annotation(com.agenticcp.core.domain.ui.annotation.MenuAccess) || @within(com.agenticcp.core.domain.ui.annotation.MenuAccess)")
    public Object checkMenuAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 클래스 레벨 어노테이션 확인
        MenuAccess menuAccess = AnnotationUtils.findAnnotation(method, MenuAccess.class);
        if (menuAccess == null) {
            menuAccess = AnnotationUtils.findAnnotation(method.getDeclaringClass(), MenuAccess.class);
        }
        
        if (menuAccess == null) {
            log.warn("[MenuAccessAspect] MenuAccess annotation not found on method: {}", method.getName());
            return joinPoint.proceed();
        }

        // 현재 사용자 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("[MenuAccessAspect] User not authenticated");
            if (menuAccess.throwOnAccessDenied()) {
                throw new AuthorizationException();
            }
            return joinPoint.proceed();
        }

        String username = authentication.getName();
        String menuKey = menuAccess.menuKey();
        MenuPermission.AccessType accessType = menuAccess.accessType();
        boolean throwOnAccessDenied = menuAccess.throwOnAccessDenied();

        log.info("[MenuAccessAspect] checkMenuAccess - username={} menuKey={} accessType={} throwOnAccessDenied={} tenantKey={}", 
                username, menuKey, accessType, throwOnAccessDenied, LogMaskingUtils.maskTenantKey(TenantContextHolder.getCurrentTenantOrThrow().getTenantKey()));

        try {
            // 메뉴 키로 메뉴 ID 조회 (실제 구현에서는 MenuService를 통해 조회)
            Long menuId = getMenuIdByKey(menuKey);
            if (menuId == null) {
                log.warn("[MenuAccessAspect] Menu not found: menuKey={}", menuKey);
                if (throwOnAccessDenied) {
                    throw new AuthorizationException();
                }
                return joinPoint.proceed();
            }

            // 메뉴 접근 권한 확인
            boolean hasAccess = menuAuthorizationService.hasMenuAccess(username, menuId, accessType);
            
            if (!hasAccess) {
                log.warn("[MenuAccessAspect] Access denied - username={} menuKey={} accessType={} tenantKey={}", 
                        username, menuKey, accessType, LogMaskingUtils.maskTenantKey(TenantContextHolder.getCurrentTenantOrThrow().getTenantKey()));
                
                if (throwOnAccessDenied) {
                    throw new AuthorizationException();
                }
            } else {
                log.info("[MenuAccessAspect] Access granted - username={} menuKey={} accessType={} tenantKey={}", 
                        username, menuKey, accessType, LogMaskingUtils.maskTenantKey(TenantContextHolder.getCurrentTenantOrThrow().getTenantKey()));
            }

        } catch (Exception e) {
            log.error("[MenuAccessAspect] Error checking menu access - username={} menuKey={} accessType={} error={}", 
                    username, menuKey, accessType, e.getMessage(), e);
            
            if (throwOnAccessDenied) {
                throw new AuthorizationException();
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 메뉴 키로 메뉴 ID 조회
     * 실제 구현에서는 MenuService를 주입받아 사용
     * 
     * @param menuKey 메뉴 키
     * @return 메뉴 ID
     */
    private Long getMenuIdByKey(String menuKey) {
        // TODO: MenuService를 주입받아 실제 메뉴 ID 조회 로직 구현
        // 현재는 임시로 null 반환
        log.debug("[MenuAccessAspect] getMenuIdByKey - menuKey={} (not implemented)", menuKey);
        return null;
    }
}