package com.agenticcp.core.domain.tenant.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenant_isolation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantIsolation extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "isolation_level")
    @Enumerated(EnumType.STRING)
    private IsolationLevel isolationLevel;

    @Column(name = "network_isolation")
    private Boolean networkIsolation = false;

    @Column(name = "data_isolation")
    private Boolean dataIsolation = false;

    @Column(name = "compute_isolation")
    private Boolean computeIsolation = false;

    @Column(name = "storage_isolation")
    private Boolean storageIsolation = false;

    @Column(name = "vpc_id")
    private String vpcId;

    @Column(name = "subnet_ids", columnDefinition = "TEXT")
    private String subnetIds; // JSON array of subnet IDs

    @Column(name = "security_group_ids", columnDefinition = "TEXT")
    private String securityGroupIds; // JSON array of security group IDs

    @Column(name = "encryption_key_id")
    private String encryptionKeyId;

    @Column(name = "backup_retention_days")
    private Integer backupRetentionDays;

    @Column(name = "compliance_requirements", columnDefinition = "TEXT")
    private String complianceRequirements; // JSON array of compliance requirements

    @Column(name = "isolation_policies", columnDefinition = "TEXT")
    private String isolationPolicies; // JSON for custom isolation policies

    public enum IsolationLevel {
        SHARED,
        DEDICATED,
        PRIVATE,
        GOVERNMENT
    }
}
