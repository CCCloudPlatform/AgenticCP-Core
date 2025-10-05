package com.agenticcp.core.common.enums;

import lombok.Getter;

/**
 * 감사 로깅 심각도 레벨
 * 
 * 각 감사 이벤트의 중요도와 위험도를 나타냅니다.
 * 모니터링 및 알림 정책 수립에 활용됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Getter
public enum AuditSeverity {

    LOW("낮음", 1),
    MEDIUM("보통", 2),
    HIGH("높음", 3),
    CRITICAL("매우높음", 4),
    INFO("정보", 0);
    
    private final String description;
    private final int level;
    
    AuditSeverity(String description, int level) {
        this.description = description;
        this.level = level;
    }

    public boolean isAtLeast(AuditSeverity other) {
        return this.level >= other.level;
    }
}
