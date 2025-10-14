package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * 기본 마스킹 전략
 * 
 * 특별한 마스킹 규칙이 없는 경우 사용하는 기본 전략입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class DefaultMaskingStrategy implements MaskingStrategy {
    
    private static final String DEFAULT_MASK = "******";
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return DEFAULT_MASK;
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.DEFAULT;
    }
}
