package com.agenticcp.core.common.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * RetryService 단위 테스트
 * TaskExecutor 사용 부분을 중점적으로 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-27
 */
@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private TaskExecutor tenantTaskExecutor;
    
    private RetryService retryService;

    @BeforeEach
    void setUp() {
        retryService = new RetryService(redisTemplate);
        
        // TaskExecutor 주입 (리플렉션 사용)
        try {
            var field = RetryService.class.getDeclaredField("tenantTaskExecutor");
            field.setAccessible(true);
            field.set(retryService, tenantTaskExecutor);
        } catch (Exception e) {
            throw new RuntimeException("TaskExecutor 주입 실패", e);
        }
        
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("비동기 재시도 로직에서 TaskExecutor가 사용되는지 테스트")
    void retryAsync_ShouldUseTaskExecutor() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        String operation = "TEST_OPERATION";
        String identifier = "test-user";
        Supplier<String> operationToRetry = () -> "success";
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        // TaskExecutor가 호출될 때 실제로 실행될 작업을 캡처
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            // 작업을 실행하면서 Tenant Context 캡처
            task.run();
            return null;
        }).when(tenantTaskExecutor).execute(any(Runnable.class));

        // When
        CompletableFuture<String> future = retryService.retryAsync(operation, identifier, operationToRetry, 3);
        String result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(result).isEqualTo("success");
        verify(tenantTaskExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("비동기 재시도 로직에서 Tenant Context가 전파되는지 테스트")
    void retryAsync_ShouldPropagateTenantContext() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        String operation = "TEST_OPERATION";
        String identifier = "test-user";
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        Supplier<String> operationToRetry = () -> {
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
            return "success";
        };
        
        // TaskExecutor가 호출될 때 실제로 실행될 작업을 캡처
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(tenantTaskExecutor).execute(any(Runnable.class));

        // When
        CompletableFuture<String> future = retryService.retryAsync(operation, identifier, operationToRetry, 3);
        String result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(result).isEqualTo("success");
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("비동기 재시도 로직에서 예외 발생 시 TaskExecutor가 사용되는지 테스트")
    void retryAsync_WithException_ShouldUseTaskExecutor() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        String operation = "TEST_OPERATION";
        String identifier = "test-user";
        
        Supplier<String> failingOperation = () -> {
            throw new RuntimeException("Test exception");
        };
        
        // TaskExecutor가 호출될 때 실제로 실행될 작업을 캡처
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(tenantTaskExecutor).execute(any(Runnable.class));

        // When & Then
        CompletableFuture<String> future = retryService.retryAsync(operation, identifier, failingOperation, 3);
        
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
        
        verify(tenantTaskExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("토큰 갱신 재시도 로직에서 TaskExecutor가 사용되는지 테스트")
    void retryTokenRefresh_ShouldUseTaskExecutor() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        String username = "test-user";
        Supplier<String> tokenRefreshOperation = () -> "new-token";
        
        // TaskExecutor가 호출될 때 실제로 실행될 작업을 캡처
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(tenantTaskExecutor).execute(any(Runnable.class));

        // When
        CompletableFuture<String> future = retryService.retryAsync("TOKEN_REFRESH", username, tokenRefreshOperation, 3);
        String result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(result).isEqualTo("new-token");
        verify(tenantTaskExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("여러 비동기 재시도 작업이 동시에 실행될 때 각각의 TaskExecutor 사용 테스트")
    void retryAsync_MultipleConcurrentTasks_ShouldUseTaskExecutor() throws Exception {
        // Given
        Tenant tenant1 = createTestTenant("tenant-1", "Tenant 1");
        Tenant tenant2 = createTestTenant("tenant-2", "Tenant 2");
        
        AtomicReference<String> capturedTenant1Key = new AtomicReference<>();
        AtomicReference<String> capturedTenant2Key = new AtomicReference<>();
        
        Supplier<String> operation1 = () -> {
            capturedTenant1Key.set(TenantContextHolder.getCurrentTenantKey());
            return "result1";
        };
        
        Supplier<String> operation2 = () -> {
            capturedTenant2Key.set(TenantContextHolder.getCurrentTenantKey());
            return "result2";
        };
        
        // TaskExecutor가 호출될 때 실제로 실행될 작업을 캡처
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(tenantTaskExecutor).execute(any(Runnable.class));

        // When
        TenantContextHolder.setTenant(tenant1);
        CompletableFuture<String> future1 = retryService.retryAsync("OP1", "user1", operation1, 3);
        
        TenantContextHolder.setTenant(tenant2);
        CompletableFuture<String> future2 = retryService.retryAsync("OP2", "user2", operation2, 3);
        
        CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

        // Then
        assertThat(future1.get()).isEqualTo("result1");
        assertThat(future2.get()).isEqualTo("result2");
        assertThat(capturedTenant1Key.get()).isEqualTo("tenant-1");
        assertThat(capturedTenant2Key.get()).isEqualTo("tenant-2");
        verify(tenantTaskExecutor, times(2)).execute(any(Runnable.class));
    }

    /**
     * 테스트용 Tenant 객체 생성
     */
    private Tenant createTestTenant(String tenantKey, String tenantName) {
        return Tenant.builder()
                .tenantKey(tenantKey)
                .tenantName(tenantName)
                .description("Test tenant for RetryService testing")
                .build();
    }
}