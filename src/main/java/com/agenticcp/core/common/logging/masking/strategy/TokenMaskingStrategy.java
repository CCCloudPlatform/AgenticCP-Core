package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * 토큰 마스킹 전략입니다. JWT 등 토큰 문자열의 앞/뒤 일부만 노출합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Component
public class TokenMaskingStrategy implements MaskingStrategy {
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 너무 짧은 경우 전체 마스킹
        if (value.length() < 8) {
            return "****-****-****-****";
        }
        
        // JWT 토큰 형식인지 확인 (3개의 점으로 구분)
        if (value.contains(".") && value.split("\\.").length == 3) {
            String[] parts = value.split("\\.");
            if (parts.length == 3) {
                // 각 부분을 개별적으로 마스킹
                String header = maskTokenPart(parts[0]);
                String payload = maskTokenPart(parts[1]);
                String signature = maskTokenPart(parts[2]);
                return header + "." + payload + "." + signature;
            }
        }
        
        // 일반 토큰 형식 (앞 4자리, 뒤 4자리)
        return maskTokenPart(value);
    }
    
    private String maskTokenPart(String part) {
        if (part.length() < 8) {
            return "****";
        }
        
        String firstFour = part.substring(0, 4);
        String lastFour = part.substring(part.length() - 4);
        return firstFour + "-****-****-" + lastFour;
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.TOKEN;
    }
}
