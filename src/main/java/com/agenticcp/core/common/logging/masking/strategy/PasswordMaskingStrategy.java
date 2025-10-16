package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 마스킹 전략
 * 
 * 비밀번호는 보안상 전체를 별표로 마스킹 처리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class PasswordMaskingStrategy implements MaskingStrategy {
    
    private static final String MASKED_PASSWORD = "********";
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return MASKED_PASSWORD;
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.PASSWORD;
    }
}
