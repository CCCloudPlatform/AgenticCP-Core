package com.agenticcp.core.common.entity;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 감사 로그 엔티티입니다. 감사 이벤트를 데이터베이스에 영구 저장합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_logs_tenant", columnList = "tenant_id"),
    @Index(name = "idx_audit_logs_user", columnList = "user_id"),
    @Index(name = "idx_audit_logs_action", columnList = "action"),
    @Index(name = "idx_audit_logs_resource_type", columnList = "resource_type"),
    @Index(name = "idx_audit_logs_severity", columnList = "severity"),
    @Index(name = "idx_audit_logs_success", columnList = "success")
})
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditLog extends BaseEntity {

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private AuditResourceType resourceType;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    @Column(name = "operation_summary", length = 200)
    private String operationSummary;

    @Column(name = "controller_name", length = 100)
    private String controllerName;

    @Column(name = "method_name", length = 100)
    private String methodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AuditSeverity severity;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "request_data", columnDefinition = "JSON")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "JSON")
    private String responseData;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    // 값 변경 추적 필드
    @Column(name = "old_value", columnDefinition = "JSON")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "JSON")
    private String newValue;

    @Column(name = "target_resource_id", length = 100)
    private String targetResourceId;
}
