package com.agenticcp.core.domain.platform.util;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 로깅 시 민감한 정보를 마스킹하는 유틸리티 클래스.
 * - ENCRYPTED 타입 설정값은 로그에 출력 시 마스킹
 * - 시스템 설정값도 보안을 위해 마스킹 처리
 */
@Slf4j
public class LoggingUtils {
    
    private static final String MASKED_VALUE = "******";
    
    /**
     * PlatformConfig의 민감한 값을 마스킹하여 반환.
     * 
     * @param config PlatformConfig 객체
     * @return 마스킹된 설정값 (ENCRYPTED 타입이면 "******", 아니면 원본값)
     */
    public static String maskSensitiveValue(PlatformConfig config) {
        if (config == null || config.getConfigValue() == null) {
            return null;
        }
        
        // ENCRYPTED 타입이면 마스킹
        if (Boolean.TRUE.equals(config.getIsEncrypted()) || 
            PlatformConfig.ConfigType.ENCRYPTED.equals(config.getConfigType())) {
            return MASKED_VALUE;
        }
        
        // 시스템 설정이면서 민감할 수 있는 값들도 마스킹
        if (Boolean.TRUE.equals(config.getIsSystem()) && isSensitiveKey(config.getConfigKey())) {
            return MASKED_VALUE;
        }
        
        return config.getConfigValue();
    }
    
    /**
     * 설정 키가 민감한 정보를 포함하는지 확인.
     * 
     * @param configKey 설정 키
     * @return 민감한 키 여부
     */
    private static boolean isSensitiveKey(String configKey) {
        if (configKey == null) {
            return false;
        }
        
        String lowerKey = configKey.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("key") ||
               lowerKey.contains("token") ||
               lowerKey.contains("credential") ||
               lowerKey.contains("auth");
    }
    
    /**
     * 로깅용 설정 정보를 안전하게 포맷팅.
     * 
     * @param config PlatformConfig 객체
     * @return 로깅용 문자열
     */
    public static String formatConfigForLogging(PlatformConfig config) {
        if (config == null) {
            return "null";
        }
        
        return String.format("PlatformConfig{key='%s', type=%s, value='%s', isEncrypted=%s, isSystem=%s}", 
            config.getConfigKey(),
            config.getConfigType(),
            maskSensitiveValue(config),
            config.getIsEncrypted(),
            config.getIsSystem());
    }
}
