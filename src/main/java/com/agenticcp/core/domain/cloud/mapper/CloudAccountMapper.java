package com.agenticcp.core.domain.cloud.mapper;

import com.agenticcp.core.domain.cloud.dto.CloudAccountDto;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudAccountCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CloudAccount Entity와 DTO 간의 변환을 담당하는 매퍼
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public class CloudAccountMapper {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * CloudAccount Entity를 CloudAccountDto로 변환합니다.
     * 
     * @param entity CloudAccount 엔티티
     * @return CloudAccountDto
     */
    public static CloudAccountDto toDto(CloudAccount entity) {
        if (entity == null) {
            return null;
        }
        
        // metadata JSON 파싱하여 region 추출
        String region = extractRegionFromMetadata(entity);
        
        return CloudAccountDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .tenantKey(entity.getTenant() != null ? entity.getTenant().getTenantKey() : null)
                .providerId(entity.getProvider() != null ? entity.getProvider().getId() : null)
                .providerType(entity.getProvider() != null ? entity.getProvider().getProviderType() : null)
                .providerName(entity.getProvider() != null ? entity.getProvider().getProviderName() : null)
                .accountName(entity.getAccountName())
                .accountScope(entity.getAccountScope())
                .accountStatus(entity.getAccountStatus())
                .isDefault(entity.getIsDefault())
                .region(region)
                .verifiedAt(entity.getVerifiedAt())
                .lastSyncAt(entity.getLastSyncAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
    
    /**
     * CloudAccount Entity 리스트를 CloudAccountDto 리스트로 변환합니다.
     * 
     * @param entities CloudAccount 엔티티 리스트
     * @return CloudAccountDto 리스트
     */
    public static List<CloudAccountDto> toDtoList(List<CloudAccount> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(CloudAccountMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * CloudAccount 엔티티의 metadata에서 region 정보를 추출합니다.
     * credential이나 metadata에서 region을 찾습니다.
     * 
     * @param entity CloudAccount 엔티티
     * @return region 문자열
     */
    private static String extractRegionFromMetadata(CloudAccount entity) {
        // 1. credential에서 region 가져오기
        CloudAccountCredential credential = entity.getCredential();
        if (credential != null && credential.getRegion() != null) {
            return credential.getRegion();
        }
        
        // 2. metadata JSON에서 region 파싱
        if (entity.getMetadata() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = objectMapper.readValue(entity.getMetadata(), Map.class);
                Object regionObj = metadata.get("region");
                if (regionObj != null) {
                    return regionObj.toString();
                }
            } catch (JsonProcessingException e) {
                // JSON 파싱 실패 시 무시
            }
        }
        
        return null;
    }
    
    /**
     * Map 형태의 metadata를 JSON 문자열로 변환합니다.
     * 
     * @param metadata 메타데이터 맵
     * @return JSON 문자열
     */
    public static String metadataToJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * JSON 문자열을 Map 형태의 metadata로 변환합니다.
     * 
     * @param json JSON 문자열
     * @return 메타데이터 맵
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> jsonToMetadata(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
}

