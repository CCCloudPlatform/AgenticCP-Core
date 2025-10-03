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
 * @since 2024-01-01
 */
public class HealthCheckException extends BusinessException {
    
    public HealthCheckException(MonitoringErrorCode errorCode) {
        super(errorCode);
    }
    
    public HealthCheckException(MonitoringErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
}
