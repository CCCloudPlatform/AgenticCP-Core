package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.platform.dto.PlatformComplianceConfigRequest;
import com.agenticcp.core.domain.platform.dto.PlatformComplianceConfigResponse;
import com.agenticcp.core.domain.platform.dto.PlatformComplianceReportResponse;
import com.agenticcp.core.domain.platform.entity.PlatformComplianceConfig;
import com.agenticcp.core.domain.platform.service.PlatformComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 플랫폼 컴플라이언스 컨트롤러
 * 
 * 기능 플래그 변경에 대한 컴플라이언스 설정 및 보고서를 관리하는 API를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/platform/feature-flags/compliance")
@RequiredArgsConstructor
@Tag(name = "Platform Compliance", description = "플랫폼 컴플라이언스 관리 API")
public class PlatformComplianceController {

    private final PlatformComplianceService complianceService;

    /**
     * 컴플라이언스 설정 조회
     * 
     * @return 컴플라이언스 설정
     */
    @GetMapping("/config")
    @Operation(
            summary = "컴플라이언스 설정 조회",
            description = "플랫폼 컴플라이언스 설정을 조회합니다. 설정이 없으면 기본값으로 생성됩니다."
    )
    public ResponseEntity<ApiResponse<PlatformComplianceConfigResponse>> getConfig() {
        log.info("[PlatformComplianceController] getConfig");

        PlatformComplianceConfig config = complianceService.getConfig();
        PlatformComplianceConfigResponse response = convertToResponse(config);

        log.info("[PlatformComplianceController] getConfig - success id={}", config.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "컴플라이언스 설정 조회가 완료되었습니다."));
    }

    /**
     * 컴플라이언스 설정 업데이트
     * 
     * @param request 설정 업데이트 요청
     * @return 업데이트된 설정
     */
    @PutMapping("/config")
    @Operation(
            summary = "컴플라이언스 설정 업데이트",
            description = "플랫폼 컴플라이언스 설정을 업데이트합니다."
    )
    public ResponseEntity<ApiResponse<PlatformComplianceConfigResponse>> updateConfig(
            @Parameter(description = "설정 업데이트 요청")
            @Valid @RequestBody PlatformComplianceConfigRequest request) {
        
        log.info("[PlatformComplianceController] updateConfig");

        PlatformComplianceConfig config = complianceService.updateConfig(request);
        PlatformComplianceConfigResponse response = convertToResponse(config);

        log.info("[PlatformComplianceController] updateConfig - success id={}", config.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "컴플라이언스 설정이 업데이트되었습니다."));
    }

    /**
     * 컴플라이언스 보고서 생성
     * 
     * @param periodStart 보고서 기간 시작일 (선택, ISO 8601 형식)
     * @param periodEnd 보고서 기간 종료일 (선택, ISO 8601 형식)
     * @return 컴플라이언스 보고서
     */
    @GetMapping("/report")
    @Operation(
            summary = "컴플라이언스 보고서 생성",
            description = "기능 플래그 변경에 대한 컴플라이언스 보고서를 생성합니다. " +
                         "기간을 지정하지 않으면 설정의 보관 기간 기준으로 생성됩니다."
    )
    public ResponseEntity<ApiResponse<PlatformComplianceReportResponse>> generateReport(
            @Parameter(description = "보고서 기간 시작일 (ISO 8601 형식)", example = "2025-01-01T00:00:00")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime periodStart,
            
            @Parameter(description = "보고서 기간 종료일 (ISO 8601 형식)", example = "2025-12-31T23:59:59")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime periodEnd) {
        
        log.info("[PlatformComplianceController] generateReport - periodStart={}, periodEnd={}", 
                periodStart, periodEnd);

        PlatformComplianceReportResponse report = complianceService.generateReport(periodStart, periodEnd);

        log.info("[PlatformComplianceController] generateReport - success reportId={} totalChanges={}", 
                report.getReportId(), report.getTotalChanges());

        return ResponseEntity.ok(ApiResponse.success(
                report, 
                String.format("컴플라이언스 보고서가 생성되었습니다. (총 변경: %d건)", report.getTotalChanges())
        ));
    }

    /**
     * 엔티티를 응답 DTO로 변환
     * 
     * @param config 컴플라이언스 설정 엔티티
     * @return 응답 DTO
     */
    private PlatformComplianceConfigResponse convertToResponse(PlatformComplianceConfig config) {
        if (config == null) {
            return null;
        }

        return PlatformComplianceConfigResponse.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .reportGenerationIntervalDays(config.getReportGenerationIntervalDays())
                .auditLogRetentionDays(config.getAuditLogRetentionDays())
                .requiredApprovalSeverity(config.getRequiredApprovalSeverity())
                .allowAutoApproval(config.getAllowAutoApproval())
                .autoApprovalConditions(config.getAutoApprovalConditions())
                .enableViolationAlerts(config.getEnableViolationAlerts())
                .alertRecipients(config.getAlertRecipients())
                .reportFormat(config.getReportFormat())
                .reportStorageConfig(config.getReportStorageConfig())
                .lastReportGeneratedAt(config.getLastReportGeneratedAt())
                .nextReportGenerationAt(config.getNextReportGenerationAt())
                .metadata(config.getMetadata())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}

