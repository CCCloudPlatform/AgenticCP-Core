package com.agenticcp.core.domain.tenant.adapter.dto;

import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 격리 정책 적용 결과를 담는 DTO
 */
@Builder
public record IsolationResult(
        boolean success,
        String message,
        String tenantKey,
        TenantIsolation.IsolationLevel isolationLevel,
        LocalDateTime appliedAt,
        Map<String, Object> resourceDetails, // CSP별 생성된 리소스 정보
        Map<String, String> errors // 오류 정보
) {
    
    public static IsolationResult success(String tenantKey, TenantIsolation.IsolationLevel level, 
                                        Map<String, Object> resourceDetails) {
        return IsolationResult.builder()
                .success(true)
                .message("Isolation policy applied successfully")
                .tenantKey(tenantKey)
                .isolationLevel(level)
                .appliedAt(LocalDateTime.now())
                .resourceDetails(resourceDetails)
                .build();
    }
    
    public static IsolationResult failure(String tenantKey, String message, Map<String, String> errors) {
        return IsolationResult.builder()
                .success(false)
                .message(message)
                .tenantKey(tenantKey)
                .appliedAt(LocalDateTime.now())
                .errors(errors)
                .build();
    }
}