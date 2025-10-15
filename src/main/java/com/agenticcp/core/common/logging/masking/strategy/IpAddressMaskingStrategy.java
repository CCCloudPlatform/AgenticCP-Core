package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * IP 주소 마스킹 전략
 * 
 * IP 주소의 마지막 두 옥텟을 마스킹 처리합니다.
 * 예: 192.168.1.100 -> 192.168.***.***
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class IpAddressMaskingStrategy implements MaskingStrategy {
    
    // IPv4 주소 패턴: 0-255.0-255.0-255.0-255
    private static final String IPV4_PATTERN = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$";
    
    // IPv4 마스킹 결과
    private static final String IPV4_MASKED = "***.***.***.***";
    
    // IPv6 마스킹 결과
    private static final String IPV6_MASKED = "****:****:****:****:****:****:****:****";
    
    // IPv4 구분자
    private static final String IPV4_SEPARATOR = "\\.";
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // IPv4 주소 패턴 체크 - 기존 LogMaskingUtils와 동일한 방식(추후 통합)
        if (value.contains(".")) { // IPv4
            String[] parts = value.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
            }
        } else if (value.contains(":")) { // IPv6
            String[] parts = value.split(":");
            if (parts.length >= 4) {
                return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":****";
            }
        }
        
        // 패턴에 맞지 않으면 원본 반환
        return value;
    }
    
    @Override
    public MaskingType getType() {
        return MaskingType.IP_ADDRESS;
    }
}
