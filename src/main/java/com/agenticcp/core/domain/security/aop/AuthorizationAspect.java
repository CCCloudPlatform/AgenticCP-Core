package com.agenticcp.core.domain.security.aop;

import com.agenticcp.core.domain.security.annotation.RequirePermission;
import com.agenticcp.core.domain.security.annotation.RequireRole;
import com.agenticcp.core.domain.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationAspect {

    private final AuthorizationService authorizationService;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String username = getCurrentUsername();
        String permissionKey = requirePermission.value();

        if (!authorizationService.hasPermission(username, permissionKey)) {
            throw new AccessDeniedException("접근 권한이 없습니다");
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        String username = getCurrentUsername();
        String[] roles = requireRole.value();
        boolean requireAll = requireRole.requireAll();

        boolean allowed = requireAll
                ? authorizationService.hasAllRoles(username, roles)
                : authorizationService.hasAnyRole(username, roles);

        if (!allowed) {
            throw new AccessDeniedException("접근 권한이 없습니다");
        }

        return joinPoint.proceed();
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new AccessDeniedException("인증되지 않은 사용자입니다");
    }
}


