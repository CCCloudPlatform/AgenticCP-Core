package com.agenticcp.core.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 암복호화 키/알고리즘 설정 바인딩.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "config.cipher")
public class CryptoProperties {
    /**
     * Base64로 인코딩된 AES 키(256bit 권장)
     */
    private String key;

    /**
     * 알고리즘 명(기본 AES/GCM/NoPadding)
     */
    private String algorithm = "AES/GCM/NoPadding";
}


