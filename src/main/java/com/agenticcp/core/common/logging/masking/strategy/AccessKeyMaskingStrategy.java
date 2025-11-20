package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * Access Key 마스킹 전략
 * 
 * AWS Access Key ID와 같은 Access Key는 앞 4자리만 표시하고 나머지는 마스킹 처리합니다.
 * 예: AKIAIOSFODNN7EXAMPLE -> AKIA-****-****-****-EXAMPLE
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
        
        // 너무 짧은 경우 전체 마스킹
        if (value.length() < 8) {
            return "****";
        }
        
        // AWS Access Key ID는 보통 20자리이므로 앞 4자리만 노출
        // 일반적으로 앞 4자리만 노출하는 것이 안전
        if (value.length() <= 4) {
            return "****";
        }
        
        // 앞 4자리만 노출하고 나머지는 마스킹
        String firstFour = value.substring(0, 4);
        int remainingLength = value.length() - 4;
        
        // 나머지 길이에 따라 마스킹 패턴 조정
        if (remainingLength <= 4) {
            return firstFour + "-****";
        } else if (remainingLength <= 8) {
            return firstFour + "-****-****";
        } else if (remainingLength <= 12) {
            return firstFour + "-****-****-****";
        } else {
            return firstFour + "-****-****-****-****";
        }
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.ACCESS_KEY;
    }
}

