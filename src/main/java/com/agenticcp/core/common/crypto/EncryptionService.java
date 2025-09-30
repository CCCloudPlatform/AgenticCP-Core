package com.agenticcp.core.common.crypto;

/**
 * 민감한 설정 값을 안전하게 저장/조회하기 위한 암복호화 서비스의 계약입니다.
 */
public interface EncryptionService {

    /**
     * 평문을 암호화하여 직렬화 문자열(Base64 등)로 반환합니다.
     * @param plaintext 원본 평문
     * @return 암호문 직렬화 문자열
     */
    String encrypt(String plaintext);

    /**
     * 직렬화 문자열을 복호화하여 평문으로 반환합니다.
     * @param serializedCipher 직렬화된 암호문 문자열
     * @return 복호화된 평문
     */
    String decrypt(String serializedCipher);
}