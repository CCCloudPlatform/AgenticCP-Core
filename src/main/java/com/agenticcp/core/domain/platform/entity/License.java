package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "licenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class License extends BaseEntity {

    @Column(name = "license_key", nullable = false, unique = true)
    private String licenseKey;

    @Column(name = "license_name", nullable = false)
    private String licenseName;

    @Column(name = "license_type")
    @Enumerated(EnumType.STRING)
    private LicenseType licenseType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(name = "max_tenants")
    private Integer maxTenants;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_resources")
    private Integer maxResources;

    @Column(name = "features", columnDefinition = "TEXT")
    private String features; // JSON array of enabled features

    @Column(name = "issued_to")
    private String issuedTo;

    @Column(name = "issued_by")
    private String issuedBy;

    @Column(name = "issued_date")
    private LocalDateTime issuedDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_trial")
    private Boolean isTrial = false;

    @Column(name = "trial_days")
    private Integer trialDays;

    public enum LicenseType {
        TRIAL,
        BASIC,
        PROFESSIONAL,
        ENTERPRISE,
        CUSTOM
    }
}
