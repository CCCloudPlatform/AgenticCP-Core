package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * 기본 마스킹 전략입니다. 특별 규칙이 없을 때 사용됩니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
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
