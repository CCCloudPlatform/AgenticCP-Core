package com.agenticcp.core.domain.monitoring.health.exception;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;

/**
 * 헬스체크 관련 비즈니스 예외
 * 
 * 헬스체크 수행 중 발생하는 비즈니스 로직 예외를 처리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-09
 */
public class HealthCheckException extends BusinessException {
    
    /**
     * 헬스체크 예외 생성
     * 
     * @param errorCode 모니터링 에러 코드
     */
    public HealthCheckException(MonitoringErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 헬스체크 예외 생성 (커스텀 메시지)
     * 
     * @param errorCode 모니터링 에러 코드
     * @param message 예외 메시지
     */
    public HealthCheckException(MonitoringErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
}
