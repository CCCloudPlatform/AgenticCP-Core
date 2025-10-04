package com.agenticcp.core.common.util;

import com.agenticcp.core.common.audit.AuditController;
import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.AuditContextDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 감사 로깅에 필요한 메타데이터 정보를 추출하는 유틸리티 클래스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
public final class AuditInfoExtractor {

    private record MethodInfo(Method method, Operation operation) {}

    private static final Map<Class<? extends Annotation>, String> HTTP_METHOD_MAP = Map.of(
        GetMapping.class, "GET",
        PostMapping.class, "POST",
        PutMapping.class, "PUT",
        PatchMapping.class, "PATCH",
        DeleteMapping.class, "DELETE"
    );

    private static final Map<Class<? extends Annotation>, PathExtractor> PATH_EXTRACTOR_MAP = Map.of(
        GetMapping.class, method -> method.getAnnotation(GetMapping.class).value(),
        PostMapping.class, method -> method.getAnnotation(PostMapping.class).value(),
        PutMapping.class, method -> method.getAnnotation(PutMapping.class).value(),
        PatchMapping.class, method -> method.getAnnotation(PatchMapping.class).value(),
        DeleteMapping.class, method -> method.getAnnotation(DeleteMapping.class).value()
    );

    @FunctionalInterface
    private interface PathExtractor {
        String[] extract(Method method);
    }

    private AuditInfoExtractor() {
    }

    private static MethodInfo extractMethodInfo(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Operation operation = method.getAnnotation(Operation.class);
        return new MethodInfo(method, operation);
    }

    public static AuditContextDto extractAuditInfo(ProceedingJoinPoint joinPoint, AuditController classAudit, AuditRequired methodAudit) {
        MethodInfo methodInfo = extractMethodInfo(joinPoint);

        if (methodAudit != null) {
            return buildFromMethodAnnotation(methodInfo.method, methodInfo.operation, methodAudit);
        }
        return buildFromClassAnnotation(methodInfo.method, methodInfo.operation, classAudit);
    }


    private static AuditContextDto.AuditContextDtoBuilder buildCommonInfo(Method method, Operation operation, String fallbackDescription) {
        String description = operation != null ? operation.summary() : fallbackDescription;
        String requestPath = extractRequestPath(method);
        String httpMethod = extractHttpMethod(method);

        return AuditContextDto.builder()
            .requestId(null)
            .tenantId(null)
            .clientIp(null)
            .userId(null)
            .httpMethod(httpMethod)
            .requestPath(requestPath)
            .operationSummary(description)
            .controllerName(method.getDeclaringClass().getSimpleName())
            .methodName(method.getName());
    }

    private static AuditContextDto buildFromMethodAnnotation(Method method, Operation operation, AuditRequired methodAudit) {
        return buildCommonInfo(method, operation, methodAudit.description())
            .action(methodAudit.action())
            .resourceType(methodAudit.resourceType())
            .severity(methodAudit.severity())
            .includeRequestData(methodAudit.includeRequestData())
            .includeResponseData(methodAudit.includeResponseData())
            .build();
    }

    private static AuditContextDto buildFromClassAnnotation(Method method, Operation operation, AuditController classAudit) {
        String action = AuditActionGenerator.generateActionName(method.getName());
        return buildCommonInfo(method, operation, method.getName())
            .action(action)
            .resourceType(classAudit.resourceType())
            .severity(classAudit.defaultSeverity())
            .includeRequestData(classAudit.defaultIncludeRequestData())
            .includeResponseData(classAudit.defaultIncludeResponseData())
            .build();
    }

    public static Map<String, Object> extractRequestData(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        
        Map<String, Object> requestData = new HashMap<>();
        
        if (args == null || args.length == 0) {
            return requestData;
        }
        
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        
        for (int i = 0; i < args.length && i < parameters.length; i++) {
            Object arg = args[i];
            java.lang.reflect.Parameter parameter = parameters[i];
            
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                requestData.put("requestBody", arg);
            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                String paramName = requestParam.value().isEmpty() ? parameter.getName() : requestParam.value();
                requestData.put("requestParam_" + paramName, arg);
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                String varName = pathVariable.value().isEmpty() ? parameter.getName() : pathVariable.value();
                requestData.put("pathVariable_" + varName, arg);
            } else {
                requestData.put(parameter.getName(), arg);
            }
        }
        
        return requestData;
    }

    public static String extractHttpMethod(Method method) {
        for (var entry : HTTP_METHOD_MAP.entrySet()) {
            if (method.isAnnotationPresent(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            RequestMethod[] methods = requestMapping.method();
            if (methods.length > 0) {
                return methods[0].name();
            }
        }
        
        return "UNKNOWN";
    }

    public static String extractRequestPath(Method method) {
        StringBuilder path = new StringBuilder();
        
        RequestMapping classMapping = method.getDeclaringClass().getAnnotation(RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            path.append(classMapping.value()[0]);
        }
        
        for (var entry : PATH_EXTRACTOR_MAP.entrySet()) {
            if (method.isAnnotationPresent(entry.getKey())) {
                String[] values = entry.getValue().extract(method);
                if (values.length > 0) {
                    path.append(values[0]);
                }
                return path.toString();
            }
        }
        
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.value().length > 0) {
                path.append(mapping.value()[0]);
            }
        }
        return path.toString();
    }

    public static boolean isExcludedMethod(String methodName, String[] excludeMethods) {
        return Arrays.asList(excludeMethods).contains(methodName);
    }

    public static boolean isTargetHttpMethod(String httpMethod, String[] targetMethods) {
        return Arrays.asList(targetMethods).contains(httpMethod);
    }
}
