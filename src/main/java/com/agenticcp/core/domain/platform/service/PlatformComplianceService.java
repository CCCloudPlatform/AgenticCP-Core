package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.agenticcp.core.domain.platform.dto.PlatformComplianceConfigRequest;
import com.agenticcp.core.domain.platform.dto.PlatformComplianceReportResponse;
import com.agenticcp.core.domain.platform.entity.FeatureFlagApproval;
import com.agenticcp.core.domain.platform.entity.PlatformComplianceConfig;
import com.agenticcp.core.domain.platform.enums.ApprovalStatus;
import com.agenticcp.core.domain.platform.repository.FeatureFlagApprovalRepository;
import com.agenticcp.core.domain.platform.repository.PlatformComplianceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 플랫폼 컴플라이언스 서비스
 * 
 * 기능 플래그 변경에 대한 컴플라이언스 정책 및 보고서를 관리합니다.
 * Singleton 패턴으로 구현되어 플랫폼 전역에 단일 설정 인스턴스만 존재합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformComplianceService {

    private static final String SINGLETON_CONFIG_KEY = "PLATFORM_COMPLIANCE_CONFIG";

    private final PlatformComplianceConfigRepository configRepository;
    private final AuditLogRepository auditLogRepository;
    private final FeatureFlagApprovalRepository approvalRepository;

    /**
     * Singleton 컴플라이언스 설정 조회
     * 설정이 없으면 기본값으로 생성합니다.
     * 
     * @return 컴플라이언스 설정
     */
    public PlatformComplianceConfig getConfig() {
        log.info("[PlatformComplianceService] getConfig");
        
        Optional<PlatformComplianceConfig> configOpt = configRepository.findSingleton();
        
        if (configOpt.isPresent()) {
            PlatformComplianceConfig config = configOpt.get();
            log.info("[PlatformComplianceService] getConfig - success (existing) id={}", config.getId());
            return config;
        }
        
        // Singleton 인스턴스가 없으면 기본값으로 생성
        log.info("[PlatformComplianceService] getConfig - creating default singleton instance");
        return createDefaultConfig();
    }

    /**
     * 기본 설정으로 Singleton 인스턴스 생성
     * 
     * @return 생성된 설정
     */
    @Transactional
    private PlatformComplianceConfig createDefaultConfig() {
        PlatformComplianceConfig defaultConfig = PlatformComplianceConfig.builder()
                .configKey(SINGLETON_CONFIG_KEY)
                .reportGenerationIntervalDays(30)
                .auditLogRetentionDays(365)
                .requiredApprovalSeverity("HIGH")
                .allowAutoApproval(false)
                .enableViolationAlerts(true)
                .reportFormat("JSON")
                .build();
        
        PlatformComplianceConfig saved = configRepository.save(defaultConfig);
        log.info("[PlatformComplianceService] createDefaultConfig - success id={}", saved.getId());
        return saved;
    }

    /**
     * 컴플라이언스 설정 업데이트
     * 
     * @param request 설정 업데이트 요청
     * @return 업데이트된 설정
     */
    @Transactional
    public PlatformComplianceConfig updateConfig(PlatformComplianceConfigRequest request) {
        log.info("[PlatformComplianceService] updateConfig");
        
        PlatformComplianceConfig config = getConfigOrCreate();
        
        // 설정 업데이트
        config.setReportGenerationIntervalDays(request.getReportGenerationIntervalDays());
        config.setAuditLogRetentionDays(request.getAuditLogRetentionDays());
        config.setRequiredApprovalSeverity(request.getRequiredApprovalSeverity());
        config.setAllowAutoApproval(request.getAllowAutoApproval());
        config.setAutoApprovalConditions(request.getAutoApprovalConditions());
        config.setEnableViolationAlerts(request.getEnableViolationAlerts());
        config.setAlertRecipients(request.getAlertRecipients());
        config.setReportFormat(request.getReportFormat());
        config.setReportStorageConfig(request.getReportStorageConfig());
        config.setMetadata(request.getMetadata());
        
        PlatformComplianceConfig saved = configRepository.save(config);
        log.info("[PlatformComplianceService] updateConfig - success id={}", saved.getId());
        return saved;
    }

    /**
     * 설정 조회 또는 생성 (내부 메서드)
     * 
     * @return 설정
     */
    private PlatformComplianceConfig getConfigOrCreate() {
        return configRepository.findSingleton()
                .orElseGet(this::createDefaultConfig);
    }

    /**
     * 컴플라이언스 보고서 생성
     * 
     * @param periodStart 보고서 기간 시작일 (null인 경우 설정의 보관 기간 기준)
     * @param periodEnd 보고서 기간 종료일 (null인 경우 현재 시간)
     * @return 컴플라이언스 보고서
     */
    @Transactional
    public PlatformComplianceReportResponse generateReport(LocalDateTime periodStart, LocalDateTime periodEnd) {
        log.info("[PlatformComplianceService] generateReport - periodStart={}, periodEnd={}", periodStart, periodEnd);
        
        PlatformComplianceConfig config = getConfigOrCreate();
        
        // 보고서 기간 결정
        LocalDateTime reportStart = periodStart != null ? periodStart : 
                LocalDateTime.now().minusDays(config.getAuditLogRetentionDays());
        LocalDateTime reportEnd = periodEnd != null ? periodEnd : LocalDateTime.now();
        
        // Instant로 변환
        Instant startInstant = reportStart.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = reportEnd.atZone(ZoneId.systemDefault()).toInstant();
        
        // 감사 로그 조회 (FEATURE_FLAG 리소스 타입, 모든 테넌트)
        // 플랫폼 레벨 보고서이므로 모든 테넌트의 로그를 조회
        List<AuditLog> auditLogs = auditLogRepository.findByResourceTypeAndTimestampBetween(
                AuditResourceType.FEATURE_FLAG,
                startInstant,
                endInstant
        );
        
        log.info("[PlatformComplianceService] generateReport - found {} audit logs", auditLogs.size());
        
        // 승인 정보 조회
        List<FeatureFlagApproval> approvals = approvalRepository.findByRequestedAtBetween(
                reportStart,
                reportEnd
        );
        
        log.info("[PlatformComplianceService] generateReport - found {} approvals", approvals.size());
        
        // 통계 계산
        ReportStatistics stats = calculateStatistics(auditLogs, approvals, config);
        
        // 정책 위반 감지
        List<PlatformComplianceReportResponse.PolicyViolation> violations = detectViolations(
                auditLogs, approvals, config);
        
        // 감사 로그 요약 생성
        List<PlatformComplianceReportResponse.AuditLogSummary> auditLogSummaries = createAuditLogSummaries(
                auditLogs);
        
        // 보고서 생성
        String reportId = UUID.randomUUID().toString();
        PlatformComplianceReportResponse report = PlatformComplianceReportResponse.builder()
                .reportId(reportId)
                .generatedAt(LocalDateTime.now())
                .periodStart(reportStart)
                .periodEnd(reportEnd)
                .totalChanges(stats.totalChanges)
                .approvedChanges(stats.approvedChanges)
                .rejectedChanges(stats.rejectedChanges)
                .unapprovedChanges(stats.unapprovedChanges)
                .changesBySeverity(stats.changesBySeverity)
                .changesByUser(stats.changesByUser)
                .violations(violations)
                .auditLogSummaries(auditLogSummaries)
                .reportFormat(config.getReportFormat())
                .build();
        
        // 보고서 생성 일시 업데이트
        config.setLastReportGeneratedAt(LocalDateTime.now());
        config.setNextReportGenerationAt(config.calculateNextReportGeneration());
        configRepository.save(config);
        
        log.info("[PlatformComplianceService] generateReport - success reportId={} totalChanges={}", 
                reportId, stats.totalChanges);
        
        return report;
    }

    /**
     * 통계 계산
     */
    private ReportStatistics calculateStatistics(List<AuditLog> auditLogs, 
                                                List<FeatureFlagApproval> approvals,
                                                PlatformComplianceConfig config) {
        ReportStatistics stats = new ReportStatistics();
        
        // 총 변경 횟수
        stats.totalChanges = (long) auditLogs.size();
        
        // 승인 정보를 Map으로 변환 (flagKey -> 최신 승인)
        Map<String, FeatureFlagApproval> approvalMap = approvals.stream()
                .collect(Collectors.toMap(
                        approval -> approval.getFeatureFlag().getFlagKey(),
                        approval -> approval,
                        (existing, replacement) -> 
                                replacement.getRequestedAt().isAfter(existing.getRequestedAt()) 
                                        ? replacement : existing
                ));
        
        // 승인/거부 통계
        stats.approvedChanges = approvals.stream()
                .filter(approval -> approval.getStatus() == ApprovalStatus.APPROVED)
                .count();
        
        stats.rejectedChanges = approvals.stream()
                .filter(approval -> approval.getStatus() == ApprovalStatus.REJECTED)
                .count();
        
        // 심각도별 통계
        stats.changesBySeverity = auditLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getSeverity() != null ? log.getSeverity().name() : "UNKNOWN",
                        Collectors.counting()
                ));
        
        // 사용자별 통계
        stats.changesByUser = auditLogs.stream()
                .filter(log -> log.getUserId() != null)
                .collect(Collectors.groupingBy(
                        AuditLog::getUserId,
                        Collectors.counting()
                ));
        
        // 승인 없이 변경된 횟수 계산 (정책 위반)
        stats.unapprovedChanges = auditLogs.stream()
                .filter(log -> {
                    // HIGH 또는 CRITICAL 심각도인 경우
                    boolean requiresApproval = log.getSeverity() == AuditSeverity.HIGH 
                            || log.getSeverity() == AuditSeverity.CRITICAL;
                    
                    if (!requiresApproval) {
                        return false;
                    }
                    
                    // 해당 플래그에 대한 승인이 있는지 확인
                    String flagKey = log.getTargetResourceId();
                    if (flagKey == null) {
                        return true; // 플래그 키가 없으면 위반으로 간주
                    }
                    
                    FeatureFlagApproval approval = approvalMap.get(flagKey);
                    if (approval == null) {
                        return true; // 승인이 없으면 위반
                    }
                    
                    // 승인이 있더라도, 해당 변경 시점 이전의 승인인지 확인
                    Instant logTimestamp = log.getTimestamp();
                    LocalDateTime approvalRequestedAt = approval.getRequestedAt();
                    if (approvalRequestedAt == null) {
                        return true;
                    }
                    
                    Instant approvalInstant = approvalRequestedAt.atZone(ZoneId.systemDefault()).toInstant();
                    // 승인이 로그 이후에 요청되었으면 위반
                    return approvalInstant.isAfter(logTimestamp) || 
                           (approval.getStatus() != ApprovalStatus.APPROVED);
                })
                .count();
        
        return stats;
    }

    /**
     * 정책 위반 감지
     */
    private List<PlatformComplianceReportResponse.PolicyViolation> detectViolations(
            List<AuditLog> auditLogs,
            List<FeatureFlagApproval> approvals,
            PlatformComplianceConfig config) {
        
        List<PlatformComplianceReportResponse.PolicyViolation> violations = new ArrayList<>();
        
        // 승인 정보를 Map으로 변환
        Map<String, FeatureFlagApproval> approvalMap = approvals.stream()
                .collect(Collectors.toMap(
                        approval -> approval.getFeatureFlag().getFlagKey(),
                        approval -> approval,
                        (existing, replacement) -> 
                                replacement.getRequestedAt().isAfter(existing.getRequestedAt()) 
                                        ? replacement : existing
                ));
        
        for (AuditLog log : auditLogs) {
            // HIGH 또는 CRITICAL 심각도인 경우만 확인
            boolean requiresApproval = log.getSeverity() == AuditSeverity.HIGH 
                    || log.getSeverity() == AuditSeverity.CRITICAL;
            
            if (!requiresApproval) {
                continue;
            }
            
            String flagKey = log.getTargetResourceId();
            if (flagKey == null) {
                continue;
            }
            
            FeatureFlagApproval approval = approvalMap.get(flagKey);
            
            // 승인이 없거나, 승인되지 않은 경우 위반
            boolean isViolation = approval == null 
                    || approval.getStatus() != ApprovalStatus.APPROVED
                    || (approval.getRequestedAt() != null && 
                        approval.getRequestedAt().atZone(ZoneId.systemDefault()).toInstant()
                                .isAfter(log.getTimestamp()));
            
            if (isViolation) {
                LocalDateTime violationTime = LocalDateTime.ofInstant(
                        log.getTimestamp(), ZoneId.systemDefault());
                
                PlatformComplianceReportResponse.PolicyViolation violation = 
                        PlatformComplianceReportResponse.PolicyViolation.builder()
                                .violationTime(violationTime)
                                .flagKey(flagKey)
                                .userId(log.getUserId())
                                .violationType("UNAPPROVED_CHANGE")
                                .description(String.format(
                                        "심각도 %s 플래그 변경이 승인 없이 수행되었습니다. (액션: %s)",
                                        log.getSeverity().name(), log.getAction()))
                                .build();
                
                violations.add(violation);
            }
        }
        
        return violations;
    }

    /**
     * 감사 로그 요약 생성
     */
    private List<PlatformComplianceReportResponse.AuditLogSummary> createAuditLogSummaries(
            List<AuditLog> auditLogs) {
        
        return auditLogs.stream()
                .map(log -> {
                    LocalDateTime timestamp = LocalDateTime.ofInstant(
                            log.getTimestamp(), ZoneId.systemDefault());
                    
                    return PlatformComplianceReportResponse.AuditLogSummary.builder()
                            .logId(log.getId())
                            .flagKey(log.getTargetResourceId())
                            .action(log.getAction())
                            .severity(log.getSeverity() != null ? log.getSeverity().name() : null)
                            .timestamp(timestamp)
                            .userId(log.getUserId())
                            .success(log.getSuccess())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 통계 데이터를 담는 내부 클래스
     */
    private static class ReportStatistics {
        Long totalChanges = 0L;
        Long approvedChanges = 0L;
        Long rejectedChanges = 0L;
        Long unapprovedChanges = 0L;
        Map<String, Long> changesBySeverity = new HashMap<>();
        Map<String, Long> changesByUser = new HashMap<>();
    }
}

