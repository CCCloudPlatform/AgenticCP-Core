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
     * 
     * ⚠️ 주의: 객체인 경우 실제 객체를 변경합니다.
     * 컨트롤러 파라미터는 요청 데이터이므로 마스킹해도 되지만,
     * 비즈니스 로직에 사용되는 객체에는 사용하지 마세요.
     * 
     * @param value 마스킹할 파라미터 값
     * @param annotation @Masked 어노테이션
     * @return 마스킹된 값 (문자열은 새 문자열, 객체는 원본이 변경됨)
     */
    private Object maskParameter(Object value, Masked annotation) {
        if (value == null) {
            return value;
        }

        if (value instanceof String) {
            // 문자열은 새 문자열 반환 (원본 변경 없음)
            return maskingService.applyMaskingStrategy((String) value, annotation.type());
        } else {
            // ⚠️ 객체인 경우 실제 객체를 변경합니다.
            // 컨트롤러 파라미터는 요청 데이터이므로 마스킹해도 되지만,
            // 비즈니스 로직에 사용되는 객체에는 사용하지 마세요.
            maskingService.mask(value);
            return value;
        }
    }

    /**
     * 서비스 레이어 결과 객체를 마스킹하여 로깅 시 노출을 방지합니다.
     * 
     * 주의: 이 메서드는 비활성화되었습니다.
     * MaskingService.mask()가 실제 객체의 필드 값을 변경하여 자격증명 등이 손상되는 문제가 발생했습니다.
     * 로깅이 필요한 경우 명시적으로 마스킹을 적용해야 합니다.
     * 
     * @param joinPoint 현재 실행 중인 서비스 조인포인트
     * @return 원본 결과 객체 (마스킹하지 않음)
     */
    // @Around("@within(org.springframework.stereotype.Service)") // 비활성화: 실제 객체 값 변경 방지
    public Object maskServiceMethodLogs(ProceedingJoinPoint joinPoint) throws Throwable {
        // 원본 객체를 변경하지 않고 그대로 반환
        // 로깅이 필요한 경우 명시적으로 마스킹을 적용해야 합니다.
        return joinPoint.proceed();
    }
}
