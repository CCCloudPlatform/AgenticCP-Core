package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.audit.AuditPublishEvent;
import com.agenticcp.core.common.dto.audit.AuditEventDto;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * ConfigAuditService 단위 테스트
 * - 하이브리드 접근법: 파일 기반 감사 로그 + RDBMS 기반 설정 이력
 * - CREATE/UPDATE/DELETE 시 감사 이벤트 생성
 * - ENCRYPTED 평문 노출 금지("***" 마스킹)
 * - ConfigAuditService 직접 호출 검증
 */
@Disabled("Integration test disabled")
public class PlatformConfigAuditIntegrationTest {

    private AuditLogger auditLogger;
    private ApplicationEventPublisher eventPublisher;

    private ConfigAuditService configAuditService;

    @BeforeEach
    void setUp() {
        auditLogger = Mockito.mock(AuditLogger.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

        configAuditService = new ConfigAuditService(auditLogger, eventPublisher);
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

        // then: 이벤트 퍼블리시 검증 (DB 저장은 리스너에서 처리)
        Mockito.verify(eventPublisher).publishEvent(Mockito.any(AuditPublishEvent.class));
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

        // then: 이벤트 퍼블리시 검증 (DB 저장은 리스너에서 처리)
        Mockito.verify(eventPublisher).publishEvent(Mockito.any(AuditPublishEvent.class));
    }
}


