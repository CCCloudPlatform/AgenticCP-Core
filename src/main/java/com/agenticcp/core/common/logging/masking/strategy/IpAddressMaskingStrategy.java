package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingStrategy;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.stereotype.Component;

/**
 * IP 주소 마스킹 전략입니다. IPv4/IPv6 일부만 노출합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Component
public class IpAddressMaskingStrategy implements MaskingStrategy {
    
    @Override
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // IPv4
        if (value.contains(".")) {
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
