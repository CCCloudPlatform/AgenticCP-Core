package com.agenticcp.core.domain.platform.enums;

import lombok.Getter;

/**
 * 승인 상태 열거형
 * 
 * 기능 플래그 변경 승인 요청의 상태를 나타냅니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Getter
public enum ApprovalStatus {

    /**
     * 대기 중
     * 승인 요청이 생성되었으나 아직 승인/거부되지 않은 상태
     */
    PENDING("대기중"),

    /**
     * 승인됨
     * 승인자가 승인한 상태
     */
    APPROVED("승인됨"),

    /**
     * 거부됨
     * 승인자가 거부한 상태
     */
    REJECTED("거부됨"),

    /**
     * 취소됨
     * 요청자가 승인 요청을 취소한 상태
     */
    CANCELLED("취소됨");

    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }
}

