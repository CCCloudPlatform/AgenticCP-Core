package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.context.AuditChangeContext;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.repository.AuditLogRepository;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 감사 애노테이션 통합 테스트
 * 
 * - @AuditController 애노테이션 동작 검증
 * - @AuditRequired 애노테이션 동작 검증
 * - oldValue 캡쳐 검증 (UPDATE, DELETE)
 * - AOP와 이벤트 리스너 통합 검증
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@SpringBootTest
@Import(AuditAnnotationIntegrationTest.TestUserService.class)
class AuditAnnotationIntegrationTest {

    @Autowired
    private TestUserService testUserService;

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
    @DisplayName("@AuditRequired 애노테이션이 붙은 메서드는 감사 로그가 저장되어야 한다")
    void testAuditRequired_감사로그_저장됨() {
        // Given
        String userId = "user-001";
        String username = "testuser";

        // When
        testUserService.createUser(userId, username);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("CREATE_USER");
            assertThat(log.getResourceType()).isEqualTo(AuditResourceType.USER);
            assertThat(log.getSeverity()).isEqualTo(AuditSeverity.MEDIUM);
            assertThat(log.getSuccess()).isTrue();
        });
    }

    @Test
    @DisplayName("UPDATE 작업 시 oldValue가 캡쳐되어 저장되어야 한다")
    void testAuditRequired_UPDATE시_oldValue_캡쳐됨() {
        // Given
        String userId = "user-002";

        // oldValue 설정
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("userId", userId);
        oldValue.put("username", "oldname");
        oldValue.put("email", "old@test.com");
        oldValue.put("status", "ACTIVE");

        // When
        testUserService.updateUser(userId, "newname", oldValue);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "UPDATE_USER".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("UPDATE_USER");
            assertThat(log.getOldValue()).isNotNull();
            assertThat(log.getOldValue()).contains("oldname");
            assertThat(log.getOldValue()).contains("old@test.com");
            assertThat(log.getTargetResourceId()).isEqualTo(userId);
        });
    }

    @Test
    @DisplayName("DELETE 작업 시 oldValue가 캡쳐되어 저장되어야 한다")
    void testAuditRequired_DELETE시_oldValue_캡쳐됨() {
        // Given
        String userId = "user-003";

        // oldValue 설정
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("userId", userId);
        oldValue.put("username", "deleteduser");
        oldValue.put("email", "deleted@test.com");
        oldValue.put("status", "ACTIVE");

        // When
        testUserService.deleteUser(userId, oldValue);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "DELETE_USER".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("DELETE_USER");
            assertThat(log.getSeverity()).isEqualTo(AuditSeverity.HIGH);
            assertThat(log.getOldValue()).isNotNull();
            assertThat(log.getOldValue()).contains("deleteduser");
            assertThat(log.getNewValue()).isNull();  // DELETE는 newValue가 없음
            assertThat(log.getTargetResourceId()).isEqualTo(userId);
        });
    }

    @Test
    @DisplayName("여러 UPDATE 작업의 oldValue가 각각 정확히 캡쳐되어야 한다")
    void testAuditRequired_여러UPDATE_oldValue_각각캡쳐됨() {
        // Given
        String user1Id = "user-101";
        String user2Id = "user-102";

        Map<String, Object> oldValue1 = Map.of("userId", user1Id, "name", "user1");
        Map<String, Object> oldValue2 = Map.of("userId", user2Id, "name", "user2");

        // When
        testUserService.updateUser(user1Id, "newuser1", oldValue1);
        testUserService.updateUser(user2Id, "newuser2", oldValue2);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "UPDATE_USER".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(2);

            AuditLog log1 = logs.stream()
                    .filter(l -> l.getTargetResourceId().equals(user1Id))
                    .findFirst()
                    .orElseThrow();
            assertThat(log1.getOldValue()).contains("user1");

            AuditLog log2 = logs.stream()
                    .filter(l -> l.getTargetResourceId().equals(user2Id))
                    .findFirst()
                    .orElseThrow();
            assertThat(log2.getOldValue()).contains("user2");
        });
    }

    @Test
    @DisplayName("includeRequestData가 true면 요청 데이터가 저장되어야 한다")
    void testAuditRequired_요청데이터_포함_저장됨() {
        // Given
        String userId = "user-004";
        String username = "testuser4";

        // When
        testUserService.createUserWithRequestData(userId, username);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "CREATE_USER_WITH_DATA".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getRequestData()).isNotNull();
            assertThat(log.getRequestData()).contains(userId);
            assertThat(log.getRequestData()).contains(username);
        });
    }

    @Test
    @DisplayName("includeResponseData가 true면 응답 데이터가 저장되어야 한다")
    void testAuditRequired_응답데이터_포함_저장됨() {
        // Given
        String userId = "user-005";

        // When
        TestUserDto result = testUserService.getUserWithResponseData(userId);

        // Then
        assertThat(result).isNotNull();
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "GET_USER_WITH_DATA".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getResponseData()).isNotNull();
            assertThat(log.getResponseData()).contains(userId);
        });
    }

    @Test
    @DisplayName("메서드 실행 중 예외가 발생해도 감사 로그가 저장되어야 한다")
    void testAuditRequired_예외발생시도_감사로그_저장됨() {
        // Given
        String userId = "invalid-user";

        // When
        try {
            testUserService.updateUserWithException(userId);
        } catch (RuntimeException e) {
            // 예외 무시
        }

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "UPDATE_USER_EXCEPTION".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getSuccess()).isFalse();
            assertThat(log.getError()).isNotNull();
            assertThat(log.getError()).contains("User not found");
        });
    }

    @Test
    @DisplayName("심각도 레벨이 올바르게 저장되어야 한다")
    void testAuditRequired_심각도레벨_저장됨() {
        // Given & When
        testUserService.criticalOperation();

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "CRITICAL_OPERATION".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getSeverity()).isEqualTo(AuditSeverity.CRITICAL);
        });
    }

    @Test
    @DisplayName("변경 추적 기록을 대상 리소스 ID로 조회할 수 있어야 한다")
    void testAuditRequired_변경추적_조회() {
        // Given
        String userId = "user-track-001";

        Map<String, Object> oldValue1 = Map.of("status", "PENDING");
        testUserService.updateUser(userId, "step1", oldValue1);

        Map<String, Object> oldValue2 = Map.of("status", "ACTIVE");
        testUserService.updateUser(userId, "step2", oldValue2);

        Map<String, Object> oldValue3 = Map.of("status", "SUSPENDED");
        testUserService.updateUser(userId, "step3", oldValue3);

        // When & Then
        await().untilAsserted(() -> {
            List<AuditLog> history = auditLogRepository
                    .findByTargetResourceIdOrderByTimestampDesc(userId);

            assertThat(history).hasSizeGreaterThanOrEqualTo(3);
            assertThat(history).allMatch(log -> 
                    log.getTargetResourceId().equals(userId));
        });
    }

    // 테스트용 서비스 클래스
    @Service
    public static class TestUserService {

        @AuditRequired(
                action = "CREATE_USER",
                resourceType = AuditResourceType.USER,
                severity = AuditSeverity.MEDIUM,
                description = "새 사용자 생성"
        )
        public String createUser(String userId, String username) {
            return userId;
        }

        @AuditRequired(
                action = "UPDATE_USER",
                resourceType = AuditResourceType.USER,
                severity = AuditSeverity.MEDIUM,
                description = "사용자 정보 업데이트"
        )
        public String updateUser(String userId, String newName, Map<String, Object> oldValue) {
            // oldValue를 컨텍스트에 설정
            AuditChangeContext.setChangeData(oldValue, userId);
            return userId;
        }

        @AuditRequired(
                action = "DELETE_USER",
                resourceType = AuditResourceType.USER,
                severity = AuditSeverity.HIGH,
                description = "사용자 삭제"
        )
        public void deleteUser(String userId, Map<String, Object> oldValue) {
            // oldValue를 컨텍스트에 설정
            AuditChangeContext.setChangeData(oldValue, userId);
        }

        @AuditRequired(
                action = "CREATE_USER_WITH_DATA",
                resourceType = AuditResourceType.USER,
                severity = AuditSeverity.MEDIUM,
                includeRequestData = true
        )
        public String createUserWithRequestData(String userId, String username) {
            return userId;
        }

        @AuditRequired(
                action = "GET_USER_WITH_DATA",
                resourceType = AuditResourceType.USER,
                severity = AuditSeverity.INFO,
                includeResponseData = true
        )
        public TestUserDto getUserWithResponseData(String userId) {
            return new TestUserDto(userId, "testuser", "test@example.com");
        }

        @AuditRequired(
                action = "UPDATE_USER_EXCEPTION",
                resourceType = AuditResourceType.USER,
                severity = AuditSeverity.MEDIUM
        )
        public void updateUserWithException(String userId) {
            throw new RuntimeException("User not found: " + userId);
        }

        @AuditRequired(
                action = "CRITICAL_OPERATION",
                resourceType = AuditResourceType.POLICY,
                severity = AuditSeverity.CRITICAL,
                description = "중요한 보안 작업"
        )
        public void criticalOperation() {
            // 중요 작업 수행
        }
    }

    // 테스트용 DTO
    @Data
    public static class TestUserDto {
        private final String userId;
        private final String username;
        private final String email;
    }
}

