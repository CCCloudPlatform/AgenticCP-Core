package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vpc;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudRegion;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import com.agenticcp.core.domain.cloud.port.model.vpc.CreateVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.GetVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.ListVpcsQueryRequest;
import com.agenticcp.core.domain.cloud.port.model.vpc.UpdateVpcCommand;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudRegionRepository;
import com.agenticcp.core.domain.cloud.repository.CloudServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.model.Vpc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AWS VPC 응답을 CloudResource로 변환하는 매퍼
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AwsVpcMapper {
    
    private final ObjectMapper objectMapper;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudServiceRepository cloudServiceRepository;
    private final CloudRegionRepository cloudRegionRepository;
    
    public AwsVpcMapper(
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
     * Vpc를 CloudResource로 변환 (CreateVpcCommand 사용)
     */
    public CloudResource toCloudResource(Vpc vpc, CreateVpcCommand command) {
        return buildCloudResource(vpc, command.providerType(), command.serviceKey(), command.region(), command.tenantKey(), command.tags());
    }
    
    /**
     * Vpc를 CloudResource로 변환 (GetVpcCommand 사용)
     */
    public CloudResource toCloudResource(Vpc vpc, GetVpcCommand command) {
        return buildCloudResource(vpc, command.providerType(), command.serviceKey(), command.region(), null, extractTagsFromVpc(vpc));
    }
    
    /**
     * Vpc를 CloudResource로 변환 (ListVpcsQuery 사용)
     */
    public CloudResource toCloudResource(Vpc vpc, ListVpcsQueryRequest query) {
        return buildCloudResource(vpc, query.providerType(), "EC2", query.region(), query.tenantKey(), extractTagsFromVpc(vpc));
    }
    
    /**
     * Vpc를 CloudResource로 변환 (UpdateVpcCommand 사용)
     */
    public CloudResource toCloudResource(Vpc vpc, UpdateVpcCommand command) {
        Map<String, String> tags = command.tags() != null ? command.tags() : extractTagsFromVpc(vpc);
        return buildCloudResource(vpc, command.providerType(), "EC2", command.region(), command.tenantKey(), tags);
    }
    
    private CloudResource buildCloudResource(
            Vpc vpc, 
            CloudProvider.ProviderType providerType, 
            String serviceKey, 
            String region, 
            String tenantKey, 
            Map<String, String> tags) {
        String resourceId = vpc.vpcId();
        String resourceName = vpc.vpcId(); // VPC ID를 기본 이름으로 사용
        
        // 태그에서 Name 추출
        if (vpc.tags() != null && !vpc.tags().isEmpty()) {
            String nameTag = vpc.tags().stream()
                .filter(tag -> "Name".equals(tag.key()))
                .findFirst()
                .map(tag -> tag.value())
                .orElse(null);
            if (nameTag != null && !nameTag.isEmpty()) {
                resourceName = nameTag;
            }
        }
        
        // 태그를 JSON 문자열로 변환
        String tagsJson = null;
        if (tags != null && !tags.isEmpty()) {
            try {
                tagsJson = objectMapper.writeValueAsString(tags);
            } catch (JsonProcessingException e) {
                // 로깅은 생략 (필요시 추가)
            }
        }
        
        // 메타데이터 구성
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("vpcId", vpc.vpcId());
        metadata.put("cidrBlock", vpc.cidrBlock());
        metadata.put("state", vpc.stateAsString());
        metadata.put("isDefault", vpc.isDefault());
        metadata.put("dhcpOptionsId", vpc.dhcpOptionsId());
        metadata.put("instanceTenancy", vpc.instanceTenancyAsString());
        
        String metadataJson = null;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            // 로깅은 생략
        }
        
        // 엔티티 조회
        CloudProvider provider = cloudProviderRepository.findFirstByProviderType(providerType)
                .orElseThrow(() -> new IllegalStateException("CloudProvider not found for type: " + providerType));
        
        CloudService service = cloudServiceRepository.findByProviderTypeAndServiceKey(providerType, serviceKey)
                .orElseThrow(() -> new IllegalStateException("CloudService not found for providerType: " + providerType + ", serviceKey: " + serviceKey));
        
        // region은 optional이므로 null일 수 있음
        CloudRegion cloudRegion = null;
        if (region != null && !region.isEmpty()) {
            cloudRegion = cloudRegionRepository.findByProviderTypeAndRegionKey(providerType, region)
                    .orElse(null); // region이 없어도 CloudResource는 생성 가능
            if (cloudRegion == null) {
                log.warn("[AwsVpcMapper] CloudRegion not found for providerType: {}, regionKey: {}", providerType, region);
            }
        }
        
        return CloudResource.builder()
            .resourceId(resourceId)
            .resourceName(resourceName)
            .displayName(resourceName)
            .provider(provider)
            .service(service)
            .region(cloudRegion)
            .resourceType(CloudResource.ResourceType.NETWORK)
            .lifecycleState(mapStateToLifecycleState(vpc.stateAsString()))
            .tags(tagsJson)
            .metadata(metadataJson)
            .createdInCloud(LocalDateTime.now()) // AWS VPC는 생성 시간 정보를 직접 제공하지 않으므로 현재 시간 사용
            .build();
    }
    
    private Map<String, String> extractTagsFromVpc(Vpc vpc) {
        if (vpc.tags() == null || vpc.tags().isEmpty()) {
            return new HashMap<>();
        }
        return vpc.tags().stream()
            .collect(Collectors.toMap(
                tag -> tag.key(),
                tag -> tag.value() != null ? tag.value() : ""
            ));
    }
    
    private CloudResource.LifecycleState mapStateToLifecycleState(String state) {
        if (state == null) {
            return CloudResource.LifecycleState.UNKNOWN;
        }
        return switch (state.toUpperCase()) {
            case "PENDING" -> CloudResource.LifecycleState.PENDING;
            case "AVAILABLE" -> CloudResource.LifecycleState.RUNNING;
            default -> CloudResource.LifecycleState.UNKNOWN;
        };
    }
}
