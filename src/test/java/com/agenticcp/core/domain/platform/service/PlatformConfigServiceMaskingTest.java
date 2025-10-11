package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.agenticcp.core.domain.platform.validation.ConfigValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 조회 시 ENCRYPTED 타입 기본 마스킹("***")이 적용되는지 검증한다.
 */
public class PlatformConfigServiceMaskingTest {

    private PlatformConfigRepository repository;
    private EncryptionService encryptionService;
    private PlatformConfigService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PlatformConfigRepository.class);
        encryptionService = Mockito.mock(EncryptionService.class);
        List<ConfigValidator> validators = Collections.emptyList();
        service = new PlatformConfigService(repository, validators, encryptionService);
    }

    @Test
    void getAllConfigs_shouldMaskEncryptedValues() {
        PlatformConfig enc = PlatformConfig.builder()
                .configKey("encrypted.key")
                .configValue("ciphertext-base64-like")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isEncrypted(true)
                .build();
        PlatformConfig str = PlatformConfig.builder()
                .configKey("plain.key")
                .configValue("plain-value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .build();

        when(repository.findAllActive()).thenReturn(List.of(enc, str));

        List<PlatformConfig> result = service.getAllConfigs();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("***", result.stream().filter(c -> c.getConfigKey().equals("encrypted.key")).findFirst().get().getConfigValue());
        Assertions.assertEquals("plain-value", result.stream().filter(c -> c.getConfigKey().equals("plain.key")).findFirst().get().getConfigValue());
    }

    @Test
    void getConfigByKey_shouldMaskEncryptedValue() {
        PlatformConfig enc = PlatformConfig.builder()
                .configKey("encrypted.key")
                .configValue("ciphertext-base64-like")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isEncrypted(true)
                .build();
        when(repository.findByConfigKey(any())).thenReturn(Optional.of(enc));

        PlatformConfig masked = service.getConfigByKey("encrypted.key").orElseThrow();
        Assertions.assertEquals("***", masked.getConfigValue());
    }
}


