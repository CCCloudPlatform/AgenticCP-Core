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
        String key = properties.getKey();
        if (key == null || key.isBlank()) {
            if (properties.getMissingKeyBehavior() == CryptoProperties.MissingKeyBehavior.FAIL) {
                throw new IllegalStateException("암호화 키(config.cipher.key)가 설정되지 않았습니다.");
            }
            // READ_ONLY 모드: 암복호화 불가한 더미 구현을 반환 (스켈레톤)
            return new EncryptionService() {
                @Override public String encrypt(String plaintext) { throw new IllegalStateException("READ_ONLY 모드: 암호화 비활성"); }
                @Override public String decrypt(String serializedCipher) { throw new IllegalStateException("READ_ONLY 모드: 복호화 비활성"); }
            };
        }

        byte[] keyBytes = Base64.getDecoder().decode(key);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("유효하지 않은 AES 키 길이: " + keyBytes.length);
        }
        // 기본 구현: 주 키로 암복호화. (보조 키는 차후 복호화 fallback 훅에 사용 예정)
        return new AesGcmEncryptionService(keyBytes);
    }
}


