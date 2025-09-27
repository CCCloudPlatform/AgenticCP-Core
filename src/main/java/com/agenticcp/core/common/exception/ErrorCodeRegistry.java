package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.enums.CommonErrorCode;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 모든 ErrorCode를 등록하고 관리하는 레지스트리
 * GlobalExceptionHandler에서 validation 에러 코드를 찾을 때 사용
 */
@Component
public class ErrorCodeRegistry {
    
    private final Map<String, BaseErrorCode> registry = new ConcurrentHashMap<>();
    
    public ErrorCodeRegistry() {
        // CommonErrorCode 등록
        for (CommonErrorCode errorCode : CommonErrorCode.values()) {
            registry.put(errorCode.getCode(), errorCode);
        }
        
        // AuthErrorCode 등록
        for (AuthErrorCode errorCode : AuthErrorCode.values()) {
            registry.put(errorCode.getCode(), errorCode);
        }
        
        // TODO: 다른 도메인 ErrorCode들도 추가
        // UserErrorCode, TenantErrorCode, CloudErrorCode 등
    }
    
    public BaseErrorCode get(String code) {
        return registry.get(code);
    }
    
    public boolean contains(String code) {
        return registry.containsKey(code);
    }
}
