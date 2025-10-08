package com.agenticcp.core.common.enums;

import lombok.Getter;

/**
 * 감사 로깅 리소스 타입
 * 
 * 시스템에서 관리하는 주요 리소스 타입을 정의합니다.
 * 각 리소스 타입은 고유한 감사 로깅 정책을 가질 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Getter
public enum AuditResourceType {

    USER("사용자"),
    TENANT("테넌트"),
    POLICY("보안정책"),
    ROLE("역할"),
    PERMISSION("권한"),
    CONFIG("시스템설정"),
    FEATURE_FLAG("기능플래그"),
    CLOUD_PROVIDER("클라우드프로바이더"),
    TARGETING_RULE("타겟팅규칙"),
    PLATFORM_CONFIG("플랫폼설정");
    
    private final String description;
    
    AuditResourceType(String description) {
        this.description = description;
    }

}
