package com.agenticcp.core.domain.monitoring.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 헬스체크 요약 응답 DTO
 * 
 * <p>전체 서비스의 헬스 상태 통계를 반환하는 응답 객체입니다.</p>
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
@Schema(description = "헬스체크 요약 정보")
public class HealthCheckSummary {
    
    /** 전체 서비스 수 */
    @Schema(description = "전체 서비스 수", example = "10")
    private Long totalServices;
    
    /** 정상 서비스 수 */
    @Schema(description = "정상 서비스 수", example = "8")
    private Long healthyServices;
    
    /** 경고 서비스 수 */
    @Schema(description = "경고 서비스 수", example = "1")
    private Long warningServices;
    
    /** 치명적 오류 서비스 수 */
    @Schema(description = "치명적 오류 서비스 수", example = "1")
    private Long criticalServices;
    
    /** 알 수 없는 서비스 수 */
    @Schema(description = "알 수 없는 서비스 수", example = "0")
    private Long unknownServices;
    
    /** 마지막 업데이트 시간 */
    @Schema(description = "마지막 업데이트 시간")
    private LocalDateTime lastUpdated;
}
