package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.common.dto.AuditEventDto;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.agenticcp.core.domain.platform.validation.ConfigValidator;
import com.agenticcp.core.domain.security.entity.AuditLog;
import com.agenticcp.core.domain.security.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * - 하이브리드 접근법: 파일 기반 감사 로그 + RDBMS 기반 설정 이력
 * - UPDATE 시 감사 이벤트 생성
 * - ENCRYPTED 평문 노출 금지("***" 마스킹)
 * - 트랜잭션 경계 내 호출 보장(서비스 호출 후 즉시 로깅 호출 검증)
 */
public class PlatformConfigAuditIntegrationTest {

    private PlatformConfigRepository repository;
    private EncryptionService encryptionService;
    private AuditLogger auditLogger;
    private AuditLogRepository auditLogRepository;
    private ObjectMapper objectMapper;

    private ConfigAuditService configAuditService;
    private PlatformConfigService platformConfigService;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PlatformConfigRepository.class);
        encryptionService = Mockito.mock(EncryptionService.class);
        auditLogger = Mockito.mock(AuditLogger.class);
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        objectMapper = new ObjectMapper();

        List<ConfigValidator> validators = Collections.emptyList();
        configAuditService = new ConfigAuditService(auditLogger, auditLogRepository, objectMapper);
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
        assertNotNull(event.requestData());
        assertEquals("***", event.requestData().get("oldValue"));
        assertEquals("***", event.requestData().get("newValue"));
        assertEquals("ENCRYPTED", event.requestData().get("valueType"));
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
        assertEquals("123", event.requestData().get("newValue"));
        assertEquals("", event.requestData().get("oldValue")); // EncryptedValueMasker.safeString()이 null을 ""로 변환
    }

    @Test
    void updateConfig_ShouldSaveToBothFileAndDatabase() {
        // given: 기존 설정
        PlatformConfig existing = PlatformConfig.builder()
                .configKey("test.key")
                .configValue("old-value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .description("test config")
                .build();

        PlatformConfig incoming = PlatformConfig.builder()
                .configKey("test.key")
                .configValue("new-value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .description("updated test config")
                .build();

        when(repository.findByConfigKey("test.key")).thenReturn(Optional.of(existing));
        when(repository.save(any(PlatformConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        // when: 업데이트 수행
        platformConfigService.updateConfig("test.key", incoming);

        // then: 파일 기반 감사 로그 검증
        ArgumentCaptor<AuditEventDto> auditEventCaptor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(auditEventCaptor.capture());
        AuditEventDto event = auditEventCaptor.getValue();
        assertEquals("UPDATE", event.action());

        // then: RDBMS 기반 설정 이력 저장 검증
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertEquals(AuditLog.EventType.CONFIGURATION_CHANGE, savedLog.getEventType());
        assertEquals(AuditLog.EventCategory.CONFIGURE, savedLog.getEventCategory());
        assertEquals("PlatformConfig", savedLog.getResourceType());
        assertEquals("test.key", savedLog.getResourceId());
        assertEquals("UPDATE", savedLog.getAction());
        assertEquals(AuditLog.Result.SUCCESS, savedLog.getResult());
        assertNotNull(savedLog.getEventTimestamp());
        assertNotNull(savedLog.getDetails());

        // details JSON에 변경 정보가 포함되어 있는지 확인
        assertTrue(savedLog.getDetails().contains("test.key"));
        assertTrue(savedLog.getDetails().contains("old-value"));
        assertTrue(savedLog.getDetails().contains("new-value"));
        assertTrue(savedLog.getDetails().contains("STRING"));
    }

    @Test
    void createConfig_ShouldSaveToBothFileAndDatabase() {
        // given: 새 설정 생성
        PlatformConfig incoming = PlatformConfig.builder()
                .configKey("new.key")
                .configValue("new-value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .description("new config")
                .build();

        when(repository.findByConfigKey("new.key")).thenReturn(Optional.empty());
        when(repository.save(any(PlatformConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        // when: 생성 수행
        platformConfigService.createConfig(incoming);

        // then: 파일 기반 감사 로그 검증
        ArgumentCaptor<AuditEventDto> auditEventCaptor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(auditEventCaptor.capture());
        AuditEventDto event = auditEventCaptor.getValue();
        assertEquals("CREATE", event.action());

        // then: RDBMS 기반 설정 이력 저장 검증
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertEquals(AuditLog.EventType.CONFIGURATION_CHANGE, savedLog.getEventType());
        assertEquals(AuditLog.EventCategory.CONFIGURE, savedLog.getEventCategory());
        assertEquals("PlatformConfig", savedLog.getResourceType());
        assertEquals("new.key", savedLog.getResourceId());
        assertEquals("CREATE", savedLog.getAction());
        assertEquals(AuditLog.Result.SUCCESS, savedLog.getResult());
        assertNotNull(savedLog.getEventTimestamp());
        assertNotNull(savedLog.getDetails());

        // details JSON에 생성 정보가 포함되어 있는지 확인
        assertTrue(savedLog.getDetails().contains("new.key"));
        assertTrue(savedLog.getDetails().contains("new-value"));
        assertTrue(savedLog.getDetails().contains("STRING"));
    }
}


