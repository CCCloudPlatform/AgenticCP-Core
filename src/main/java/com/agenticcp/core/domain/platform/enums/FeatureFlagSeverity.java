package com.agenticcp.core.domain.platform.enums;

import com.agenticcp.core.common.enums.AuditSeverity;
import lombok.Getter;

/**
 * 기능 플래그 심각도 레벨
 * 
 * 기능 플래그 변경의 중요도와 위험도를 나타냅니다.
 * 심각도에 따라 승인 워크플로우가 적용됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Getter
public enum FeatureFlagSeverity {

    /**
     * 낮은 심각도
     * - 일반적인 기능 플래그 변경
     * - 승인 불필요
     */
    LOW("낮음", 1),

    /**
     * 중간 심각도
     * - 일반적인 기능 플래그 변경
     * - 승인 불필요
     */
    MEDIUM("보통", 2),

    /**
     * 높은 심각도
     * - 중요한 기능 플래그 변경
     * - 승인 필요
     */
    HIGH("높음", 3),

    /**
     * 매우 높은 심각도
     * - 매우 중요한 기능 플래그 변경
     * - 승인 필요
     */
    CRITICAL("매우높음", 4);

    private final String description;
    private final int level;

    FeatureFlagSeverity(String description, int level) {
        this.description = description;
        this.level = level;
    }

    /**
     * AuditSeverity로 변환
     * 
     * @return 해당하는 AuditSeverity
     */
    public AuditSeverity toAuditSeverity() {
        return switch (this) {
            case LOW -> AuditSeverity.LOW;
            case MEDIUM -> AuditSeverity.MEDIUM;
            case HIGH -> AuditSeverity.HIGH;
            case CRITICAL -> AuditSeverity.CRITICAL;
        };
    }

    /**
     * 다른 심각도와 비교하여 최소한의 레벨인지 확인
     * 
     * @param other 비교할 심각도
     * @return this.level >= other.level
     */
    public boolean isAtLeast(FeatureFlagSeverity other) {
        return this.level >= other.level;
    }
}

