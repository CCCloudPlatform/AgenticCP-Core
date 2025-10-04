package com.agenticcp.core.common.dto;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import lombok.Builder;

/**
 * 감사 컨텍스트 정보
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder(toBuilder = true)
public record AuditContextDto(
    String requestId,
    String tenantId,
    String clientIp,
    String userId,
    
    String action,
    AuditResourceType resourceType,
    String httpMethod,
    String requestPath,
    String operationSummary,
    String controllerName,
    String methodName,
    AuditSeverity severity,
    boolean includeRequestData,
    boolean includeResponseData
) {}
