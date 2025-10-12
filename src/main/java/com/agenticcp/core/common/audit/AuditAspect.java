package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.AuditContextDto;
import com.agenticcp.core.common.util.AuditInfoExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 통합 감사 로깅 AOP Aspect
 * 
 * @AuditController가 붙은 클래스 내부의 모든 메서드 또는
 * @AuditRequired가 붙은 모든 메서드를 대상으로 합니다.
 * 
 * 우선순위: 메서드 레벨 @AuditRequired > 클래스 레벨 @AuditController
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Pointcut("@within(com.agenticcp.core.common.audit.AuditController) || @annotation(com.agenticcp.core.common.audit.AuditRequired)")
    public void auditPointcut() {}

    @Around("auditPointcut()")
    public Object auditAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        log.debug("감사 로깅 적용 검사: {}.{}", method.getDeclaringClass().getSimpleName(), method.getName());

        AuditRequired methodAudit = method.getAnnotation(AuditRequired.class);
        if (methodAudit != null) {
            log.debug("메서드 레벨 감사 로깅 적용: {}", method.getName());
            AuditContextDto auditInfo = AuditInfoExtractor.extractAuditInfo(joinPoint, null, methodAudit);
            return auditService.audit(joinPoint, auditInfo);
        }

        AuditController classAudit = method.getDeclaringClass().getAnnotation(AuditController.class);
        if (classAudit != null) {
            if (shouldSkipClassLevelAudit(method, classAudit)) {
                return joinPoint.proceed();
            }

            log.debug("클래스 레벨 감사 로깅 적용: {}", method.getName());
            AuditContextDto auditInfo = AuditInfoExtractor.extractAuditInfo(joinPoint, classAudit, null);
            return auditService.audit(joinPoint, auditInfo);
        }

        return joinPoint.proceed();
    }

    private boolean shouldSkipClassLevelAudit(Method method, AuditController classAudit) {
        String methodName = method.getName();

        if (AuditInfoExtractor.isExcludedMethod(methodName, classAudit.excludeMethods())) {
            log.debug("제외된 메서드로 감사 로깅 스킵: {}", methodName);
            return true;
        }

        String httpMethod = AuditInfoExtractor.extractHttpMethod(method);
        if (!AuditInfoExtractor.isTargetHttpMethod(httpMethod, classAudit.targetHttpMethods())) {
            log.debug("대상 HTTP 메서드가 아니므로 감사 로깅 스킵: {}", httpMethod);
            return true;
        }

        String action = com.agenticcp.core.common.util.AuditActionGenerator.generateActionName(methodName);
        if (action == null) {
            log.debug("액션 생성 실패로 감사 로깅 스킵: {}", methodName);
            return true;
        }

        return false;
    }
}
