package com.agenticcp.core.domain.security.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityPolicy extends BaseEntity {

    @Column(name = "policy_key", nullable = false, unique = true)
    private String policyKey;

    @Column(name = "policy_name", nullable = false)
    private String policyName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type")
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity = Severity.MEDIUM;

    @Column(name = "is_global")
    private Boolean isGlobal = false;

    @Column(name = "is_system")
    private Boolean isSystem = false;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules; // JSON for policy rules

    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions; // JSON for policy conditions

    @Column(name = "actions", columnDefinition = "TEXT")
    private String actions; // JSON for policy actions

    @Column(name = "target_resources", columnDefinition = "TEXT")
    private String targetResources; // JSON array of target resource types

    @Column(name = "exceptions", columnDefinition = "TEXT")
    private String exceptions; // JSON array of policy exceptions

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "version")
    private String version = "1.0";

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional policy metadata

    public enum PolicyType {
        ACCESS_CONTROL,
        DATA_PROTECTION,
        NETWORK_SECURITY,
        ENCRYPTION,
        AUTHENTICATION,
        AUTHORIZATION,
        AUDIT_LOGGING,
        INCIDENT_RESPONSE,
        BACKUP_RECOVERY,
        COMPLIANCE,
        VULNERABILITY_MANAGEMENT,
        THREAT_DETECTION
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
