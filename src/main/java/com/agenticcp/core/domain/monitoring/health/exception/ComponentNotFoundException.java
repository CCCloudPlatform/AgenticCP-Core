package com.agenticcp.core.domain.monitoring.health.exception;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;

/**
 * 컴포넌트를 찾을 수 없을 때 발생하는 예외
 * 
 * 요청한 헬스체크 컴포넌트가 존재하지 않을 때 사용됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class ComponentNotFoundException extends BusinessException {
    
    public ComponentNotFoundException(String componentName) {
        super(MonitoringErrorCode.COMPONENT_NOT_FOUND, 
              String.format("Component '%s' not found", componentName));
    }
    
    public ComponentNotFoundException(String componentName, String message) {
        super(MonitoringErrorCode.COMPONENT_NOT_FOUND, message);
    }
}
