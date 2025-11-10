package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.VmUpdateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AWS EC2 인스턴스와 도메인 모델 간의 데이터 변환을 담당하는 매퍼
 * 
 * 이 매퍼는 AWS SDK의 Instance 객체를 우리 도메인의 CloudResource로 변환하고,
 * 도메인 모델을 AWS SDK 요청 객체로 변환하는 역할을 합니다.
 * 
 * Canonical 모델 패턴을 사용하여 CSP별 차이를 흡수합니다.
 */
@Slf4j
@Component
public class AwsVmMapper {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // ==================== AWS Instance → CloudResource 변환 ====================
    
    /**
     * AWS Instance를 CloudResource로 변환합니다.
     * 
     * @param awsInstance AWS SDK Instance 객체
     * @return CloudResource 도메인 객체
     */
    public CloudResource toCloudResource(Instance awsInstance) {
        try {
            log.debug("[AwsVmMapper] Converting AWS Instance to CloudResource: {}", awsInstance.instanceId());
            
            return CloudResource.builder()
                .resourceId(awsInstance.instanceId())
                .resourceName(getInstanceName(awsInstance))
                .displayName(getInstanceName(awsInstance))
                .lifecycleState(mapInstanceState(awsInstance.state().nameAsString()))
                .instanceType(awsInstance.instanceTypeAsString())
                .cpuCores(getCpuCores(awsInstance.instanceTypeAsString()))
                .memoryGb(getMemoryGb(awsInstance.instanceTypeAsString()))
                .publicIpAddress(awsInstance.publicIpAddress())
                .privateIpAddress(awsInstance.privateIpAddress())
                .tags(toJson(mapTags(awsInstance.tags())))
                .configuration(toJson(awsInstance))
                .metadata(buildMetadata(awsInstance))
                .build();
                
        } catch (Exception e) {
            log.error("[AwsVmMapper] Failed to convert AWS Instance to CloudResource: {}", awsInstance.instanceId(), e);
            throw new RuntimeException("Failed to convert AWS Instance to CloudResource", e);
        }
    }
    
    // ==================== VmQuery → AWS 요청 변환 ====================
    
    /**
     * VmQuery를 AWS DescribeInstancesRequest로 변환합니다.
     * 
     * @param query 도메인 쿼리 객체
     * @return AWS DescribeInstancesRequest 객체
     */
    public DescribeInstancesRequest toDescribeInstancesRequest(VmQuery query) {
        log.debug("[AwsVmMapper] Converting VmQuery to DescribeInstancesRequest");
        
        DescribeInstancesRequest.Builder builder = DescribeInstancesRequest.builder();
        
        if (query.getInstanceId() != null) {
            builder.instanceIds(query.getInstanceId());
        }
        
        // 필터 구성
        List<Filter> filters = buildFilters(query);
        if (!filters.isEmpty()) {
            builder.filters(filters);
        }
        
        return builder.build();
    }
    
    /**
     * VmQuery에서 AWS 필터를 구성합니다.
     */
    private List<Filter> buildFilters(VmQuery query) {
        List<Filter> filters = new java.util.ArrayList<>();
        
        if (query.getInstanceName() != null) {
            filters.add(Filter.builder()
                .name("tag:Name")
                .values(query.getInstanceName())
                .build());
        }
        
        if (query.getState() != null) {
            filters.add(Filter.builder()
                .name("instance-state-name")
                .values(query.getState())
                .build());
        }
        
        if (query.getInstanceType() != null) {
            filters.add(Filter.builder()
                .name("instance-type")
                .values(query.getInstanceType())
                .build());
        }
        
        if (query.getAvailabilityZone() != null) {
            filters.add(Filter.builder()
                .name("availability-zone")
                .values(query.getAvailabilityZone())
                .build());
        }
        
        if (query.getTags() != null && !query.getTags().isEmpty()) {
            query.getTags().forEach((key, value) -> {
                filters.add(Filter.builder()
                    .name("tag:" + key)
                    .values(value)
                    .build());
            });
        }
        
        return filters;
    }
    
    // ==================== VmCreateRequest → AWS 요청 변환 ====================
    
    /**
     * VmCreateRequest를 AWS RunInstancesRequest로 변환합니다.
     * 
     * @param request 도메인 생성 요청 객체
     * @return AWS RunInstancesRequest 객체
     */
    public RunInstancesRequest toRunInstancesRequest(VmCreateRequest request) {
        log.debug("[AwsVmMapper] Converting VmCreateRequest to RunInstancesRequest");
        
        RunInstancesRequest.Builder builder = RunInstancesRequest.builder()
            .imageId(request.getImageId())
            .instanceType(InstanceType.fromValue(request.getInstanceType()))
            .minCount(request.getMinCount())
            .maxCount(request.getMaxCount());
        
        if (request.getKeyName() != null) {
            builder.keyName(request.getKeyName());
        }
        
        if (request.getSecurityGroupId() != null) {
            builder.securityGroupIds(request.getSecurityGroupId());
        }
        
        if (request.getSubnetId() != null) {
            builder.subnetId(request.getSubnetId());
        }
        
        if (request.getUserData() != null) {
            builder.userData(request.getUserData());
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            builder.tagSpecifications(buildTagSpecifications(request.getTags()));
        }
        
        return builder.build();
    }
    
    /**
     * 태그를 AWS TagSpecification 형태로 변환합니다.
     */
    private List<TagSpecification> buildTagSpecifications(Map<String, String> tags) {
        List<Tag> tagList = tags.entrySet().stream()
            .map(entry -> Tag.builder()
                .key(entry.getKey())
                .value(entry.getValue())
                .build())
            .collect(Collectors.toList());
        
        return List.of(TagSpecification.builder()
            .resourceType(ResourceType.INSTANCE)
            .tags(tagList)
            .build());
    }
    
    // ==================== VmUpdateRequest → AWS 요청 변환 ====================
    
    /**
     * VmUpdateRequest를 AWS ModifyInstanceAttributeRequest로 변환합니다.
     * 
     * @param request 도메인 수정 요청 객체
     * @return AWS ModifyInstanceAttributeRequest 객체
     */
    public ModifyInstanceAttributeRequest toModifyInstanceAttributeRequest(VmUpdateRequest request) {
        log.debug("[AwsVmMapper] Converting VmUpdateRequest to ModifyInstanceAttributeRequest");
        
        ModifyInstanceAttributeRequest.Builder builder = ModifyInstanceAttributeRequest.builder()
            .instanceId(request.getInstanceId());
        
        if (request.getInstanceType() != null) {
            builder.instanceType(AttributeValue.builder()
                .value(request.getInstanceType())
                .build());
        }
        
        if (request.getUserData() != null) {
            builder.userData(BlobAttributeValue.builder()
                .value(software.amazon.awssdk.core.SdkBytes.fromUtf8String(request.getUserData()))
                .build());
        }
        
        return builder.build();
    }
    
    // ==================== VmDeleteRequest → AWS 요청 변환 ====================
    
    /**
     * VmDeleteRequest를 AWS TerminateInstancesRequest로 변환합니다.
     * 
     * @param request 도메인 삭제 요청 객체
     * @return AWS TerminateInstancesRequest 객체
     */
    public TerminateInstancesRequest toTerminateInstancesRequest(VmDeleteRequest request) {
        log.debug("[AwsVmMapper] Converting VmDeleteRequest to TerminateInstancesRequest");
        
        return TerminateInstancesRequest.builder()
            .instanceIds(request.getInstanceId())
            .build();
    }
    
    // ==================== 유틸리티 메서드 ====================
    
    /**
     * AWS Instance에서 인스턴스 이름을 추출합니다.
     */
    private String getInstanceName(Instance awsInstance) {
        return awsInstance.tags().stream()
            .filter(tag -> "Name".equals(tag.key()))
            .map(Tag::value)
            .findFirst()
            .orElse(awsInstance.instanceId());
    }
    
    /**
     * AWS Instance 상태를 도메인 LifecycleState로 매핑합니다.
     */
    private CloudResource.LifecycleState mapInstanceState(String awsState) {
        return switch (awsState.toLowerCase()) {
            case "running" -> CloudResource.LifecycleState.RUNNING;
            case "stopped" -> CloudResource.LifecycleState.STOPPED;
            case "stopping" -> CloudResource.LifecycleState.STOPPING;
            case "pending" -> CloudResource.LifecycleState.PENDING;
            case "terminated" -> CloudResource.LifecycleState.TERMINATED;
            case "terminating" -> CloudResource.LifecycleState.TERMINATING;
            default -> CloudResource.LifecycleState.UNKNOWN;
        };
    }
    
    /**
     * 인스턴스 타입에서 CPU 코어 수를 추출합니다.
     */
    private Integer getCpuCores(String instanceType) {
        return switch (instanceType.toLowerCase()) {
            case "t2.micro", "t2.nano" -> 1;
            case "t2.small" -> 1;
            case "t2.medium" -> 2;
            case "t2.large" -> 2;
            case "t2.xlarge" -> 4;
            case "t2.2xlarge" -> 8;
            case "t3.micro", "t3.nano" -> 2;
            case "t3.small" -> 2;
            case "t3.medium" -> 2;
            case "t3.large" -> 2;
            case "t3.xlarge" -> 4;
            case "t3.2xlarge" -> 8;
            case "m5.large" -> 2;
            case "m5.xlarge" -> 4;
            case "m5.2xlarge" -> 8;
            case "m5.4xlarge" -> 16;
            case "c5.large" -> 2;
            case "c5.xlarge" -> 4;
            case "c5.2xlarge" -> 8;
            case "c5.4xlarge" -> 16;
            default -> 1;
        };
    }
    
    /**
     * 인스턴스 타입에서 메모리 크기를 추출합니다.
     */
    private Integer getMemoryGb(String instanceType) {
        double memoryGb = switch (instanceType.toLowerCase()) {
            case "t2.nano" -> 0.5;
            case "t2.micro" -> 1;
            case "t2.small" -> 2;
            case "t2.medium" -> 4;
            case "t2.large" -> 8;
            case "t2.xlarge" -> 16;
            case "t2.2xlarge" -> 32;
            case "t3.nano" -> 0.5;
            case "t3.micro" -> 1;
            case "t3.small" -> 2;
            case "t3.medium" -> 4;
            case "t3.large" -> 8;
            case "t3.xlarge" -> 16;
            case "t3.2xlarge" -> 32;
            case "m5.large" -> 8;
            case "m5.xlarge" -> 16;
            case "m5.2xlarge" -> 32;
            case "m5.4xlarge" -> 64;
            case "c5.large" -> 4;
            case "c5.xlarge" -> 8;
            case "c5.2xlarge" -> 16;
            case "c5.4xlarge" -> 32;
            default -> 1;
        };
        return (int) memoryGb;
    }
    
    /**
     * AWS 태그를 Map으로 변환합니다.
     */
    private Map<String, String> mapTags(List<Tag> awsTags) {
        if (awsTags == null || awsTags.isEmpty()) {
            return Map.of();
        }
        
        return awsTags.stream()
            .collect(Collectors.toMap(Tag::key, Tag::value));
    }
    
    /**
     * AWS Instance의 메타데이터를 구성합니다.
     */
    private String buildMetadata(Instance awsInstance) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("launchTime", awsInstance.launchTime());
        metadata.put("architecture", awsInstance.architectureAsString());
        metadata.put("platform", awsInstance.platformAsString());
        metadata.put("hypervisor", awsInstance.hypervisorAsString());
        metadata.put("virtualizationType", awsInstance.virtualizationTypeAsString());
        metadata.put("rootDeviceType", awsInstance.rootDeviceTypeAsString());
        metadata.put("monitoring", awsInstance.monitoring().stateAsString());
        
        return toJson(metadata);
    }
    
    /**
     * 객체를 JSON 문자열로 변환합니다.
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("[AwsVmMapper] Failed to convert object to JSON", e);
            return "{}";
        }
    }
}
