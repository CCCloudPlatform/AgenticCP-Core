package com.agenticcp.core.domain.monitoring.dto;

import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 테넌트별 수집기 설정 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TenantCollectorConfigDto {

    /**
     * 테넌트 ID
     */
    @NotBlank(message = "테넌트 ID는 필수입니다")
    @Size(max = 50, message = "테넌트 ID는 50자를 초과할 수 없습니다")
    private final String tenantId;

    /**
     * 수집기 타입
     */
    @NotNull(message = "수집기 타입은 필수입니다")
    private final CollectorType collectorType;

    /**
     * 활성화 여부
     */
    private final Boolean isEnabled;

    /**
     * 수집 주기 (밀리초) - 1분 이하로 제한
     */
    @Min(value = 1000, message = "수집 주기는 최소 1초(1000ms) 이상이어야 합니다")
    @Max(value = 60000, message = "메트릭 수집 주기는 1분(60000ms) 이하여야 합니다")
    private final Long collectionInterval;

    /**
     * 재시도 횟수
     */
    @Min(value = 0, message = "재시도 횟수는 0 이상이어야 합니다")
    @Max(value = 10, message = "재시도 횟수는 10 이하여야 합니다")
    private final Integer retryCount;

    /**
     * 타임아웃 (밀리초)
     */
    @Min(value = 1000, message = "타임아웃은 최소 1초(1000ms) 이상이어야 합니다")
    @Max(value = 300000, message = "타임아웃은 최대 5분(300000ms) 이하여야 합니다")
    private final Long timeout;

    /**
     * 수집할 메트릭 목록
     */
    private final List<String> targetMetrics;

    /**
     * 수집기별 추가 설정
     */
    private final Map<String, Object> collectorSettings;

    /**
     * 우선순위
     */
    @Min(value = 1, message = "우선순위는 1 이상이어야 합니다")
    @Max(value = 1000, message = "우선순위는 1000 이하여야 합니다")
    private final Integer priority;

    /**
     * 메타데이터
     */
    private final Map<String, String> metadata;

    // ===== 할당량 관련 필드들 =====
    
    /**
     * 일일 메트릭 수집량 제한
     */
    @Min(value = 1, message = "일일 메트릭 수집량 제한은 1 이상이어야 합니다")
    @Max(value = 10000000, message = "일일 메트릭 수집량 제한은 10,000,000 이하여야 합니다")
    private final Long dailyMetricLimit;
    
    /**
     * 메트릭 저장 공간 할당량 (MB)
     */
    @Min(value = 1, message = "저장 공간 할당량은 최소 1MB 이상이어야 합니다")
    @Max(value = 1048576, message = "저장 공간 할당량은 최대 1TB(1048576MB) 이하여야 합니다")
    private final Long storageQuotaMb;
    
    /**
     * 현재 일일 사용량
     */
    private final Long currentDailyUsage;
    
    /**
     * 현재 저장 공간 사용량 (MB)
     */
    private final Long currentStorageUsageMb;
    
    /**
     * 할당량 초과 시 동작
     */
    private final QuotaExceededAction quotaExceededAction;
    
    /**
     * 할당량 리셋 일시
     */
    private final LocalDateTime lastResetAt;
    
    /**
     * 할당량 초과 여부
     */
    private final Boolean isQuotaExceeded;
    
    /**
     * 일일 사용량 비율 (%)
     */
    private final Double dailyUsagePercentage;
    
    /**
     * 저장 공간 사용량 비율 (%)
     */
    private final Double storageUsagePercentage;
}
