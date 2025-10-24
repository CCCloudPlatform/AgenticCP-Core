package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.dto.audit.AuditLogResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSearchRequest;
import com.agenticcp.core.common.dto.audit.AuditLogSearchResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSummaryResponse;
import com.agenticcp.core.common.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * 감사 로그 조회 API 컨트롤러
 * 테넌트별, 권한별 필터링을 적용하여 감사 로그를 조회합니다.
 * 
 * @author AgenticCP Team
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
            @Parameter(description = "검색 조건") @Valid AuditLogSearchRequest request) {
        
        log.info("감사 로그 검색 요청 - 페이지: {}, 크기: {}", request.page(), request.size());
        
        AuditLogSearchResponse response = auditLogService.searchAuditLogs(request);
        
        return ResponseEntity.ok(ApiResponse.success(response, "감사 로그 검색이 완료되었습니다"));
    }

    @GetMapping("/resource/{targetResourceId}")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('SUPER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "특정 리소스의 감사 로그 조회", description = "특정 리소스에 대한 모든 감사 로그를 조회합니다")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByResource(
            @Parameter(description = "대상 리소스 ID") @PathVariable String targetResourceId) {
        
        log.info("리소스별 감사 로그 조회 요청 - 리소스 ID: {}", targetResourceId);
        
        List<AuditLogResponse> response = auditLogService.getAuditLogsByResource(targetResourceId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "리소스별 감사 로그 조회가 완료되었습니다"));
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('SUPER_ADMIN') or hasRole('TENANT_ADMIN')")
    @Operation(summary = "감사 로그 대시보드 요약", description = "감사 로그 대시보드용 요약 정보를 제공합니다")
    public ResponseEntity<ApiResponse<AuditLogSummaryResponse>> getAuditLogSummary(
            @Parameter(description = "시작 날짜 (ISO 8601)") @RequestParam(required = false) String startDate,
            @Parameter(description = "종료 날짜 (ISO 8601)") @RequestParam(required = false) String endDate) {
        
        log.info("감사 로그 대시보드 요약 요청 - 시작: {}, 종료: {}", startDate, endDate);
        
        // 날짜 파싱
        Instant start = null;
        Instant end = null;
        
        try {
            if (startDate != null && !startDate.isEmpty()) {
                start = Instant.parse(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = Instant.parse(endDate);
            }
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AuditLogSummaryResponse>builder()
                            .success(false)
                            .message("잘못된 날짜 형식입니다. ISO 8601 형식을 사용해주세요.")
                            .build());
        }
        
        AuditLogSummaryResponse response = auditLogService.getAuditLogSummary(start, end);
        
        return ResponseEntity.ok(ApiResponse.success(response, "감사 로그 대시보드 요약이 완료되었습니다"));
    }
}
