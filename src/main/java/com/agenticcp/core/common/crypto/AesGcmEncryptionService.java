package com.agenticcp.core.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM(256) 기반 암복호화 구현.
 * 직렬화 포맷: Base64( IV(12바이트) || CIPHERTEXT+TAG ).
 */
public class AesGcmEncryptionService implements EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BIT = 128; // 16 bytes
    private static final int IV_LENGTH_BYTE = 12; // 96 bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AesGcmEncryptionService(byte[] keyBytes) {
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("암호화 실패", e);
        }
    }

    @Override
    public String decrypt(String serializedCipher) {
        if (serializedCipher == null) {
            return null;
        }
        try {
            byte[] allBytes = Base64.getDecoder().decode(serializedCipher);
            if (allBytes.length <= IV_LENGTH_BYTE) {
                throw new IllegalArgumentException("암호문 포맷 오류");
            }
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherAndTag = new byte[allBytes.length - IV_LENGTH_BYTE];
            System.arraycopy(allBytes, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(allBytes, IV_LENGTH_BYTE, cipherAndTag, 0, cipherAndTag.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv));
            byte[] plaintext = cipher.doFinal(cipherAndTag);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("복호화 실패", e);
        }
    }
}


