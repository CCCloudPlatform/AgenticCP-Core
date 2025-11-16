package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.CommonErrorCode;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 시스템 전역에서 사용하는 에러 코드를 코드 문자열로 조회하기 위한 레지스트리입니다.
 * 애플리케이션 시작 시 공통 에러 코드를 미리 등록합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Component
public class ErrorCodeRegistry {
    
    private final Map<String, BaseErrorCode> registry = new ConcurrentHashMap<>();
    
    public ErrorCodeRegistry() {
        for (CommonErrorCode errorCode : CommonErrorCode.values()) {
            registry.put(errorCode.getCode(), errorCode);
        }
    }
    
    public BaseErrorCode get(String code) {
        return registry.get(code);
    }
}
