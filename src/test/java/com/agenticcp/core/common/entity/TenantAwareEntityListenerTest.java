package com.agenticcp.core.common.entity;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * TenantAwareEntityListener 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantAwareEntityListener 단위 테스트")
class TenantAwareEntityListenerTest {

    private TenantAwareEntityListener listener;
    private Tenant testTenant;
    private Tenant existingTenant;

    @BeforeEach
    void setUp() {
        listener = new TenantAwareEntityListener();
        
        testTenant = Tenant.builder()
                .tenantKey("test-tenant")
                .tenantName("Test Tenant")
                .status(Status.ACTIVE)
                .build();
        testTenant.setId(1L);

        existingTenant = Tenant.builder()
                .tenantKey("existing-tenant")
                .tenantName("Existing Tenant")
                .status(Status.ACTIVE)
                .build();
        existingTenant.setId(2L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("엔티티 생성 시 테넌트 자동 설정 - 성공")
    void testPrePersist_SetTenant_Success() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // When
        listener.prePersist(user);

        // Then
        assertThat(user.getTenant()).isNotNull();
        assertThat(user.getTenant().getId()).isEqualTo(1L);
        assertThat(user.getTenant().getTenantKey()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("엔티티 생성 시 테넌트 컨텍스트 없음 - 예외 발생")
    void testPrePersist_NoTenantContext_ThrowsException() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // When & Then
        assertThatThrownBy(() -> listener.prePersist(user))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.TENANT_CONTEXT_NOT_SET)
                .hasMessageContaining("Tenant context is required for entity operations");
    }

    @Test
    @DisplayName("엔티티 생성 시 이미 테넌트 설정됨 - 건너뛰기")
    void testPrePersist_TenantAlreadySet_Skip() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        user.setTenant(existingTenant); // 이미 다른 테넌트 설정

        // When
        listener.prePersist(user);

        // Then - 기존 테넌트 유지
        assertThat(user.getTenant()).isNotNull();
        assertThat(user.getTenant().getId()).isEqualTo(2L);
        assertThat(user.getTenant().getTenantKey()).isEqualTo("existing-tenant");
    }

    @Test
    @DisplayName("엔티티 수정 시 테넌트 자동 설정 - 성공")
    void testPreUpdate_SetTenant_Success() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // When
        listener.preUpdate(user);

        // Then
        assertThat(user.getTenant()).isNotNull();
        assertThat(user.getTenant().getId()).isEqualTo(1L);
        assertThat(user.getTenant().getTenantKey()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("엔티티 수정 시 테넌트 컨텍스트 없음 - 예외 발생")
    void testPreUpdate_NoTenantContext_ThrowsException() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // When & Then
        assertThatThrownBy(() -> listener.preUpdate(user))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.TENANT_CONTEXT_NOT_SET)
                .hasMessageContaining("Tenant context is required for entity operations");
    }

    @Test
    @DisplayName("엔티티 수정 시 이미 테넌트 설정됨 - 건너뛰기")
    void testPreUpdate_TenantAlreadySet_Skip() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        user.setTenant(existingTenant); // 이미 다른 테넌트 설정

        // When
        listener.preUpdate(user);

        // Then - 기존 테넌트 유지
        assertThat(user.getTenant()).isNotNull();
        assertThat(user.getTenant().getId()).isEqualTo(2L);
        assertThat(user.getTenant().getTenantKey()).isEqualTo("existing-tenant");
    }

    @Test
    @DisplayName("BaseEntity가 아닌 객체 처리 - 건너뛰기")
    void testPrePersist_NonBaseEntity_Skip() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String nonEntity = "not an entity";

        // When & Then - 예외 없이 실행되어야 함
        assertThatCode(() -> listener.prePersist(nonEntity))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("BaseEntity가 아닌 객체 처리 (preUpdate) - 건너뛰기")
    void testPreUpdate_NonBaseEntity_Skip() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String nonEntity = "not an entity";

        // When & Then - 예외 없이 실행되어야 함
        assertThatCode(() -> listener.preUpdate(nonEntity))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null 객체 처리 - 건너뛰기")
    void testPrePersist_NullObject_Skip() {
        // Given
        TenantContextHolder.setTenant(testTenant);

        // When & Then - 예외 없이 실행되어야 함
        assertThatCode(() -> listener.prePersist(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null 객체 처리 (preUpdate) - 건너뛰기")
    void testPreUpdate_NullObject_Skip() {
        // Given
        TenantContextHolder.setTenant(testTenant);

        // When & Then - 예외 없이 실행되어야 함
        assertThatCode(() -> listener.preUpdate(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("테넌트 키만 설정된 경우 - 예외 발생")
    void testPrePersist_TenantKeyOnly_ThrowsException() {
        // Given
        TenantContextHolder.setTenantKey("test-tenant");
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // When & Then
        assertThatThrownBy(() -> listener.prePersist(user))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.TENANT_CONTEXT_NOT_SET)
                .hasMessageContaining("Tenant context is required for entity operations");
    }

    @Test
    @DisplayName("다양한 BaseEntity 상속 객체 처리 - 성공")
    void testPrePersist_DifferentBaseEntityTypes_Success() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        
        // User 엔티티
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // When
        listener.prePersist(user);

        // Then
        assertThat(user.getTenant()).isNotNull();
        assertThat(user.getTenant().getId()).isEqualTo(1L);
    }
}
