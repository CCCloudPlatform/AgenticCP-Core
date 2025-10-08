package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.dto.AuditEventDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * ConfigAuditService 단위 테스트
 * - 마스킹 규칙(ENCRYPTED) 적용
 * - 액션 정규화(CREATE/UPDATE/DELETE)
 * - 이벤트 타입/카테고리 매핑 메타데이터 확인
 */
public class ConfigAuditServiceTest {

    private AuditLogger auditLogger;
    private ConfigAuditService service;

    @BeforeEach
    void setUp() {
        auditLogger = Mockito.mock(AuditLogger.class);
        service = new ConfigAuditService(auditLogger);
    }

    @Test
    void shouldMaskOldAndNewValuesWhenEncrypted() {
        // when
        service.logUpdate(
                "secure.key",
                "old-secret-plain",
                "new-secret-plain",
                "user-1",
                "rotate secret",
                "ENCRYPTED"
        );

        // then
        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(captor.capture());
        AuditEventDto event = captor.getValue();

        assertEquals("UPDATE", event.action());
        assertEquals(AuditResourceType.PLATFORM_CONFIG, event.resourceType());
        assertEquals(AuditSeverity.INFO, event.severity());

        Map<String, Object> metadata = event.metadata(); // details 맵이 metadata로 전달됨
        assertNotNull(metadata);
        assertEquals("secure.key", metadata.get("configKey"));
        assertEquals("***", metadata.get("oldValue"));
        assertEquals("***", metadata.get("newValue"));
        assertEquals("ENCRYPTED", metadata.get("valueType"));
        assertEquals("CONFIGURATION_CHANGE", metadata.get("eventType"));
        assertEquals("CONFIGURE", metadata.get("eventCategory"));
    }

    @Test
    void shouldNormalizeActionAndSetTypeCategoryWhenPlain() {
        // when: 다양한 동의어 액션을 UPDATE로 정규화
        service.logConfigChange(
                "plain.key",
                "10",
                "20",
                "modified",
                "user-2",
                "policy tighten",
                "NUMBER"
        );

        // then
        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(captor.capture());
        AuditEventDto event = captor.getValue();

        assertEquals("UPDATE", event.action());
        assertEquals(AuditResourceType.PLATFORM_CONFIG, event.resourceType());
        assertNull(event.error());
        assertTrue(event.success());

        // responseData에는 eventType/eventCategory/resourceType 요약 메타가 들어감
        Map<String, Object> responseData = event.responseData();
        assertNotNull(responseData);
        assertEquals("CONFIGURATION_CHANGE", responseData.get("eventType"));
        assertEquals("CONFIGURE", responseData.get("eventCategory"));
        assertEquals("PlatformConfig", responseData.get("resourceType"));

        // metadata(details)에는 값들이 원문으로(비민감) 들어감
        Map<String, Object> metadata = event.metadata();
        assertEquals("NUMBER", metadata.get("valueType"));
        assertEquals("10", metadata.get("oldValue"));
        assertEquals("20", metadata.get("newValue"));
    }
}


