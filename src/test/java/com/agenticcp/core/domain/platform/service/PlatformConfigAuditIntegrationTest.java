package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.common.dto.AuditEventDto;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.agenticcp.core.domain.platform.validation.ConfigValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 설정 CRUD ↔ 감사 로깅 통합 테스트(서비스 레벨)
 * - UPDATE 시 감사 이벤트 생성
 * - ENCRYPTED 평문 노출 금지("***" 마스킹)
 * - 트랜잭션 경계 내 호출 보장(서비스 호출 후 즉시 로깅 호출 검증)
 */
public class PlatformConfigAuditIntegrationTest {

    private PlatformConfigRepository repository;
    private EncryptionService encryptionService;
    private AuditLogger auditLogger;

    private ConfigAuditService configAuditService;
    private PlatformConfigService platformConfigService;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PlatformConfigRepository.class);
        encryptionService = Mockito.mock(EncryptionService.class);
        auditLogger = Mockito.mock(AuditLogger.class);

        List<ConfigValidator> validators = Collections.emptyList();
        configAuditService = new ConfigAuditService(auditLogger);
        platformConfigService = new PlatformConfigService(repository, validators, encryptionService, configAuditService);
    }

    @Test
    void updateEncryptedConfig_ShouldEmitMaskedAuditEvent() {
        // given: 기존 ENCRYPTED 설정
        PlatformConfig existing = PlatformConfig.builder()
                .configKey("secure.key")
                .configValue("ciphertext-old")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isEncrypted(true)
                .description("secret")
                .build();

        PlatformConfig incoming = PlatformConfig.builder()
                .configKey("secure.key")
                .configValue("new-plaintext") // 평문으로 들어와도 저장 직전 암호화 가정
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isEncrypted(true)
                .description("secret-updated")
                .build();

        when(repository.findByConfigKey("secure.key")).thenReturn(Optional.of(existing));
        when(encryptionService.encrypt("new-plaintext")).thenReturn("ciphertext-new");
        when(repository.save(any(PlatformConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        // when: 업데이트 수행
        platformConfigService.updateConfig("secure.key", incoming);

        // then: 감사 로그 호출 캡처 및 검증 (마스킹 적용 확인)
        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(captor.capture());
        AuditEventDto event = captor.getValue();

        assertEquals("UPDATE", event.action());
        assertNotNull(event.metadata());
        assertEquals("***", event.metadata().get("oldValue"));
        assertEquals("***", event.metadata().get("newValue"));
        assertEquals("ENCRYPTED", event.metadata().get("valueType"));
    }

    @Test
    void createPlainConfig_ShouldEmitPlainValuesInAudit() {
        // given: STRING 설정 생성
        PlatformConfig incoming = PlatformConfig.builder()
                .configKey("plain.key")
                .configValue("123")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .description("number as string")
                .build();

        when(repository.findByConfigKey("plain.key")).thenReturn(Optional.empty());
        when(repository.save(any(PlatformConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        platformConfigService.createConfig(incoming);

        // then: CREATE 감사 로그 생성되고 값은 마스킹 없이 기록
        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(captor.capture());
        AuditEventDto event = captor.getValue();

        assertEquals("CREATE", event.action());
        assertEquals("plain.key", event.requestData().get("configKey"));
        assertEquals("STRING", event.requestData().get("valueType"));
        assertEquals("123", event.metadata().get("newValue"));
        assertEquals("", event.metadata().get("oldValue")); // EncryptedValueMasker.safeString()이 null을 ""로 변환
    }
}


