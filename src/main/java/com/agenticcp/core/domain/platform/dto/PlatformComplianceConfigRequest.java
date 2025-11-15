package com.agenticcp.core.domain.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 플랫폼 컴플라이언스 설정 요청 DTO
 * 
 * 컴플라이언스 설정을 업데이트하기 위한 요청 DTO입니다.
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
public class PlatformComplianceConfigRequest {

    /**
     * 보고서 생성 주기 (일 단위)
     * 예: 7 = 주간 보고서, 30 = 월간 보고서
     */
    @NotNull(message = "보고서 생성 주기는 필수입니다")
    @Min(value = 1, message = "보고서 생성 주기는 최소 1일 이상이어야 합니다")
    private Integer reportGenerationIntervalDays;

    /**
     * 감사 로그 보관 기간 (일 단위)
     */
    @NotNull(message = "감사 로그 보관 기간은 필수입니다")
    @Min(value = 1, message = "감사 로그 보관 기간은 최소 1일 이상이어야 합니다")
    private Integer auditLogRetentionDays;

    /**
     * 승인이 필요한 최소 심각도
     * 값: "HIGH" 또는 "CRITICAL"
     */
    @NotBlank(message = "승인이 필요한 최소 심각도는 필수입니다")
    private String requiredApprovalSeverity;

    /**
     * 자동 승인 허용 여부
     */
    @NotNull(message = "자동 승인 허용 여부는 필수입니다")
    private Boolean allowAutoApproval;

    /**
     * 자동 승인 조건 (JSON 형태)
     */
    private String autoApprovalConditions;

    /**
     * 정책 위반 알림 활성화 여부
     */
    @NotNull(message = "정책 위반 알림 활성화 여부는 필수입니다")
    private Boolean enableViolationAlerts;

    /**
     * 알림 수신자 목록 (JSON 형태)
     */
    private String alertRecipients;

    /**
     * 보고서 형식 (JSON, CSV, PDF 등)
     */
    @NotBlank(message = "보고서 형식은 필수입니다")
    private String reportFormat;

    /**
     * 보고서 저장 경로 또는 설정 (JSON 형태)
     */
    private String reportStorageConfig;

    /**
     * 추가 설정 (JSON 형태)
     */
    private String metadata;
}

