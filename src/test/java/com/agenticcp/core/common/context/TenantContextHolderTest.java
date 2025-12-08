package com.agenticcp.core.common.context;

import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TenantContextHolder 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@DisplayName("TenantContextHolder 단위 테스트")
class TenantContextHolderTest {

    private Tenant testTenant1;
    private Tenant testTenant2;

    @BeforeEach
    void setUp() {
        // 테스트용 테넌트 생성
        testTenant1 = Tenant.builder()
                .tenantKey("tenant1")
                .tenantName("Test Tenant 1")
                .status(Status.ACTIVE)
                .build();
        testTenant1.setId(1L);

        testTenant2 = Tenant.builder()
                .tenantKey("tenant2")
                .tenantName("Test Tenant 2")
                .status(Status.ACTIVE)
                .build();
        testTenant2.setId(2L);
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 컨텍스트 정리
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("테넌트 설정 및 조회 - 성공")
    void testSetAndGetTenant_Success() {
        // Given
        TenantContextHolder.setTenant(testTenant1);

        // When
        Tenant retrievedTenant = TenantContextHolder.getCurrentTenant();
        String retrievedTenantKey = TenantContextHolder.getCurrentTenantKey();

        // Then
        assertThat(retrievedTenant).isNotNull();
        assertThat(retrievedTenant.getId()).isEqualTo(1L);
        assertThat(retrievedTenant.getTenantKey()).isEqualTo("tenant1");
        assertThat(retrievedTenantKey).isEqualTo("tenant1");
    }

    @Test
    @DisplayName("테넌트 키로 설정 및 조회 - 성공")
    void testSetAndGetTenantKey_Success() {
        // Given
        TenantContextHolder.setTenantKey("tenant1");

        // When
        String retrievedTenantKey = TenantContextHolder.getCurrentTenantKey();
        boolean hasContext = TenantContextHolder.hasTenantContext();

        // Then
        assertThat(retrievedTenantKey).isEqualTo("tenant1");
        assertThat(hasContext).isTrue();
    }

    @Test
    @DisplayName("테넌트 컨텍스트 존재 여부 확인 - 성공")
    void testHasTenantContext_Success() {
        // Given
        assertThat(TenantContextHolder.hasTenantContext()).isFalse();

        // When
        TenantContextHolder.setTenant(testTenant1);

        // Then
        assertThat(TenantContextHolder.hasTenantContext()).isTrue();
    }

    @Test
    @DisplayName("테넌트 컨텍스트 존재 여부 확인 - 실패")
    void testHasTenantContext_Failure() {
        // When & Then
        assertThat(TenantContextHolder.hasTenantContext()).isFalse();
    }

    @Test
    @DisplayName("테넌트 조회 - 컨텍스트 없음")
    void testGetCurrentTenant_NoContext() {
        // When
        Tenant retrievedTenant = TenantContextHolder.getCurrentTenant();

        // Then
        assertThat(retrievedTenant).isNull();
    }

    @Test
    @DisplayName("테넌트 키 조회 - 컨텍스트 없음")
    void testGetCurrentTenantKey_NoContext() {
        // When
        String retrievedTenantKey = TenantContextHolder.getCurrentTenantKey();

        // Then
        assertThat(retrievedTenantKey).isNull();
    }

    @Test
    @DisplayName("테넌트 조회 - 예외 발생 (컨텍스트 없음)")
    void testGetCurrentTenantOrThrow_NoContext() {
        // When & Then
        assertThatThrownBy(() -> TenantContextHolder.getCurrentTenantOrThrow())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.TENANT_CONTEXT_NOT_SET)
                .hasMessageContaining("테넌트 컨텍스트가 설정되지 않았습니다.");
    }

    @Test
    @DisplayName("테넌트 키 조회 - 예외 발생 (컨텍스트 없음)")
    void testGetCurrentTenantKeyOrThrow_NoContext() {
        // When & Then
        assertThatThrownBy(() -> TenantContextHolder.getCurrentTenantKeyOrThrow())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.TENANT_CONTEXT_NOT_SET)
                .hasMessageContaining("테넌트 컨텍스트가 설정되지 않았습니다.");
    }

    @Test
    @DisplayName("테넌트 조회 - 예외 발생 (성공)")
    void testGetCurrentTenantOrThrow_Success() {
        // Given
        TenantContextHolder.setTenant(testTenant1);

        // When
        Tenant retrievedTenant = TenantContextHolder.getCurrentTenantOrThrow();

        // Then
        assertThat(retrievedTenant).isNotNull();
        assertThat(retrievedTenant.getId()).isEqualTo(1L);
        assertThat(retrievedTenant.getTenantKey()).isEqualTo("tenant1");
    }

    @Test
    @DisplayName("테넌트 키 조회 - 예외 발생 (성공)")
    void testGetCurrentTenantKeyOrThrow_Success() {
        // Given
        TenantContextHolder.setTenantKey("tenant1");

        // When
        String retrievedTenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();

        // Then
        assertThat(retrievedTenantKey).isEqualTo("tenant1");
    }

    @Test
    @DisplayName("테넌트 컨텍스트 변경 - 성공")
    void testChangeTenantContext_Success() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("tenant1");

        // When
        TenantContextHolder.setTenant(testTenant2);

        // Then
        assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("tenant2");
        assertThat(TenantContextHolder.getCurrentTenant().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("테넌트 컨텍스트 정리 - 성공")
    void testClearTenantContext_Success() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        assertThat(TenantContextHolder.hasTenantContext()).isTrue();

        // When
        TenantContextHolder.clear();

        // Then
        assertThat(TenantContextHolder.hasTenantContext()).isFalse();
        assertThat(TenantContextHolder.getCurrentTenant()).isNull();
        assertThat(TenantContextHolder.getCurrentTenantKey()).isNull();
    }

    @Test
    @DisplayName("ThreadLocal 격리 테스트 - 성공")
    void testThreadLocalIsolation_Success() throws InterruptedException {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("tenant1");

        // When - 다른 스레드에서 다른 테넌트 설정
        Thread otherThread = new Thread(() -> {
            TenantContextHolder.setTenant(testTenant2);
            assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("tenant2");
        });
        
        otherThread.start();
        otherThread.join();

        // Then - 원래 스레드의 컨텍스트는 변경되지 않음
        assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("tenant1");
    }
}
