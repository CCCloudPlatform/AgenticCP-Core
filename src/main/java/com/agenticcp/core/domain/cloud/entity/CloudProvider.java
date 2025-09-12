package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cloud_providers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudProvider extends BaseEntity {

    @Column(name = "provider_key", nullable = false, unique = true)
    private String providerKey;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type")
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;

    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Column(name = "api_version")
    private String apiVersion;

    @Column(name = "authentication_type")
    @Enumerated(EnumType.STRING)
    private AuthenticationType authenticationType;

    @Column(name = "supported_regions", columnDefinition = "TEXT")
    private String supportedRegions; // JSON array of supported regions

    @Column(name = "supported_services", columnDefinition = "TEXT")
    private String supportedServices; // JSON array of supported services

    @Column(name = "pricing_model")
    @Enumerated(EnumType.STRING)
    private PricingModel pricingModel;

    @Column(name = "is_global")
    private Boolean isGlobal = false;

    @Column(name = "is_government")
    private Boolean isGovernment = false;

    @Column(name = "compliance_certifications", columnDefinition = "TEXT")
    private String complianceCertifications; // JSON array of compliance certifications

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional provider metadata

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    public enum ProviderType {
        AWS,
        AZURE,
        GCP,
        ALIBABA_CLOUD,
        IBM_CLOUD,
        ORACLE_CLOUD,
        VMWARE,
        OPENSTACK,
        KUBERNETES,
        DOCKER
    }

    public enum AuthenticationType {
        API_KEY,
        OAUTH2,
        IAM_ROLE,
        SERVICE_ACCOUNT,
        CERTIFICATE,
        TOKEN
    }

    public enum PricingModel {
        PAY_AS_YOU_GO,
        RESERVED_INSTANCE,
        SPOT_INSTANCE,
        SAVINGS_PLANS,
        COMMITTED_USE,
        PREPAID
    }
}
