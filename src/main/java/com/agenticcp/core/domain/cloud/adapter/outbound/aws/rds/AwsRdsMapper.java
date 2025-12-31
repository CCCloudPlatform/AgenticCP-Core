package com.agenticcp.core.domain.cloud.adapter.outbound.aws.rds;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudRegion;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudRegionRepository;
import com.agenticcp.core.domain.cloud.repository.CloudServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.model.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AWS RDS와 도메인 모델 간의 데이터 변환을 담당하는 매퍼
 *
 * 이 매퍼는 AWS SDK의 DBInstance 객체를 우리 도메인의 CloudResource로 변환하는 역할을 합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AwsRdsMapper {
    
    private final ObjectMapper objectMapper;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudServiceRepository cloudServiceRepository;
    private final CloudRegionRepository cloudRegionRepository;
    
    public AwsRdsMapper(
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
     * AWS DBInstance를 CloudResource로 변환합니다.
     * 
     * @param dbInstance AWS SDK DBInstance 객체
     * @param providerType 프로바이더 타입
     * @param serviceKey 서비스 키 (예: "RDS")
     * @param region 리전
     * @return CloudResource 도메인 객체
     */
    public CloudResource toCloudResource(DBInstance dbInstance, CloudProvider.ProviderType providerType, 
                                         String serviceKey, String region) {
        try {
            log.debug("[AwsRdsMapper] Converting AWS DBInstance to CloudResource: {}", dbInstance.dbInstanceIdentifier());
            
            CloudProvider provider = findProvider(providerType);
            CloudService service = findService(providerType, serviceKey);
            CloudRegion cloudRegion = findRegion(providerType, region);
            
            return CloudResource.builder()
                .resourceId(dbInstance.dbInstanceIdentifier())
                .resourceName(dbInstance.dbInstanceIdentifier())
                .displayName(dbInstance.dbInstanceIdentifier())
                .provider(provider)
                .region(cloudRegion)
                .service(service)
                .resourceType(CloudResource.ResourceType.DATABASE)
                .lifecycleState(mapLifecycleState(dbInstance.dbInstanceStatus()))
                .instanceType(dbInstance.dbInstanceClass())
                .instanceSize(dbInstance.dbInstanceClass())
                .storageGb((long) dbInstance.allocatedStorage())
                .ipAddress(dbInstance.endpoint() != null ? dbInstance.endpoint().address() : null)
                .tags(convertTagsToMap(dbInstance.tagList()))
                .configuration(convertConfigurationToJson(dbInstance))
                .status(mapStatus(dbInstance.dbInstanceStatus()))
                .createdInCloud(dbInstance.instanceCreateTime() != null 
                    ? LocalDateTime.ofInstant(dbInstance.instanceCreateTime(), ZoneId.systemDefault())
                    : LocalDateTime.now())
                .lastSync(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("[AwsRdsMapper] Failed to convert AWS DBInstance to CloudResource: {}", 
                dbInstance.dbInstanceIdentifier(), e);
            throw new RuntimeException("Failed to convert AWS DBInstance to CloudResource", e);
        }
    }

    /**
     * AWS DBInstance 상태를 도메인 LifecycleState로 매핑합니다.
     */
    private CloudResource.LifecycleState mapLifecycleState(String awsStatus) {
        if (awsStatus == null) {
            return CloudResource.LifecycleState.UNKNOWN;
        }
        
        return switch (awsStatus.toLowerCase()) {
            case "available" -> CloudResource.LifecycleState.RUNNING;
            case "creating" -> CloudResource.LifecycleState.PENDING;
            case "deleting" -> CloudResource.LifecycleState.TERMINATING;
            case "deleted" -> CloudResource.LifecycleState.TERMINATED;
            case "modifying" -> CloudResource.LifecycleState.PENDING;
            case "rebooting" -> CloudResource.LifecycleState.PENDING;
            case "stopped" -> CloudResource.LifecycleState.STOPPED;
            case "stopping" -> CloudResource.LifecycleState.STOPPING;
            case "failed" -> CloudResource.LifecycleState.FAILED;
            default -> CloudResource.LifecycleState.UNKNOWN;
        };
    }
    
    /**
     * AWS DBInstance 상태를 Status로 매핑합니다.
     */
    private Status mapStatus(String awsStatus) {
        if (awsStatus == null) {
            return com.agenticcp.core.common.enums.Status.INACTIVE;
        }
        
        return switch (awsStatus.toLowerCase()) {
            case "available" -> com.agenticcp.core.common.enums.Status.ACTIVE;
            case "creating", "modifying", "rebooting" -> com.agenticcp.core.common.enums.Status.PENDING;
            case "deleting", "deleted" -> com.agenticcp.core.common.enums.Status.DELETED;
            case "stopped", "stopping" -> com.agenticcp.core.common.enums.Status.INACTIVE;
            case "failed" -> com.agenticcp.core.common.enums.Status.INACTIVE;
            default -> com.agenticcp.core.common.enums.Status.INACTIVE;
        };
    }
    
    /**
     * AWS 태그 리스트를 Map으로 변환합니다.
     */
    private Map<String, String> convertTagsToMap(List<Tag> awsTags) {
        if (awsTags == null || awsTags.isEmpty()) {
            return Map.of();
        }
        
        return awsTags.stream()
            .collect(Collectors.toMap(Tag::key, Tag::value));
    }
    
    /**
     * DBInstance의 설정을 JSON 문자열로 변환합니다.
     */
    private String convertConfigurationToJson(DBInstance dbInstance) {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("engine", dbInstance.engine());
            config.put("engineVersion", dbInstance.engineVersion());
            config.put("dbInstanceClass", dbInstance.dbInstanceClass());
            config.put("allocatedStorage", dbInstance.allocatedStorage());
            config.put("storageType", dbInstance.storageType());
            config.put("multiAZ", dbInstance.multiAZ());
            config.put("publiclyAccessible", dbInstance.publiclyAccessible());
            config.put("availabilityZone", dbInstance.availabilityZone());
            config.put("preferredBackupWindow", dbInstance.preferredBackupWindow());
            config.put("preferredMaintenanceWindow", dbInstance.preferredMaintenanceWindow());
            config.put("backupRetentionPeriod", dbInstance.backupRetentionPeriod());
            
            if (dbInstance.endpoint() != null) {
                Map<String, Object> endpoint = new HashMap<>();
                endpoint.put("address", dbInstance.endpoint().address());
                endpoint.put("port", dbInstance.endpoint().port());
                config.put("endpoint", endpoint);
            }
            
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.warn("[AwsRdsMapper] Failed to convert DBInstance configuration to JSON", e);
            return "{}";
        }
    }
    
    /**
     * CloudProvider 조회
     */
    private CloudProvider findProvider(CloudProvider.ProviderType providerType) {
        return cloudProviderRepository.findFirstByProviderType(providerType)
                .orElseThrow(() -> new IllegalStateException(
                        "CloudProvider not found for type: " + providerType));
    }
    
    /**
     * CloudService 조회
     */
    private CloudService findService(CloudProvider.ProviderType providerType, String serviceKey) {
        return cloudServiceRepository.findByProviderTypeAndServiceKey(providerType, serviceKey)
                .orElse(null); // 서비스가 없어도 null 반환 (선택적)
    }
    
    /**
     * CloudRegion 조회
     */
    private CloudRegion findRegion(CloudProvider.ProviderType providerType, String region) {
        if (region == null) {
            return null;
        }
        return cloudRegionRepository.findByProviderTypeAndRegionKey(providerType, region)
                .orElse(null); // 리전이 없어도 null 반환 (선택적)
    }
}
