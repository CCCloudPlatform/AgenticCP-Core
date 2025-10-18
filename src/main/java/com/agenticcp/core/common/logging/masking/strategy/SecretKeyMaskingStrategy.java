package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * 시크릿 키 마스킹 전략
 * 
 * 시크릿 키는 앞 4자리와 뒤 4자리만 표시하고 중간은 마스킹 처리합니다.
 * 예: abc123def456ghi789jkl -> abc1-****-****-****-89jkl
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class SecretKeyMaskingStrategy implements MaskingStrategy {
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 너무 짧은 경우 전체 마스킹
        if (value.length() < 8) {
            return "****-****-****-****";
        }
        
        // 앞 4자리와 뒤 4자리만 표시
        String firstFour = value.substring(0, 4);
        String lastFour = value.substring(value.length() - 4);
        
        // 중간 부분의 길이에 따라 마스킹 패턴 조정
        int middleLength = value.length() - 8;
        if (middleLength <= 0) {
            return firstFour + "-" + lastFour;
        }
        if (middleLength <= 4) {
            return firstFour + "-****-" + lastFour;
        }
        if (middleLength <= 8) {
            return firstFour + "-****-****-" + lastFour;
        }
        return firstFour + "-****-****-****-" + lastFour;
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.SECRET_KEY;
    }
}
