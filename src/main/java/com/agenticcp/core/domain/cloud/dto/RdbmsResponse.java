package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RDBMS 응답 DTO (CSP 중립적)
 * 
 * RDBMS 인스턴스 정보를 반환하는 응답 객체입니다.
 * CloudResource 엔티티에서 변환됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RdbmsResponse {
    
    private Long id;
    private String resourceId;
    private String resourceName;
    private CloudProvider.ProviderType providerType;
    private String region;
    private String engine;
    private String engineVersion;
    private String instanceSize;  // CSP 중립적: AWS(db.t3.micro), Azure(GP_Gen5_2), GCP(db-custom-2-7680)
    private Long storageGb;
    private String status;
    private String lifecycleState;
    private String endpoint;  // 데이터베이스 엔드포인트 주소
    private Integer port;     // 데이터베이스 포트
    private Boolean highAvailability;  // 고가용성 설정 여부
    private Boolean publiclyAccessible;  // 공개 접근 허용 여부
    private Map<String, String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime lastSync;
    
    /**
     * CloudResource → RdbmsResponse 변환
     * 
     * @param resource CloudResource 엔티티
     * @return RdbmsResponse DTO
     */
    public static RdbmsResponse from(CloudResource resource) {
        if (resource == null) {
            return null;
        }
        
        try {
            // configuration JSON에서 추가 정보 추출
            Map<String, Object> config = parseConfiguration(resource.getConfiguration());
            
            RdbmsResponse.RdbmsResponseBuilder builder = RdbmsResponse.builder()
                .id(resource.getId())
                .resourceId(resource.getResourceId())
                .resourceName(resource.getResourceName())
                .providerType(resource.getProvider() != null 
                    ? resource.getProvider().getProviderType() 
                    : null)
                .region(resource.getRegion() != null 
                    ? resource.getRegion().getRegionKey() 
                    : null)
                .instanceSize(resource.getInstanceSize())
                .storageGb(resource.getStorageGb())
                .status(resource.getStatus() != null 
                    ? resource.getStatus().name() 
                    : null)
                .lifecycleState(resource.getLifecycleState() != null 
                    ? resource.getLifecycleState().name() 
                    : null)
                .tags(parseTags(resource.getTags()))
                .createdAt(resource.getCreatedAt())
                .lastSync(resource.getLastSync());
            
            // configuration에서 엔진 정보 추출
            if (config != null) {
                builder.engine((String) config.get("engine"));
                builder.engineVersion((String) config.get("engineVersion"));
                
                // endpoint 정보 추출
                Object endpointObj = config.get("endpoint");
                if (endpointObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> endpoint = (Map<String, Object>) endpointObj;
                    builder.endpoint((String) endpoint.get("address"));
                    Object portObj = endpoint.get("port");
                    if (portObj instanceof Number) {
                        builder.port(((Number) portObj).intValue());
                    }
                } else if (resource.getIpAddress() != null) {
                    // endpoint가 없으면 ipAddress 사용
                    builder.endpoint(resource.getIpAddress());
                }
                
                // highAvailability (multiAz)
                Object multiAz = config.get("multiAz");
                if (multiAz instanceof Boolean) {
                    builder.highAvailability((Boolean) multiAz);
                }
                
                // publiclyAccessible
                Object publiclyAccessible = config.get("publiclyAccessible");
                if (publiclyAccessible instanceof Boolean) {
                    builder.publiclyAccessible((Boolean) publiclyAccessible);
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("[RdbmsResponse] Failed to convert CloudResource to RdbmsResponse: {}", 
                resource.getResourceId(), e);
            // 기본 정보만 반환
            return RdbmsResponse.builder()
                .id(resource.getId())
                .resourceId(resource.getResourceId())
                .resourceName(resource.getResourceName())
                .providerType(resource.getProvider() != null 
                    ? resource.getProvider().getProviderType() 
                    : null)
                .region(resource.getRegion() != null 
                    ? resource.getRegion().getRegionKey() 
                    : null)
                .instanceSize(resource.getInstanceSize())
                .storageGb(resource.getStorageGb())
                .status(resource.getStatus() != null 
                    ? resource.getStatus().name() 
                    : null)
                .lifecycleState(resource.getLifecycleState() != null 
                    ? resource.getLifecycleState().name() 
                    : null)
                .tags(parseTags(resource.getTags()))
                .createdAt(resource.getCreatedAt())
                .lastSync(resource.getLastSync())
                .build();
        }
    }
    
    /**
     * CloudResource 목록을 RdbmsResponse 목록으로 변환
     * 
     * @param resources CloudResource 목록
     * @return RdbmsResponse 목록
     */
    public static List<RdbmsResponse> fromList(List<CloudResource> resources) {
        if (resources == null) {
            return List.of();
        }
        return resources.stream()
            .map(RdbmsResponse::from)
            .collect(Collectors.toList());
    }
    
    /**
     * configuration JSON 문자열을 Map으로 파싱
     */
    private static Map<String, Object> parseConfiguration(String configurationJson) {
        if (configurationJson == null || configurationJson.isEmpty() || configurationJson.equals("{}")) {
            return new HashMap<>();
        }
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(configurationJson, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("[RdbmsResponse] Failed to parse configuration JSON: {}", configurationJson, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 태그를 Map으로 변환
     */
    private static Map<String, String> parseTags(Map<String, String> tags) {
        return tags != null ? tags : Map.of();
    }
}
