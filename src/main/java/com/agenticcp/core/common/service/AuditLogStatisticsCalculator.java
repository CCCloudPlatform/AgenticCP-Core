package com.agenticcp.core.common.service;

import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditSeverity;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 감사 로그 통계 계산기
 * 
 * 감사 로그 목록을 받아서 다양한 통계 정보를 계산합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
public class AuditLogStatisticsCalculator {

    private final List<AuditLog> auditLogs;

    /**
     * 생성자
     * 
     * @param auditLogs 통계 계산할 감사 로그 목록
     */
    public AuditLogStatisticsCalculator(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
        log.debug("AuditLogStatisticsCalculator 생성 - 로그 수: {}", auditLogs.size());
    }

    /**
     * 전체 로그 수 조회
     * 
     * @return 전체 로그 수
     */
    public long getTotalLogs() {
        return auditLogs.size();
    }

    /**
     * 성공한 로그 수 조회
     * 
     * @return 성공한 로그 수
     */
    public long getSuccessLogs() {
        return auditLogs.stream()
                .mapToLong(log -> log.getSuccess() ? 1 : 0)
                .sum();
    }

    /**
     * 실패한 로그 수 조회
     * 
     * @param totalLogs 전체 로그 수
     * @param successLogs 성공한 로그 수
     * @return 실패한 로그 수
     */
    public long getFailedLogs(long totalLogs, long successLogs) {
        return totalLogs - successLogs;
    }

    /**
     * 성공률 계산
     * 
     * @param totalLogs 전체 로그 수
     * @param successLogs 성공한 로그 수
     * @return 성공률 (퍼센트)
     */
    public double getSuccessRate(long totalLogs, long successLogs) {
        return totalLogs > 0 ? (double) successLogs / totalLogs * 100 : 0.0;
    }

    /**
     * 심각도별 분포 조회
     * 
     * @return 심각도별 로그 수 맵
     */
    public Map<AuditSeverity, Long> getSeverityDistribution() {
        return auditLogs.stream()
                .collect(Collectors.groupingBy(
                    AuditLog::getSeverity,
                    Collectors.counting()
                ));
    }

    /**
     * 액션별 분포 조회
     * 
     * @return 액션별 로그 수 맵
     */
    public Map<String, Long> getActionDistribution() {
        return auditLogs.stream()
                .collect(Collectors.groupingBy(
                    AuditLog::getAction,
                    Collectors.counting()
                ));
    }

    /**
     * 일별 로그 수 조회
     * 
     * @return 일별 로그 수 맵 (날짜 문자열을 키로 사용)
     */
    public Map<String, Long> getDailyLogCount() {
        return auditLogs.stream()
                .collect(Collectors.groupingBy(
                    log -> LocalDate.from(log.getTimestamp().atZone(ZoneId.systemDefault())).toString(),
                    Collectors.counting()
                ));
    }

    /**
     * 시간별 로그 수 조회
     * 
     * @return 시간별 로그 수 맵 (시간 문자열을 키로 사용)
     */
    public Map<String, Long> getHourlyLogCount() {
        return auditLogs.stream()
                .collect(Collectors.groupingBy(
                    log -> String.valueOf(LocalDateTime.from(log.getTimestamp().atZone(ZoneId.systemDefault())).getHour()),
                    Collectors.counting()
                ));
    }
}
