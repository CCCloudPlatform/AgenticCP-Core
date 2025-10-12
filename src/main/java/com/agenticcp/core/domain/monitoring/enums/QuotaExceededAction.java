package com.agenticcp.core.domain.monitoring.enums;

import lombok.Getter;

/**
 * 할당량 초과 시 동작 열거형
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
public enum QuotaExceededAction {
    
    /**
     * 수집 차단 - 메트릭 수집을 완전히 차단
     */
    BLOCK_COLLECTION("수집 차단", "메트릭 수집을 완전히 차단합니다."),
    
    /**
     * 수집 속도 제한 - 수집 속도를 50% 감소
     */
    THROTTLE_COLLECTION("수집 속도 제한", "수집 속도를 50% 감소시킵니다."),
    
    /**
     * 경고만 표시 - 경고만 표시하고 계속 수집
     */
    WARN_ONLY("경고만 표시", "경고만 표시하고 계속 수집합니다."),
    
    /**
     * 자동 업그레이드 - 자동으로 상위 플랜으로 업그레이드
     */
    AUTO_UPGRADE("자동 업그레이드", "자동으로 상위 플랜으로 업그레이드합니다.");
    
    private final String displayName;
    private final String description;
    
    QuotaExceededAction(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * 할당량 초과 시 동작이 차단인지 확인
     */
    public boolean isBlocking() {
        return this == BLOCK_COLLECTION;
    }
    
    /**
     * 할당량 초과 시 동작이 제한인지 확인
     */
    public boolean isThrottling() {
        return this == THROTTLE_COLLECTION;
    }
    
    /**
     * 할당량 초과 시 동작이 경고인지 확인
     */
    public boolean isWarning() {
        return this == WARN_ONLY;
    }
    
    /**
     * 할당량 초과 시 동작이 업그레이드인지 확인
     */
    public boolean isUpgrade() {
        return this == AUTO_UPGRADE;
    }
}
