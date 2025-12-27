package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.vm.VmCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmUpdateCommand;
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
 * CSP 중립적인 도메인 모델을 AWS SDK 요청 객체로 변환하는 역할을 합니다.
 * 
 * CSP별 차이를 흡수하는 핵심 컴포넌트입니다:
 * - 도메인 모델(CSP 중립) → AWS SDK 요청(CSP 특화)
 * - AWS SDK 응답(CSP 특화) → CloudResource(Canonical 모델)
 * 
 * @author AgenticCP Team
 * @version 2.0.0
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
            
            // 쿠버네티스 스타일: properties (Spec) 구성
            Map<String, Object> properties = new HashMap<>();
            properties.put("instanceType", awsInstance.instanceTypeAsString());
            properties.put("cpuCores", getCpuCores(awsInstance.instanceTypeAsString()));
            properties.put("memoryGb", getMemoryGb(awsInstance.instanceTypeAsString()));
            properties.put("architecture", awsInstance.architectureAsString());
            properties.put("platform", awsInstance.platformAsString());
            
            // 쿠버네티스 스타일: status (Status) 구성
            Map<String, Object> status = new HashMap<>();
            status.put("state", awsInstance.state().nameAsString().toLowerCase());
            status.put("publicIpAddress", awsInstance.publicIpAddress());
            status.put("privateIpAddress", awsInstance.privateIpAddress());
            status.put("launchTime", awsInstance.launchTime() != null ? awsInstance.launchTime().toString() : null);
            
            return CloudResource.builder()
                .resourceId(awsInstance.instanceId())
                .name(getInstanceName(awsInstance))
                .provider("AWS")
                .region(awsInstance.placement() != null ? awsInstance.placement().availabilityZone() : null)
                .type("INSTANCE")
                .properties(toJson(properties))
                .status(toJson(status))
                .labels(toJson(mapTags(awsInstance.tags())))
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
    
    // ==================== VmCreateCommand → AWS 요청 변환 ====================
    
    /**
     * VmCreateCommand(CSP 중립)를 AWS RunInstancesRequest(AWS 특화)로 변환합니다.
     * 
     * CSP 중립 필드 → AWS 특화 필드 매핑:
     * - image → imageId (AWS AMI ID)
     * - instanceSize → instanceType (AWS Instance Type)
     * - sshKey → keyName (AWS Key Pair 이름)
     * - networkSecurityId → securityGroupIds (AWS Security Group ID)
     * - zone → placement.availabilityZone
     *
     * @param command CSP 중립적인 도메인 생성 커맨드
     * @return AWS SDK RunInstancesRequest 객체
     */
    public RunInstancesRequest toRunInstancesRequest(VmCreateCommand command) {
        log.debug("[AwsVmMapper] Converting VmCreateCommand to RunInstancesRequest: image={}, instanceSize={}", 
            command.getImage(), command.getInstanceSize());
        
        RunInstancesRequest.Builder builder = RunInstancesRequest.builder()
            .imageId(command.getImage())                                      // image → imageId (AMI)
            .instanceType(InstanceType.fromValue(command.getInstanceSize()))  // instanceSize → instanceType
            .minCount(command.getMinCount())
            .maxCount(command.getMaxCount());
        
        // sshKey → keyName (AWS Key Pair)
        if (command.getSshKey() != null) {
            builder.keyName(command.getSshKey());
        }
        
        // networkSecurityId → securityGroupIds (AWS Security Group)
        if (command.getNetworkSecurityId() != null) {
            builder.securityGroupIds(command.getNetworkSecurityId());
        }
        
        // subnetId → subnetId (동일)
        if (command.getSubnetId() != null) {
            builder.subnetId(command.getSubnetId());
        }
        
        // zone → placement.availabilityZone
        if (command.getZone() != null) {
            builder.placement(Placement.builder()
                .availabilityZone(command.getZone())
                .build());
        }
        
        if (command.getUserData() != null) {
            builder.userData(command.getUserData());
        }
        
        if (command.getTags() != null && !command.getTags().isEmpty()) {
            builder.tagSpecifications(buildTagSpecifications(command.getTags()));
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
    
    // ==================== VmUpdateCommand → AWS 요청 변환 ====================
    
    /**
     * VmUpdateCommand를 AWS ModifyInstanceAttributeRequest로 변환합니다.
     */
    public ModifyInstanceAttributeRequest toModifyInstanceAttributeRequest(VmUpdateCommand command) {
        log.debug("[AwsVmMapper] Converting VmUpdateCommand to ModifyInstanceAttributeRequest");

        ModifyInstanceAttributeRequest.Builder builder = ModifyInstanceAttributeRequest.builder()
            .instanceId(command.getInstanceId());

        if (command.getInstanceType() != null) {
            builder.instanceType(AttributeValue.builder()
                .value(command.getInstanceType())
                .build());
        }
        
        if (command.getUserData() != null) {
            builder.userData(BlobAttributeValue.builder()
                .value(software.amazon.awssdk.core.SdkBytes.fromUtf8String(command.getUserData()))
                .build());
        }
        
        return builder.build();
    }
    
    // ==================== VmDeleteCommand → AWS 요청 변환 ====================
    
    /**
     * VmDeleteCommand를 AWS TerminateInstancesRequest로 변환합니다.
     */
    public TerminateInstancesRequest toTerminateInstancesRequest(VmDeleteCommand command) {
        log.debug("[AwsVmMapper] Converting VmDeleteCommand to TerminateInstancesRequest");

        return TerminateInstancesRequest.builder()
            .instanceIds(command.getInstanceId())
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
     * AWS Instance 상태를 JSON status로 매핑합니다.
     * 쿠버네티스 스타일: status 필드에 JSON으로 저장됩니다.
     */
    private String mapInstanceStateToJson(String awsState) {
        Map<String, Object> status = new HashMap<>();
        status.put("state", awsState.toLowerCase());
        return toJson(status);
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
