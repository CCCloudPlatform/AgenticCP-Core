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
@Table(name = "threat_detections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatDetection extends BaseEntity {

    @Column(name = "threat_id", nullable = false, unique = true)
    private String threatId;

    @Column(name = "threat_name", nullable = false)
    private String threatName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_type")
    private ThreatType threatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity = Severity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level")
    private ConfidenceLevel confidenceLevel = ConfidenceLevel.MEDIUM;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_auto_remediate")
    private Boolean isAutoRemediate = false;

    @Column(name = "detection_rules", columnDefinition = "TEXT")
    private String detectionRules; // JSON for detection rules

    @Column(name = "indicators", columnDefinition = "TEXT")
    private String indicators; // JSON for threat indicators

    @Column(name = "mitigation_actions", columnDefinition = "TEXT")
    private String mitigationActions; // JSON for mitigation actions

    @Column(name = "target_resources", columnDefinition = "TEXT")
    private String targetResources; // JSON array of target resource types

    @Column(name = "exceptions", columnDefinition = "TEXT")
    private String exceptions; // JSON array of detection exceptions

    @Column(name = "last_detected")
    private LocalDateTime lastDetected;

    @Column(name = "detection_count")
    private Long detectionCount = 0L;

    @Column(name = "false_positive_count")
    private Long falsePositiveCount = 0L;

    @Column(name = "true_positive_count")
    private Long truePositiveCount = 0L;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional threat metadata

    public enum ThreatType {
        MALWARE,
        PHISHING,
        RANSOMWARE,
        DDoS,
        BRUTE_FORCE,
        SQL_INJECTION,
        XSS,
        CSRF,
        PRIVILEGE_ESCALATION,
        DATA_EXFILTRATION,
        INSIDER_THREAT,
        APT,
        BOTNET,
        CRYPTOCURRENCY_MINING,
        VULNERABILITY_EXPLOIT,
        CONFIGURATION_DRIFT,
        UNAUTHORIZED_ACCESS,
        DATA_BREACH,
        ACCOUNT_TAKEOVER,
        CUSTOM
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ConfidenceLevel {
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }
}
