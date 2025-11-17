package com.agenticcp.core.common.logging.masking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 로그 마스킹 AOP Aspect입니다. @Masked가 지정된 파라미터/객체에 마스킹을 적용합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogMaskingAspect {

    private final MaskingService maskingService;

    /**
     * 컨트롤러 진입 시점에서 @Masked 파라미터를 찾아 마스킹합니다.
     * HTTP 메서드 매핑 애노테이션이 선언된 메서드에만 적용됩니다.
     *
     * @param joinPoint 현재 실행 중인 컨트롤러 조인포인트
     * @return 원본 메서드 실행 결과
     */
    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object maskSensitiveParameters(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();

        // 파라미터 마스킹 처리
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(Masked.class)) {
                args[i] = maskParameter(args[i], parameter.getAnnotation(Masked.class));
            }
        }

        return joinPoint.proceed(args);
    }

    /**
     * 개별 파라미터 마스킹 처리.
     * 문자열은 지정된 마스킹 전략을 적용하고, 객체는 재귀적으로 마스킹합니다.
     */
    private Object maskParameter(Object value, Masked annotation) {
        if (value == null) {
            return value;
        }

        if (value instanceof String) {
            return maskingService.applyMaskingStrategy((String) value, annotation.type());
        } else {
            // 객체인 경우 재귀적으로 마스킹 처리
            maskingService.mask(value);
            return value;
        }
    }

    /**
     * 서비스 레이어 결과 객체를 마스킹하여 로깅 시 노출을 방지합니다.
     * 클래스에 @Service가 선언된 빈의 모든 메서드 실행 후 결과 객체에 마스킹을 적용합니다.
     *
     * @param joinPoint 현재 실행 중인 서비스 조인포인트
     * @return 마스킹된 결과 객체
     */
    @Around("@within(org.springframework.stereotype.Service)")
    public Object maskServiceMethodLogs(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        
        // 결과 객체가 있는 경우 마스킹 처리
        if (result != null) {
            maskingService.mask(result);
        }
        
        return result;
    }
}
