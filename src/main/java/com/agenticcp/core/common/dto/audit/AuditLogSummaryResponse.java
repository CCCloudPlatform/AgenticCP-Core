package com.agenticcp.core.common.dto.audit;

import com.agenticcp.core.common.enums.AuditSeverity;

import java.time.Instant;
import java.util.Map;

/**
 * 감사 로그 대시보드 요약 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public record AuditLogSummaryResponse(
    long totalLogs,
    long successLogs,
    long failedLogs,
    double successRate,
    Map<AuditSeverity, Long> severityDistribution,
    Map<String, Long> actionDistribution,
    Map<String, Long> dailyLogCount,
    Map<String, Long> hourlyLogCount,
    Instant lastUpdated
) {}
