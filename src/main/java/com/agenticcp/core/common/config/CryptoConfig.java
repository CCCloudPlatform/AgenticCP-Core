package com.agenticcp.core.common.config;

import com.agenticcp.core.common.crypto.AesGcmEncryptionService;
import com.agenticcp.core.common.crypto.EncryptionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoConfig {

    @Bean
    public EncryptionService encryptionService(CryptoProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.getKey());
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("유효하지 않은 AES 키 길이: " + keyBytes.length);
        }
        return new AesGcmEncryptionService(keyBytes);
    }
}


