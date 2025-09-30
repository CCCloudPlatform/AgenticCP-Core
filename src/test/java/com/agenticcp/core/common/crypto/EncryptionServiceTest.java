package com.agenticcp.core.common.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionServiceTest {

    private static byte[] generateKey(int size) {
        byte[] key = new byte[size];
        new SecureRandom().nextBytes(key);
        return key;
    }

    @Test
    void encryptDecrypt_roundTrip_success() {
        byte[] key = generateKey(32); // 256-bit
        EncryptionService service = new AesGcmEncryptionService(key);
        String plaintext = "secret-value-한글-😊";

        String serialized = service.encrypt(plaintext);
        String decrypted = service.decrypt(serialized);

        Assertions.assertEquals(plaintext, decrypted);
    }

    @Test
    void decrypt_withDifferentKey_fail() {
        byte[] key1 = generateKey(32);
        byte[] key2 = generateKey(32);
        EncryptionService service1 = new AesGcmEncryptionService(key1);
        EncryptionService service2 = new AesGcmEncryptionService(key2);

        String serialized = service1.encrypt("hello");

        Assertions.assertThrows(RuntimeException.class, () -> service2.decrypt(serialized));
    }

    @Test
    void decrypt_invalidPayload_fail() {
        byte[] key = generateKey(32);
        EncryptionService service = new AesGcmEncryptionService(key);
        String invalid = Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(RuntimeException.class, () -> service.decrypt(invalid));
    }
}


