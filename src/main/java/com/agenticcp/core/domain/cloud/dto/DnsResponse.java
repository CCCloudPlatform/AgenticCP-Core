package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DNS 호스팅 존 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnsResponse {

    private Long id;
    private String resourceId;
    private String resourceName;  // zone name (example.com)
    private ProviderType providerType;
    private String region;
    private String zoneType;  // "PUBLIC", "PRIVATE"
    private String vpcId;  // Private Zone인 경우 VPC ID
    private String nameServers;  // 네임서버 목록 (JSON 배열 문자열)
    private String status;
    private String lifecycleState;
    private String comment;  // 호스팅 존 설명
    private Map<String, String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime lastSync;

    /**
     * CloudResource → DnsResponse 변환
     */
    public static DnsResponse from(CloudResource resource) {
        ObjectMapper objectMapper = new ObjectMapper();

        // configuration에서 추가 정보 추출
        Map<String, Object> config = parseConfiguration(resource.getConfiguration(), objectMapper);
        String zoneType = extractString(config, "zoneType");
        String vpcId = extractString(config, "vpcId");
        String comment = extractString(config, "comment");

        return DnsResponse.builder()
                .id(resource.getId())
                .resourceId(resource.getResourceId())
                .resourceName(resource.getResourceName())
                .providerType(resource.getProvider().getProviderType())
                .region(resource.getRegion() != null ? resource.getRegion().getRegionKey() : null)
                .zoneType(zoneType)
                .vpcId(vpcId)
                .status(resource.getStatus() != null ? resource.getStatus().name() : null)
                .lifecycleState(resource.getLifecycleState() != null ? resource.getLifecycleState().name() : null)
                .comment(comment)
                .tags(resource.getTags() != null ? resource.getTags() : new HashMap<>())
                .createdAt(resource.getCreatedAt())
                .lastSync(resource.getLastSync())
                .build();
    }

    private static Map<String, Object> parseConfiguration(String configJson, ObjectMapper objectMapper) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
