package com.agenticcp.core.domain.platform.dto;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 타겟팅 규칙 응답 DTO
 * 
 * 타겟팅 규칙 정보를 클라이언트에게 반환할 때 사용되는 응답 데이터입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetRuleResponseDto {

    private Long id;
    private String flagKey;
    private String ruleName;
    private String description;
    private Status status;
    private Integer priority;
    private Boolean isEnabled;
    
    // 타겟팅 기준
    private String cloudProvider;
    private String region;
    private String tenantType;
    private String tenantGrade;
    private String userRole;
    private String userAttributes;
    private String customAttributes;
    
    // 롤아웃 설정
    private Integer rolloutPercentage;
    private String rolloutStrategy;
    
    // 시간 기반 타겟팅
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // 추가 설정
    private String metadata;
    
    // 시스템 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long tenantId;

    /**
     * Entity를 DTO로 변환하는 정적 메서드
     * 
     * @param targetRule 타겟팅 규칙 엔티티
     * @return 타겟팅 규칙 응답 DTO
     */
    public static TargetRuleResponseDto from(FeatureFlagTargetRule targetRule) {
        return TargetRuleResponseDto.builder()
                .id(targetRule.getId())
                .flagKey(targetRule.getFlagKey())
                .ruleName(targetRule.getRuleName())
                .description(targetRule.getDescription())
                .status(targetRule.getStatus())
                .priority(targetRule.getPriority())
                .isEnabled(targetRule.getIsEnabled())
                .cloudProvider(targetRule.getCloudProvider())
                .region(targetRule.getRegion())
                .tenantType(targetRule.getTenantType())
                .tenantGrade(targetRule.getTenantGrade())
                .userRole(targetRule.getUserRole())
                .userAttributes(targetRule.getUserAttributes())
                .customAttributes(targetRule.getCustomAttributes())
                .rolloutPercentage(targetRule.getRolloutPercentage())
                .rolloutStrategy(targetRule.getRolloutStrategy())
                .startDate(targetRule.getStartDate())
                .endDate(targetRule.getEndDate())
                .metadata(targetRule.getMetadata())
                .createdAt(targetRule.getCreatedAt())
                .updatedAt(targetRule.getUpdatedAt())
                .createdBy(targetRule.getCreatedBy())
                .updatedBy(targetRule.getUpdatedBy())
                .tenantId(targetRule.getTenantId())
                .build();
    }

    /**
     * Entity 리스트를 DTO 리스트로 변환하는 정적 메서드
     * 
     * @param targetRules 타겟팅 규칙 엔티티 리스트
     * @return 타겟팅 규칙 응답 DTO 리스트
     */
    public static List<TargetRuleResponseDto> from(List<FeatureFlagTargetRule> targetRules) {
        return targetRules.stream()
                .map(TargetRuleResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 현재 활성 상태인지 확인하는 메서드
     * 
     * @return 활성 상태 여부
     */
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        boolean timeActive = true;
        
        if (startDate != null && now.isBefore(startDate)) {
            timeActive = false;
        }
        if (endDate != null && now.isAfter(endDate)) {
            timeActive = false;
        }
        
        return Status.ACTIVE.equals(status) && Boolean.TRUE.equals(isEnabled) && timeActive;
    }

    /**
     * 시간 기반 타겟팅인지 확인하는 메서드
     * 
     * @return 시간 기반 타겟팅 여부
     */
    public boolean isTimeBased() {
        return startDate != null || endDate != null;
    }
}
