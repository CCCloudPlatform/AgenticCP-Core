package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.context.AuditChangeContext;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.repository.AuditLogRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 감사 로그 변경 추적 테스트
 * 
 * - AuditChangeContext의 ThreadLocal 동작 검증
 * - oldValue 설정 및 조회 검증
 * - targetResourceId 설정 및 조회 검증
 * - 멀티스레드 환경에서 ThreadLocal 격리 검증
 * - 컨텍스트 자동 정리 검증
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@SpringBootTest
@Import(AuditChangeTrackingTest.ChangeTrackingTestService.class)
class AuditChangeTrackingTest {

    @Autowired
    private ChangeTrackingTestService testService;

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
    @DisplayName("AuditChangeContext에 oldValue를 설정하고 조회할 수 있어야 한다")
    void testChangeContext_oldValue_설정조회() {
        // Given
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("field1", "value1");
        oldValue.put("field2", 123);

        // When
        AuditChangeContext.setOldValue(oldValue);
        Map<String, Object> retrieved = AuditChangeContext.getOldValue();

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).containsEntry("field1", "value1");
        assertThat(retrieved).containsEntry("field2", 123);
    }

    @Test
    @DisplayName("AuditChangeContext에 targetResourceId를 설정하고 조회할 수 있어야 한다")
    void testChangeContext_targetResourceId_설정조회() {
        // Given
        String targetResourceId = "resource-001";

        // When
        AuditChangeContext.setTargetResourceId(targetResourceId);
        String retrieved = AuditChangeContext.getTargetResourceId();

        // Then
        assertThat(retrieved).isEqualTo(targetResourceId);
    }

    @Test
    @DisplayName("setChangeData로 oldValue와 targetResourceId를 한번에 설정할 수 있어야 한다")
    void testChangeContext_setChangeData_한번에설정() {
        // Given
        Map<String, Object> oldValue = Map.of("status", "ACTIVE");
        String targetResourceId = "resource-002";

        // When
        AuditChangeContext.setChangeData(oldValue, targetResourceId);

        // Then
        assertThat(AuditChangeContext.getOldValue()).isEqualTo(oldValue);
        assertThat(AuditChangeContext.getTargetResourceId()).isEqualTo(targetResourceId);
    }

    @Test
    @DisplayName("clear()를 호출하면 컨텍스트가 정리되어야 한다")
    void testChangeContext_clear_정리됨() {
        // Given
        Map<String, Object> oldValue = Map.of("field", "value");
        AuditChangeContext.setChangeData(oldValue, "resource-003");

        // When
        AuditChangeContext.clear();

        // Then
        assertThat(AuditChangeContext.getOldValue()).isNull();
        assertThat(AuditChangeContext.getTargetResourceId()).isNull();
    }

    @Test
    @DisplayName("멀티스레드 환경에서 ThreadLocal이 격리되어야 한다")
    void testChangeContext_멀티스레드_격리() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // When
        CompletableFuture<Void> thread1 = CompletableFuture.runAsync(() -> {
            Map<String, Object> oldValue1 = Map.of("thread", "thread-1");
            AuditChangeContext.setChangeData(oldValue1, "resource-thread-1");
            
            sleep(100);
            
            assertThat(AuditChangeContext.getTargetResourceId()).isEqualTo("resource-thread-1");
            assertThat(AuditChangeContext.getOldValue()).containsEntry("thread", "thread-1");
        }, executor);

        CompletableFuture<Void> thread2 = CompletableFuture.runAsync(() -> {
            Map<String, Object> oldValue2 = Map.of("thread", "thread-2");
            AuditChangeContext.setChangeData(oldValue2, "resource-thread-2");
            
            sleep(100);
            
            assertThat(AuditChangeContext.getTargetResourceId()).isEqualTo("resource-thread-2");
            assertThat(AuditChangeContext.getOldValue()).containsEntry("thread", "thread-2");
        }, executor);

        CompletableFuture<Void> thread3 = CompletableFuture.runAsync(() -> {
            Map<String, Object> oldValue3 = Map.of("thread", "thread-3");
            AuditChangeContext.setChangeData(oldValue3, "resource-thread-3");
            
            sleep(100);
            
            assertThat(AuditChangeContext.getTargetResourceId()).isEqualTo("resource-thread-3");
            assertThat(AuditChangeContext.getOldValue()).containsEntry("thread", "thread-3");
        }, executor);

        // Then
        CompletableFuture.allOf(thread1, thread2, thread3).get(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    @DisplayName("서비스 메서드에서 설정한 oldValue가 감사 로그에 저장되어야 한다")
    void testChangeContext_서비스에서설정_감사로그저장() {
        // Given
        String resourceId = "resource-004";
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("name", "Old Name");
        oldValue.put("status", "ACTIVE");

        // When
        testService.updateResource(resourceId, oldValue);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "UPDATE_RESOURCE".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getOldValue()).isNotNull();
            assertThat(log.getOldValue()).contains("Old Name");
            assertThat(log.getTargetResourceId()).isEqualTo(resourceId);
        });
    }

    @Test
    @DisplayName("연속된 작업에서 각 작업의 oldValue가 독립적으로 저장되어야 한다")
    void testChangeContext_연속작업_독립적저장() {
        // Given
        String resource1 = "resource-005";
        String resource2 = "resource-006";

        Map<String, Object> old1 = Map.of("name", "Resource 1", "version", 1);
        Map<String, Object> old2 = Map.of("name", "Resource 2", "version", 2);

        // When
        testService.updateResource(resource1, old1);
        testService.updateResource(resource2, old2);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "UPDATE_RESOURCE".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(2);

            AuditLog log1 = logs.stream()
                    .filter(l -> l.getTargetResourceId().equals(resource1))
                    .findFirst()
                    .orElseThrow();
            assertThat(log1.getOldValue()).contains("Resource 1");
            assertThat(log1.getOldValue()).containsPattern("\"version\"\\s*:\\s*1");

            AuditLog log2 = logs.stream()
                    .filter(l -> l.getTargetResourceId().equals(resource2))
                    .findFirst()
                    .orElseThrow();
            assertThat(log2.getOldValue()).contains("Resource 2");
            assertThat(log2.getOldValue()).containsPattern("\"version\"\\s*:\\s*2");
        });
    }

    @Test
    @DisplayName("DELETE 작업에서 oldValue만 설정되고 newValue는 null이어야 한다")
    void testChangeContext_DELETE작업_oldValue만설정() {
        // Given
        String resourceId = "resource-007";
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("name", "To Be Deleted");
        oldValue.put("status", "ACTIVE");

        // When
        testService.deleteResource(resourceId, oldValue);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "DELETE_RESOURCE".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getOldValue()).isNotNull();
            assertThat(log.getOldValue()).contains("To Be Deleted");
            assertThat(log.getNewValue()).isNull();
            assertThat(log.getTargetResourceId()).isEqualTo(resourceId);
        });
    }

    @Test
    @DisplayName("복잡한 객체가 oldValue로 저장되어야 한다")
    void testChangeContext_복잡한객체_저장() {
        // Given
        String resourceId = "resource-008";
        
        Map<String, Object> nestedObject = new HashMap<>();
        nestedObject.put("nested1", "value1");
        nestedObject.put("nested2", 123);

        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("simpleField", "simple");
        oldValue.put("numberField", 456);
        oldValue.put("booleanField", true);
        oldValue.put("nestedObject", nestedObject);

        // When
        testService.updateResource(resourceId, oldValue);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findByTargetResourceIdOrderByTimestampDesc(resourceId);
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getOldValue()).contains("simple");
            assertThat(log.getOldValue()).contains("456");
            assertThat(log.getOldValue()).contains("true");
            assertThat(log.getOldValue()).contains("nested1");
        });
    }

    @Test
    @DisplayName("예외가 발생해도 oldValue가 감사 로그에 저장되어야 한다")
    void testChangeContext_예외발생시도_oldValue저장() {
        // Given
        String resourceId = "resource-009";
        Map<String, Object> oldValue = Map.of("field", "value-before-error");

        // When
        try {
            testService.updateResourceWithError(resourceId, oldValue);
        } catch (RuntimeException e) {
            // 예외 무시
        }

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "UPDATE_RESOURCE_ERROR".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getSuccess()).isFalse();
            assertThat(log.getOldValue()).isNotNull();
            assertThat(log.getOldValue()).contains("value-before-error");
        });
    }

    // 테스트용 서비스
    @Service
    public static class ChangeTrackingTestService {

        @AuditRequired(
                action = "UPDATE_RESOURCE",
                resourceType = AuditResourceType.CLOUD_PROVIDER,
                severity = AuditSeverity.MEDIUM
        )
        public void updateResource(String resourceId, Map<String, Object> oldValue) {
            AuditChangeContext.setChangeData(oldValue, resourceId);
        }

        @AuditRequired(
                action = "DELETE_RESOURCE",
                resourceType = AuditResourceType.CLOUD_PROVIDER,
                severity = AuditSeverity.HIGH
        )
        public void deleteResource(String resourceId, Map<String, Object> oldValue) {
            AuditChangeContext.setChangeData(oldValue, resourceId);
        }

        @AuditRequired(
                action = "UPDATE_RESOURCE_ERROR",
                resourceType = AuditResourceType.CLOUD_PROVIDER,
                severity = AuditSeverity.MEDIUM
        )
        public void updateResourceWithError(String resourceId, Map<String, Object> oldValue) {
            AuditChangeContext.setChangeData(oldValue, resourceId);
            throw new RuntimeException("Intentional error for testing");
        }
    }

    // 헬퍼 메서드
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

