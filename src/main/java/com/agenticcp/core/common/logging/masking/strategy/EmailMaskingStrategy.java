package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * 이메일 주소 마스킹 전략
 * 
 * 이메일 주소는 사용자명의 일부만 표시하고 나머지는 마스킹 처리합니다.
 * 예: john.doe@example.com -> j***.d***@example.com
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class EmailMaskingStrategy implements MaskingStrategy {
    
    private static final String EMAIL_SEPARATOR = "@";
    private static final String MASKED_EMAIL_PATTERN = "****@****.***";
    private static final String SINGLE_CHAR_MASK = "*";
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        if (!value.contains(EMAIL_SEPARATOR)) {
            return MASKED_EMAIL_PATTERN;
        }
        
        String[] parts = value.split(EMAIL_SEPARATOR);
        if (parts.length != 2) {
            return MASKED_EMAIL_PATTERN;
        }
        
        String username = parts[0];
        String domain = parts[1];
        
        // 사용자명 마스킹 (첫 글자와 마지막 글자만 표시)
        String maskedUsername = maskUsername(username);
        
        // 도메인은 그대로 유지 (보통 공개 정보)
        return maskedUsername + EMAIL_SEPARATOR + domain;
    }
    
    private String maskUsername(String username) {
        if (username.length() <= 1) {
            return SINGLE_CHAR_MASK;
        }
        if (username.length() == 2) {
            return username.charAt(0) + SINGLE_CHAR_MASK;
        }
        return username.charAt(0) + SINGLE_CHAR_MASK.repeat(username.length() - 2) + username.charAt(username.length() - 1);
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.EMAIL;
    }
}
