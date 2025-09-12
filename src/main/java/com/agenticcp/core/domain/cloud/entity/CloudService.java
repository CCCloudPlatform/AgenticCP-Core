package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cloud_services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudService extends BaseEntity {

    @Column(name = "service_key", nullable = false)
    private String serviceKey;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "display_name")
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private CloudProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_category")
    private ServiceCategory serviceCategory;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "version")
    private String version;

    @Column(name = "is_global")
    private Boolean isGlobal = false;

    @Column(name = "is_region_specific")
    private Boolean isRegionSpecific = true;

    @Column(name = "is_managed")
    private Boolean isManaged = false;

    @Column(name = "is_serverless")
    private Boolean isServerless = false;

    @Column(name = "pricing_model")
    @Enumerated(EnumType.STRING)
    private PricingModel pricingModel;

    @Column(name = "free_tier_available")
    private Boolean freeTierAvailable = false;

    @Column(name = "free_tier_limit", columnDefinition = "TEXT")
    private String freeTierLimit; // JSON for free tier limits

    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Column(name = "documentation_url")
    private String documentationUrl;

    @Column(name = "supported_regions", columnDefinition = "TEXT")
    private String supportedRegions; // JSON array of supported regions

    @Column(name = "dependencies", columnDefinition = "TEXT")
    private String dependencies; // JSON array of service dependencies

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional service metadata

    public enum ServiceType {
        COMPUTE,
        STORAGE,
        DATABASE,
        NETWORKING,
        SECURITY,
        MONITORING,
        ANALYTICS,
        MACHINE_LEARNING,
        CONTAINER,
        SERVERLESS,
        MESSAGING,
        CACHING,
        CDN,
        DNS,
        LOAD_BALANCER,
        VPN,
        FIREWALL,
        BACKUP,
        DISASTER_RECOVERY
    }

    public enum ServiceCategory {
        INFRASTRUCTURE,
        PLATFORM,
        SOFTWARE,
        SECURITY,
        MONITORING,
        ANALYTICS,
        AI_ML,
        CONTAINER,
        SERVERLESS,
        STORAGE,
        NETWORKING,
        DATABASE,
        MESSAGING,
        CACHING,
        CDN
    }

    public enum PricingModel {
        PAY_AS_YOU_GO,
        RESERVED,
        SPOT,
        SAVINGS_PLANS,
        COMMITTED_USE,
        PREPAID,
        FREE_TIER,
        USAGE_BASED
    }
}
