package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vpc;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.vpc.CreateVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.GetVpcCommand;
import com.agenticcp.core.domain.cloud.dto.ListVpcsQueryRequest;
import com.agenticcp.core.domain.cloud.port.model.vpc.UpdateVpcCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.model.Vpc;

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
    
    public AwsVpcMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Vpc를 CloudResource로 변환 (CreateVpcCommand 사용)
     * 
     * 주의: VPC 생성 직후에는 태그가 아직 VPC 객체에 반영되지 않으므로,
     * command.vpcName()을 직접 사용하여 resourceName을 설정합니다.
     */
    public CloudResource toCloudResource(Vpc vpc, CreateVpcCommand command) {
        return buildCloudResourceForCreate(vpc, command);
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
    
    /**
     * VPC 생성 시 사용하는 CloudResource 빌더
     * 
     * VPC 생성 직후에는 태그가 VPC 응답 객체에 반영되지 않으므로,
     * command.vpcName()을 직접 사용하여 resourceName을 설정합니다.
     */
    private CloudResource buildCloudResourceForCreate(Vpc vpc, CreateVpcCommand command) {
        String resourceId = vpc.vpcId();
        // command.vpcName()을 직접 사용 (VPC 생성 직후에는 태그가 반영되지 않음)
        String resourceName = (command.vpcName() != null && !command.vpcName().isEmpty()) 
                ? command.vpcName() 
                : vpc.vpcId();
        
        // 쿠버네티스 스타일: properties (Spec) 구성
        Map<String, Object> properties = new HashMap<>();
        properties.put("vpcId", vpc.vpcId());
        properties.put("cidrBlock", vpc.cidrBlock());
        properties.put("isDefault", vpc.isDefault());
        properties.put("dhcpOptionsId", vpc.dhcpOptionsId());
        properties.put("instanceTenancy", vpc.instanceTenancyAsString());
        
        // 쿠버네티스 스타일: status (Status) 구성
        Map<String, Object> status = new HashMap<>();
        status.put("state", mapStateToLifecycleState(vpc.stateAsString()));
        status.put("stateRaw", vpc.stateAsString());
        
        return CloudResource.builder()
            .resourceId(resourceId)
            .name(resourceName)
            .provider(command.providerType().name())
            .region(command.region() != null ? command.region() : "us-east-1")
            .type("NETWORK")
            .properties(toJson(properties))
            .status(toJson(status))
            .labels(toJson(command.tags() != null ? command.tags() : new HashMap<>()))
            .build();
    }

    private CloudResource buildCloudResource(
            Vpc vpc, 
            com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType providerType, 
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
        
        // 쿠버네티스 스타일: properties (Spec) 구성
        Map<String, Object> properties = new HashMap<>();
        properties.put("vpcId", vpc.vpcId());
        properties.put("cidrBlock", vpc.cidrBlock());
        properties.put("isDefault", vpc.isDefault());
        properties.put("dhcpOptionsId", vpc.dhcpOptionsId());
        properties.put("instanceTenancy", vpc.instanceTenancyAsString());
        
        // 쿠버네티스 스타일: status (Status) 구성
        Map<String, Object> status = new HashMap<>();
        status.put("state", mapStateToLifecycleState(vpc.stateAsString()));
        status.put("stateRaw", vpc.stateAsString());
        
        return CloudResource.builder()
            .resourceId(resourceId)
            .name(resourceName)
            .provider(providerType.name())
            .region(region != null ? region : "us-east-1")
            .type("NETWORK")
            .properties(toJson(properties))
            .status(toJson(status))
            .labels(toJson(tags != null ? tags : new HashMap<>()))
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
    
    private String mapStateToLifecycleState(String state) {
        if (state == null) {
            return "unknown";
        }
        return switch (state.toUpperCase()) {
            case "PENDING" -> "pending";
            case "AVAILABLE" -> "running";
            default -> "unknown";
        };
    }
    
    /**
     * Map을 JSON 문자열로 변환합니다.
     * 
     * @param map 변환할 Map
     * @return JSON 문자열 (실패 시 null)
     */
    private String toJson(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("[AwsVpcMapper] Failed to serialize to JSON: {}", e.getMessage());
            return null;
        }
    }
}
