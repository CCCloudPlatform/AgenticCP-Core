package com.agenticcp.core.common.util;

import com.agenticcp.core.common.audit.AuditController;
import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.AuditContextDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import io.swagger.v3.oas.annotations.Operation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * AuditInfoExtractor 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("감사 정보 추출기 테스트")
class AuditInfoExtractorTest {

    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private MethodSignature methodSignature;

    // 테스트용 컨트롤러 클래스
    @RestController
    @RequestMapping("/api/users")
    @AuditController(resourceType = AuditResourceType.USER, defaultSeverity = AuditSeverity.MEDIUM)
    static class TestController {
        
        @GetMapping("/{id}")
        @Operation(summary = "사용자 조회")
        public String getUser(@PathVariable Long id) {
            return "user";
        }
        
        @PostMapping
        @Operation(summary = "사용자 생성")
        @AuditRequired(
            action = "CREATE_USER",
            resourceType = AuditResourceType.USER,
            severity = AuditSeverity.HIGH,
            includeRequestData = true
        )
        public String createUser(@RequestBody String userData) {
            return "created";
        }
        
        @PutMapping("/{id}")
        public String updateUser(@PathVariable Long id, @RequestBody String userData) {
            return "updated";
        }
        
        @DeleteMapping("/{id}")
        public String deleteUser(@PathVariable Long id) {
            return "deleted";
        }
        
        @RequestMapping(value = "/custom", method = RequestMethod.PATCH)
        public String customMethod() {
            return "custom";
        }
        
        public String nonAnnotatedMethod() {
            return "non-annotated";
        }
    }

    @Test
    @DisplayName("GetMapping 애노테이션에서 HTTP 메서드를 정상 추출한다")
    void extractHttpMethod_shouldExtractGet_whenGetMappingAnnotation() throws Exception {
        // given
        Method method = TestController.class.getMethod("getUser", Long.class);
        
        // when
        String result = AuditInfoExtractor.extractHttpMethod(method);
        
        // then
        assertThat(result).isEqualTo("GET");
    }

    @Test
    @DisplayName("PostMapping 애노테이션에서 HTTP 메서드를 정상 추출한다")
    void extractHttpMethod_shouldExtractPost_whenPostMappingAnnotation() throws Exception {
        // given
        Method method = TestController.class.getMethod("createUser", String.class);
        
        // when
        String result = AuditInfoExtractor.extractHttpMethod(method);
        
        // then
        assertThat(result).isEqualTo("POST");
    }

    @Test
    @DisplayName("RequestMapping 애노테이션에서 HTTP 메서드를 정상 추출한다")
    void extractHttpMethod_shouldExtractPatch_whenRequestMappingAnnotation() throws Exception {
        // given
        Method method = TestController.class.getMethod("customMethod");
        
        // when
        String result = AuditInfoExtractor.extractHttpMethod(method);
        
        // then
        assertThat(result).isEqualTo("PATCH");
    }

    @Test
    @DisplayName("애노테이션이 없는 메서드는 UNKNOWN을 반환한다")
    void extractHttpMethod_shouldReturnUnknown_whenNoAnnotation() throws Exception {
        // given
        Method method = TestController.class.getMethod("nonAnnotatedMethod");
        
        // when
        String result = AuditInfoExtractor.extractHttpMethod(method);
        
        // then
        assertThat(result).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("클래스와 메서드 경로를 조합하여 요청 경로를 추출한다")
    void extractRequestPath_shouldCombineClassAndMethodPath_whenBothExist() throws Exception {
        // given
        Method method = TestController.class.getMethod("getUser", Long.class);
        
        // when
        String result = AuditInfoExtractor.extractRequestPath(method);
        
        // then
        assertThat(result).isEqualTo("/api/users/{id}");
    }

    @Test
    @DisplayName("클래스 경로만 있는 경우 요청 경로를 추출한다")
    void extractRequestPath_shouldExtractClassPath_whenOnlyClassPathExists() throws Exception {
        // given
        Method method = TestController.class.getMethod("createUser", String.class);
        
        // when
        String result = AuditInfoExtractor.extractRequestPath(method);
        
        // then
        assertThat(result).isEqualTo("/api/users");
    }

    @Test
    @DisplayName("RequestBody 파라미터를 정상적으로 추출한다")
    void extractRequestData_shouldExtractRequestBody_whenRequestBodyParameter() throws Exception {
        // given
        Method method = TestController.class.getMethod("createUser", String.class);
        Object[] args = {"userData"};
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        
        // when
        Map<String, Object> result = AuditInfoExtractor.extractRequestData(joinPoint);
        
        // then
        assertThat(result).containsEntry("requestBody", "userData");
    }

    @Test
    @DisplayName("PathVariable 파라미터를 정상적으로 추출한다")
    void extractRequestData_shouldExtractPathVariable_whenPathVariableParameter() throws Exception {
        // given
        Method method = TestController.class.getMethod("getUser", Long.class);
        Object[] args = {123L};
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        
        // when
        Map<String, Object> result = AuditInfoExtractor.extractRequestData(joinPoint);
        
        // then
        assertThat(result).containsEntry("pathVariable_id", 123L);
    }

    @Test
    @DisplayName("파라미터가 없는 메서드는 빈 맵을 반환한다")
    void extractRequestData_shouldReturnEmptyMap_whenNoParameters() throws Exception {
        // given
        Method method = TestController.class.getMethod("nonAnnotatedMethod");
        Object[] args = {};
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        
        // when
        Map<String, Object> result = AuditInfoExtractor.extractRequestData(joinPoint);
        
        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("args가 null인 경우 빈 맵을 반환한다")
    void extractRequestData_shouldReturnEmptyMap_whenArgsIsNull() throws Exception {
        // given
        Method method = TestController.class.getMethod("getUser", Long.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(null);
        
        // when
        Map<String, Object> result = AuditInfoExtractor.extractRequestData(joinPoint);
        
        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("제외 메서드 목록에 포함된 경우 true를 반환한다")
    void isExcludedMethod_shouldReturnTrue_whenMethodInExcludeList() {
        // given
        String methodName = "getUser";
        String[] excludeMethods = {"getUser", "deleteUser"};
        
        // when
        boolean result = AuditInfoExtractor.isExcludedMethod(methodName, excludeMethods);
        
        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("제외 메서드 목록에 포함되지 않은 경우 false를 반환한다")
    void isExcludedMethod_shouldReturnFalse_whenMethodNotInExcludeList() {
        // given
        String methodName = "updateUser";
        String[] excludeMethods = {"getUser", "deleteUser"};
        
        // when
        boolean result = AuditInfoExtractor.isExcludedMethod(methodName, excludeMethods);
        
        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("대상 HTTP 메서드 목록에 포함된 경우 true를 반환한다")
    void isTargetHttpMethod_shouldReturnTrue_whenMethodInTargetList() {
        // given
        String httpMethod = "POST";
        String[] targetMethods = {"POST", "PUT", "DELETE"};
        
        // when
        boolean result = AuditInfoExtractor.isTargetHttpMethod(httpMethod, targetMethods);
        
        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("대상 HTTP 메서드 목록에 포함되지 않은 경우 false를 반환한다")
    void isTargetHttpMethod_shouldReturnFalse_whenMethodNotInTargetList() {
        // given
        String httpMethod = "GET";
        String[] targetMethods = {"POST", "PUT", "DELETE"};
        
        // when
        boolean result = AuditInfoExtractor.isTargetHttpMethod(httpMethod, targetMethods);
        
        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("메서드 애노테이션을 우선적으로 사용한다")
    void extractAuditInfo_shouldUseMethodAnnotation_whenMethodAnnotationExists() throws Exception {
        // given
        Method method = TestController.class.getMethod("createUser", String.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        
        AuditController classAudit = TestController.class.getAnnotation(AuditController.class);
        AuditRequired methodAudit = method.getAnnotation(AuditRequired.class);
        
        // when
        AuditContextDto result = AuditInfoExtractor.extractAuditInfo(joinPoint, classAudit, methodAudit);
        
        // then
        assertThat(result.action()).isEqualTo("CREATE_USER");
        assertThat(result.resourceType()).isEqualTo(AuditResourceType.USER);
        assertThat(result.severity()).isEqualTo(AuditSeverity.HIGH);
        assertThat(result.includeRequestData()).isTrue();
        assertThat(result.httpMethod()).isEqualTo("POST");
        assertThat(result.requestPath()).isEqualTo("/api/users");
        assertThat(result.operationSummary()).isEqualTo("사용자 생성");
        assertThat(result.controllerName()).isEqualTo("TestController");
        assertThat(result.methodName()).isEqualTo("createUser");
    }

    @Test
    @DisplayName("클래스 애노테이션을 사용한다")
    void extractAuditInfo_shouldUseClassAnnotation_whenNoMethodAnnotation() throws Exception {
        // given
        Method method = TestController.class.getMethod("getUser", Long.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        
        AuditController classAudit = TestController.class.getAnnotation(AuditController.class);
        
        // when
        AuditContextDto result = AuditInfoExtractor.extractAuditInfo(joinPoint, classAudit, null);
        
        // then
        assertThat(result.action()).isEqualTo("GET_USER");
        assertThat(result.resourceType()).isEqualTo(AuditResourceType.USER);
        assertThat(result.severity()).isEqualTo(AuditSeverity.MEDIUM);
        assertThat(result.includeRequestData()).isFalse();
        assertThat(result.httpMethod()).isEqualTo("GET");
        assertThat(result.requestPath()).isEqualTo("/api/users/{id}");
        assertThat(result.operationSummary()).isEqualTo("사용자 조회");
        assertThat(result.controllerName()).isEqualTo("TestController");
        assertThat(result.methodName()).isEqualTo("getUser");
    }
}
