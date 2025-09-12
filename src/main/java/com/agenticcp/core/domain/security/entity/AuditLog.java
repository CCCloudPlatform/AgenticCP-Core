package com.agenticcp.core.domain.security.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_category")
    private EventCategory eventCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity = Severity.INFO;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "result")
    @Enumerated(EnumType.STRING)
    private Result result = Result.SUCCESS;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON for additional event details

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array of event tags

    @Column(name = "is_retained")
    private Boolean isRetained = true;

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;

    public enum EventType {
        AUTHENTICATION,
        AUTHORIZATION,
        DATA_ACCESS,
        DATA_MODIFICATION,
        CONFIGURATION_CHANGE,
        SYSTEM_EVENT,
        SECURITY_EVENT,
        COMPLIANCE_EVENT,
        ADMINISTRATIVE_ACTION,
        USER_ACTION,
        API_CALL,
        DATABASE_OPERATION,
        FILE_OPERATION,
        NETWORK_OPERATION,
        CUSTOM
    }

    public enum EventCategory {
        LOGIN,
        LOGOUT,
        CREATE,
        READ,
        UPDATE,
        DELETE,
        EXPORT,
        IMPORT,
        BACKUP,
        RESTORE,
        CONFIGURE,
        DEPLOY,
        START,
        STOP,
        RESTART,
        SCALE,
        MIGRATE,
        MONITOR,
        ALERT,
        NOTIFICATION
    }

    public enum Severity {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }

    public enum Result {
        SUCCESS,
        FAILURE,
        PARTIAL,
        TIMEOUT,
        CANCELLED
    }
}
