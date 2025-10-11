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
 * ConfigAuditService 단위 테스트
 * - 하이브리드 접근법: 파일 기반 감사 로그 + RDBMS 기반 설정 이력
 * - CREATE/UPDATE/DELETE 시 감사 이벤트 생성
 * - ENCRYPTED 평문 노출 금지("***" 마스킹)
 * - ConfigAuditService 직접 호출 검증
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
        platformConfigService = new PlatformConfigService(repository, validators, encryptionService);
    }

    @Test
    void updateEncryptedConfig_ShouldEmitMaskedAuditEvent() {
        // when: ConfigAuditService 직접 호출
        configAuditService.logUpdate(
                "secure.key",
                "ciphertext-old",
                "ciphertext-new",
                "user-1",
                "secret rotation",
                "ENCRYPTED"
        );

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
        // when: ConfigAuditService 직접 호출
        configAuditService.logCreate(
                "plain.key",
                "123",
                "user-1",
                "initial setup",
                "STRING"
        );

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
        // when: ConfigAuditService 직접 호출
        configAuditService.logUpdate(
                "test.key",
                "old-value",
                "new-value",
                "user-1",
                "test reason",
                "STRING"
        );

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
        // when: ConfigAuditService 직접 호출
        configAuditService.logCreate(
                "new.key",
                "new-value",
                "user-1",
                "initial setup",
                "STRING"
        );

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


