package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;

/**
 * 감사 로깅에 필요한 메타데이터 정보를 담는 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public record AuditInfo(
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
