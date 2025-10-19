package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * 신용카드 번호 마스킹 전략 (PCI DSS 표준 준수)
 * 
 * PCI DSS 표준에 따라 신용카드 번호는 앞 6자리와 뒤 4자리만 표시하고 중간은 마스킹 처리합니다.
 * 예: 1234-5678-9012-3456 -> 1234-56XX-XXXX-3456
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class CreditCardMaskingStrategy implements MaskingStrategy {
    
    // 숫자가 아닌 문자를 제거하는 정규표현식
    private static final String NON_DIGIT_PATTERN = "[^0-9]";
    
    // 전체 마스킹 결과
    private static final String FULLY_MASKED = "XXXX-XXXX-XXXX-XXXX";
    
    // 중간 마스킹 부분 (하이픈 형식)
    private static final String MIDDLE_MASK_HYPHEN = "-XX-XXXX-";
    
    // 중간 마스킹 부분 (공백 형식)
    private static final String MIDDLE_MASK_SPACE = " XX XXXX ";
    
    // 중간 마스킹 부분 (공백 없는 형식)
    private static final String MIDDLE_MASK_NONE = "XXXXXX";
    
    // 최소 길이 (앞 6자리 + 뒤 4자리)
    private static final int MIN_LENGTH = 10;
    
    // 앞 표시 길이 (PCI DSS 표준: 6자리)
    private static final int FRONT_VISIBLE_LENGTH = 6;
    
    // 뒤 표시 길이 (PCI DSS 표준: 4자리)
    private static final int BACK_VISIBLE_LENGTH = 4;
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 원본 형식 유지를 위해 구분자 추출
        String separator = detectSeparator(value);
        
        // 숫자만 추출
        String numbersOnly = value.replaceAll(NON_DIGIT_PATTERN, "");
        
        if (numbersOnly.length() < MIN_LENGTH) {
            // 너무 짧으면 전체 마스킹
            return FULLY_MASKED;
        }
        
        // PCI DSS 표준: 앞 6자리와 뒤 4자리만 표시
        String firstSix = numbersOnly.substring(0, FRONT_VISIBLE_LENGTH);
        String lastFour = numbersOnly.substring(numbersOnly.length() - BACK_VISIBLE_LENGTH);
        
        // 구분자에 따라 마스킹 형식 결정
        return formatMaskedCard(firstSix, lastFour, separator);
    }
    
    /**
     * 원본 문자열에서 구분자 감지
     */
    private String detectSeparator(String value) {
        if (value.contains("-")) {
            return "-";
        } else if (value.contains(" ")) {
            return " ";
        } else {
            return "";
        }
    }
    
    /**
     * 구분자에 따라 마스킹된 카드 번호 형식 생성
     */
    private String formatMaskedCard(String firstSix, String lastFour, String separator) {
        switch (separator) {
            case "-":
                return firstSix + MIDDLE_MASK_HYPHEN + lastFour;
            case " ":
                return firstSix + MIDDLE_MASK_SPACE + lastFour;
            default:
                return firstSix + MIDDLE_MASK_NONE + lastFour;
        }
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.CREDIT_CARD;
    }
}
