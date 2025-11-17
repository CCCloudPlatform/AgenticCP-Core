package com.agenticcp.core.domain.monitoring.health.dto;

import com.agenticcp.core.domain.platform.entity.PlatformHealth.HealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 전체 헬스 상태 응답 DTO
 * 
 * <p>시스템의 모든 컴포넌트 상태를 종합하여 반환하는 응답 객체입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "전체 헬스 상태 응답")
public class HealthStatusResponse {
    
    /** 전체 상태 */
    @Schema(description = "전체 상태", example = "HEALTHY")
    private HealthStatus overallStatus;
    
    /** 타임스탬프 */
    @Schema(description = "헬스체크 수행 시간")
    private LocalDateTime timestamp;
    
    /** 컴포넌트별 헬스 상태 */
    @Schema(description = "컴포넌트별 헬스 상태 맵")
    private Map<String, HealthIndicatorResult> components;
    
    /** 응답 시간 (밀리초) */
    @Schema(description = "헬스체크 응답 시간 (밀리초)", example = "50")
    private Long responseTime;
    
    /** 메시지 */
    @Schema(description = "헬스체크 결과 메시지", example = "Health check completed")
    private String message;
}
