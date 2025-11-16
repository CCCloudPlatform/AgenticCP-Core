package com.agenticcp.core.common.dto.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

/**
 * 단일 감사 이벤트를 표현하는 DTO입니다.
 * 요청/응답 요약 및 메타데이터를 포함합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
public record AuditEventDto(
    String action,
    AuditResourceType resourceType,
    String httpMethod,
    String requestPath,
    String operationSummary,
    String controllerName,
    String methodName,
    AuditSeverity severity,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "KST")
    Instant timestamp,
    String requestId,
    String tenantId,
    String userId,
    String clientIp,
    boolean success,
    String error,
    Map<String, Object> requestData,
    Map<String, Object> responseData,
    Map<String, Object> metadata,
    
    Map<String, Object> oldValue,
    Map<String, Object> newValue,
    String targetResourceId
) {}
