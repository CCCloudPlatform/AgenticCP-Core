package com.agenticcp.core.common.dto;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

/**
 * 감사 이벤트 데이터 모델
 * @author AgenticCP Team
 * @version 1.0.0
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
    Map<String, Object> metadata
) {}
