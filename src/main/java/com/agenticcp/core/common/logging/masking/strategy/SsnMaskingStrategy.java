package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * 주민등록번호 마스킹 전략입니다. 뒤 6자리를 마스킹합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Component
public class SsnMaskingStrategy implements MaskingStrategy {
    
    // 주민등록번호 패턴 (13자리 숫자 또는 6자리-7자리 형태)
    private static final String SSN_PATTERN = "^\\d{6}-?\\d{7}$";
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 공백 제거
        String cleaned = value.replaceAll("\\s", "");
        
        // 주민등록번호 패턴 체크
        if (cleaned.matches(SSN_PATTERN)) {
            if (cleaned.contains("-")) {
                // 하이픈이 있는 경우: 900101-1234567 -> 900101-1******
                String[] parts = cleaned.split("-");
                if (parts.length == 2 && parts[0].length() == 6 && parts[1].length() == 7) {
                    return parts[0] + "-" + parts[1].charAt(0) + "******";
                }
            } else if (cleaned.length() == 13) {
                // 하이픈이 없는 경우: 9001011234567 -> 9001011******
                return cleaned.substring(0, 7) + "******";
            }
        }
        
        // 패턴에 맞지 않으면 전체 마스킹
        return "***-*******";
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.SSN;
    }
}
