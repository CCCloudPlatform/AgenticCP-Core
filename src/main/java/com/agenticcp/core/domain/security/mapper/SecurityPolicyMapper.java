package com.agenticcp.core.domain.security.mapper;

import com.agenticcp.core.domain.security.dto.SecurityPolicyDTO;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SecurityPolicy Entity <-> DTO 변환 유틸리티
 * 
 * <p>SecurityPolicy 엔티티와 SecurityPolicyDTO 간의 변환을 담당합니다.</p>
 * <p>Lazy Loading 문제 해결을 위해 tenant는 tenantId만 포함합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class SecurityPolicyMapper {
    
    /**
     * SecurityPolicy Entity -> SecurityPolicyDTO 변환
     * 
     * @param entity SecurityPolicy 엔티티
     * @return SecurityPolicyDTO
     */
    public static SecurityPolicyDTO toDTO(SecurityPolicy entity) {
        if (entity == null) {
            return null;
        }
        
        return SecurityPolicyDTO.builder()
                .id(entity.getId())
                .policyKey(entity.getPolicyKey())
                .policyName(entity.getPolicyName())
                .description(entity.getDescription())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .status(entity.getStatus())
                .policyType(entity.getPolicyType())
                .severity(entity.getSeverity())
                .isGlobal(entity.getIsGlobal())
                .isSystem(entity.getIsSystem())
                .isEnabled(entity.getIsEnabled())
                .rules(entity.getRules())
                .conditions(entity.getConditions())
                .actions(entity.getActions())
                .targetResources(entity.getTargetResources())
                .exceptions(entity.getExceptions())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveUntil(entity.getEffectiveUntil())
                .priority(entity.getPriority())
                .version(entity.getVersion())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .isDeleted(entity.getIsDeleted())
                .build();
    }
    
    /**
     * SecurityPolicy Entity 리스트 -> SecurityPolicyDTO 리스트 변환
     * 
     * @param entities SecurityPolicy 엔티티 리스트
     * @return SecurityPolicyDTO 리스트
     */
    public static List<SecurityPolicyDTO> toDTOList(List<SecurityPolicy> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(SecurityPolicyMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * SecurityPolicyDTO -> SecurityPolicy Entity 변환
     * 
     * <p>주의: 이 메서드는 DTO의 데이터로만 엔티티를 생성하며,
     * id와 tenant 관계, 감사 필드는 별도로 설정해야 합니다.</p>
     * <p>주로 새 엔티티 생성 시 사용됩니다.</p>
     * 
     * @param dto SecurityPolicyDTO
     * @return SecurityPolicy 엔티티
     */
    public static SecurityPolicy toEntity(SecurityPolicyDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return SecurityPolicy.builder()
                // id는 자동 생성되므로 제외
                .policyKey(dto.getPolicyKey())
                .policyName(dto.getPolicyName())
                .description(dto.getDescription())
                // tenant는 별도로 설정 필요
                .status(dto.getStatus())
                .policyType(dto.getPolicyType())
                .severity(dto.getSeverity())
                .isGlobal(dto.getIsGlobal())
                .isSystem(dto.getIsSystem())
                .isEnabled(dto.getIsEnabled())
                .rules(dto.getRules())
                .conditions(dto.getConditions())
                .actions(dto.getActions())
                .targetResources(dto.getTargetResources())
                .exceptions(dto.getExceptions())
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveUntil(dto.getEffectiveUntil())
                .priority(dto.getPriority())
                .version(dto.getVersion())
                .metadata(dto.getMetadata())
                // createdAt, createdBy, updatedAt, updatedBy, isDeleted는 BaseEntity에서 자동 관리
                .build();
    }
}

