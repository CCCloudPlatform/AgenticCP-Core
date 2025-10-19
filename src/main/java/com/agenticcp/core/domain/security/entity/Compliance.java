package com.agenticcp.core.domain.security.entity;

import com.agenticcp.core.common.entity.TenantAwareEntity;
import com.agenticcp.core.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "compliance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compliance extends TenantAwareEntity {

    @Column(name = "compliance_key", nullable = false, unique = true)
    private String complianceKey;

    @Column(name = "compliance_name", nullable = false)
    private String complianceName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_type")
    private ComplianceType complianceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_level")
    private ComplianceLevel complianceLevel;

    @Column(name = "version")
    private String version;

    @Column(name = "is_mandatory")
    private Boolean isMandatory = false;

    @Column(name = "is_global")
    private Boolean isGlobal = false;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements; // JSON for compliance requirements

    @Column(name = "controls", columnDefinition = "TEXT")
    private String controls; // JSON for compliance controls

    @Column(name = "evidence_requirements", columnDefinition = "TEXT")
    private String evidenceRequirements; // JSON for evidence requirements

    @Column(name = "assessment_criteria", columnDefinition = "TEXT")
    private String assessmentCriteria; // JSON for assessment criteria

    @Column(name = "remediation_guidance", columnDefinition = "TEXT")
    private String remediationGuidance; // JSON for remediation guidance

    @Column(name = "applicable_regions", columnDefinition = "TEXT")
    private String applicableRegions; // JSON array of applicable regions

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "last_assessment")
    private LocalDateTime lastAssessment;

    @Column(name = "next_assessment")
    private LocalDateTime nextAssessment;

    @Column(name = "assessment_frequency_days")
    private Integer assessmentFrequencyDays;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional compliance metadata

    public enum ComplianceType {
        GDPR,
        HIPAA,
        SOX,
        PCI_DSS,
        ISO_27001,
        SOC_2,
        FEDRAMP,
        NIST,
        CIS,
        CCPA,
        PIPEDA,
        LGPD,
        CUSTOM
    }

    public enum ComplianceLevel {
        BASIC,
        INTERMEDIATE,
        ADVANCED,
        ENTERPRISE,
        GOVERNMENT
    }
}
