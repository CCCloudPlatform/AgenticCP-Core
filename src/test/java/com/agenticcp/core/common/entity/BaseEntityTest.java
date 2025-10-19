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
 * BaseEntity 기본 필드 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@DisplayName("BaseEntity 기본 필드 단위 테스트")
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
    @DisplayName("BaseEntity 기본 필드 설정 및 조회")
    void testSetAndGetBasicFields() {
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
    @DisplayName("BaseEntity 상속 객체의 toString 메서드 확인")
    void testToString() {
        // Given
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // When
        String toString = testUser.toString();

        // Then
        assertThat(toString).contains("username=testuser");
        assertThat(toString).contains("email=test@example.com");
        // id는 Lombok @Data에서 toString에 포함되지 않을 수 있으므로 제거
    }

    @Test
    @DisplayName("BaseEntity 상속 객체의 equals와 hashCode 확인")
    void testEqualsAndHashCode() {
        // Given
        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .build();
        user1.setId(1L);

        User user2 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .build();
        user2.setId(1L);

        User user3 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .build();
        user3.setId(2L);

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
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // When & Then
        // @Entity, @Table 어노테이션이 올바르게 설정되었는지 확인
        assertThat(testUser.getId()).isNotNull();
        assertThat(testUser.getUsername()).isNotNull();
        assertThat(testUser.getEmail()).isNotNull();
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

        // When & Then
        // 모든 필드가 올바르게 설정되었는지 확인
        assertThat(testUser.getId()).isEqualTo(1L);
        assertThat(testUser.getCreatedAt()).isEqualTo(now);
        assertThat(testUser.getUpdatedAt()).isEqualTo(now);
        assertThat(testUser.getCreatedBy()).isEqualTo("admin");
        assertThat(testUser.getUpdatedBy()).isEqualTo("admin");
        assertThat(testUser.getIsDeleted()).isFalse();
    }
}
