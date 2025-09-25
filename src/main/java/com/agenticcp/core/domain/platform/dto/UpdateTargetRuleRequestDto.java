package com.agenticcp.core.domain.platform.dto;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.enums.PlatformErrorCode;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 타겟팅 규칙 수정 요청 DTO
 * 
 * 기존 타겟팅 규칙을 수정할 때 사용되는 요청 데이터입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UpdateTargetRuleRequestDto {

    @Size(max = 200, message = "규칙명은 200자를 초과할 수 없습니다")
    private String ruleName;

    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    private String description;

    @Min(value = 0, message = "우선순위는 0 이상이어야 합니다")
    @Max(value = 999, message = "우선순위는 999 이하여야 합니다")
    private Integer priority;

    private Boolean isEnabled;

    // 타겟팅 기준
    @Size(max = 50, message = "클라우드 프로바이더는 50자를 초과할 수 없습니다")
    private String cloudProvider;

    @Size(max = 100, message = "리전은 100자를 초과할 수 없습니다")
    private String region;

    @Size(max = 50, message = "테넌트 타입은 50자를 초과할 수 없습니다")
    private String tenantType;

    @Size(max = 50, message = "테넌트 등급은 50자를 초과할 수 없습니다")
    private String tenantGrade;

    @Size(max = 50, message = "사용자 역할은 50자를 초과할 수 없습니다")
    private String userRole;

    @Size(max = 2000, message = "사용자 속성은 2000자를 초과할 수 없습니다")
    private String userAttributes; // JSON for user-specific attributes

    @Size(max = 2000, message = "커스텀 속성은 2000자를 초과할 수 없습니다")
    private String customAttributes; // JSON for custom targeting attributes

    // 롤아웃 설정
    @Min(value = 0, message = "롤아웃 비율은 0% 이상이어야 합니다")
    @Max(value = 100, message = "롤아웃 비율은 100% 이하여야 합니다")
    private Integer rolloutPercentage;

    @Size(max = 50, message = "롤아웃 전략은 50자를 초과할 수 없습니다")
    private String rolloutStrategy;

    // 시간 기반 타겟팅
    private LocalDateTime startDate;

    @Future(message = "종료일은 미래 날짜여야 합니다")
    private LocalDateTime endDate;

    // 추가 설정
    @Size(max = 2000, message = "메타데이터는 2000자를 초과할 수 없습니다")
    private String metadata; // JSON for additional configuration

    // 비즈니스 검증 메서드
    public void validateBusinessRules() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(PlatformErrorCode.TARGET_RULE_INVALID_DATE_RANGE);
        }
        
        if (rolloutPercentage != null && (rolloutPercentage < 0 || rolloutPercentage > 100)) {
            throw new BusinessException(PlatformErrorCode.TARGET_RULE_INVALID_ROLLOUT_PERCENTAGE);
        }
        
        if (priority != null && (priority < 0 || priority > 999)) {
            throw new BusinessException(PlatformErrorCode.TARGET_RULE_INVALID_PRIORITY);
        }
    }
}
