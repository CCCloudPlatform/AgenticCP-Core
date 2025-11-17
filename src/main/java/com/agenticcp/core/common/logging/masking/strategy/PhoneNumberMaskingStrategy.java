package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * 전화번호 마스킹 전략입니다. 중간 4자리를 마스킹합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Component
public class PhoneNumberMaskingStrategy implements MaskingStrategy {
    
    // 숫자가 아닌 문자를 제거하는 정규표현식
    private static final String NON_DIGIT_PATTERN = "[^0-9]";
    
    // 기본 마스킹 결과
    private static final String DEFAULT_MASKED = "***-****-****";
    
    // 휴대폰 번호 마스킹 패턴
    private static final String MOBILE_MASK = "-****-";
    
    // 최소 길이
    private static final int MIN_LENGTH = 7;
    
    // 휴대폰 번호 길이
    private static final int MOBILE_LENGTH = 11;
    
    // 휴대폰 번호 앞자리
    private static final String MOBILE_PREFIX = "010";
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 숫자만 추출
        String numbersOnly = value.replaceAll(NON_DIGIT_PATTERN, "");
        
        if (numbersOnly.length() < MIN_LENGTH) {
            return DEFAULT_MASKED;
        }
        
        // 한국 휴대폰 번호 형식 (010-1234-5678)
        if (numbersOnly.length() == MOBILE_LENGTH && numbersOnly.startsWith(MOBILE_PREFIX)) {
            return numbersOnly.substring(0, 3) + MOBILE_MASK + numbersOnly.substring(7);
        }

        // 기본 마스킹 (앞 3자리, 뒤 4자리만 표시)
        String prefix = numbersOnly.substring(0, 3);
        String suffix = numbersOnly.substring(numbersOnly.length() - 4);
        return prefix + MOBILE_MASK + suffix;

    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.PHONE_NUMBER;
    }
}
