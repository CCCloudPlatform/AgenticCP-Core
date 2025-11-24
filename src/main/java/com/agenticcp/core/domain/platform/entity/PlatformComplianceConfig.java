package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 플랫폼 컴플라이언스 설정 엔티티
 * 
 * 기능 플래그 변경에 대한 컴플라이언스 정책 및 보고서 설정을 관리합니다.
 * Singleton 패턴으로 구현되어 플랫폼 전역에 단일 인스턴스만 존재합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Entity
@Table(name = "platform_compliance_config", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_platform_compliance_config_singleton", columnNames = {"config_key"})
       })
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformComplianceConfig extends BaseEntity {

    /**
     * 설정 키 (Singleton 보장을 위한 고정값)
     * 항상 "PLATFORM_COMPLIANCE_CONFIG" 값을 가집니다.
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    @Builder.Default
    private String configKey = "PLATFORM_COMPLIANCE_CONFIG";

    /**
     * 보고서 생성 주기 (일 단위)
     * 예: 7 = 주간 보고서, 30 = 월간 보고서
     */
    @Column(name = "report_generation_interval_days", nullable = false)
    @Builder.Default
    private Integer reportGenerationIntervalDays = 30;

    /**
     * 감사 로그 보관 기간 (일 단위)
     * 이 기간 이후의 로그는 보고서 생성 시 제외될 수 있습니다.
     */
    @Column(name = "audit_log_retention_days", nullable = false)
    @Builder.Default
    private Integer auditLogRetentionDays = 365;

    /**
     * 승인이 필요한 최소 심각도
     * HIGH 또는 CRITICAL 심각도의 플래그 변경 시 승인이 필요합니다.
     * 값: "HIGH" 또는 "CRITICAL"
     */
    @Column(name = "required_approval_severity", nullable = false, length = 20)
    @Builder.Default
    private String requiredApprovalSeverity = "HIGH";

    /**
     * 자동 승인 허용 여부
     * true인 경우, 특정 조건에서 자동 승인이 가능합니다.
     */
    @Column(name = "allow_auto_approval", nullable = false)
    @Builder.Default
    private Boolean allowAutoApproval = false;

    /**
     * 자동 승인 조건 (JSON 형태)
     * 자동 승인이 허용된 경우의 조건을 정의합니다.
     */
    @Column(name = "auto_approval_conditions", columnDefinition = "TEXT")
    private String autoApprovalConditions;

    /**
     * 정책 위반 알림 활성화 여부
     */
    @Column(name = "enable_violation_alerts", nullable = false)
    @Builder.Default
    private Boolean enableViolationAlerts = true;

    /**
     * 알림 수신자 목록 (JSON 형태)
     * 정책 위반 시 알림을 받을 사용자/그룹 목록
     */
    @Column(name = "alert_recipients", columnDefinition = "TEXT")
    private String alertRecipients;

    /**
     * 보고서 형식 (JSON, CSV, PDF 등)
     */
    @Column(name = "report_format", nullable = false, length = 20)
    @Builder.Default
    private String reportFormat = "JSON";

    /**
     * 보고서 저장 경로 또는 설정 (JSON 형태)
     */
    @Column(name = "report_storage_config", columnDefinition = "TEXT")
    private String reportStorageConfig;

    /**
     * 마지막 보고서 생성 일시
     */
    @Column(name = "last_report_generated_at")
    private LocalDateTime lastReportGeneratedAt;

    /**
     * 다음 보고서 생성 예정 일시
     */
    @Column(name = "next_report_generation_at")
    private LocalDateTime nextReportGenerationAt;

    /**
     * 추가 설정 (JSON 형태)
     * 향후 확장을 위한 메타데이터
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Singleton 인스턴스인지 확인
     * 
     * @return 항상 true (단일 인스턴스)
     */
    public boolean isSingleton() {
        return "PLATFORM_COMPLIANCE_CONFIG".equals(configKey);
    }

    /**
     * 다음 보고서 생성 일시 계산
     * 
     * @return 다음 보고서 생성 예정 일시
     */
    public LocalDateTime calculateNextReportGeneration() {
        if (lastReportGeneratedAt == null) {
            return LocalDateTime.now().plusDays(reportGenerationIntervalDays);
        }
        return lastReportGeneratedAt.plusDays(reportGenerationIntervalDays);
    }

    /**
     * 보고서 생성 필요 여부 확인
     * 
     * @return 보고서 생성이 필요한 경우 true
     */
    public boolean shouldGenerateReport() {
        if (nextReportGenerationAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(nextReportGenerationAt) || 
               LocalDateTime.now().isEqual(nextReportGenerationAt);
    }
}

