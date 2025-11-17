package com.agenticcp.core.domain.security.mapper;

import com.agenticcp.core.domain.security.dto.PolicyViolationDTO;
import com.agenticcp.core.domain.security.entity.PolicyViolation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PolicyViolation Entity <-> DTO 변환 유틸리티
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class PolicyViolationMapper {

    /**
     * PolicyViolation Entity -> PolicyViolationDTO 변환
     */
    public static PolicyViolationDTO toDTO(PolicyViolation entity) {
        if (entity == null) {
            return null;
        }

        return PolicyViolationDTO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .policyId(entity.getPolicyId())
                .policyName(entity.getPolicyName())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .violationType(entity.getViolationType())
                .severity(entity.getSeverity())
                .description(entity.getDescription())
                .detectedAt(entity.getDetectedAt())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .actionAttempted(entity.getActionAttempted())
                .violationDetails(entity.getViolationDetails())
                .status(entity.getStatus())
                .autoResponseExecuted(entity.getAutoResponseExecuted())
                .responseAction(entity.getResponseAction())
                .responseExecutedAt(entity.getResponseExecutedAt())
                .notificationSent(entity.getNotificationSent())
                .notificationSentAt(entity.getNotificationSentAt())
                .resolvedAt(entity.getResolvedAt())
                .resolvedBy(entity.getResolvedBy())
                .resolutionNotes(entity.getResolutionNotes())
                .falsePositive(entity.getFalsePositive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * PolicyViolation List -> PolicyViolationDTO List 변환
     */
    public static List<PolicyViolationDTO> toDTOList(List<PolicyViolation> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(PolicyViolationMapper::toDTO)
                .collect(Collectors.toList());
    }
}

