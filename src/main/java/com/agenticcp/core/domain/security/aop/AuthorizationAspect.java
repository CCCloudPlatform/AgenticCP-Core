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

import java.util.Arrays;

/**
 * 권한 및 역할 기반 접근 제어를 처리하는 AOP 컴포넌트입니다.
 *
 * <p>보안 애노테이션을 해석하여 현재 인증된 사용자의 권한을 검사합니다.</p>
 *
 * @author AgenticCP Team
 * @version 1.0
 * @since 2025-11-08
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationAspect {

    private final AuthorizationService authorizationService;

    /**
     * 권한 애노테이션을 검사하여 접근 가능 여부를 판단합니다.
     *
     * @param joinPoint        실행 대상 조인 포인트
     * @param requirePermission 권한 검증 애노테이션
     * @return 원본 메서드 실행 결과
     * @throws Throwable 접근 권한이 없거나 원본 메서드 실행 중 예외가 발생한 경우
     */
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String username = getCurrentUsername();
        String permissionKey = requirePermission.value();
        log.debug("[AuthorizationAspect] checkPermission - username={}, permissionKey={}", username, permissionKey);

        if (!authorizationService.hasPermission(username, permissionKey)) {
            log.warn("[AuthorizationAspect] Permission denied - username={}, permissionKey={}", username, permissionKey);
            throw new AccessDeniedException("접근 권한이 없습니다");
        }

        return joinPoint.proceed();
    }

    /**
     * 역할 애노테이션을 검사하여 접근 가능 여부를 판단합니다.
     *
     * @param joinPoint  실행 대상 조인 포인트
     * @param requireRole 역할 검증 애노테이션
     * @return 원본 메서드 실행 결과
     * @throws Throwable 접근 권한이 없거나 원본 메서드 실행 중 예외가 발생한 경우
     */
    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        String username = getCurrentUsername();
        String[] roles = requireRole.value();
        boolean requireAll = requireRole.requireAll();
        log.debug("[AuthorizationAspect] checkRole - username={}, roles={}, requireAll={}", username, Arrays.toString(roles), requireAll);

        boolean allowed = requireAll
                ? authorizationService.hasAllRoles(username, roles)
                : authorizationService.hasAnyRole(username, roles);

        if (!allowed) {
            log.warn("[AuthorizationAspect] Role check failed - username={}, roles={}, requireAll={}", username, Arrays.toString(roles), requireAll);
            throw new AccessDeniedException("접근 권한이 없습니다");
        }

        return joinPoint.proceed();
    }

    /**
     * 현재 인증된 사용자의 사용자명을 반환합니다.
     *
     * @return 인증된 사용자명
     * @throws AccessDeniedException 인증 정보가 없는 경우
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new AccessDeniedException("인증되지 않은 사용자입니다");
    }
}


