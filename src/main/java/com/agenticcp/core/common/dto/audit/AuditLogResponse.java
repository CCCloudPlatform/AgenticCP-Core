package com.agenticcp.core.common.dto.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

/**
 * 감사 로그 조회 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public record AuditLogResponse(
    Long id,
    String action,
    AuditResourceType resourceType,
    String httpMethod,
    String requestPath,
    String operationSummary,
    AuditSeverity severity,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "KST")
    Instant timestamp,
    String requestId,
    String tenantId,
    String userId,
    String clientIp,
    Boolean success,
    String error,
    String targetResourceId,
    Map<String, Object> requestData,
    Map<String, Object> responseData,
    Map<String, Object> metadata,
    Map<String, Object> oldValue,
    Map<String, Object> newValue
) {}
