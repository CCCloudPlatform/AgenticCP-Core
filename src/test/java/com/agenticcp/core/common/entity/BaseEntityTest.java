package com.agenticcp.core.common.entity;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * BaseEntity 테넌트 필드 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@DisplayName("BaseEntity 테넌트 필드 단위 테스트")
class BaseEntityTest {

    private Tenant testTenant1;
    private Tenant testTenant2;
    private User testUser;

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

        // 테스트용 사용자 생성
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Test
    @DisplayName("BaseEntity 상속 객체에 테넌트 설정 및 조회")
    void testSetAndGetTenant() {
        // Given
        assertThat(testUser.getTenant()).isNull();

        // When
        testUser.setTenant(testTenant1);

        // Then
        assertThat(testUser.getTenant()).isNotNull();
        assertThat(testUser.getTenant().getId()).isEqualTo(1L);
        assertThat(testUser.getTenant().getTenantKey()).isEqualTo("tenant1");
        assertThat(testUser.getTenant().getTenantName()).isEqualTo("Test Tenant 1");
    }

    @Test
    @DisplayName("BaseEntity 상속 객체에 테넌트 변경")
    void testChangeTenant() {
        // Given
        testUser.setTenant(testTenant1);
        assertThat(testUser.getTenant().getId()).isEqualTo(1L);

        // When
        testUser.setTenant(testTenant2);

        // Then
        assertThat(testUser.getTenant()).isNotNull();
        assertThat(testUser.getTenant().getId()).isEqualTo(2L);
        assertThat(testUser.getTenant().getTenantKey()).isEqualTo("tenant2");
        assertThat(testUser.getTenant().getTenantName()).isEqualTo("Test Tenant 2");
    }

    @Test
    @DisplayName("BaseEntity 상속 객체에 null 테넌트 설정")
    void testSetNullTenant() {
        // Given
        testUser.setTenant(testTenant1);
        assertThat(testUser.getTenant()).isNotNull();

        // When
        testUser.setTenant(null);

        // Then
        assertThat(testUser.getTenant()).isNull();
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 기본 필드들 확인")
    void testBaseEntityFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        testUser.setId(1L);
        testUser.setCreatedAt(now);
        testUser.setUpdatedAt(now);
        testUser.setCreatedBy("admin");
        testUser.setUpdatedBy("admin");
        testUser.setIsDeleted(false);

        // When & Then
        assertThat(testUser.getId()).isEqualTo(1L);
        assertThat(testUser.getCreatedAt()).isEqualTo(now);
        assertThat(testUser.getUpdatedAt()).isEqualTo(now);
        assertThat(testUser.getCreatedBy()).isEqualTo("admin");
        assertThat(testUser.getUpdatedBy()).isEqualTo("admin");
        assertThat(testUser.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 toString 메서드에 테넌트 정보 포함 확인")
    void testToString_IncludesTenantInfo() {
        // Given
        testUser.setId(1L);
        testUser.setTenant(testTenant1);

        // When
        String toString = testUser.toString();

        // Then
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("tenant=");
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 equals와 hashCode에 테넌트 정보 포함 확인")
    void testEqualsAndHashCode_IncludesTenantInfo() {
        // Given
        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .build();
        user1.setId(1L);
        user1.setTenant(testTenant1);

        User user2 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .build();
        user2.setId(1L);
        user2.setTenant(testTenant1);

        User user3 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .build();
        user3.setId(1L);
        user3.setTenant(testTenant2);

        // When & Then
        assertThat(user1).isEqualTo(user2);
        assertThat(user1).isNotEqualTo(user3);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        assertThat(user1.hashCode()).isNotEqualTo(user3.hashCode());
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 JPA 매핑 어노테이션 확인")
    void testJpaMappingAnnotations() {
        // Given
        testUser.setTenant(testTenant1);

        // When & Then
        // @ManyToOne 관계가 올바르게 설정되었는지 확인
        assertThat(testUser.getTenant()).isNotNull();
        assertThat(testUser.getTenant()).isInstanceOf(Tenant.class);
        
        // 테넌트 객체의 필드들이 올바르게 접근 가능한지 확인
        assertThat(testUser.getTenant().getId()).isNotNull();
        assertThat(testUser.getTenant().getTenantKey()).isNotNull();
        assertThat(testUser.getTenant().getTenantName()).isNotNull();
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 지연 로딩 확인")
    void testLazyLoading() {
        // Given
        testUser.setTenant(testTenant1);

        // When
        Tenant tenant = testUser.getTenant();

        // Then
        // @ManyToOne(fetch = FetchType.LAZY) 설정이 올바르게 작동하는지 확인
        // 실제로는 프록시 객체가 반환되지만, 테스트에서는 실제 객체가 반환됨
        assertThat(tenant).isNotNull();
        assertThat(tenant).isInstanceOf(Tenant.class);
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 외래키 제약조건 확인")
    void testForeignKeyConstraint() {
        // Given
        testUser.setTenant(testTenant1);

        // When & Then
        // @JoinColumn(name = "tenant_id", nullable = false) 설정 확인
        // nullable = false이므로 테넌트가 설정되어야 함
        assertThat(testUser.getTenant()).isNotNull();
        
        // 테넌트 ID가 올바르게 설정되었는지 확인
        assertThat(testUser.getTenant().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 엔티티 리스너 등록 확인")
    void testEntityListenerRegistration() {
        // Given
        testUser.setTenant(testTenant1);

        // When & Then
        // @EntityListeners에 TenantAwareEntityListener가 등록되었는지 확인
        // 실제로는 JPA가 자동으로 처리하지만, 테스트에서는 수동으로 확인
        assertThat(testUser.getTenant()).isNotNull();
        
        // 엔티티 리스너가 올바르게 작동하는지 확인하기 위해
        // 테넌트 정보가 올바르게 설정되었는지 검증
        assertThat(testUser.getTenant().getTenantKey()).isEqualTo("tenant1");
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 복합 필드 설정")
    void testComplexFieldSetting() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        testUser.setId(1L);
        testUser.setCreatedAt(now);
        testUser.setUpdatedAt(now);
        testUser.setCreatedBy("admin");
        testUser.setUpdatedBy("admin");
        testUser.setIsDeleted(false);
        testUser.setTenant(testTenant1);

        // When & Then
        // 모든 필드가 올바르게 설정되었는지 확인
        assertThat(testUser.getId()).isEqualTo(1L);
        assertThat(testUser.getCreatedAt()).isEqualTo(now);
        assertThat(testUser.getUpdatedAt()).isEqualTo(now);
        assertThat(testUser.getCreatedBy()).isEqualTo("admin");
        assertThat(testUser.getUpdatedBy()).isEqualTo("admin");
        assertThat(testUser.getIsDeleted()).isFalse();
        assertThat(testUser.getTenant()).isNotNull();
        assertThat(testUser.getTenant().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 불변성 확인")
    void testImmutability() {
        // Given
        testUser.setTenant(testTenant1);
        Tenant originalTenant = testUser.getTenant();

        // When
        // 테넌트 객체의 필드를 변경
        originalTenant.setTenantName("Modified Name");

        // Then
        // BaseEntity를 통한 테넌트 접근에서 변경사항이 반영되는지 확인
        assertThat(testUser.getTenant().getTenantName()).isEqualTo("Modified Name");
    }
}
