package com.agenticcp.core.domain.security.dto;

import com.agenticcp.core.domain.security.entity.PolicyViolation;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 정책 위반 통계 DTO
 * 
 * <p>특정 기간 동안의 정책 위반 통계 정보를 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Schema(description = "정책 위반 통계")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationStatistics {

    @Schema(description = "테넌트 ID")
    private Long tenantId;

    @Schema(description = "통계 시작 시간")
    private LocalDateTime startTime;

    @Schema(description = "통계 종료 시간")
    private LocalDateTime endTime;

    @Schema(description = "총 위반 건수")
    private Long totalViolations;

    @Schema(description = "해결된 위반 건수")
    private Long resolvedViolations;

    @Schema(description = "처리 중인 위반 건수")
    private Long pendingViolations;

    @Schema(description = "오탐지 건수")
    private Long falsePositives;

    @Schema(description = "자동 대응 실행 건수")
    private Long autoResponseExecuted;

    @Schema(description = "위반 타입별 통계")
    private Map<PolicyViolation.ViolationType, Long> violationsByType;

    @Schema(description = "심각도별 통계")
    private Map<SecurityPolicy.Severity, Long> violationsBySeverity;

    @Schema(description = "정책별 위반 통계 (Top 10)")
    private Map<String, Long> violationsByPolicy;

    @Schema(description = "사용자별 위반 통계 (Top 10)")
    private Map<String, Long> violationsByUser;

    @Schema(description = "IP별 위반 통계 (Top 10)")
    private Map<String, Long> violationsByIp;

    @Schema(description = "평균 해결 시간 (분)")
    private Double averageResolutionTimeMinutes;

    @Schema(description = "가장 많이 위반된 정책 이름")
    private String mostViolatedPolicy;

    @Schema(description = "가장 문제가 많은 사용자")
    private String mostProblematicUser;

    @Schema(description = "시간대별 위반 분포")
    private Map<Integer, Long> violationsByHour;

    @Schema(description = "일별 위반 추이")
    private Map<String, Long> violationTrend;
}

