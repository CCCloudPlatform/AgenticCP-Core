package com.agenticcp.core.common.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 비동기 로직만 테스트하는 간단한 테스트
 * Spring Context나 다른 의존성 없이 순수 비동기 로직만 검증
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-27
 */
class AsyncLogicTest {

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("CompletableFuture에서 Tenant Context가 전파되지 않는 문제 확인")
    void testCompletableFutureWithoutTenantContext() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        // When - 기본 CompletableFuture 사용 (Tenant Context 전파 안됨)
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
            return "result";
        });
        
        String result = future.get(5, TimeUnit.SECONDS);

        // Then - Tenant Context가 전파되지 않음을 확인
        assertThat(result).isEqualTo("result");
        assertThat(capturedTenantKey.get()).isNull(); // 전파되지 않음
    }

    @Test
    @DisplayName("수동으로 Tenant Context를 전파하는 방법 테스트")
    void testManualTenantContextPropagation() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        // When - 수동으로 Tenant Context 전파
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 수동으로 Tenant Context 복원
                TenantContextHolder.setTenant(testTenant);
                capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
                return "result";
            } finally {
                // 수동으로 컨텍스트 정리
                TenantContextHolder.clear();
            }
        });
        
        String result = future.get(5, TimeUnit.SECONDS);

        // Then - Tenant Context가 전파됨을 확인
        assertThat(result).isEqualTo("result");
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("Supplier를 사용한 비동기 작업에서 Tenant Context 전파 테스트")
    void testSupplierWithTenantContext() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        Supplier<String> operation = () -> {
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
            return "operation result";
        };

        // When - Supplier를 CompletableFuture로 실행
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 수동으로 Tenant Context 복원
                TenantContextHolder.setTenant(testTenant);
                return operation.get();
            } finally {
                TenantContextHolder.clear();
            }
        });
        
        String result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(result).isEqualTo("operation result");
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("여러 비동기 작업이 동시에 실행될 때 Tenant Context 독립성 테스트")
    void testConcurrentAsyncTasksWithTenantContext() throws Exception {
        // Given
        Tenant tenant1 = createTestTenant("tenant-1", "Tenant 1");
        Tenant tenant2 = createTestTenant("tenant-2", "Tenant 2");
        
        AtomicReference<String> capturedTenant1Key = new AtomicReference<>();
        AtomicReference<String> capturedTenant2Key = new AtomicReference<>();
        
        Supplier<String> operation1 = () -> {
            try {
                TenantContextHolder.setTenant(tenant1);
                capturedTenant1Key.set(TenantContextHolder.getCurrentTenantKey());
                return "result1";
            } finally {
                TenantContextHolder.clear();
            }
        };
        
        Supplier<String> operation2 = () -> {
            try {
                TenantContextHolder.setTenant(tenant2);
                capturedTenant2Key.set(TenantContextHolder.getCurrentTenantKey());
                return "result2";
            } finally {
                TenantContextHolder.clear();
            }
        };

        // When
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(operation1);
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(operation2);
        
        CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

        // Then
        assertThat(future1.get()).isEqualTo("result1");
        assertThat(future2.get()).isEqualTo("result2");
        assertThat(capturedTenant1Key.get()).isEqualTo("tenant-1");
        assertThat(capturedTenant2Key.get()).isEqualTo("tenant-2");
    }

    @Test
    @DisplayName("비동기 작업에서 예외 발생 시 Tenant Context 정리 테스트")
    void testAsyncTaskExceptionWithTenantContext() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        Supplier<String> failingOperation = () -> {
            try {
                TenantContextHolder.setTenant(testTenant);
                capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
                throw new RuntimeException("Test exception");
            } finally {
                TenantContextHolder.clear();
            }
        };

        // When & Then
        CompletableFuture<String> future = CompletableFuture.supplyAsync(failingOperation);
        
        // 예외가 발생했는지 확인
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
        
        assertThat(future.isCompletedExceptionally()).isTrue();
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant");
    }

    /**
     * 테스트용 Tenant 객체 생성
     */
    private Tenant createTestTenant(String tenantKey, String tenantName) {
        return Tenant.builder()
                .tenantKey(tenantKey)
                .tenantName(tenantName)
                .description("Test tenant")
                .build();
    }
}