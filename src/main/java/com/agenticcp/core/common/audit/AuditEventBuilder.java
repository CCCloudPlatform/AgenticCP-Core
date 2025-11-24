package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditContextDto;
import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.enums.AuditErrorCode;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.exception.BusinessException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 감사 이벤트 DTO를 생성하는 빌더입니다.
 * 컨텍스트를 기반으로 필드를 채워 {@link com.agenticcp.core.common.dto.audit.AuditEventDto}를 만듭니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
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
    
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private String targetResourceId;
    
    private AuditEventBuilder(AuditContextDto context) {
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
        this.requestData = new HashMap<>();
        this.responseData = new HashMap<>();
        this.oldValue = new HashMap<>();
        this.newValue = new HashMap<>();
        
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
        
        // requestPath가 null이거나 blank인 경우 기본값 설정 (나중에 덮어쓸 수 있음)
        if (this.requestPath == null || this.requestPath.isBlank()) {
            this.requestPath = "/api/unknown";
        }
    }

    /**
     * 감사 컨텍스트 기반 빌더를 생성합니다.
     *
     * @param context 감사 컨텍스트 정보 (null 가능)
     * @return 빌더 인스턴스
     */
    public static AuditEventBuilder builder(AuditContextDto context) {
        return new AuditEventBuilder(context);
    }
    
    
    /**
     * 감사 이벤트 성공 여부를 지정합니다.
     *
     * @param success 성공 여부
     * @return 빌더
     */
    public AuditEventBuilder success(boolean success) {
        this.success = success;
        return this;
    }
    
    /**
     * 감사 이벤트 에러 메시지를 설정합니다.
     *
     * @param error 에러 메시지
     * @return 빌더
     */
    public AuditEventBuilder error(String error) {
        this.error = error;
        return this;
    }

    /**
     * 요청 데이터를 설정합니다.
     *
     * @param requestData 요청 데이터 맵
     * @return 빌더
     */
    public AuditEventBuilder requestData(Map<String, Object> requestData) {
        this.requestData = requestData;
        return this;
    }

    /**
     * 응답 데이터를 설정합니다.
     *
     * @param responseData 응답 데이터 맵
     * @return 빌더
     */
    public AuditEventBuilder responseData(Map<String, Object> responseData) {
        this.responseData = responseData;
        return this;
    }
    
    /**
     * 변경 전 값을 설정합니다.
     *
     * @param oldValue 변경 전 값
     * @return 빌더
     */
    public AuditEventBuilder oldValue(Map<String, Object> oldValue) {
        this.oldValue = oldValue;
        return this;
    }
    
    /**
     * 변경 후 값을 설정합니다.
     *
     * @param newValue 변경 후 값
     * @return 빌더
     */
    public AuditEventBuilder newValue(Map<String, Object> newValue) {
        this.newValue = newValue;
        return this;
    }
    
    /**
     * 타깃 리소스 ID를 설정합니다.
     *
     * @param targetResourceId 리소스 식별자
     * @return 빌더
     */
    public AuditEventBuilder targetResourceId(String targetResourceId) {
        this.targetResourceId = targetResourceId;
        return this;
    }
    
    /**
     * 요청 경로를 설정합니다.
     *
     * @param requestPath 요청 경로
     * @return 빌더
     */
    public AuditEventBuilder requestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }
    
    /**
     * HTTP 메서드를 설정합니다.
     *
     * @param httpMethod HTTP 메서드
     * @return 빌더
     */
    public AuditEventBuilder httpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }
    
    /**
     * 감사 이벤트 DTO를 생성합니다.
     * 필수 필드 누락 시 {@link BusinessException}을 발생시킵니다.
     *
     * @return {@link AuditEventDto}
     */
    public AuditEventDto build() {
        validateRequiredFields();
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
                metadata,
                oldValue,
                newValue,
                targetResourceId
        );
    }

    private void validateRequiredFields() {
        var missingFields = new ArrayList<String>();
        if (action == null || action.isBlank()) {
            missingFields.add("action");
        }
        if (resourceType == null) {
            missingFields.add("resourceType");
        }
        if (requestPath == null || requestPath.isBlank()) {
            missingFields.add("requestPath");
        }
        if (!missingFields.isEmpty()) {
            throw new BusinessException(
                    AuditErrorCode.AUDIT_LOG_CONVERSION_FAILED,
                    "필수 감사 메타데이터 누락: " + String.join(", ", missingFields)
            );
        }
    }
}
