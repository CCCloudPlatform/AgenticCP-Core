package com.agenticcp.core.domain.platform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 플랫폼 컴플라이언스 설정 응답 DTO
 * 
 * 컴플라이언스 설정 정보를 반환하는 응답 DTO입니다.
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
public class PlatformComplianceConfigResponse {

    /**
     * 설정 ID
     */
    private Long id;

    /**
     * 설정 키
     */
    private String configKey;

    /**
     * 보고서 생성 주기 (일 단위)
     */
    private Integer reportGenerationIntervalDays;

    /**
     * 감사 로그 보관 기간 (일 단위)
     */
    private Integer auditLogRetentionDays;

    /**
     * 승인이 필요한 최소 심각도
     */
    private String requiredApprovalSeverity;

    /**
     * 자동 승인 허용 여부
     */
    private Boolean allowAutoApproval;

    /**
     * 자동 승인 조건 (JSON 형태)
     */
    private String autoApprovalConditions;

    /**
     * 정책 위반 알림 활성화 여부
     */
    private Boolean enableViolationAlerts;

    /**
     * 알림 수신자 목록 (JSON 형태)
     */
    private String alertRecipients;

    /**
     * 보고서 형식
     */
    private String reportFormat;

    /**
     * 보고서 저장 경로 또는 설정 (JSON 형태)
     */
    private String reportStorageConfig;

    /**
     * 마지막 보고서 생성 일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime lastReportGeneratedAt;

    /**
     * 다음 보고서 생성 예정 일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime nextReportGenerationAt;

    /**
     * 추가 설정 (JSON 형태)
     */
    private String metadata;

    /**
     * 생성일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime updatedAt;
}

