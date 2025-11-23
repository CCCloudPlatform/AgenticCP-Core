package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * Access Key 마스킹 전략
 * 
 * AWS Access Key ID, Azure Client ID, GCP Service Account Key 등을 마스킹합니다.
 * 예: AKIAIOSFODNN7EXAMPLE -> AKIA************MPLE
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class AccessKeyMaskingStrategy implements MaskingStrategy {
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 길이가 12 미만인 경우 전체 마스킹
        if (value.length() < 12) {
            return "****";
        }
        
        // 앞 4자리와 뒤 4자리만 노출하고 나머지는 마스킹
        String firstFour = value.substring(0, 4);
        String lastFour = value.substring(value.length() - 4);
        int maskedLength = value.length() - 8;
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maskedLength; i++) {
            masked.append('*');
        }
        return firstFour + masked + lastFour;
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.ACCESS_KEY;
    }
}

