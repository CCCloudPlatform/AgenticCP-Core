package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.dto.audit.AuditLogResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSearchRequest;
import com.agenticcp.core.common.dto.audit.AuditLogSearchResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSummaryResponse;
import com.agenticcp.core.common.service.AuditLogService;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.util.LogMaskingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 감사 로그 조회 REST 컨트롤러입니다.
 * 테넌트별/리소스별 감사 로그 및 요약 정보를 조회합니다.
 *
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Audit Logs", description = "감사 로그 조회 API")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('AUDITOR') or hasRole('SUPER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "감사 로그 검색", description = "테넌트별, 권한별 필터링을 적용하여 감사 로그를 검색합니다")
    public ResponseEntity<ApiResponse<AuditLogSearchResponse>> searchAuditLogs(
            @Parameter(description = "검색 조건") @Valid @ModelAttribute AuditLogSearchRequest request) {

        log.info("[AuditLogController] searchAuditLogs - userId={}, action={}, page={}, size={}",
                LogMaskingUtils.maskUserIdentifier(request.userId()),
                request.action(),
                request.page(),
                request.size());

        AuditLogSearchResponse response = auditLogService.searchAuditLogs(request);

        return ResponseEntity.ok(ApiResponse.success(response, "감사 로그 검색이 완료되었습니다"));
    }

    @GetMapping("/resource/{targetResourceId}")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('SUPER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "특정 리소스의 감사 로그 조회", description = "특정 리소스에 대한 모든 감사 로그를 조회합니다")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByResource(
            @Parameter(description = "대상 리소스 ID") @PathVariable String targetResourceId) {
        log.info("[AuditLogController] getAuditLogsByResource - resourceId={}", targetResourceId);

        List<AuditLogResponse> response = auditLogService.getAuditLogsByResource(targetResourceId);

        return ResponseEntity.ok(ApiResponse.success(response, "리소스별 감사 로그 조회가 완료되었습니다"));
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('SUPER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "감사 로그 대시보드 요약", description = "감사 로그 대시보드용 요약 정보를 제공합니다")
    public ResponseEntity<ApiResponse<AuditLogSummaryResponse>> getAuditLogSummary(
            @Parameter(description = "시작 날짜 (ISO 8601)") @RequestParam(required = false) String startDate,
            @Parameter(description = "종료 날짜 (ISO 8601)") @RequestParam(required = false) String endDate) {

        log.info("[AuditLogController] getAuditLogSummary - start={}, end={}", startDate, endDate);

        // UTC 기준
        Instant start = parseIsoInstantOrThrow(startDate, "startDate");
        Instant end = parseIsoInstantOrThrow(endDate, "endDate");

        AuditLogSummaryResponse response = auditLogService.getAuditLogSummary(start, end);

        return ResponseEntity.ok(ApiResponse.success(response, "감사 로그 대시보드 요약이 완료되었습니다"));
    }

    /**
     * ISO 8601 문자열을 {@link Instant}로 변환합니다.
     *
     * @param value ISO 8601 문자열
     * @param label 파라미터 식별자
     * @return 변환된 {@link Instant} 또는 {@code null}
     */
    private Instant parseIsoInstantOrThrow(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            log.warn("[AuditLogController] Invalid {} format: {}", label, value);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST,
                    String.format("%s는 ISO 8601 형식이어야 합니다.", label));
        }
    }
}
