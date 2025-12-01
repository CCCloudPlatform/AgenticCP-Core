package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.agenticcp.core.domain.platform.dto.PlatformComplianceConfigRequest;
import com.agenticcp.core.domain.platform.dto.PlatformComplianceReportResponse;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagApproval;
import com.agenticcp.core.domain.platform.entity.PlatformComplianceConfig;
import com.agenticcp.core.domain.platform.enums.ApprovalStatus;
import com.agenticcp.core.domain.platform.enums.FeatureFlagSeverity;
import com.agenticcp.core.domain.platform.repository.FeatureFlagApprovalRepository;
import com.agenticcp.core.domain.platform.repository.PlatformComplianceConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PlatformComplianceService 단위 테스트
 * 
 * 플랫폼 컴플라이언스 서비스의 핵심 기능을 검증합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformComplianceService 테스트")
class PlatformComplianceServiceTest {

    @Mock
    private PlatformComplianceConfigRepository configRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private FeatureFlagApprovalRepository approvalRepository;

    @InjectMocks
    private PlatformComplianceService complianceService;

    private PlatformComplianceConfig testConfig;
    private FeatureFlag testFlag;

    @BeforeEach
    void setUp() {
        // 테스트용 PlatformComplianceConfig 생성
        testConfig = PlatformComplianceConfig.builder()
                .configKey("PLATFORM_COMPLIANCE_CONFIG")
                .reportGenerationIntervalDays(30)
                .auditLogRetentionDays(365)
                .requiredApprovalSeverity("HIGH")
                .allowAutoApproval(false)
                .enableViolationAlerts(true)
                .reportFormat("JSON")
                .build();

        // 테스트용 FeatureFlag 생성
        testFlag = FeatureFlag.builder()
                .flagKey("test-flag")
                .flagName("Test Flag")
                .severity(FeatureFlagSeverity.HIGH)
                .build();
    }

    @Test
    @DisplayName("Singleton 설정 조회 - 기존 설정이 있는 경우")
    void testGetConfig_Singleton() {
        // Given
        when(configRepository.findSingleton()).thenReturn(Optional.of(testConfig));

        // When
        PlatformComplianceConfig result = complianceService.getConfig();

        // Then
        assertNotNull(result);
        assertEquals("PLATFORM_COMPLIANCE_CONFIG", result.getConfigKey());
        assertEquals(30, result.getReportGenerationIntervalDays());
        assertEquals(365, result.getAuditLogRetentionDays());
        assertEquals("HIGH", result.getRequiredApprovalSeverity());
        verify(configRepository).findSingleton();
        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("Singleton 설정 조회 - 설정이 없는 경우 기본값으로 생성")
    void testGetConfig_Singleton_CreateDefault() {
        // Given
        when(configRepository.findSingleton()).thenReturn(Optional.empty());
        when(configRepository.save(any(PlatformComplianceConfig.class))).thenAnswer(invocation -> {
            PlatformComplianceConfig config = invocation.getArgument(0);
            // ID 설정을 위해 Reflection 사용 (실제로는 JPA가 처리)
            return config;
        });

        // When
        PlatformComplianceConfig result = complianceService.getConfig();

        // Then
        assertNotNull(result);
        assertEquals("PLATFORM_COMPLIANCE_CONFIG", result.getConfigKey());
        assertEquals(30, result.getReportGenerationIntervalDays());
        assertEquals(365, result.getAuditLogRetentionDays());
        assertEquals("HIGH", result.getRequiredApprovalSeverity());
        assertEquals("JSON", result.getReportFormat());
        verify(configRepository).findSingleton();
        verify(configRepository).save(any(PlatformComplianceConfig.class));
    }

    @Test
    @DisplayName("설정 업데이트")
    void testUpdateConfig() {
        // Given
        PlatformComplianceConfigRequest request = PlatformComplianceConfigRequest.builder()
                .reportGenerationIntervalDays(7)
                .auditLogRetentionDays(180)
                .requiredApprovalSeverity("CRITICAL")
                .allowAutoApproval(true)
                .enableViolationAlerts(false)
                .reportFormat("CSV")
                .autoApprovalConditions("{\"maxSeverity\":\"MEDIUM\"}")
                .alertRecipients("[\"admin@example.com\"]")
                .reportStorageConfig("{\"path\":\"/reports\"}")
                .metadata("{\"version\":\"1.0\"}")
                .build();

        when(configRepository.findSingleton()).thenReturn(Optional.of(testConfig));
        when(configRepository.save(any(PlatformComplianceConfig.class))).thenAnswer(invocation -> {
            PlatformComplianceConfig config = invocation.getArgument(0);
            return config;
        });

        // When
        PlatformComplianceConfig result = complianceService.updateConfig(request);

        // Then
        assertNotNull(result);
        assertEquals(7, result.getReportGenerationIntervalDays());
        assertEquals(180, result.getAuditLogRetentionDays());
        assertEquals("CRITICAL", result.getRequiredApprovalSeverity());
        assertTrue(result.getAllowAutoApproval());
        assertFalse(result.getEnableViolationAlerts());
        assertEquals("CSV", result.getReportFormat());
        assertEquals("{\"maxSeverity\":\"MEDIUM\"}", result.getAutoApprovalConditions());
        assertEquals("[\"admin@example.com\"]", result.getAlertRecipients());
        assertEquals("{\"path\":\"/reports\"}", result.getReportStorageConfig());
        assertEquals("{\"version\":\"1.0\"}", result.getMetadata());

        ArgumentCaptor<PlatformComplianceConfig> captor = ArgumentCaptor.forClass(PlatformComplianceConfig.class);
        verify(configRepository).save(captor.capture());
        PlatformComplianceConfig savedConfig = captor.getValue();
        assertEquals(7, savedConfig.getReportGenerationIntervalDays());
    }

    @Test
    @DisplayName("보고서 생성 성공 - 감사 로그와 승인 정보가 있는 경우")
    void testGenerateReport_Success() {
        // Given
        LocalDateTime periodStart = LocalDateTime.now().minusDays(30);
        LocalDateTime periodEnd = LocalDateTime.now();

        when(configRepository.findSingleton()).thenReturn(Optional.of(testConfig));
        when(configRepository.save(any(PlatformComplianceConfig.class))).thenAnswer(invocation -> {
            PlatformComplianceConfig config = invocation.getArgument(0);
            return config;
        });

        // 감사 로그 생성
        AuditLog auditLog1 = createAuditLog(1L, "test-flag-1", "UPDATE", AuditSeverity.HIGH, "user-1", periodStart.plusDays(1));
        AuditLog auditLog2 = createAuditLog(2L, "test-flag-2", "CREATE", AuditSeverity.LOW, "user-2", periodStart.plusDays(2));
        AuditLog auditLog3 = createAuditLog(3L, "test-flag-3", "TOGGLE", AuditSeverity.CRITICAL, "user-1", periodStart.plusDays(3));
        List<AuditLog> auditLogs = Arrays.asList(auditLog1, auditLog2, auditLog3);

        when(auditLogRepository.findByResourceTypeAndTimestampBetween(
                eq(AuditResourceType.FEATURE_FLAG),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(auditLogs);

        // 승인 정보 생성
        FeatureFlagApproval approval1 = createApproval(1L, testFlag, ApprovalStatus.APPROVED, "user-1", periodStart.plusDays(1));
        FeatureFlagApproval approval2 = createApproval(2L, testFlag, ApprovalStatus.REJECTED, "user-2", periodStart.plusDays(2));
        List<FeatureFlagApproval> approvals = Arrays.asList(approval1, approval2);

        when(approvalRepository.findByRequestedAtBetween(
                eq(periodStart),
                eq(periodEnd)
        )).thenReturn(approvals);

        // When
        PlatformComplianceReportResponse report = complianceService.generateReport(periodStart, periodEnd);

        // Then
        assertNotNull(report);
        assertNotNull(report.getReportId());
        assertEquals(periodStart, report.getPeriodStart());
        assertEquals(periodEnd, report.getPeriodEnd());
        assertEquals(3L, report.getTotalChanges());
        assertEquals(1L, report.getApprovedChanges());
        assertEquals(1L, report.getRejectedChanges());
        assertNotNull(report.getChangesBySeverity());
        assertNotNull(report.getChangesByUser());
        assertNotNull(report.getViolations());
        assertNotNull(report.getAuditLogSummaries());
        assertEquals(3, report.getAuditLogSummaries().size());
        assertEquals("JSON", report.getReportFormat());

        // 보고서 생성 일시 업데이트 검증
        ArgumentCaptor<PlatformComplianceConfig> captor = ArgumentCaptor.forClass(PlatformComplianceConfig.class);
        verify(configRepository).save(captor.capture());
        PlatformComplianceConfig savedConfig = captor.getValue();
        assertNotNull(savedConfig.getLastReportGeneratedAt());
        assertNotNull(savedConfig.getNextReportGenerationAt());
    }

    @Test
    @DisplayName("보고서 생성 성공 - 빈 로그인 경우")
    void testGenerateReport_EmptyLogs() {
        // Given
        LocalDateTime periodStart = LocalDateTime.now().minusDays(30);
        LocalDateTime periodEnd = LocalDateTime.now();

        when(configRepository.findSingleton()).thenReturn(Optional.of(testConfig));
        when(configRepository.save(any(PlatformComplianceConfig.class))).thenAnswer(invocation -> {
            PlatformComplianceConfig config = invocation.getArgument(0);
            return config;
        });

        // 빈 감사 로그
        when(auditLogRepository.findByResourceTypeAndTimestampBetween(
                eq(AuditResourceType.FEATURE_FLAG),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(Collections.emptyList());

        // 빈 승인 정보
        when(approvalRepository.findByRequestedAtBetween(
                eq(periodStart),
                eq(periodEnd)
        )).thenReturn(Collections.emptyList());

        // When
        PlatformComplianceReportResponse report = complianceService.generateReport(periodStart, periodEnd);

        // Then
        assertNotNull(report);
        assertNotNull(report.getReportId());
        assertEquals(periodStart, report.getPeriodStart());
        assertEquals(periodEnd, report.getPeriodEnd());
        assertEquals(0L, report.getTotalChanges());
        assertEquals(0L, report.getApprovedChanges());
        assertEquals(0L, report.getRejectedChanges());
        assertEquals(0L, report.getUnapprovedChanges());
        assertTrue(report.getChangesBySeverity().isEmpty());
        assertTrue(report.getChangesByUser().isEmpty());
        assertTrue(report.getViolations().isEmpty());
        assertTrue(report.getAuditLogSummaries().isEmpty());
        assertEquals("JSON", report.getReportFormat());

        // 보고서 생성 일시 업데이트 검증
        verify(configRepository).save(any(PlatformComplianceConfig.class));
    }

    @Test
    @DisplayName("보고서 생성 - 기간을 지정하지 않은 경우")
    void testGenerateReport_NoPeriod() {
        // Given
        when(configRepository.findSingleton()).thenReturn(Optional.of(testConfig));
        when(configRepository.save(any(PlatformComplianceConfig.class))).thenAnswer(invocation -> {
            PlatformComplianceConfig config = invocation.getArgument(0);
            return config;
        });

        when(auditLogRepository.findByResourceTypeAndTimestampBetween(
                eq(AuditResourceType.FEATURE_FLAG),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(approvalRepository.findByRequestedAtBetween(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());

        // When
        PlatformComplianceReportResponse report = complianceService.generateReport(null, null);

        // Then
        assertNotNull(report);
        assertNotNull(report.getPeriodStart());
        assertNotNull(report.getPeriodEnd());
        // periodStart는 설정의 auditLogRetentionDays 기준으로 계산됨
        // periodEnd는 현재 시간
    }

    /**
     * 테스트용 AuditLog 생성
     */
    private AuditLog createAuditLog(Long id, String flagKey, String action, AuditSeverity severity, 
                                    String userId, LocalDateTime timestamp) {
        return AuditLog.builder()
                .action(action)
                .resourceType(AuditResourceType.FEATURE_FLAG)
                .severity(severity)
                .timestamp(timestamp.atZone(ZoneId.systemDefault()).toInstant())
                .userId(userId)
                .targetResourceId(flagKey)
                .success(true)
                .build();
    }

    /**
     * 테스트용 FeatureFlagApproval 생성
     */
    private FeatureFlagApproval createApproval(Long id, FeatureFlag flag, ApprovalStatus status, 
                                               String requestedBy, LocalDateTime requestedAt) {
        FeatureFlagApproval approval = FeatureFlagApproval.builder()
                .featureFlag(flag)
                .status(status)
                .requestedBy(requestedBy)
                .requestedAt(requestedAt)
                .build();

        if (status == ApprovalStatus.APPROVED) {
            approval.setApprovedBy("approver-1");
            approval.setApprovedAt(requestedAt.plusHours(1));
        } else if (status == ApprovalStatus.REJECTED) {
            approval.setRejectedBy("rejector-1");
            approval.setRejectedAt(requestedAt.plusHours(1));
        }

        return approval;
    }
}

