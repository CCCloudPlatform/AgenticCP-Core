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

    // ==================== Factory Methods ====================

    /**
     * VM 인스턴스용 CloudResource 생성
     * CSP에서 생성된 VM 인스턴스 정보를 CloudResource 엔티티로 변환합니다.
     *
     * @param resourceId   인스턴스 ID (CSP에서 부여한 ID)
     * @param resourceName 리소스 이름 (태그에서 추출 또는 resourceId)
     * @param provider     클라우드 프로바이더
     * @param service      클라우드 서비스 (EC2, Compute Engine 등)
     * @param tenant       테넌트
     * @param instanceSize 인스턴스 크기
     * @param tagsJson     태그 (JSON 형식)
     * @return CloudResource 엔티티
     */
    public static CloudResource createVmInstance(
            String resourceId,
            String resourceName,
            CloudProvider provider,
            CloudService service,
            Tenant tenant,
            String instanceSize,
            String tagsJson
    ) {
        LocalDateTime now = LocalDateTime.now();
        return CloudResource.builder()
                .resourceId(resourceId)
                .resourceName(resourceName)
                .displayName(resourceName)
                .provider(provider)
                .service(service)
                .tenant(tenant)
                .status(Status.ACTIVE)
                .resourceType(ResourceType.INSTANCE)
                .lifecycleState(LifecycleState.PENDING)
                .instanceSize(instanceSize)
                .tags(tagsJson)
                .createdInCloud(now)
                .lastModifiedInCloud(now)
                .lastSync(now)
                .build();
    }

    /**
     * Object Storage (버킷/컨테이너)용 CloudResource 생성
     * CSP에서 생성된 스토리지 컨테이너 정보를 CloudResource 엔티티로 변환합니다.
     *
     * @param containerName 컨테이너 이름 (S3 버킷명, Azure Blob 컨테이너명 등)
     * @param provider      클라우드 프로바이더
     * @param service       클라우드 서비스 (S3, BlobStorage 등)
     * @param tenant        테넌트
     * @param tagsJson      태그 (JSON 형식)
     * @return CloudResource 엔티티
     */
    public static CloudResource createStorageBucket(
            String containerName,
            CloudProvider provider,
            CloudService service,
            Tenant tenant,
            String tagsJson
    ) {
        LocalDateTime now = LocalDateTime.now();
        return CloudResource.builder()
                .resourceId(containerName)
                .resourceName(containerName)
                .displayName(containerName)
                .provider(provider)
                .service(service)
                .tenant(tenant)
                .status(Status.ACTIVE)
                .resourceType(ResourceType.BUCKET)
                .lifecycleState(LifecycleState.RUNNING)
                .tags(tagsJson)
                .createdInCloud(now)
                .lastModifiedInCloud(now)
                .lastSync(now)
                .build();
    }

    /**
     * VPC 네트워크용 CloudResource 생성
     * CSP에서 생성된 VPC 정보를 CloudResource 엔티티로 변환합니다.
     *
     * @param vpcId        VPC ID (CSP에서 부여한 ID)
     * @param resourceName 리소스 이름 (VPC 이름 또는 vpcId)
     * @param provider     클라우드 프로바이더
     * @param service      클라우드 서비스 (EC2, VirtualNetwork 등)
     * @param tenant       테넌트
     * @param cidrBlock    CIDR 블록 (configuration에 저장)
     * @param tagsJson     태그 (JSON 형식)
     * @return CloudResource 엔티티
     */
    public static CloudResource createVpc(
            String vpcId,
            String resourceName,
            CloudProvider provider,
            CloudService service,
            Tenant tenant,
            String cidrBlock,
            String tagsJson
    ) {
        LocalDateTime now = LocalDateTime.now();
        return CloudResource.builder()
                .resourceId(vpcId)
                .resourceName(resourceName)
                .displayName(resourceName)
                .provider(provider)
                .service(service)
                .tenant(tenant)
                .status(Status.ACTIVE)
                .resourceType(ResourceType.NETWORK)
                .lifecycleState(LifecycleState.RUNNING)
                .tags(tagsJson)
                .configuration(cidrBlock)
                .createdInCloud(now)
                .lastModifiedInCloud(now)
                .lastSync(now)
                .build();
    }

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
