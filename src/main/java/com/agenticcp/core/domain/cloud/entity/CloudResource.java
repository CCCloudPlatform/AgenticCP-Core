package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.config.TagMapConverter;
import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

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

    @Convert(converter = TagMapConverter.class)
    @Column(name = "tags", columnDefinition = "TEXT")
    private Map<String, String> tags;

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
     * 통합 CloudResource 생성 팩토리 메서드
     * 
     * 모든 리소스 타입(VM, Storage, VPC, RDS 등)을 하나의 메서드로 생성합니다.
     * 도메인별 상세 속성은 ResourceRegistrationRequest의 attributes에서 추출합니다.
     *
     * @param request  리소스 등록 요청 DTO
     * @param provider 클라우드 프로바이더
     * @param service  클라우드 서비스
     * @param tenant   테넌트
     * @return CloudResource 엔티티
     */
    public static CloudResource create(
            ResourceRegistrationRequest request,
            CloudProvider provider,
            CloudService service,
            Tenant tenant
    ) {
        LocalDateTime now = LocalDateTime.now();
        
        CloudResource resource = CloudResource.builder()
                .resourceId(request.getResourceId())
                .resourceName(request.getResourceName())
                .displayName(request.getResourceName())
                .provider(provider)
                .service(service)
                .tenant(tenant)
                .status(Status.ACTIVE)
                .resourceType(request.getResourceType())
                .lifecycleState(determineInitialLifecycleState(request))
                .tags(request.getTags())
                .createdInCloud(now)
                .lastModifiedInCloud(now)
                .lastSync(now)
                .build();
        
        // 도메인별 속성 적용
        applyAttributes(resource, request);
        
        return resource;
    }

    /**
     * 초기 생명주기 상태 결정
     * 요청에 명시된 상태가 있으면 사용, 없으면 리소스 타입에 따라 기본값 적용
     */
    private static LifecycleState determineInitialLifecycleState(ResourceRegistrationRequest request) {
        if (request.getInitialLifecycleState() != null) {
            return request.getInitialLifecycleState();
        }
        
        // 리소스 타입별 기본 생명주기 상태
        return switch (request.getResourceType()) {
            case INSTANCE -> LifecycleState.PENDING;
            default -> LifecycleState.RUNNING;
        };
    }

    /**
     * 도메인별 속성을 CloudResource에 적용
     */
    private static void applyAttributes(CloudResource resource, ResourceRegistrationRequest request) {
        // instanceSize
        String instanceSize = request.getAttributeAsString(
                ResourceRegistrationRequest.AttributeKeys.INSTANCE_SIZE);
        if (instanceSize != null) {
            resource.setInstanceSize(instanceSize);
        }
        
        // configuration (cidrBlock, JSON 설정 등)
        String configuration = request.getAttributeAsString(
                ResourceRegistrationRequest.AttributeKeys.CONFIGURATION);
        if (configuration != null) {
            resource.setConfiguration(configuration);
        }
        
        // cpuCores
        Integer cpuCores = request.getAttributeAsInteger(
                ResourceRegistrationRequest.AttributeKeys.CPU_CORES);
        if (cpuCores != null) {
            resource.setCpuCores(cpuCores);
        }
        
        // memoryGb
        Integer memoryGb = request.getAttributeAsInteger(
                ResourceRegistrationRequest.AttributeKeys.MEMORY_GB);
        if (memoryGb != null) {
            resource.setMemoryGb(memoryGb);
        }
        
        // storageGb
        Long storageGb = request.getAttributeAsLong(
                ResourceRegistrationRequest.AttributeKeys.STORAGE_GB);
        if (storageGb != null) {
            resource.setStorageGb(storageGb);
        }
        
        // instanceType
        String instanceType = request.getAttributeAsString(
                ResourceRegistrationRequest.AttributeKeys.INSTANCE_TYPE);
        if (instanceType != null) {
            resource.setInstanceType(instanceType);
        }
    }

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
     * @param tags         태그 맵
     * @return CloudResource 엔티티
     */
    public static CloudResource createVmInstance(
            String resourceId,
            String resourceName,
            CloudProvider provider,
            CloudService service,
            Tenant tenant,
            String instanceSize,
            Map<String, String> tags
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
                .tags(tags)
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
     * @param tags          태그 맵
     * @return CloudResource 엔티티
     */
    public static CloudResource createStorageBucket(
            String containerName,
            CloudProvider provider,
            CloudService service,
            Tenant tenant,
            Map<String, String> tags
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
                .tags(tags)
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
     * @param tags         태그 맵
     * @return CloudResource 엔티티
     */
    public static CloudResource createVpc(
            String vpcId,
            String resourceName,
            CloudProvider provider,
            CloudService service,
            Tenant tenant,
            String cidrBlock,
            Map<String, String> tags
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
                .tags(tags)
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
        CDN_DISTRIBUTION,
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
        PERSISTENT_VOLUME_CLAIM,
        DNS_ZONE
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
