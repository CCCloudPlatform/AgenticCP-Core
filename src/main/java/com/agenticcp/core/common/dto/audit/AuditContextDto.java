package com.agenticcp.core.common.dto.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import lombok.Builder;

/**
 * 감사(감사로그) 기록 시 함께 저장되는 요청/동작 컨텍스트 정보 DTO입니다.
 * {@link com.agenticcp.core.common.audit.AuditAspect}와 {@link com.agenticcp.core.common.audit.AuditService} 간 전달에 사용됩니다.
 *
 * @author AgenticCP Team
 * @since 2025-10-01
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
