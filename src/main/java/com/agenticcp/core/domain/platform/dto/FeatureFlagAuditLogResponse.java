package com.agenticcp.core.domain.platform.dto;

import com.agenticcp.core.common.enums.AuditSeverity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 기능 플래그 감사 로그 응답 DTO
 * 
 * 기능 플래그 변경에 대한 감사 로그 정보를 담습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureFlagAuditLogResponse {

    /**
     * 감사 로그 ID
     */
    private Long id;

    /**
     * 기능 플래그 키
     */
    private String flagKey;

    /**
     * 액션 (예: "UPDATE_FLAG", "TOGGLE_FLAG", "CREATE_FLAG", "DELETE_FLAG")
     */
    private String action;

    /**
     * 심각도
     */
    private AuditSeverity severity;

    /**
     * 타임스탬프
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    /**
     * 요청 ID
     */
    private String requestId;

    /**
     * 테넌트 ID
     */
    private String tenantId;

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 클라이언트 IP
     */
    private String clientIp;

    /**
     * 성공 여부
     */
    private Boolean success;

    /**
     * 에러 메시지
     */
    private String error;

    /**
     * 변경 전 값 (JSON)
     */
    private Map<String, Object> oldValue;

    /**
     * 변경 후 값 (JSON)
     */
    private Map<String, Object> newValue;

    /**
     * 변경 상세 정보 (JSON)
     */
    private Map<String, Object> changeDetails;

    /**
     * 메타데이터 (JSON)
     */
    private Map<String, Object> metadata;
}

