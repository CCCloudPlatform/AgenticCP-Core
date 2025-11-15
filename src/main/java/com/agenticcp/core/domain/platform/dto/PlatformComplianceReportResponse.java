package com.agenticcp.core.domain.platform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 플랫폼 컴플라이언스 보고서 응답 DTO
 * 
 * 기능 플래그 변경에 대한 컴플라이언스 보고서 정보를 담습니다.
 * JSON 형식으로 생성됩니다.
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
public class PlatformComplianceReportResponse {

    /**
     * 보고서 ID
     */
    private String reportId;

    /**
     * 보고서 생성 일시
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime generatedAt;

    /**
     * 보고서 기간 시작일
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime periodStart;

    /**
     * 보고서 기간 종료일
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime periodEnd;

    /**
     * 총 플래그 변경 횟수
     */
    private Long totalChanges;

    /**
     * 승인된 변경 횟수
     */
    private Long approvedChanges;

    /**
     * 거부된 변경 횟수
     */
    private Long rejectedChanges;

    /**
     * 승인 없이 변경된 횟수 (정책 위반)
     */
    private Long unapprovedChanges;

    /**
     * 심각도별 변경 통계
     */
    private Map<String, Long> changesBySeverity;

    /**
     * 사용자별 변경 통계
     */
    private Map<String, Long> changesByUser;

    /**
     * 정책 위반 목록
     */
    private List<PolicyViolation> violations;

    /**
     * 감사 로그 요약
     */
    private List<AuditLogSummary> auditLogSummaries;

    /**
     * 보고서 형식
     */
    private String reportFormat;

    /**
     * 정책 위반 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyViolation {
        /**
         * 위반 일시
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
        private LocalDateTime violationTime;

        /**
         * 플래그 키
         */
        private String flagKey;

        /**
         * 사용자 ID
         */
        private String userId;

        /**
         * 위반 유형
         */
        private String violationType;

        /**
         * 위반 설명
         */
        private String description;
    }

    /**
     * 감사 로그 요약
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogSummary {
        /**
         * 로그 ID
         */
        private Long logId;

        /**
         * 플래그 키
         */
        private String flagKey;

        /**
         * 액션
         */
        private String action;

        /**
         * 심각도
         */
        private String severity;

        /**
         * 타임스탬프
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
        private LocalDateTime timestamp;

        /**
         * 사용자 ID
         */
        private String userId;

        /**
         * 성공 여부
         */
        private Boolean success;
    }
}

