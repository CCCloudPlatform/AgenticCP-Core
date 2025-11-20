package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * Account Scope 마스킹 전략
 * 
 * AWS Account ID, Azure Subscription ID, GCP Project ID 등을 마스킹합니다.
 * 앞 4자리와 뒤 4자리만 표시하고 중간은 마스킹 처리합니다.
 * 예: 123456789012 -> 1234-****-9012
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class AccountScopeMaskingStrategy implements MaskingStrategy {
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 너무 짧은 경우 전체 마스킹
        if (value.length() < 8) {
            return "****";
        }
        
        // AWS Account ID는 12자리 숫자이므로 앞 4자리, 뒤 4자리 노출
        // UUID 형식의 경우도 앞뒤 일부만 노출
        if (value.length() <= 8) {
            // 8자리 이하인 경우 앞 2자리, 뒤 2자리만 노출
            String firstTwo = value.substring(0, 2);
            String lastTwo = value.substring(value.length() - 2);
            return firstTwo + "-****-" + lastTwo;
        }
        
        // 앞 4자리와 뒤 4자리만 표시
        String firstFour = value.substring(0, 4);
        String lastFour = value.substring(value.length() - 4);
        
        // 중간 부분의 길이에 따라 마스킹 패턴 조정
        int middleLength = value.length() - 8;
        if (middleLength <= 0) {
            return firstFour + "-" + lastFour;
        } else if (middleLength <= 4) {
            return firstFour + "-****-" + lastFour;
        } else if (middleLength <= 8) {
            return firstFour + "-****-****-" + lastFour;
        } else {
            return firstFour + "-****-****-****-" + lastFour;
        }
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.ACCOUNT_SCOPE;
    }
}

