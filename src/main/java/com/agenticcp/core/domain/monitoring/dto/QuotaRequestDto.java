package com.agenticcp.core.domain.monitoring.dto;

import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 테넌트별 할당량 설정 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-02
 */
@Getter
@Builder
@AllArgsConstructor
public class QuotaRequestDto {
    
    /**
     * 일일 메트릭 수집량 제한
     */
    @NotNull(message = "일일 메트릭 수집량 제한은 필수입니다")
    @Min(value = 1, message = "일일 메트릭 수집량 제한은 최소 1 이상이어야 합니다")
    @Max(value = 10000000, message = "일일 메트릭 수집량 제한은 최대 10,000,000 이하여야 합니다")
    private final Long dailyMetricLimit;
    
    /**
     * 메트릭 저장 공간 할당량 (MB)
     */
    @NotNull(message = "메트릭 저장 공간 할당량은 필수입니다")
    @Min(value = 1, message = "저장 공간 할당량은 최소 1MB 이상이어야 합니다")
    @Max(value = 1048576, message = "저장 공간 할당량은 최대 1TB(1048576MB) 이하여야 합니다")
    private final Long storageQuotaMb;
    
    /**
     * 할당량 초과 시 동작
     */
    @NotNull(message = "할당량 초과 시 동작은 필수입니다")
    private final QuotaExceededAction quotaExceededAction;
}
