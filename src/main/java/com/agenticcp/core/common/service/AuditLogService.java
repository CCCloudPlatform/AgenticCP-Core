package com.agenticcp.core.common.service;

import com.agenticcp.core.common.dto.audit.AuditLogResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSearchRequest;
import com.agenticcp.core.common.dto.audit.AuditLogSearchResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSummaryResponse;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.enums.AuditErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.agenticcp.core.common.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 감사 로그 조회 서비스
 * 
 * 테넌트별, 권한별 필터링을 적용하여 감사 로그를 조회합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 감사 로그 검색 (테넌트별 필터링 적용)
     */
    public AuditLogSearchResponse searchAuditLogs(AuditLogSearchRequest request) {
        // 현재 사용자 정보 추출
        String currentTenantId = getCurrentTenantId();
        String currentUserId = getCurrentUserId();
        
        log.debug("감사 로그 검색 요청 - 테넌트: {}, 사용자: {}", 
                 currentTenantId, currentUserId);

        validateAuditPermission();
        validateTenantAccess(currentTenantId);
        Pageable pageable = createPageable(request);
        
        Page<AuditLog> auditLogs = auditLogRepository.findByTenantIdAndFilters(
            currentTenantId,
            request.startDate(),
            request.endDate(),
            request.action(),
            request.resourceType(),
            request.severity(),
            request.userId(),
            request.success(),
            request.targetResourceId(),
            pageable
        );

        // 응답 변환
        List<AuditLogResponse> responses = auditLogs.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return new AuditLogSearchResponse(
            responses,
            auditLogs.getNumber(),
            auditLogs.getSize(),
            auditLogs.getTotalElements(),
            auditLogs.getTotalPages(),
            auditLogs.isFirst(),
            auditLogs.isLast()
        );
    }

    /**
     * 특정 리소스의 감사 로그 조회
     */
    public List<AuditLogResponse> getAuditLogsByResource(String targetResourceId) {
        // 권한 검증 (필수)
        validateAuditPermission();
        
        String currentTenantId = getCurrentTenantId();
        
        // 테넌트 정보 검증 (필수) - 보안상 중요하므로 유지
        validateTenantAccess(currentTenantId);

        List<AuditLog> auditLogs = auditLogRepository.findByTargetResourceIdAndTenantId(
            targetResourceId, currentTenantId);

        return auditLogs.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 감사 로그 조회 권한 검증 (필수)
     */
    private void validateAuditPermission() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("인증되지 않은 사용자의 감사 로그 조회 시도");
            throw new BusinessException(AuditErrorCode.INSUFFICIENT_AUDIT_PERMISSION);
        }
        
        // 권한 확인: SUPER_ADMIN, TENANT_ADMIN, AUDITOR 역할만 허용
        boolean hasPermission = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> 
                    "ROLE_SUPER_ADMIN".equals(authority) ||
                    "ROLE_TENANT_ADMIN".equals(authority) ||
                    "ROLE_AUDITOR".equals(authority)
                );
        
        if (!hasPermission) {
            log.error("감사 로그 조회 권한이 없는 사용자: {}", authentication.getName());
            throw new BusinessException(AuditErrorCode.INSUFFICIENT_AUDIT_PERMISSION);
        }
        
        log.debug("감사 로그 조회 권한 검증 완료 - 사용자: {}", authentication.getName());
    }

    /**
     * 테넌트 접근 권한 검증 (필수)
     */
    private void validateTenantAccess(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.error("테넌트 정보가 없습니다. 감사 로그 조회가 거부되었습니다.");
            throw new BusinessException(AuditErrorCode.INVALID_TENANT_CONTEXT);
        }
        
        log.debug("테넌트 접근 검증 완료 - 테넌트 ID: {}", tenantId);
    }


    /**
     * 현재 사용자의 테넌트 ID 추출
     */
    private String getCurrentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof JwtAuthenticationFilter.JwtAuthenticationDetails details) {
            return details.getTenantId() != null ? details.getTenantId().toString() : null;
        }
        return null;
    }

    /**
     * 현재 사용자 ID 추출
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return null;
    }


    /**
     * 페이징 및 정렬 설정
     */
    private Pageable createPageable(AuditLogSearchRequest request) {
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(request.sortDirection()) ? 
                Sort.Direction.DESC : Sort.Direction.ASC, 
            request.sortBy()
        );
        
        return PageRequest.of(request.page(), request.size(), sort);
    }

    /**
     * AuditLog 엔티티를 AuditLogResponse로 변환
     */
    private AuditLogResponse convertToResponse(AuditLog auditLog) {
        try {
            return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getHttpMethod(),
                auditLog.getRequestPath(),
                auditLog.getOperationSummary(),
                auditLog.getSeverity(),
                auditLog.getTimestamp(),
                auditLog.getRequestId(),
                auditLog.getTenantId(),
                auditLog.getUserId(),
                auditLog.getClientIp(),
                auditLog.getSuccess(),
                auditLog.getError(),
                auditLog.getTargetResourceId(),
                parseJsonToMap(auditLog.getRequestData()),
                parseJsonToMap(auditLog.getResponseData()),
                parseJsonToMap(auditLog.getMetadata()),
                parseJsonToMap(auditLog.getOldValue()),
                parseJsonToMap(auditLog.getNewValue())
            );
        } catch (Exception e) {
            log.error("AuditLog 변환 중 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(AuditErrorCode.AUDIT_LOG_CONVERSION_FAILED, 
                "감사 로그 변환 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 감사 로그 대시보드 요약 정보 조회
     */
    public AuditLogSummaryResponse getAuditLogSummary(Instant startDate, Instant endDate) {
        // 권한 검증
        validateAuditPermission();
        String currentTenantId = getCurrentTenantId();
        validateTenantAccess(currentTenantId);

        // 입력값 처리 (날짜 기본값 설정)
        Instant effectiveStartDate = (startDate != null) ? startDate : Instant.now().minusSeconds(30 * 24 * 60 * 60);
        Instant effectiveEndDate = (endDate != null) ? endDate : Instant.now();

        // 데이터 조회
        List<AuditLog> auditLogs = auditLogRepository.findByTenantIdAndTimestampBetween(
            currentTenantId, effectiveStartDate, effectiveEndDate);

        // 통계 계산
        AuditLogStatisticsCalculator calculator = new AuditLogStatisticsCalculator(auditLogs);

        long totalLogs = calculator.getTotalLogs();
        long successLogs = calculator.getSuccessLogs();
        long failedLogs = calculator.getFailedLogs(totalLogs, successLogs);
        double successRate = calculator.getSuccessRate(totalLogs, successLogs);
        Map<AuditSeverity, Long> severityDistribution = calculator.getSeverityDistribution();
        Map<String, Long> actionDistribution = calculator.getActionDistribution();
        Map<String, Long> dailyLogCount = calculator.getDailyLogCount();
        Map<String, Long> hourlyLogCount = calculator.getHourlyLogCount();

        return new AuditLogSummaryResponse(
            totalLogs,
            successLogs,
            failedLogs,
            successRate,
            severityDistribution,
            actionDistribution,
            dailyLogCount,
            hourlyLogCount,
            Instant.now()
        );
    }

    /**
     * JSON 문자열을 Map으로 파싱
     */
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JSON 파싱 실패: {}", json, e);
            return null;
        }
    }
}
