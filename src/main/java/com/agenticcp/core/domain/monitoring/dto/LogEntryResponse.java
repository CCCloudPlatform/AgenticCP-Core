package com.agenticcp.core.domain.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 로그 엔트리 응답 DTO
 * 
 * @author AgenticCP Team
 * @since 2025-10-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntryResponse {
    
    /**
     * 로그 ID
     */
    private Long id;
    
    /**
     * 로그 레벨 (INFO, WARNING, ERROR, CRITICAL)
     */
    private String level;
    
    /**
     * 로그 타입 (success, failure, info, alert)
     */
    private String type;
    
    /**
     * 로그 소스 (monitoring, system, application, alert)
     */
    private String source;
    
    /**
     * 로그 메시지
     */
    private String message;
    
    /**
     * 서비스명
     */
    private String service;
    
    /**
     * 컴포넌트명
     */
    private String component;
    
    /**
     * 로그 생성 시간
     */
    private LocalDateTime timestamp;
    
    /**
     * 추가 메타데이터
     */
    private Map<String, Object> metadata;
    
    /**
     * 테넌트 ID
     */
    private String tenantId;
}
