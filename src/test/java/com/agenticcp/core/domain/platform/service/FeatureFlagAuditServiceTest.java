package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.audit.AuditLogger;
import com.agenticcp.core.common.audit.AuditPublishEvent;
import com.agenticcp.core.common.context.AuditContextProvider;
import com.agenticcp.core.common.dto.audit.AuditContextDto;
import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.enums.FeatureFlagSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FeatureFlagAuditService 단위 테스트
 * 
 * 기능 플래그 변경에 대한 감사 로깅 서비스의 핵심 기능을 검증합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagAuditService 테스트")
class FeatureFlagAuditServiceTest {

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditContextProvider auditContextProvider;

    @InjectMocks
    private FeatureFlagAuditService auditService;

    private FeatureFlag testFlag;
    private FeatureFlag oldFlag;
    private AuditContextDto mockContext;

    @BeforeEach
    void setUp() {
        // Mock AuditContextProvider 설정 
        mockContext = AuditContextDto.builder()
                .requestId("test-request-id")
                .tenantId("test-tenant-id")
                .clientIp("127.0.0.1")
                .userId("test-user-id")
                .build();
        lenient().when(auditContextProvider.getCurrentContext()).thenReturn(mockContext);

        // 테스트용 FeatureFlag 생성
        testFlag = FeatureFlag.builder()
                .flagKey("test-flag")
                .flagName("Test Flag")
                .description("Test Description")
                .isEnabled(true)
                .status(Status.ACTIVE)
                .severity(FeatureFlagSeverity.LOW)
                .rolloutPercentage(100)
                .build();

        oldFlag = FeatureFlag.builder()
                .flagKey("test-flag")
                .flagName("Old Flag Name")
                .description("Old Description")
                .isEnabled(false)
                .status(Status.ACTIVE)
                .severity(FeatureFlagSeverity.LOW)
                .rolloutPercentage(50)
                .build();
    }

    @Test
    @DisplayName("LOW 심각도 플래그 변경 로깅")
    void testLogFlagChange_LowSeverity() {
        // Given
        testFlag.setSeverity(FeatureFlagSeverity.LOW);
        String action = "UPDATE";
        String userId = "user-123";

        // When
        auditService.logFlagChange(oldFlag, testFlag, action, userId, FeatureFlagSeverity.LOW);

        // Then
        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(captor.capture());
        AuditEventDto event = captor.getValue();

        assertEquals("UPDATE", event.action());
        assertEquals(AuditResourceType.FEATURE_FLAG, event.resourceType());
        assertEquals(AuditSeverity.LOW, event.severity());
        assertEquals("test-flag", event.targetResourceId());
        assertEquals(userId, event.userId());
        assertTrue(event.success());

        // 이벤트 발행 검증
        verify(eventPublisher).publishEvent(any(AuditPublishEvent.class));
    }

    @Test
    @DisplayName("HIGH 심각도 플래그 변경 로깅")
    void testLogFlagChange_HighSeverity() {
        // Given
        testFlag.setSeverity(FeatureFlagSeverity.HIGH);
        String action = "UPDATE";
        String userId = "user-456";

        // When
        auditService.logFlagChange(oldFlag, testFlag, action, userId, FeatureFlagSeverity.HIGH);

        // Then
        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(captor.capture());
        AuditEventDto event = captor.getValue();

        assertEquals("UPDATE", event.action());
        assertEquals(AuditResourceType.FEATURE_FLAG, event.resourceType());
        assertEquals(AuditSeverity.HIGH, event.severity());
        assertEquals("test-flag", event.targetResourceId());
        assertEquals(userId, event.userId());
        assertTrue(event.success());

        // 이벤트 발행 검증
        verify(eventPublisher).publishEvent(any(AuditPublishEvent.class));
    }

    @Test
    @DisplayName("FeatureFlagSeverity를 AuditSeverity로 변환")
    void testConvertToAuditSeverity() {
        // Given & When & Then
        assertEquals(AuditSeverity.LOW, auditService.convertToAuditSeverity(FeatureFlagSeverity.LOW));
        assertEquals(AuditSeverity.MEDIUM, auditService.convertToAuditSeverity(FeatureFlagSeverity.MEDIUM));
        assertEquals(AuditSeverity.HIGH, auditService.convertToAuditSeverity(FeatureFlagSeverity.HIGH));
        assertEquals(AuditSeverity.CRITICAL, auditService.convertToAuditSeverity(FeatureFlagSeverity.CRITICAL));
        assertEquals(AuditSeverity.INFO, auditService.convertToAuditSeverity(null));
    }

    @Test
    @DisplayName("플래그 변경 로깅 성공")
    void testLogFlagChange_Success() {
        // Given
        String action = "CREATE";
        String userId = "user-789";

        // When
        auditService.logFlagChange(null, testFlag, action, userId);

        // Then
        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(captor.capture());
        AuditEventDto event = captor.getValue();

        assertEquals("CREATE", event.action());
        assertEquals(AuditResourceType.FEATURE_FLAG, event.resourceType());
        assertEquals("test-flag", event.targetResourceId());
        assertEquals(userId, event.userId());
        assertTrue(event.success());
        assertNotNull(event.newValue());
        assertNull(event.oldValue());

        // 이벤트 발행 검증
        verify(eventPublisher).publishEvent(any(AuditPublishEvent.class));
    }

    @Test
    @DisplayName("변경 상세 정보 생성")
    void testCreateChangeDetails() {
        // Given
        String action = "UPDATE";

        // When
        Map<String, Object> changeDetails = auditService.createChangeDetails(oldFlag, testFlag, action);

        // Then
        assertNotNull(changeDetails);
        assertEquals("test-flag", changeDetails.get("flagKey"));
        assertEquals("UPDATE", changeDetails.get("action"));
        assertEquals("FEATURE_FLAG_CHANGE", changeDetails.get("eventType"));
        assertEquals("FEATURE_FLAG", changeDetails.get("eventCategory"));
        assertEquals("Old Flag Name", changeDetails.get("oldFlagName"));
        assertEquals("Test Flag", changeDetails.get("newFlagName"));
        assertEquals(false, changeDetails.get("oldIsEnabled"));
        assertEquals(true, changeDetails.get("newIsEnabled"));

        // 변경된 필드 확인
        @SuppressWarnings("unchecked")
        Map<String, Object> changedFields = (Map<String, Object>) changeDetails.get("changedFields");
        assertNotNull(changedFields);
        assertTrue(changedFields.containsKey("flagName"));
        assertTrue(changedFields.containsKey("isEnabled"));
        assertTrue(changedFields.containsKey("rolloutPercentage"));
    }

    @Test
    @DisplayName("이벤트 발행")
    void testPublishEvent() {
        // Given
        AuditEventDto event = new AuditEventDto(
                "UPDATE",
                AuditResourceType.FEATURE_FLAG,
                null,
                null,
                null,
                null,
                null,
                AuditSeverity.MEDIUM,
                java.time.Instant.now(),
                null,
                null,
                "user-123",
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                "test-flag"
        );

        // When
        auditService.publishEvent(event);

        // Then
        ArgumentCaptor<AuditPublishEvent> captor = ArgumentCaptor.forClass(AuditPublishEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        AuditPublishEvent publishedEvent = captor.getValue();
        assertNotNull(publishedEvent);
    }

    @Test
    @DisplayName("액션 정규화 - CREATE")
    void testNormalizeAction_Create() {
        // Given
        String[] createActions = {"create", "CREATED", "add", "ADDED"};

        // When & Then
        for (String action : createActions) {
            auditService.logFlagChange(null, testFlag, action, "user-1");
            ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
            verify(auditLogger, atLeastOnce()).log(captor.capture());
            assertEquals("CREATE", captor.getValue().action());
        }
    }

    @Test
    @DisplayName("액션 정규화 - UPDATE")
    void testNormalizeAction_Update() {
        // Given
        String[] updateActions = {"update", "UPDATED", "modify", "MODIFIED", "change", "CHANGED"};

        // When & Then
        for (String action : updateActions) {
            auditService.logFlagChange(oldFlag, testFlag, action, "user-1");
            ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
            verify(auditLogger, atLeastOnce()).log(captor.capture());
            assertEquals("UPDATE", captor.getValue().action());
        }
    }

    @Test
    @DisplayName("액션 정규화 - DELETE")
    void testNormalizeAction_Delete() {
        // Given
        String[] deleteActions = {"delete", "DELETED", "remove", "REMOVED"};

        // When & Then
        for (String action : deleteActions) {
            auditService.logFlagChange(testFlag, null, action, "user-1");
            ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
            verify(auditLogger, atLeastOnce()).log(captor.capture());
            assertEquals("DELETE", captor.getValue().action());
        }
    }

    @Test
    @DisplayName("액션 정규화 - TOGGLE")
    void testNormalizeAction_Toggle() {
        // Given
        String[] toggleActions = {"toggle", "TOGGLED", "enable", "disable"};

        // When & Then
        for (String action : toggleActions) {
            auditService.logFlagChange(oldFlag, testFlag, action, "user-1");
            ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
            verify(auditLogger, atLeastOnce()).log(captor.capture());
            assertEquals("TOGGLE", captor.getValue().action());
        }
    }

    @Test
    @DisplayName("null 액션 처리")
    void testNormalizeAction_Null() {
        // When
        auditService.logFlagChange(oldFlag, testFlag, null, "user-1");

        // Then
        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditLogger).log(captor.capture());
        assertEquals("UPDATE", captor.getValue().action());
    }

    @Test
    @DisplayName("변경 상세 정보 - CREATE 케이스")
    void testCreateChangeDetails_Create() {
        // When
        Map<String, Object> changeDetails = auditService.createChangeDetails(null, testFlag, "CREATE");

        // Then
        assertNotNull(changeDetails);
        assertEquals("test-flag", changeDetails.get("flagKey"));
        assertEquals("CREATE", changeDetails.get("action"));
        assertEquals("Test Flag", changeDetails.get("newFlagName"));
        assertNull(changeDetails.get("oldFlagName"));
    }

    @Test
    @DisplayName("변경 상세 정보 - DELETE 케이스")
    void testCreateChangeDetails_Delete() {
        // When
        Map<String, Object> changeDetails = auditService.createChangeDetails(testFlag, null, "DELETE");

        // Then
        assertNotNull(changeDetails);
        assertEquals("test-flag", changeDetails.get("flagKey"));
        assertEquals("DELETE", changeDetails.get("action"));
        assertEquals("Test Flag", changeDetails.get("oldFlagName"));
        assertNull(changeDetails.get("newFlagName"));
    }

    @Test
    @DisplayName("변경 상세 정보 - 변경 없음")
    void testCreateChangeDetails_NoChanges() {
        // Given
        FeatureFlag sameFlag = FeatureFlag.builder()
                .flagKey("test-flag")
                .flagName("Test Flag")
                .description("Test Description")
                .isEnabled(true)
                .status(Status.ACTIVE)
                .severity(FeatureFlagSeverity.LOW)
                .rolloutPercentage(100)
                .build();

        // When
        Map<String, Object> changeDetails = auditService.createChangeDetails(testFlag, sameFlag, "UPDATE");

        // Then
        assertNotNull(changeDetails);
        assertEquals("test-flag", changeDetails.get("flagKey"));
        assertEquals("UPDATE", changeDetails.get("action"));
        // changedFields가 없거나 비어있어야 함
        @SuppressWarnings("unchecked")
        Map<String, Object> changedFields = (Map<String, Object>) changeDetails.get("changedFields");
        assertTrue(changedFields == null || changedFields.isEmpty());
    }
}

