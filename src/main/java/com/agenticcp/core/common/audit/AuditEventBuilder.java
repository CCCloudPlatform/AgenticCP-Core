package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.AuditContextDto;
import com.agenticcp.core.common.dto.AuditEventDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 감사 이벤트 빌더
 * 
 * AuditEvent 객체를 생성하기 위한 빌더 클래스입니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
public class AuditEventBuilder {
    
    private String action;
    private AuditResourceType resourceType;
    private String httpMethod;
    private String requestPath;
    private String operationSummary;
    private String controllerName;
    private String methodName;
    private AuditSeverity severity;
    private final Instant timestamp;
    private String requestId;
    private String tenantId;
    private String userId;
    private String clientIp;
    private boolean success;
    private String error;
    private Map<String, Object> requestData;
    private Map<String, Object> responseData;
    private final Map<String, Object> metadata;
    
    private AuditEventBuilder(AuditContextDto context) {
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
        
        if (context != null) {
            this.action = context.action();
            this.resourceType = context.resourceType();
            this.httpMethod = context.httpMethod();
            this.requestPath = context.requestPath();
            this.operationSummary = context.operationSummary();
            this.controllerName = context.controllerName();
            this.methodName = context.methodName();
            this.severity = context.severity();
            this.requestId = context.requestId();
            this.tenantId = context.tenantId();
            this.userId = context.userId();
            this.clientIp = context.clientIp();
        }
    }

    public static AuditEventBuilder builder(AuditContextDto context) {
        return new AuditEventBuilder(context);
    }
    
    
    public AuditEventBuilder success(boolean success) {
        this.success = success;
        return this;
    }
    
    public AuditEventBuilder error(String error) {
        this.error = error;
        return this;
    }

    public AuditEventBuilder requestData(Map<String, Object> requestData) {
        this.requestData = requestData;
        return this;
    }

    public AuditEventBuilder responseData(Map<String, Object> responseData) {
        this.responseData = responseData;
        return this;
    }
    
    
    public AuditEventDto build() {
        return new AuditEventDto(
                action,
                resourceType,
                httpMethod,
                requestPath,
                operationSummary,
                controllerName,
                methodName,
                severity,
                timestamp,
                requestId,
                tenantId,
                userId,
                clientIp,
                success,
                error,
                requestData,
                responseData,
                metadata
        );
    }
}
