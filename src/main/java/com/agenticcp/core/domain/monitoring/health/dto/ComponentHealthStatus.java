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
 * 컴포넌트 헬스 상태 응답 DTO
 * 
 * <p>특정 컴포넌트의 헬스 상태를 반환하는 응답 객체입니다.</p>
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
@Schema(description = "컴포넌트 헬스 상태 응답")
public class ComponentHealthStatus {
    
    /** 컴포넌트 이름 */
    @Schema(description = "컴포넌트 이름", example = "database")
    private String component;
    
    /** 상태 */
    @Schema(description = "헬스 상태", example = "HEALTHY")
    private HealthStatus status;
    
    /** 메시지 */
    @Schema(description = "헬스체크 결과 메시지", example = "Database is healthy")
    private String message;
    
    /** 상세 정보 */
    @Schema(description = "헬스체크 상세 정보 (키-값 맵)")
    private Map<String, Object> details;
    
    /** 타임스탬프 */
    @Schema(description = "헬스체크 수행 시간")
    private LocalDateTime timestamp;
    
    /** 응답 시간 (밀리초) */
    @Schema(description = "헬스체크 응답 시간 (밀리초)", example = "25")
    private Long responseTime;
}
