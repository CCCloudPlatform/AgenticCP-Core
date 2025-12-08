package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.CommonErrorCode;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 시스템 전역에서 사용하는 에러 코드를 코드 문자열로 조회하기 위한 레지스트리입니다.
 * 애플리케이션 시작 시 공통 에러 코드를 미리 등록하고,
 * 차후 각 도메인의 에러 코드를 확장 등록할 수 있습니다.
 *
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Component
public class ErrorCodeRegistry {
    
    private final Map<String, BaseErrorCode> registry = new ConcurrentHashMap<>();
    
    public ErrorCodeRegistry() {
        for (CommonErrorCode errorCode : CommonErrorCode.values()) {
            registry.put(errorCode.getCode(), errorCode);
        }
    }
    
    /**
     * 등록된 에러 코드를 코드 문자열 기준으로 조회합니다.
     *
     * @param code {@link BaseErrorCode#getCode()} 값
     * @return 등록된 에러 코드, 없으면 {@code null}
     */
    public BaseErrorCode get(String code) {
        return registry.get(code);
    }
}
