package com.agenticcp.core.common.service;

import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * AuditLogStatisticsCalculator 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
class AuditLogStatisticsCalculatorTest {

    private AuditLogStatisticsCalculator calculator;
    private List<AuditLog> testAuditLogs;

    @BeforeEach
    void setUp() {
        // 테스트용 감사 로그 데이터 생성
        testAuditLogs = List.of(
            AuditLog.builder()
                .action("CREATE")
                .resourceType(AuditResourceType.USER)
                .severity(AuditSeverity.INFO)
                .success(true)
                .timestamp(Instant.now())
                .build(),
            AuditLog.builder()
                .action("UPDATE")
                .resourceType(AuditResourceType.USER)
                .severity(AuditSeverity.HIGH)
                .success(false)
                .timestamp(Instant.now())
                .build(),
            AuditLog.builder()
                .action("DELETE")
                .resourceType(AuditResourceType.USER)
                .severity(AuditSeverity.CRITICAL)
                .success(true)
                .timestamp(Instant.now())
                .build()
        );
        
        calculator = new AuditLogStatisticsCalculator(testAuditLogs);
    }

    @Test
    @DisplayName("전체 로그 수 조회")
    void getTotalLogs() {
        // When
        long totalLogs = calculator.getTotalLogs();

        // Then
        assertThat(totalLogs).isEqualTo(3);
    }

    @Test
    @DisplayName("성공한 로그 수 조회")
    void getSuccessLogs() {
        // When
        long successLogs = calculator.getSuccessLogs();

        // Then
        assertThat(successLogs).isEqualTo(2);
    }

    @Test
    @DisplayName("실패한 로그 수 조회")
    void getFailedLogs() {
        // When
        long totalLogs = calculator.getTotalLogs();
        long successLogs = calculator.getSuccessLogs();
        long failedLogs = calculator.getFailedLogs(totalLogs, successLogs);

        // Then
        assertThat(failedLogs).isEqualTo(1);
    }

    @Test
    @DisplayName("성공률 계산")
    void getSuccessRate() {
        // When
        long totalLogs = calculator.getTotalLogs();
        long successLogs = calculator.getSuccessLogs();
        double successRate = calculator.getSuccessRate(totalLogs, successLogs);

        // Then
        assertThat(successRate).isEqualTo(66.67, offset(0.01));
    }

    @Test
    @DisplayName("성공률 계산 - 빈 로그 목록")
    void getSuccessRate_EmptyLogs() {
        // Given
        AuditLogStatisticsCalculator emptyCalculator = new AuditLogStatisticsCalculator(List.of());

        // When
        double successRate = emptyCalculator.getSuccessRate(0, 0);

        // Then
        assertThat(successRate).isEqualTo(0.0);
    }

    @Test
    @DisplayName("심각도별 분포 조회")
    void getSeverityDistribution() {
        // When
        Map<AuditSeverity, Long> distribution = calculator.getSeverityDistribution();

        // Then
        assertThat(distribution).hasSize(3);
        assertThat(distribution).containsEntry(AuditSeverity.INFO, 1L);
        assertThat(distribution).containsEntry(AuditSeverity.HIGH, 1L);
        assertThat(distribution).containsEntry(AuditSeverity.CRITICAL, 1L);
    }

    @Test
    @DisplayName("액션별 분포 조회")
    void getActionDistribution() {
        // When
        Map<String, Long> distribution = calculator.getActionDistribution();

        // Then
        assertThat(distribution).hasSize(3);
        assertThat(distribution).containsEntry("CREATE", 1L);
        assertThat(distribution).containsEntry("UPDATE", 1L);
        assertThat(distribution).containsEntry("DELETE", 1L);
    }

    @Test
    @DisplayName("일별 로그 수 조회")
    void getDailyLogCount() {
        // When
        Map<String, Long> dailyCount = calculator.getDailyLogCount();

        // Then
        assertThat(dailyCount).isNotEmpty();
        // 모든 로그가 같은 날짜에 생성되므로 총 3개의 로그가 하나의 날짜에 집계됨
        assertThat(dailyCount.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(3);
    }

    @Test
    @DisplayName("시간별 로그 수 조회")
    void getHourlyLogCount() {
        // When
        Map<String, Long> hourlyCount = calculator.getHourlyLogCount();

        // Then
        assertThat(hourlyCount).isNotEmpty();
        // 모든 로그가 같은 시간에 생성되므로 총 3개의 로그가 하나의 시간에 집계됨
        assertThat(hourlyCount.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(3);
    }

    @Test
    @DisplayName("빈 로그 목록으로 계산기 생성")
    void constructor_EmptyLogs() {
        // When
        AuditLogStatisticsCalculator emptyCalculator = new AuditLogStatisticsCalculator(List.of());

        // Then
        assertThat(emptyCalculator.getTotalLogs()).isEqualTo(0);
        assertThat(emptyCalculator.getSuccessLogs()).isEqualTo(0);
        assertThat(emptyCalculator.getSeverityDistribution()).isEmpty();
        assertThat(emptyCalculator.getActionDistribution()).isEmpty();
    }
}
