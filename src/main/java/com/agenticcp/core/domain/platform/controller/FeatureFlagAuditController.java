package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.agenticcp.core.domain.platform.dto.FeatureFlagAuditLogResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 기능 플래그 감사 로그 컨트롤러
 * 
 * 기능 플래그 변경에 대한 감사 로그를 조회하는 API를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/platform/feature-flags")
@RequiredArgsConstructor
@Tag(name = "Feature Flag Audit", description = "기능 플래그 감사 로그 조회 API")
public class FeatureFlagAuditController {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 특정 플래그의 감사 로그 조회
     * 
     * @param flagKey 플래그 키
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 감사 로그 목록
     */
    @GetMapping("/{flagKey}/audit-logs")
    @Operation(
            summary = "특정 플래그의 감사 로그 조회",
            description = "특정 기능 플래그의 변경 이력을 감사 로그로 조회합니다."
    )
    public ResponseEntity<ApiResponse<Page<FeatureFlagAuditLogResponse>>> getAuditLogsByFlagKey(
            @Parameter(description = "기능 플래그 키", example = "new-feature")
            @PathVariable String flagKey,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("[FeatureFlagAuditController] getAuditLogsByFlagKey - flagKey={} page={} size={}", 
                flagKey, page, size);

        // 페이징 설정
        Pageable pageable = PageRequest.of(
                page, 
                Math.min(Math.max(size, 1), 100), // 1~100 사이로 제한
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        // 감사 로그 조회
        Page<AuditLog> auditLogs = auditLogRepository.findByResourceTypeAndTargetResourceId(
                AuditResourceType.FEATURE_FLAG,
                flagKey,
                pageable
        );

        // 응답 변환
        Page<FeatureFlagAuditLogResponse> responses = auditLogs.map(this::convertToResponse);

        log.info("[FeatureFlagAuditController] getAuditLogsByFlagKey - success flagKey={} total={}", 
                flagKey, responses.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(
                responses, 
                String.format("감사 로그 조회가 완료되었습니다. (총 %d건)", responses.getTotalElements())
        ));
    }

    /**
     * 전체 기능 플래그 감사 로그 조회
     * 
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @param flagKey 필터: 플래그 키 (선택)
     * @param action 필터: 액션 (선택)
     * @return 감사 로그 목록
     */
    @GetMapping("/audit-logs")
    @Operation(
            summary = "전체 기능 플래그 감사 로그 조회",
            description = "모든 기능 플래그의 변경 이력을 감사 로그로 조회합니다. 필터링 옵션을 제공합니다."
    )
    public ResponseEntity<ApiResponse<Page<FeatureFlagAuditLogResponse>>> getAllAuditLogs(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "필터: 플래그 키", example = "new-feature")
            @RequestParam(required = false) String flagKey,
            @Parameter(description = "필터: 액션", example = "UPDATE")
            @RequestParam(required = false) String action) {
        
        log.info("[FeatureFlagAuditController] getAllAuditLogs - page={} size={} flagKey={} action={}", 
                page, size, flagKey, action);

        // 페이징 설정
        Pageable pageable = PageRequest.of(
                page, 
                Math.min(Math.max(size, 1), 100), // 1~100 사이로 제한
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Page<AuditLog> auditLogs;

        if (flagKey != null && !flagKey.trim().isEmpty()) {
            // 특정 플래그 키로 필터링
            auditLogs = auditLogRepository.findByResourceTypeAndTargetResourceId(
                    AuditResourceType.FEATURE_FLAG,
                    flagKey,
                    pageable
            );
        } else if (action != null && !action.trim().isEmpty()) {
            // 리소스 타입과 액션으로 필터링
            auditLogs = auditLogRepository.findByResourceTypeAndAction(
                    AuditResourceType.FEATURE_FLAG,
                    action,
                    pageable
            );
        } else {
            // 리소스 타입으로만 필터링
            auditLogs = auditLogRepository.findByResourceType(
                    AuditResourceType.FEATURE_FLAG,
                    pageable
            );
        }

        // 응답 변환
        Page<FeatureFlagAuditLogResponse> responses = auditLogs.map(this::convertToResponse);

        log.info("[FeatureFlagAuditController] getAllAuditLogs - success total={}", 
                responses.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(
                responses, 
                String.format("감사 로그 조회가 완료되었습니다. (총 %d건)", responses.getTotalElements())
        ));
    }

    /**
     * AuditLog 엔티티를 FeatureFlagAuditLogResponse로 변환
     * 
     * @param auditLog 감사 로그 엔티티
     * @return 응답 DTO
     */
    private FeatureFlagAuditLogResponse convertToResponse(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }

        try {
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> oldValue = parseJson(auditLog.getOldValue());
            Map<String, Object> newValue = parseJson(auditLog.getNewValue());
            Map<String, Object> changeDetails = parseJson(auditLog.getRequestData());
            Map<String, Object> metadata = parseJson(auditLog.getMetadata());

            return FeatureFlagAuditLogResponse.builder()
                    .id(auditLog.getId())
                    .flagKey(auditLog.getTargetResourceId())
                    .action(auditLog.getAction())
                    .severity(auditLog.getSeverity())
                    .timestamp(auditLog.getTimestamp())
                    .requestId(auditLog.getRequestId())
                    .tenantId(auditLog.getTenantId())
                    .userId(auditLog.getUserId())
                    .clientIp(auditLog.getClientIp())
                    .success(auditLog.getSuccess())
                    .error(auditLog.getError())
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .changeDetails(changeDetails)
                    .metadata(metadata)
                    .build();
        } catch (Exception e) {
            log.warn("[FeatureFlagAuditController] convertToResponse - JSON 파싱 실패 auditLogId={} error={}", 
                    auditLog.getId(), e.getMessage());
            
            // 파싱 실패 시에도 기본 정보는 반환
            return FeatureFlagAuditLogResponse.builder()
                    .id(auditLog.getId())
                    .flagKey(auditLog.getTargetResourceId())
                    .action(auditLog.getAction())
                    .severity(auditLog.getSeverity())
                    .timestamp(auditLog.getTimestamp())
                    .requestId(auditLog.getRequestId())
                    .tenantId(auditLog.getTenantId())
                    .userId(auditLog.getUserId())
                    .clientIp(auditLog.getClientIp())
                    .success(auditLog.getSuccess())
                    .error(auditLog.getError())
                    .build();
        }
    }

    /**
     * JSON 문자열을 Map으로 파싱
     * 
     * @param jsonString JSON 문자열
     * @return Map 객체 (파싱 실패 시 null)
     */
    private Map<String, Object> parseJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.debug("[FeatureFlagAuditController] parseJson - JSON 파싱 실패 jsonString={} error={}", 
                    jsonString, e.getMessage());
            return null;
        }
    }
}

