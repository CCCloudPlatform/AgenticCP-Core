package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.context.AuditChangeContext;
import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 감사 로깅 이벤트 통합 테스트
 * 
 * - 이벤트 발행 및 리스너 동작 검증
 * - 데이터베이스 저장 검증
 * - oldValue/newValue 캡쳐 검증
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@SpringBootTest
@Disabled("Integration test disabled")
class AuditEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        AuditChangeContext.clear();
    }

    @AfterEach
    void tearDown() {
        auditLogRepository.deleteAll();
        AuditChangeContext.clear();
    }

    @Test
    @DisplayName("감사 이벤트가 발행되면 DB에 저장되어야 한다")
    void testAuditEvent_발행되면_DB저장됨() {
        // Given
        AuditEventDto eventDto = createSampleAuditEvent("req-test-001", "test-user-001");

        // When
        AuditPublishEvent event = new AuditPublishEvent(this, eventDto);
        eventPublisher.publishEvent(event);

        // Then - 비동기 처리를 위해 대기
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "req-test-001".equals(log.getRequestId()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog savedLog = logs.get(0);
            assertThat(savedLog.getAction()).isEqualTo("CREATE_USER");
            assertThat(savedLog.getResourceType()).isEqualTo(AuditResourceType.USER);
            assertThat(savedLog.getSeverity()).isEqualTo(AuditSeverity.MEDIUM);
            assertThat(savedLog.getSuccess()).isTrue();
            assertThat(savedLog.getUserId()).isEqualTo("test-user-001");
            assertThat(savedLog.getTenantId()).isEqualTo("tenant-001");
        });
    }

    @Test
    @DisplayName("oldValue와 newValue가 포함된 감사 이벤트가 저장되어야 한다")
    void testAuditEvent_변경값_포함_저장됨() {
        // Given
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("name", "Old Name");
        oldValue.put("email", "old@example.com");
        oldValue.put("status", "ACTIVE");

        Map<String, Object> newValue = new HashMap<>();
        newValue.put("name", "New Name");
        newValue.put("email", "new@example.com");
        newValue.put("status", "SUSPENDED");

        AuditEventDto eventDto = createAuditEventWithChanges(oldValue, newValue, "user-123");

        // When
        AuditPublishEvent event = new AuditPublishEvent(this, eventDto);
        eventPublisher.publishEvent(event);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findByTargetResourceIdOrderByTimestampDesc("user-123");
            assertThat(logs).hasSize(1);

            AuditLog savedLog = logs.get(0);
            assertThat(savedLog.getOldValue()).isNotNull();
            assertThat(savedLog.getNewValue()).isNotNull();
            assertThat(savedLog.getTargetResourceId()).isEqualTo("user-123");
            assertThat(savedLog.getOldValue()).contains("Old Name");
            assertThat(savedLog.getNewValue()).contains("New Name");
        });
    }

    @Test
    @DisplayName("UPDATE 작업 시 oldValue가 캡쳐되어야 한다")
    void testAuditEvent_UPDATE작업시_oldValue_캡쳐됨() {
        // Given
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("userId", "user-456");
        oldValue.put("username", "olduser");
        oldValue.put("email", "old@test.com");
        oldValue.put("role", "VIEWER");

        Map<String, Object> newValue = new HashMap<>();
        newValue.put("userId", "user-456");
        newValue.put("username", "newuser");
        newValue.put("email", "new@test.com");
        newValue.put("role", "ADMIN");

        AuditEventDto eventDto = new AuditEventDto(
                "UPDATE_USER",
                AuditResourceType.USER,
                "PUT",
                "/api/users/user-456",
                "사용자 정보 업데이트",
                null,
                null,
                AuditSeverity.MEDIUM,
                Instant.now(),
                "req-update-001",
                "tenant-001",
                "admin-user",
                "127.0.0.1",
                true,
                null,
                null,
                null,
                new HashMap<>(),
                oldValue,
                newValue,
                "user-456"
        );

        // When
        eventPublisher.publishEvent(new AuditPublishEvent(this, eventDto));

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "UPDATE_USER".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getOldValue()).contains("olduser");
            assertThat(log.getNewValue()).contains("newuser");
            assertThat(log.getOldValue()).contains("VIEWER");
            assertThat(log.getNewValue()).contains("ADMIN");
        });
    }

    @Test
    @DisplayName("DELETE 작업 시 oldValue가 캡쳐되어야 한다")
    void testAuditEvent_DELETE작업시_oldValue_캡쳐됨() {
        // Given
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("userId", "user-789");
        oldValue.put("username", "deleteduser");
        oldValue.put("email", "deleted@test.com");
        oldValue.put("status", "ACTIVE");

        AuditEventDto eventDto = new AuditEventDto(
                "DELETE_USER",
                AuditResourceType.USER,
                "DELETE",
                "/api/users/user-789",
                "사용자 삭제",
                null,
                null,
                AuditSeverity.HIGH,
                Instant.now(),
                "req-delete-001",
                "tenant-001",
                "admin-user",
                "127.0.0.1",
                true,
                null,
                null,
                null,
                new HashMap<>(),
                oldValue,
                null,  // DELETE는 newValue가 null
                "user-789"
        );

        // When
        eventPublisher.publishEvent(new AuditPublishEvent(this, eventDto));

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "DELETE_USER".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getOldValue()).isNotNull();
            assertThat(log.getOldValue()).contains("deleteduser");
            assertThat(log.getOldValue()).contains("ACTIVE");
            assertThat(log.getNewValue()).isNull();
        });
    }

    @Test
    @DisplayName("변경 추적이 있는 감사 로그만 조회할 수 있어야 한다")
    void testAuditEvent_변경추적_조회() {
        // Given - 변경 추적이 있는 로그
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("field", "old");
        Map<String, Object> newValue = new HashMap<>();
        newValue.put("field", "new");

        AuditEventDto eventWithChanges = createAuditEventWithChanges(oldValue, newValue, "resource-1");
        eventPublisher.publishEvent(new AuditPublishEvent(this, eventWithChanges));

        // Given - 변경 추적이 없는 로그
        AuditEventDto eventWithoutChanges = createSampleAuditEvent("req-nochange-001", "user-nochange");
        eventPublisher.publishEvent(new AuditPublishEvent(this, eventWithoutChanges));

        // When & Then
        await().untilAsserted(() -> {
            List<AuditLog> changedLogs = auditLogRepository.findChangeHistoryByTargetResourceId("resource-1");
            assertThat(changedLogs).hasSize(1);
            assertThat(changedLogs.get(0).getOldValue()).isNotNull();
            assertThat(changedLogs.get(0).getNewValue()).isNotNull();
        });
    }

    @Test
    @DisplayName("실패한 작업도 감사 로그에 저장되어야 한다")
    void testAuditEvent_실패작업_저장됨() {
        // Given
        AuditEventDto eventDto = new AuditEventDto(
                "CREATE_USER",
                AuditResourceType.USER,
                "POST",
                "/api/users",
                null,
                null,
                null,
                AuditSeverity.MEDIUM,
                Instant.now(),
                "req-fail-001",
                "tenant-001",
                "test-user",
                "127.0.0.1",
                false,
                "Validation failed: Email already exists",
                null,
                null,
                new HashMap<>(),
                null,
                null,
                null
        );

        // When
        eventPublisher.publishEvent(new AuditPublishEvent(this, eventDto));

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "req-fail-001".equals(log.getRequestId()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getSuccess()).isFalse();
            assertThat(log.getError()).contains("Validation failed");
            assertThat(log.getError()).contains("Email already exists");
        });
    }

    @Test
    @DisplayName("대상 리소스 ID로 변경 이력을 조회할 수 있어야 한다")
    void testAuditEvent_리소스별_변경이력_조회() {
        // Given - 같은 리소스에 대한 여러 변경
        String targetResourceId = "user-999";

        // 첫 번째 변경
        Map<String, Object> old1 = Map.of("status", "PENDING");
        Map<String, Object> new1 = Map.of("status", "ACTIVE");
        AuditEventDto event1 = createAuditEventWithChanges(old1, new1, targetResourceId);
        eventPublisher.publishEvent(new AuditPublishEvent(this, event1));

        // 두 번째 변경
        Map<String, Object> old2 = Map.of("status", "ACTIVE");
        Map<String, Object> new2 = Map.of("status", "SUSPENDED");
        AuditEventDto event2 = createAuditEventWithChanges(old2, new2, targetResourceId);
        eventPublisher.publishEvent(new AuditPublishEvent(this, event2));

        // When & Then
        await().untilAsserted(() -> {
            List<AuditLog> history = auditLogRepository.findChangeHistoryByTargetResourceId(targetResourceId);
            assertThat(history).hasSize(2);
            assertThat(history).allMatch(log -> log.getTargetResourceId().equals(targetResourceId));
            assertThat(history).allMatch(log -> log.getOldValue() != null || log.getNewValue() != null);
        });
    }

    @Test
    @DisplayName("requestData와 responseData가 포함된 감사 이벤트가 저장되어야 한다")
    void testAuditEvent_요청응답데이터_포함_저장됨() {
        // Given
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("username", "newuser");
        requestData.put("email", "newuser@test.com");

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", "user-new-001");
        responseData.put("status", "CREATED");

        AuditEventDto eventDto = new AuditEventDto(
                "CREATE_USER",
                AuditResourceType.USER,
                "POST",
                "/api/users",
                null,
                null,
                null,
                AuditSeverity.MEDIUM,
                Instant.now(),
                "req-001",
                "tenant-001",
                "admin-user",
                "127.0.0.1",
                true,
                null,
                requestData,
                responseData,
                new HashMap<>(),
                null,
                null,
                null
        );

        // When
        eventPublisher.publishEvent(new AuditPublishEvent(this, eventDto));

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "req-001".equals(log.getRequestId()) 
                            && "admin-user".equals(log.getUserId()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getRequestData()).contains("newuser");
            assertThat(log.getResponseData()).contains("user-new-001");
        });
    }

    @Test
    @DisplayName("여러 감사 이벤트가 동시에 발행되어도 모두 저장되어야 한다")
    void testAuditEvent_동시발행_모두저장됨() {
        // Given
        int eventCount = 10;
        String testPrefix = "req-concurrent-";

        // When
        for (int i = 0; i < eventCount; i++) {
            AuditEventDto eventDto = new AuditEventDto(
                    "CREATE_USER",
                    AuditResourceType.USER,
                    "POST",
                    "/api/users",
                    null,
                    null,
                    null,
                    AuditSeverity.MEDIUM,
                    Instant.now(),
                    testPrefix + i,
                    "tenant-001",
                    "test-user-concurrent-" + i,
                    "127.0.0.1",
                    true,
                    null,
                    null,
                    null,
                    new HashMap<>(),
                    null,
                    null,
                    null
            );

            eventPublisher.publishEvent(new AuditPublishEvent(this, eventDto));
        }

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> log.getRequestId() != null && log.getRequestId().startsWith(testPrefix))
                    .toList();
            assertThat(logs).hasSize(eventCount);
        });
    }

    // 헬퍼 메서드들

    private AuditEventDto createSampleAuditEvent(String requestId, String userId) {
        return new AuditEventDto(
                "CREATE_USER",
                AuditResourceType.USER,
                "POST",
                "/api/users",
                "새 사용자 생성",
                null,
                null,
                AuditSeverity.MEDIUM,
                Instant.now(),
                requestId,
                "tenant-001",
                userId,
                "127.0.0.1",
                true,
                null,
                null,
                null,
                new HashMap<>(),
                null,
                null,
                null
        );
    }

    private AuditEventDto createAuditEventWithChanges(
            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            String targetResourceId
    ) {
        return new AuditEventDto(
                "UPDATE_USER",
                AuditResourceType.USER,
                "PUT",
                "/api/users/" + targetResourceId,
                "사용자 정보 업데이트",
                null,
                null,
                AuditSeverity.MEDIUM,
                Instant.now(),
                "req-change-001",
                "tenant-001",
                "admin-user",
                "127.0.0.1",
                true,
                null,
                null,
                null,
                new HashMap<>(),
                oldValue,
                newValue,
                targetResourceId
        );
    }
}

