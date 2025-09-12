package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cloud_regions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudRegion extends BaseEntity {

    @Column(name = "region_key", nullable = false)
    private String regionKey;

    @Column(name = "region_name", nullable = false)
    private String regionName;

    @Column(name = "display_name")
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private CloudProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "is_government")
    private Boolean isGovernment = false;

    @Column(name = "is_multi_zone")
    private Boolean isMultiZone = false;

    @Column(name = "availability_zones", columnDefinition = "TEXT")
    private String availabilityZones; // JSON array of availability zones

    @Column(name = "supported_services", columnDefinition = "TEXT")
    private String supportedServices; // JSON array of supported services

    @Column(name = "pricing_tier")
    private String pricingTier;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional region metadata
}
