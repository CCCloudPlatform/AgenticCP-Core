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
 * 헬스 인디케이터 결과 DTO
 * 
 * <p>개별 헬스 인디케이터의 체크 결과를 나타내는 응답 객체입니다.</p>
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
@Schema(description = "헬스 인디케이터 결과")
public class HealthIndicatorResult {
    
    /** 상태 */
    @Schema(description = "헬스 상태", example = "HEALTHY")
    private HealthStatus status;
    
    /** 메시지 */
    @Schema(description = "헬스체크 결과 메시지", example = "Service is healthy")
    private String message;
    
    /** 상세 정보 */
    @Schema(description = "헬스체크 상세 정보 (키-값 맵)")
    private Map<String, Object> details;
    
    /** 타임스탬프 */
    @Schema(description = "헬스체크 수행 시간")
    private LocalDateTime timestamp;
    
    /**
     * 정상 상태 결과 생성
     * 
     * @param message 헬스체크 결과 메시지
     * @return 정상 상태 헬스 인디케이터 결과
     */
    public static HealthIndicatorResult healthy(String message) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.HEALTHY)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 정상 상태 결과 생성 (상세 정보 포함)
     * 
     * @param message 헬스체크 결과 메시지
     * @param details 헬스체크 상세 정보
     * @return 정상 상태 헬스 인디케이터 결과
     */
    public static HealthIndicatorResult healthy(String message, Map<String, Object> details) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.HEALTHY)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 경고 상태 결과 생성
     * 
     * @param message 헬스체크 결과 메시지
     * @return 경고 상태 헬스 인디케이터 결과
     */
    public static HealthIndicatorResult warning(String message) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.WARNING)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 경고 상태 결과 생성 (상세 정보 포함)
     * 
     * @param message 헬스체크 결과 메시지
     * @param details 헬스체크 상세 정보
     * @return 경고 상태 헬스 인디케이터 결과
     */
    public static HealthIndicatorResult warning(String message, Map<String, Object> details) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.WARNING)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 치명적 오류 상태 결과 생성
     * 
     * @param message 헬스체크 결과 메시지
     * @return 치명적 오류 상태 헬스 인디케이터 결과
     */
    public static HealthIndicatorResult critical(String message) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.CRITICAL)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 치명적 오류 상태 결과 생성 (상세 정보 포함)
     * 
     * @param message 헬스체크 결과 메시지
     * @param details 헬스체크 상세 정보
     * @return 치명적 오류 상태 헬스 인디케이터 결과
     */
    public static HealthIndicatorResult critical(String message, Map<String, Object> details) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.CRITICAL)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
