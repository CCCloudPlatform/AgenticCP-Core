package com.agenticcp.core.common.logging.masking;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * LogMaskingAspect 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogMaskingAspect 테스트")
@SuppressWarnings("deprecation") // mask() 메서드는 deprecated이지만 테스트에서 검증을 위해 사용
class LogMaskingAspectTest {

    @Mock
    private MaskingService maskingService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Method method;

    private LogMaskingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new LogMaskingAspect(maskingService);
    }

    @Test
    @DisplayName("@Masked 애노테이션이 적용된 String 파라미터 마스킹")
    void maskSensitiveParametersWithString() throws Throwable {
        // given
        String originalValue = "secretPassword";
        String maskedValue = "********";
        Object[] args = {originalValue};
        Parameter[] parameters = createMockParametersWithMaskedAnnotation();

        setupMockJoinPoint(args, parameters);
        when(maskingService.applyMaskingStrategy(anyString(), any(MaskingType.class)))
                .thenReturn(maskedValue);
        when(joinPoint.proceed(any())).thenReturn("success");

        // when
        Object result = aspect.maskSensitiveParameters(joinPoint);

        // then
        assertThat(result).isEqualTo("success");
        verify(maskingService).applyMaskingStrategy(originalValue, MaskingType.PASSWORD);
        verify(joinPoint).proceed(new Object[]{maskedValue});
    }

    @Test
    @DisplayName("@Masked 애노테이션이 적용된 객체 파라미터 마스킹")
    void maskSensitiveParametersWithObject() throws Throwable {
        // given
        TestObject originalObject = new TestObject();
        originalObject.value = "testValue";
        Object[] args = {originalObject};
        Parameter[] parameters = createMockParametersWithMaskedAnnotationForObject();

        setupMockJoinPoint(args, parameters);
        when(joinPoint.proceed(any())).thenReturn("success");

        // when
        Object result = aspect.maskSensitiveParameters(joinPoint);

        // then
        assertThat(result).isEqualTo("success");
        verify(maskingService).mask(originalObject);
        verify(joinPoint).proceed(new Object[]{originalObject});
    }

    @Test
    @DisplayName("@Masked 애노테이션이 없는 파라미터는 마스킹하지 않음")
    void maskSensitiveParametersWithoutMaskedAnnotation() throws Throwable {
        // given
        String normalValue = "normalValue";
        Object[] args = {normalValue};
        Parameter[] parameters = createMockParametersWithoutMaskedAnnotation();

        setupMockJoinPoint(args, parameters);
        when(joinPoint.proceed(any())).thenReturn("success");

        // when
        Object result = aspect.maskSensitiveParameters(joinPoint);

        // then
        assertThat(result).isEqualTo("success");
        verify(maskingService, never()).applyMaskingStrategy(anyString(), any(MaskingType.class));
        verify(maskingService, never()).mask(any());
        verify(joinPoint).proceed(new Object[]{normalValue});
    }

    @Test
    @DisplayName("null 파라미터는 마스킹하지 않음")
    void maskSensitiveParametersWithNull() throws Throwable {
        // given
        Object[] args = {null};
        Parameter[] parameters = createMockParametersWithMaskedAnnotationForObject();

        setupMockJoinPoint(args, parameters);
        when(joinPoint.proceed(any())).thenReturn("success");

        // when
        Object result = aspect.maskSensitiveParameters(joinPoint);

        // then
        assertThat(result).isEqualTo("success");
        verify(maskingService, never()).applyMaskingStrategy(anyString(), any(MaskingType.class));
        verify(maskingService, never()).mask(any());
        verify(joinPoint).proceed(new Object[]{null});
    }

    @Test
    @DisplayName("여러 파라미터 중 일부만 @Masked 애노테이션 적용")
    void maskSensitiveParametersWithMixedParameters() throws Throwable {
        // given
        String password = "secretPassword";
        String normalValue = "normalValue";
        String maskedPassword = "********";
        Object[] args = {password, normalValue};
        Parameter[] parameters = createMockParametersWithMixedAnnotations();

        setupMockJoinPoint(args, parameters);
        when(maskingService.applyMaskingStrategy(anyString(), any(MaskingType.class)))
                .thenReturn(maskedPassword);
        when(joinPoint.proceed(any())).thenReturn("success");

        // when
        Object result = aspect.maskSensitiveParameters(joinPoint);

        // then
        assertThat(result).isEqualTo("success");
        verify(maskingService).applyMaskingStrategy(password, MaskingType.PASSWORD);
        verify(joinPoint).proceed(new Object[]{maskedPassword, normalValue});
    }

    @Test
    @DisplayName("서비스 메서드 결과 객체 마스킹 - 비활성화됨 (원본 객체 보호)")
    void maskServiceMethodLogs() throws Throwable {
        // given
        TestObject resultObject = new TestObject();
        resultObject.value = "testValue";
        when(joinPoint.proceed()).thenReturn(resultObject);

        // when
        Object result = aspect.maskServiceMethodLogs(joinPoint);

        // then
        // maskServiceMethodLogs는 비활성화되어 원본 객체를 변경하지 않음
        assertThat(result).isEqualTo(resultObject);
        assertThat(resultObject.value).isEqualTo("testValue"); // 원본 값이 유지됨
        verify(maskingService, never()).mask(any()); // 마스킹이 호출되지 않음
    }

    @Test
    @DisplayName("서비스 메서드 null 결과 처리 - 비활성화됨")
    void maskServiceMethodLogsWithNullResult() throws Throwable {
        // given
        when(joinPoint.proceed()).thenReturn(null);

        // when
        Object result = aspect.maskServiceMethodLogs(joinPoint);

        // then
        // maskServiceMethodLogs는 비활성화되어 null도 그대로 반환
        assertThat(result).isNull();
        verify(maskingService, never()).mask(any());
    }

    @Test
    @DisplayName("서비스 메서드 예외 발생 시 처리 - 비활성화됨")
    void maskServiceMethodLogsWithException() throws Throwable {
        // given
        RuntimeException exception = new RuntimeException("Test exception");
        when(joinPoint.proceed()).thenThrow(exception);

        // when & then
        // maskServiceMethodLogs는 비활성화되어 예외를 그대로 전파
        try {
            aspect.maskServiceMethodLogs(joinPoint);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }
        
        verify(maskingService, never()).mask(any());
    }

    private void setupMockJoinPoint(Object[] args, Parameter[] parameters) throws Throwable {
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(method.getParameters()).thenReturn(parameters);
    }

    private Parameter[] createMockParametersWithMaskedAnnotation() {
        Parameter parameter = mock(Parameter.class);
        Masked maskedAnnotation = mock(Masked.class);
        
        when(parameter.isAnnotationPresent(Masked.class)).thenReturn(true);
        when(parameter.getAnnotation(Masked.class)).thenReturn(maskedAnnotation);
        when(maskedAnnotation.type()).thenReturn(MaskingType.PASSWORD);
        
        return new Parameter[]{parameter};
    }

    private Parameter[] createMockParametersWithMaskedAnnotationForObject() {
        Parameter parameter = mock(Parameter.class);
        Masked maskedAnnotation = mock(Masked.class);
        
        when(parameter.isAnnotationPresent(Masked.class)).thenReturn(true);
        when(parameter.getAnnotation(Masked.class)).thenReturn(maskedAnnotation);
        // 객체 타입에서는 type()이 호출되지 않으므로 stubbing하지 않음
        
        return new Parameter[]{parameter};
    }

    private Parameter[] createMockParametersWithoutMaskedAnnotation() {
        Parameter parameter = mock(Parameter.class);
        
        when(parameter.isAnnotationPresent(Masked.class)).thenReturn(false);
        
        return new Parameter[]{parameter};
    }

    private Parameter[] createMockParametersWithMixedAnnotations() {
        Parameter passwordParam = mock(Parameter.class);
        Parameter normalParam = mock(Parameter.class);
        Masked maskedAnnotation = mock(Masked.class);
        
        when(passwordParam.isAnnotationPresent(Masked.class)).thenReturn(true);
        when(passwordParam.getAnnotation(Masked.class)).thenReturn(maskedAnnotation);
        when(maskedAnnotation.type()).thenReturn(MaskingType.PASSWORD);
        
        when(normalParam.isAnnotationPresent(Masked.class)).thenReturn(false);
        
        return new Parameter[]{passwordParam, normalParam};
    }

    /**
     * 테스트용 클래스
     */
    @SuppressWarnings("unused")
    private static class TestObject {
        public String value;
    }
}
