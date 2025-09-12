package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cloud_resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudResource extends BaseEntity {

    @Column(name = "resource_id", nullable = false, unique = true)
    private String resourceId;

    @Column(name = "resource_name", nullable = false)
    private String resourceName;

    @Column(name = "display_name")
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private CloudProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private CloudRegion region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private CloudService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state")
    private LifecycleState lifecycleState = LifecycleState.RUNNING;

    @Column(name = "instance_type")
    private String instanceType;

    @Column(name = "instance_size")
    private String instanceSize;

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "memory_gb")
    private Integer memoryGb;

    @Column(name = "storage_gb")
    private Long storageGb;

    @Column(name = "network_bandwidth_mbps")
    private Integer networkBandwidthMbps;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "private_ip_address")
    private String privateIpAddress;

    @Column(name = "public_ip_address")
    private String publicIpAddress;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON for resource tags

    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration; // JSON for resource configuration

    @Column(name = "cost_per_hour")
    private BigDecimal costPerHour;

    @Column(name = "monthly_cost")
    private BigDecimal monthlyCost;

    @Column(name = "created_in_cloud")
    private LocalDateTime createdInCloud;

    @Column(name = "last_modified_in_cloud")
    private LocalDateTime lastModifiedInCloud;

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional resource metadata

    public enum ResourceType {
        INSTANCE,
        VOLUME,
        SNAPSHOT,
        IMAGE,
        NETWORK,
        SUBNET,
        SECURITY_GROUP,
        LOAD_BALANCER,
        DATABASE,
        BUCKET,
        FUNCTION,
        CONTAINER,
        CLUSTER,
        NODE,
        POD,
        SERVICE,
        INGRESS,
        CONFIG_MAP,
        SECRET,
        PERSISTENT_VOLUME,
        PERSISTENT_VOLUME_CLAIM
    }

    public enum LifecycleState {
        PENDING,
        RUNNING,
        STOPPING,
        STOPPED,
        TERMINATING,
        TERMINATED,
        FAILED,
        UNKNOWN
    }
}
