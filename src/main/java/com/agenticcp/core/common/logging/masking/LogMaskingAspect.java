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
 * 로그 마스킹 AOP
 * 
 * 메서드 파라미터에 @Masked 애노테이션이 적용된 경우
 * 자동으로 마스킹 처리를 수행합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogMaskingAspect {

    private final MaskingService maskingService;

    /**
     * @Masked 애노테이션이 적용된 파라미터를 가진 메서드에 대해 마스킹 처리
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
     * 개별 파라미터 마스킹 처리
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
     * 서비스 메서드에서 민감한 정보 로깅 시 마스킹 처리
     */
    @Around("@annotation(org.springframework.stereotype.Service) && execution(* *(..))")
    public Object maskServiceMethodLogs(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        
        // 결과 객체가 있는 경우 마스킹 처리
        if (result != null) {
            maskingService.mask(result);
        }
        
        return result;
    }
}
