package com.agenticcp.core.domain.tenant.adapter.dto;

import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 테넌트의 격리 상태 정보를 담는 DTO
 */
@Builder
public record IsolationStatus(
        String tenantKey,
        TenantIsolation.IsolationLevel isolationLevel,
        boolean isActive,
        LocalDateTime lastUpdated,
        Map<String, Object> resourceStatus, // CSP별 리소스 상태
        Map<String, String> configuration // 현재 설정 정보
) {
    
    public static IsolationStatus inactive(String tenantKey) {
        return IsolationStatus.builder()
                .tenantKey(tenantKey)
                .isActive(false)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}