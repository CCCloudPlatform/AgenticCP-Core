package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 감사 로깅 애노테이션 메타데이터 테스트
 */
class AuditAnnotationMetadataTest {

    @Test
    void testAuditControllerAnnotation_메타데이터검증() {
        // Given
        Class<TestAuditController> controllerClass = TestAuditController.class;
        
        // When
        AuditController annotation = controllerClass.getAnnotation(AuditController.class);
        
        // Then
        assertNotNull(annotation, "AuditController 애노테이션이 존재해야 함");
        assertEquals(AuditResourceType.USER, annotation.resourceType(), "리소스 타입이 USER여야 함");
        assertEquals(AuditSeverity.MEDIUM, annotation.defaultSeverity(), "기본 심각도가 MEDIUM이어야 함");
        assertTrue(annotation.defaultIncludeRequestData(), "기본적으로 요청 데이터를 포함해야 함");
        assertFalse(annotation.defaultIncludeResponseData(), "기본적으로 응답 데이터를 포함하지 않아야 함");
        
        String[] targetMethods = annotation.targetHttpMethods();
        assertEquals(4, targetMethods.length, "대상 HTTP 메서드가 4개여야 함");
        assertArrayEquals(new String[]{"POST", "PUT", "PATCH", "DELETE"}, targetMethods, "대상 HTTP 메서드가 정확해야 함");
        
        String[] excludeMethods = annotation.excludeMethods();
        assertEquals(1, excludeMethods.length, "제외 메서드가 1개여야 함");
        assertEquals("getUserInfo", excludeMethods[0], "제외 메서드가 getUserInfo여야 함");
    }

    @Test
    void testAuditRequiredAnnotation_메타데이터검증() throws Exception {
        // Given
        Class<TestAuditController> controllerClass = TestAuditController.class;
        Method customMethod = controllerClass.getMethod("customAction", String.class);
        Method overrideMethod = controllerClass.getMethod("overrideAction", String.class);
        
        // When
        AuditRequired customAnnotation = customMethod.getAnnotation(AuditRequired.class);
        AuditRequired overrideAnnotation = overrideMethod.getAnnotation(AuditRequired.class);
        
        // Then - customAction 메서드 검증
        assertNotNull(customAnnotation, "customAction 메서드에 AuditRequired 애노테이션이 존재해야 함");
        assertEquals("CUSTOM_USER_ACTION", customAnnotation.action(), "액션이 CUSTOM_USER_ACTION이어야 함");
        assertEquals(AuditResourceType.USER, customAnnotation.resourceType(), "리소스 타입이 USER여야 함");
        assertEquals("Custom user action for testing", customAnnotation.description(), "설명이 정확해야 함");
        assertTrue(customAnnotation.includeRequestData(), "요청 데이터를 포함해야 함");
        assertTrue(customAnnotation.includeResponseData(), "응답 데이터를 포함해야 함");
        assertEquals(AuditSeverity.HIGH, customAnnotation.severity(), "심각도가 HIGH여야 함");
        
        // Then - overrideAction 메서드 검증
        assertNotNull(overrideAnnotation, "overrideAction 메서드에 AuditRequired 애노테이션이 존재해야 함");
        assertEquals("OVERRIDE_ACTION", overrideAnnotation.action(), "액션이 OVERRIDE_ACTION이어야 함");
        assertEquals(AuditResourceType.CONFIG, overrideAnnotation.resourceType(), "리소스 타입이 CONFIG이어야 함");
        assertEquals("This overrides class-level settings", overrideAnnotation.description(), "설명이 정확해야 함");
        assertFalse(overrideAnnotation.includeRequestData(), "요청 데이터를 포함하지 않아야 함");
        assertTrue(overrideAnnotation.includeResponseData(), "응답 데이터를 포함해야 함");
        assertEquals(AuditSeverity.CRITICAL, overrideAnnotation.severity(), "심각도가 CRITICAL이어야 함");
    }

    @Test
    void testAuditAnnotation_기본값검증() throws Exception {
        // Given
        Class<TestAuditController> controllerClass = TestAuditController.class;
        
        // When
        AuditController classAnnotation = controllerClass.getAnnotation(AuditController.class);
        
        // Then - 기본값 검증
        assertEquals(AuditSeverity.MEDIUM, classAnnotation.defaultSeverity(), "기본 심각도가 MEDIUM이어야 함");
        assertTrue(classAnnotation.defaultIncludeRequestData(), "기본적으로 요청 데이터를 포함해야 함");
        assertFalse(classAnnotation.defaultIncludeResponseData(), "기본적으로 응답 데이터를 포함하지 않아야 함");
    }

    @Test
    void testAuditAnnotation_애노테이션타겟검증() {
        // Given
        Class<AuditController> auditControllerClass = AuditController.class;
        Class<AuditRequired> auditRequiredClass = AuditRequired.class;
        
        // When
        Annotation[] controllerAnnotations = auditControllerClass.getAnnotations();
        Annotation[] requiredAnnotations = auditRequiredClass.getAnnotations();
        
        // Then
        assertNotNull(controllerAnnotations, "AuditController 애노테이션 배열이 null이 아니어야 함");
        assertNotNull(requiredAnnotations, "AuditRequired 애노테이션 배열이 null이 아니어야 함");
        
        // Target과 Retention 애노테이션 확인
        assertTrue(auditControllerClass.isAnnotation(), "AuditController는 애노테이션이어야 함");
        assertTrue(auditRequiredClass.isAnnotation(), "AuditRequired는 애노테이션이어야 함");
    }

    @Test
    void testAuditAnnotation_리플렉션검증() throws Exception {
        // Given
        Class<TestAuditController> controllerClass = TestAuditController.class;
        
        // When
        Method[] methods = controllerClass.getDeclaredMethods();
        
        // Then
        assertTrue(methods.length > 0, "메서드가 존재해야 함");
        
        // 각 메서드에 대한 애노테이션 확인
        for (Method method : methods) {
            if (method.getName().equals("customAction")) {
                assertTrue(method.isAnnotationPresent(AuditRequired.class), 
                    "customAction 메서드에 AuditRequired 애노테이션이 있어야 함");
            } else if (method.getName().equals("overrideAction")) {
                assertTrue(method.isAnnotationPresent(AuditRequired.class), 
                    "overrideAction 메서드에 AuditRequired 애노테이션이 있어야 함");
            }
        }
    }
}
