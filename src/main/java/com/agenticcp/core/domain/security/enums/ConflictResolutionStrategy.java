package com.agenticcp.core.domain.security.enums;

import lombok.Getter;

/**
 * 정책 충돌 해결 전략
 * 여러 정책이 동일한 리소스에 적용될 때 충돌을 해결하는 방법을 정의
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Getter
public enum ConflictResolutionStrategy {
    
    /**
     * 거부 우선 (DENY_OVERRIDES)
     * 하나라도 DENY가 있으면 최종 결과는 DENY
     * 보안에 가장 엄격한 전략
     */
    DENY_OVERRIDES(
        "거부 우선",
        "하나라도 거부 정책이 있으면 최종 결과는 거부입니다.",
        1
    ),
    
    /**
     * 허용 우선 (ALLOW_OVERRIDES)
     * 하나라도 ALLOW가 있으면 최종 결과는 ALLOW
     * 접근성을 우선하는 전략
     */
    ALLOW_OVERRIDES(
        "허용 우선",
        "하나라도 허용 정책이 있으면 최종 결과는 허용입니다.",
        2
    ),
    
    /**
     * 첫 번째 일치 (FIRST_MATCH)
     * 우선순위가 가장 높은(숫자가 큰) 정책의 결과를 반환
     * 명시적 우선순위 기반 전략
     */
    FIRST_MATCH(
        "첫 번째 일치",
        "우선순위가 가장 높은 정책의 결과를 적용합니다.",
        3
    ),
    
    /**
     * 최고 우선순위 (HIGHEST_PRIORITY)
     * 우선순위 값이 가장 높은(숫자가 큰) 정책을 선택
     * FIRST_MATCH와 동일하지만 명시적으로 우선순위를 강조
     */
    HIGHEST_PRIORITY(
        "최고 우선순위",
        "우선순위 값이 가장 높은 정책을 선택합니다.",
        4
    ),
    
    /**
     * 최저 우선순위 (LOWEST_PRIORITY)
     * 우선순위 값이 가장 낮은(숫자가 작은) 정책을 선택
     * 기본 정책이나 폴백 정책에 유용
     */
    LOWEST_PRIORITY(
        "최저 우선순위",
        "우선순위 값이 가장 낮은 정책을 선택합니다.",
        5
    ),
    
    /**
     * 가장 엄격한 정책 (MOST_RESTRICTIVE)
     * 여러 정책 중 가장 제한적인 정책을 선택
     * 보안 강화를 위한 전략
     */
    MOST_RESTRICTIVE(
        "가장 엄격한 정책",
        "여러 정책 중 가장 제한적인 정책을 적용합니다.",
        6
    ),
    
    /**
     * 가장 관대한 정책 (MOST_PERMISSIVE)
     * 여러 정책 중 가장 허용적인 정책을 선택
     * 사용자 편의성을 위한 전략
     */
    MOST_PERMISSIVE(
        "가장 관대한 정책",
        "여러 정책 중 가장 허용적인 정책을 적용합니다.",
        7
    ),
    
    /**
     * 다수결 (MAJORITY_WINS)
     * ALLOW와 DENY의 개수를 비교하여 더 많은 쪽을 선택
     * 동수일 경우 DENY 선택 (보안 우선)
     */
    MAJORITY_WINS(
        "다수결",
        "허용과 거부 정책의 개수를 비교하여 더 많은 쪽을 적용합니다.",
        8
    ),
    
    /**
     * 가중치 합산 (WEIGHTED_SUM)
     * 각 정책의 우선순위를 가중치로 사용하여 합산
     * 우선순위가 높은 정책이 더 큰 영향을 미침
     */
    WEIGHTED_SUM(
        "가중치 합산",
        "각 정책의 우선순위를 가중치로 사용하여 최종 결과를 계산합니다.",
        9
    );
    
    private final String displayName;
    private final String description;
    private final int order;
    
    ConflictResolutionStrategy(String displayName, String description, int order) {
        this.displayName = displayName;
        this.description = description;
        this.order = order;
    }
    
    /**
     * 기본 전략 반환
     * @return 기본 충돌 해결 전략 (DENY_OVERRIDES)
     */
    public static ConflictResolutionStrategy getDefault() {
        return DENY_OVERRIDES;
    }
    
    /**
     * 문자열로부터 전략 찾기 (대소문자 무시)
     * @param strategy 전략 이름
     * @return ConflictResolutionStrategy 또는 null
     */
    public static ConflictResolutionStrategy fromString(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            return null;
        }
        
        try {
            return ConflictResolutionStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 보안 우선 전략 여부 확인
     * @return true if 보안을 우선하는 전략
     */
    public boolean isSecurityFirst() {
        return this == DENY_OVERRIDES || 
               this == MOST_RESTRICTIVE;
    }
    
    /**
     * 우선순위 기반 전략 여부 확인
     * @return true if 우선순위를 기반으로 하는 전략
     */
    public boolean isPriorityBased() {
        return this == FIRST_MATCH || 
               this == HIGHEST_PRIORITY || 
               this == LOWEST_PRIORITY ||
               this == WEIGHTED_SUM;
    }
    
    /**
     * 다중 정책 평가가 필요한 전략 여부 확인
     * @return true if 모든 정책을 평가해야 하는 전략
     */
    public boolean requiresAllPolicies() {
        return this == DENY_OVERRIDES || 
               this == ALLOW_OVERRIDES || 
               this == MAJORITY_WINS ||
               this == WEIGHTED_SUM ||
               this == MOST_RESTRICTIVE ||
               this == MOST_PERMISSIVE;
    }
}

