package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.CommonErrorCode;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

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
