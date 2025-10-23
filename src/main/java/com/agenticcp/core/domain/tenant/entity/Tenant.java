package com.agenticcp.core.domain.tenant.entity;

import com.agenticcp.core.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "tenant_key", nullable = false, unique = true)
    private String tenantKey;

    @Column(name = "tenant_name", nullable = false)
    private String tenantName;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "tenant_type")
    @Enumerated(EnumType.STRING)
    private TenantType tenantType;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_resources")
    private Integer maxResources;

    @Column(name = "storage_quota_gb")
    private Long storageQuotaGb;

    @Column(name = "bandwidth_quota_gb")
    private Long bandwidthQuotaGb;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings; // JSON for tenant-specific settings

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    @Column(name = "is_trial")
    @Builder.Default
    private Boolean isTrial = false;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    public enum TenantType {
        INDIVIDUAL,
        SMALL_BUSINESS,
        ENTERPRISE,
        GOVERNMENT
    }
}
