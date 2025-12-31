package com.agenticcp.core.domain.cloud.adapter.outbound.aws.cloudfront;

import com.agenticcp.core.domain.cloud.dto.CDNDistributionQueryRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudRegion;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import com.agenticcp.core.domain.cloud.port.model.cdn.*;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudRegionRepository;
import com.agenticcp.core.domain.cloud.repository.CloudServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudfront.model.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * AWS CloudFront Distribution 응답을 CloudResource로 변환하는 매퍼
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AwsCloudFrontMapper {
    
    private final ObjectMapper objectMapper;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudServiceRepository cloudServiceRepository;
    private final CloudRegionRepository cloudRegionRepository;
    
    public AwsCloudFrontMapper(
            ObjectMapper objectMapper,
            CloudProviderRepository cloudProviderRepository,
            CloudServiceRepository cloudServiceRepository,
            CloudRegionRepository cloudRegionRepository) {
        this.objectMapper = objectMapper;
        this.cloudProviderRepository = cloudProviderRepository;
        this.cloudServiceRepository = cloudServiceRepository;
        this.cloudRegionRepository = cloudRegionRepository;
    }
    
    /**
     * Distribution을 CloudResource로 변환 (CreateDistributionCommand 사용)
     */
    public CloudResource toCloudResource(Distribution distribution, String etag, CreateDistributionCommand command) {
        return buildCloudResourceForCreate(distribution, etag, command);
    }
    
    /**
     * Distribution을 CloudResource로 변환 (UpdateDistributionCommand 사용)
     */
    public CloudResource toCloudResource(Distribution distribution, String etag, UpdateDistributionCommand command) {
        Map<String, String> tags = command.tags() != null ? command.tags() : new HashMap<>();
        return buildCloudResource(distribution, etag, command.providerType(), "CloudFront", command.tenantKey(), tags);
    }
    
    /**
     * Distribution을 CloudResource로 변환 (CDNDistributionQueryRequest 사용)
     */
    public CloudResource toCloudResource(DistributionSummary distributionSummary, CDNDistributionQueryRequest query) {
        Map<String, String> tags = query.tags() != null ? query.tags() : new HashMap<>();
        return buildCloudResourceFromSummary(distributionSummary, query.providerType(), "CloudFront", query.tenantKey(), tags);
    }
    
    /**
     * Distribution을 CloudResource로 변환 (조회용)
     */
    public CloudResource toCloudResource(Distribution distribution, String etag, CloudProvider.ProviderType providerType, Map<String, String> tags) {
        return buildCloudResource(distribution, etag, providerType, "CloudFront", null, tags);
    }
    
    /**
     * Distribution 생성 시 사용하는 CloudResource 빌더
     */
    private CloudResource buildCloudResourceForCreate(Distribution distribution, String etag, CreateDistributionCommand command) {
        String resourceId = distribution.id();
        String resourceName = command.distributionName() != null && !command.distributionName().isEmpty()
                ? command.distributionName()
                : distribution.id();
        
        // 메타데이터 구성
        Map<String, Object> metadata = buildMetadata(distribution, etag, command.origin(), command.cacheBehaviors());
        
        String metadataJson = null;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[AwsCloudFrontMapper] Failed to serialize metadata: {}", e.getMessage());
        }
        
        // 엔티티 조회
        CloudProvider provider = cloudProviderRepository.findFirstByProviderType(command.providerType())
                .orElseThrow(() -> new IllegalStateException("CloudProvider not found for type: " + command.providerType()));
        
        CloudService service = cloudServiceRepository.findByProviderTypeAndServiceKey(command.providerType(), command.serviceKey())
                .orElseThrow(() -> new IllegalStateException("CloudService not found for providerType: " + command.providerType() + ", serviceKey: " + command.serviceKey()));
        
        // CloudFront는 글로벌 서비스이므로 region은 GLOBAL 또는 null
        CloudRegion cloudRegion = cloudRegionRepository.findByProviderTypeAndRegionKey(command.providerType(), "GLOBAL")
                .orElse(null);
        
        return CloudResource.builder()
            .resourceId(resourceId)
            .resourceName(resourceName)
            .displayName(resourceName)
            .provider(provider)
            .service(service)
            .region(cloudRegion)
            .resourceType(CloudResource.ResourceType.CDN_DISTRIBUTION)
            .lifecycleState(mapStatusToLifecycleState(distribution.status()))
            .tags(command.tags())
            .metadata(metadataJson)
            .createdInCloud(LocalDateTime.now())
            .build();
    }
    
    /**
     * Distribution 조회/수정 시 사용하는 CloudResource 빌더
     */
    private CloudResource buildCloudResource(
            Distribution distribution,
            String etag,
            CloudProvider.ProviderType providerType,
            String serviceKey,
            String tenantKey,
            Map<String, String> tags) {
        String resourceId = distribution.id();
        String resourceName = distribution.id();
        
        // 메타데이터 구성
        Map<String, Object> metadata = buildMetadata(distribution, etag, null, null);
        
        String metadataJson = null;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[AwsCloudFrontMapper] Failed to serialize metadata: {}", e.getMessage());
        }
        
        // 엔티티 조회
        CloudProvider provider = cloudProviderRepository.findFirstByProviderType(providerType)
                .orElseThrow(() -> new IllegalStateException("CloudProvider not found for type: " + providerType));
        
        CloudService service = cloudServiceRepository.findByProviderTypeAndServiceKey(providerType, serviceKey)
                .orElseThrow(() -> new IllegalStateException("CloudService not found for providerType: " + providerType + ", serviceKey: " + serviceKey));
        
        CloudRegion cloudRegion = cloudRegionRepository.findByProviderTypeAndRegionKey(providerType, "GLOBAL")
                .orElse(null);
        
        return CloudResource.builder()
            .resourceId(resourceId)
            .resourceName(resourceName)
            .displayName(resourceName)
            .provider(provider)
            .service(service)
            .region(cloudRegion)
            .resourceType(CloudResource.ResourceType.CDN_DISTRIBUTION)
            .lifecycleState(mapStatusToLifecycleState(distribution.status()))
            .tags(tags != null ? tags : new HashMap<>())
            .metadata(metadataJson)
            .lastModifiedInCloud(distribution.lastModifiedTime() != null 
                ? LocalDateTime.ofInstant(distribution.lastModifiedTime(), ZoneId.systemDefault())
                : LocalDateTime.now())
            .build();
    }
    
    /**
     * DistributionSummary를 CloudResource로 변환 (목록 조회용)
     */
    private CloudResource buildCloudResourceFromSummary(
            DistributionSummary summary,
            CloudProvider.ProviderType providerType,
            String serviceKey,
            String tenantKey,
            Map<String, String> tags) {
        String resourceId = summary.id();
        String resourceName = summary.id();
        
        // 간단한 메타데이터 (Summary는 상세 정보가 적음)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("distributionId", summary.id());
        metadata.put("domainName", summary.domainName());
        metadata.put("status", summary.status());
        metadata.put("enabled", summary.enabled());
        metadata.put("comment", summary.comment());
        metadata.put("arn", summary.arn());
        
        String metadataJson = null;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[AwsCloudFrontMapper] Failed to serialize metadata: {}", e.getMessage());
        }
        
        // 엔티티 조회
        CloudProvider provider = cloudProviderRepository.findFirstByProviderType(providerType)
                .orElseThrow(() -> new IllegalStateException("CloudProvider not found for type: " + providerType));
        
        CloudService service = cloudServiceRepository.findByProviderTypeAndServiceKey(providerType, serviceKey)
                .orElseThrow(() -> new IllegalStateException("CloudService not found for providerType: " + providerType + ", serviceKey: " + serviceKey));
        
        CloudRegion cloudRegion = cloudRegionRepository.findByProviderTypeAndRegionKey(providerType, "GLOBAL")
                .orElse(null);
        
        return CloudResource.builder()
            .resourceId(resourceId)
            .resourceName(resourceName)
            .displayName(resourceName)
            .provider(provider)
            .service(service)
            .region(cloudRegion)
            .resourceType(CloudResource.ResourceType.CDN_DISTRIBUTION)
            .lifecycleState(mapStatusToLifecycleState(summary.status()))
            .tags(tags != null ? tags : new HashMap<>())
            .metadata(metadataJson)
            .lastModifiedInCloud(summary.lastModifiedTime() != null
                ? LocalDateTime.ofInstant(summary.lastModifiedTime(), ZoneId.systemDefault())
                : LocalDateTime.now())
            .build();
    }
    
    /**
     * Distribution 메타데이터 구성
     */
    private Map<String, Object> buildMetadata(
            Distribution distribution,
            String etag,
            OriginConfig originConfig,
            java.util.List<CacheBehaviorConfig> cacheBehaviorConfigs) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("distributionId", distribution.id());
        metadata.put("arn", distribution.arn());
        metadata.put("domainName", distribution.domainName());
        metadata.put("status", distribution.status());
        metadata.put("etag", etag);
        
        DistributionConfig config = distribution.distributionConfig();
        if (config != null) {
            metadata.put("enabled", config.enabled());
            metadata.put("comment", config.comment());
            
            // Origin 정보 저장
            if (originConfig != null) {
                metadata.put("origin", originConfig);
            } else if (config.origins() != null && config.origins().items() != null && !config.origins().items().isEmpty()) {
                Origin firstOrigin = config.origins().items().get(0);
                Map<String, Object> originMap = new HashMap<>();
                originMap.put("id", firstOrigin.id());
                originMap.put("domainName", firstOrigin.domainName());
                metadata.put("origin", originMap);
            }
            
            // CacheBehavior 정보 저장
            if (cacheBehaviorConfigs != null) {
                metadata.put("cacheBehaviors", cacheBehaviorConfigs);
            }
            
            // Aliases 저장
            if (config.aliases() != null && config.aliases().items() != null && !config.aliases().items().isEmpty()) {
                metadata.put("aliases", config.aliases().items());
            }
            
            // SSL 인증서 정보
            if (config.viewerCertificate() != null) {
                metadata.put("sslCertificateArn", config.viewerCertificate().acmCertificateArn());
                metadata.put("iamCertificateId", config.viewerCertificate().iamCertificateId());
            }
        }
        
        return metadata;
    }
    
    /**
     * CloudFront Distribution 상태를 LifecycleState로 매핑
     */
    private CloudResource.LifecycleState mapStatusToLifecycleState(String status) {
        if (status == null) {
            return CloudResource.LifecycleState.UNKNOWN;
        }
        
        return switch (status) {
            case "InProgress" -> CloudResource.LifecycleState.PENDING;
            case "Deployed" -> CloudResource.LifecycleState.RUNNING;
            default -> CloudResource.LifecycleState.UNKNOWN;
        };
    }
    
}

