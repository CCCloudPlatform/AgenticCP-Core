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
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @AuditController 클래스 레벨 애노테이션 통합 테스트
 * 
 * - 클래스 레벨 감사 로깅 동작 검증
 * - HTTP 메서드 필터링 검증
 * - excludeMethods 검증
 * - 메서드명 기반 액션 자동 생성 검증
 * - oldValue 캡쳐 검증
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@SpringBootTest
@Import(AuditControllerIntegrationTest.TestProductService.class)
@Disabled("Integration test disabled")
class AuditControllerIntegrationTest {

    @Autowired
    private TestProductService testProductService;

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
    @DisplayName("@AuditController가 붙은 클래스의 POST 메서드는 감사 로그가 저장되어야 한다")
    void testAuditController_POST메서드_감사로그저장됨() {
        // Given
        String productId = "product-001";

        // When
        testProductService.createProduct(productId, "Test Product");

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("CREATE_PRODUCT");
            assertThat(log.getResourceType()).isEqualTo(AuditResourceType.CLOUD_PROVIDER);
            assertThat(log.getSuccess()).isTrue();
        });
    }

    @Test
    @DisplayName("@AuditController가 붙은 클래스의 PUT 메서드는 감사 로그가 저장되어야 한다")
    void testAuditController_PUT메서드_감사로그저장됨() {
        // Given
        String productId = "product-002";
        Map<String, Object> oldValue = Map.of("name", "Old Product", "status", "ACTIVE");

        // When
        testProductService.updateProduct(productId, "New Product", oldValue);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "UPDATE_PRODUCT".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("UPDATE_PRODUCT");
            assertThat(log.getOldValue()).isNotNull();
            assertThat(log.getOldValue()).contains("Old Product");
            assertThat(log.getTargetResourceId()).isEqualTo(productId);
        });
    }

    @Test
    @DisplayName("@AuditController가 붙은 클래스의 DELETE 메서드는 감사 로그가 저장되어야 한다")
    void testAuditController_DELETE메서드_감사로그저장됨() {
        // Given
        String productId = "product-003";
        Map<String, Object> oldValue = Map.of("productId", productId, "name", "Deleted Product");

        // When
        testProductService.deleteProduct(productId, oldValue);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "DELETE_PRODUCT".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("DELETE_PRODUCT");
            assertThat(log.getOldValue()).isNotNull();
            assertThat(log.getOldValue()).contains("Deleted Product");
            assertThat(log.getNewValue()).isNull();
        });
    }

    @Test
    @DisplayName("@AuditController가 붙은 클래스의 GET 메서드는 감사 로그가 저장되지 않아야 한다")
    void testAuditController_GET메서드_감사로그_미저장() {
        // Given
        String productId = "product-004";

        // When
        testProductService.getProduct(productId);

        // Then
        await().pollDelay(java.time.Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<AuditLog> logs = auditLogRepository.findAll();
                    assertThat(logs).isEmpty();  // GET은 기본적으로 감사 로그 미저장
                });
    }

    @Test
    @DisplayName("excludeMethods에 포함된 메서드는 감사 로그가 저장되지 않아야 한다")
    void testAuditController_제외메서드_감사로그_미저장() {
        // Given & When
        testProductService.getProductInfo();

        // Then
        await().pollDelay(java.time.Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<AuditLog> logs = auditLogRepository.findAll();
                    assertThat(logs).isEmpty();
                });
    }

    @Test
    @DisplayName("메서드명으로부터 액션이 자동 생성되어야 한다")
    void testAuditController_메서드명_액션자동생성() {
        // Given
        String productId = "product-005";

        // When - createProduct -> CREATE
        testProductService.createProduct(productId, "Product 5");

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "CREATE_PRODUCT".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);
        });
    }

    @Test
    @DisplayName("PATCH 메서드도 감사 로그가 저장되어야 한다")
    void testAuditController_PATCH메서드_감사로그저장됨() {
        // Given
        String productId = "product-006";
        Map<String, Object> oldValue = Map.of("status", "ACTIVE");

        // When
        testProductService.patchProduct(productId, "SUSPENDED", oldValue);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(log -> "PATCH_PRODUCT".equals(log.getAction()))
                    .toList();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getOldValue()).contains("ACTIVE");
        });
    }

    @Test
    @DisplayName("클래스 레벨 defaultSeverity가 적용되어야 한다")
    void testAuditController_기본심각도_적용됨() {
        // Given
        String productId = "product-007";

        // When
        testProductService.createProduct(productId, "Product 7");

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getSeverity()).isEqualTo(AuditSeverity.MEDIUM);
        });
    }

    @Test
    @DisplayName("여러 변경 작업의 이력이 시간순으로 조회되어야 한다")
    void testAuditController_변경이력_시간순조회() {
        // Given
        String productId = "product-track-001";

        // When - 여러 변경 작업 수행
        testProductService.createProduct(productId, "Initial");

        Map<String, Object> old1 = Map.of("name", "Initial");
        testProductService.updateProduct(productId, "Updated 1", old1);

        Map<String, Object> old2 = Map.of("name", "Updated 1");
        testProductService.updateProduct(productId, "Updated 2", old2);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> history = auditLogRepository
                    .findByTargetResourceIdOrderByTimestampDesc(productId);

            assertThat(history).hasSizeGreaterThanOrEqualTo(2);
            
            // 최신 기록이 먼저 조회됨
            assertThat(history.get(0).getAction()).isEqualTo("UPDATE_PRODUCT");
        });
    }

    @Test
    @DisplayName("동일 리소스에 대한 CREATE, UPDATE, DELETE 전체 이력이 추적되어야 한다")
    void testAuditController_전체라이프사이클_추적() {
        // Given
        String productId = "product-lifecycle-001";

        // When
        // 1. CREATE
        testProductService.createProduct(productId, "New Product");

        // 2. UPDATE
        Map<String, Object> oldValue1 = Map.of("name", "New Product", "status", "PENDING");
        testProductService.updateProduct(productId, "Active Product", oldValue1);

        // 3. UPDATE again
        Map<String, Object> oldValue2 = Map.of("name", "Active Product", "status", "ACTIVE");
        testProductService.updateProduct(productId, "Suspended Product", oldValue2);

        // 4. DELETE
        Map<String, Object> oldValue3 = Map.of("name", "Suspended Product", "status", "SUSPENDED");
        testProductService.deleteProduct(productId, oldValue3);

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> history = auditLogRepository
                    .findByTargetResourceIdOrderByTimestampDesc(productId);

            assertThat(history).hasSizeGreaterThanOrEqualTo(3);

            // 변경 이력만 필터링 (oldValue 또는 newValue가 있는 것)
            List<AuditLog> changeHistory = auditLogRepository
                    .findChangeHistoryByTargetResourceId(productId);

            assertThat(changeHistory).hasSizeGreaterThanOrEqualTo(3);
            assertThat(changeHistory).allMatch(log -> 
                    log.getOldValue() != null || log.getNewValue() != null);
        });
    }

    @Test
    @DisplayName("메서드 실행 실패 시에도 감사 로그가 저장되어야 한다")
    void testAuditController_실패시도_감사로그저장됨() {
        // Given
        String productId = "invalid-product";

        // When
        try {
            testProductService.updateProductWithError(productId);
        } catch (RuntimeException e) {
            // 예외 무시
        }

        // Then
        await().untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);

            AuditLog log = logs.get(0);
            assertThat(log.getSuccess()).isFalse();
            assertThat(log.getError()).contains("Product not found");
        });
    }

    // 테스트용 서비스 클래스
    @Service
    @AuditController(
            resourceType = AuditResourceType.CLOUD_PROVIDER,
            defaultSeverity = AuditSeverity.MEDIUM,
            targetHttpMethods = {"POST", "PUT", "PATCH", "DELETE"},
            excludeMethods = {"getProductInfo"}
    )
    public static class TestProductService {

        @PostMapping
        public String createProduct(String productId, String name) {
            AuditChangeContext.setTargetResourceId(productId);
            return productId;
        }

        @PutMapping
        public String updateProduct(String productId, String name, Map<String, Object> oldValue) {
            AuditChangeContext.setChangeData(oldValue, productId);
            return productId;
        }

        @DeleteMapping
        public void deleteProduct(String productId, Map<String, Object> oldValue) {
            AuditChangeContext.setChangeData(oldValue, productId);
        }

        @GetMapping
        public TestProductDto getProduct(String productId) {
            return new TestProductDto(productId, "Test Product", "ACTIVE");
        }

        @PostMapping
        public void getProductInfo() {
            // 이 메서드는 excludeMethods에 포함되어 감사 로그가 저장되지 않음
        }

        @PatchMapping
        public String patchProduct(String productId, String status, Map<String, Object> oldValue) {
            AuditChangeContext.setChangeData(oldValue, productId);
            return productId;
        }

        @PutMapping
        public void updateProductWithError(String productId) {
            throw new RuntimeException("Product not found: " + productId);
        }
    }

    // 테스트용 DTO
    @Data
    public static class TestProductDto {
        private final String productId;
        private final String name;
        private final String status;
    }
}

