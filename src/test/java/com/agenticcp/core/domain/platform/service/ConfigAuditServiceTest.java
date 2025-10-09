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

        // requestData에서 상세 정보 확인
        Map<String, Object> requestData = event.requestData();
        assertNotNull(requestData);
        assertEquals("secure.key", requestData.get("configKey"));
        assertEquals("***", requestData.get("oldValue"));
        assertEquals("***", requestData.get("newValue"));
        assertEquals("ENCRYPTED", requestData.get("valueType"));
        assertEquals("CONFIGURATION_CHANGE", requestData.get("eventType"));
        assertEquals("CONFIGURE", requestData.get("eventCategory"));
        
        // metadata에서 메타 정보 확인
        Map<String, Object> metadata = event.metadata();
        assertNotNull(metadata);
        assertEquals("CONFIGURATION_CHANGE", metadata.get("eventType"));
        assertEquals("CONFIGURE", metadata.get("eventCategory"));
        assertEquals("PlatformConfig", metadata.get("resourceType"));
        assertEquals("secure.key", metadata.get("resourceId"));
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

        // responseData는 설정 변경 시에는 null (응답 데이터 없음)
        assertNull(event.responseData());

        // requestData에는 상세 정보가 들어감
        Map<String, Object> requestData = event.requestData();
        assertEquals("NUMBER", requestData.get("valueType"));
        assertEquals("10", requestData.get("oldValue"));
        assertEquals("20", requestData.get("newValue"));
        assertEquals("plain.key", requestData.get("configKey"));
        assertEquals("CONFIGURATION_CHANGE", requestData.get("eventType"));
        assertEquals("CONFIGURE", requestData.get("eventCategory"));
        
        // metadata에는 메타 정보가 들어감
        Map<String, Object> metadata = event.metadata();
        assertEquals("CONFIGURATION_CHANGE", metadata.get("eventType"));
        assertEquals("CONFIGURE", metadata.get("eventCategory"));
        assertEquals("PlatformConfig", metadata.get("resourceType"));
        assertEquals("plain.key", metadata.get("resourceId"));
    }
}


