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
     * 선택: 키 회전을 위한 보조 키 (Base64). decrypt 시 보조키도 시도.
     */
    private String secondaryKey;

    /**
     * 알고리즘 명(기본 AES/GCM/NoPadding)
     */
    private String algorithm = "AES/GCM/NoPadding";

    /**
     * 키 누락 시 동작 방침
     */
    private MissingKeyBehavior missingKeyBehavior = MissingKeyBehavior.FAIL;

    public enum MissingKeyBehavior {
        FAIL,      // 키 없으면 기동 실패 (fail-fast)
        READ_ONLY  // 읽기 전용 모드: 암복호화 비활성 (예외 발생)
    }
}


