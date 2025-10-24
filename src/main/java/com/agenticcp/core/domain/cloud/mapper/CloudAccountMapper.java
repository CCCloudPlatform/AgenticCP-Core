package com.agenticcp.core.domain.cloud.mapper;

import com.agenticcp.core.domain.cloud.dto.CloudAccountDto;
import com.agenticcp.core.domain.cloud.dto.UpdateCloudAccountRequest;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import org.springframework.stereotype.Component;

/**
 * CloudAccount 엔티티와 DTO 간의 변환을 담당하는 매퍼
 * 
 * MapStruct를 사용하지 않고 수동으로 구현하여 의존성을 최소화합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Component
public class CloudAccountMapper {
    
    /**
     * CloudAccount 엔티티를 CloudAccountDto로 변환합니다.
     * 
     * @param cloudAccount CloudAccount 엔티티
     * @return CloudAccountDto
     */
    public CloudAccountDto toDto(CloudAccount cloudAccount) {
        if (cloudAccount == null) {
            return null;
        }
        
        return CloudAccountDto.builder()
                .id(cloudAccount.getId())
                .tenantId(cloudAccount.getTenant() != null ? cloudAccount.getTenant().getId() : null)
                .providerId(cloudAccount.getProvider() != null ? cloudAccount.getProvider().getId() : null)
                .providerType(cloudAccount.getProvider() != null ? cloudAccount.getProvider().getProviderType().toString() : null)
                .accountId(cloudAccount.getAccountId())
                .accountName(cloudAccount.getAccountName())
                .description(cloudAccount.getDescription())
                .authMethod(cloudAccount.getAuthMethod())
                .defaultRegion(cloudAccount.getDefaultRegion())
                .status(cloudAccount.getStatus())
                .isDefault(cloudAccount.getIsDefault())
                .lastVerified(cloudAccount.getLastVerified())
                .createdAt(cloudAccount.getCreatedAt())
                .updatedAt(cloudAccount.getUpdatedAt())
                .build();
    }
    
    /**
     * UpdateCloudAccountRequest를 CloudAccount 엔티티로 변환합니다.
     * (부분 업데이트용)
     * 
     * @param request 업데이트 요청 DTO
     * @return CloudAccount 엔티티 (ID는 설정하지 않음)
     */
    public CloudAccount toEntity(UpdateCloudAccountRequest request) {
        if (request == null) {
            return null;
        }
        
        return CloudAccount.builder()
                .accountName(request.getAccountName())
                .description(request.getDescription())
                .defaultRegion(request.getDefaultRegion())
                .isDefault(request.getIsDefault())
                .build();
    }
    
    /**
     * UpdateCloudAccountRequest의 필드를 기존 CloudAccount 엔티티에 적용합니다.
     * 
     * @param existingAccount 기존 CloudAccount 엔티티
     * @param request 업데이트 요청 DTO
     */
    public void updateEntity(CloudAccount existingAccount, UpdateCloudAccountRequest request) {
        if (existingAccount == null || request == null) {
            return;
        }
        
        if (request.getAccountName() != null) {
            existingAccount.setAccountName(request.getAccountName());
        }
        
        if (request.getDescription() != null) {
            existingAccount.setDescription(request.getDescription());
        }
        
        if (request.getDefaultRegion() != null) {
            existingAccount.setDefaultRegion(request.getDefaultRegion());
        }
        
        if (request.getIsDefault() != null) {
            existingAccount.setIsDefault(request.getIsDefault());
        }
    }
}
