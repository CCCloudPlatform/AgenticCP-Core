package com.agenticcp.core.common.interceptor;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TenantAwareInterceptor 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@DisplayName("TenantAwareInterceptor 단위 테스트")
class TenantAwareInterceptorTest {

    private TenantAwareInterceptor interceptor;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        interceptor = new TenantAwareInterceptor();
        
        testTenant = Tenant.builder()
                .tenantKey("test-tenant")
                .tenantName("Test Tenant")
                .status(Status.ACTIVE)
                .build();
        testTenant.setId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("SELECT 쿼리 필터링 - 테넌트 컨텍스트 있음")
    void testInspect_SelectQuery_WithTenantContext() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "SELECT * FROM users";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("WHERE");
        assertThat(result).contains("tenant_id = 'test-tenant'");
        assertThat(result).contains("SELECT * FROM users");
    }

    @Test
    @DisplayName("SELECT 쿼리 필터링 - 테넌트 컨텍스트 없음")
    void testInspect_SelectQuery_NoTenantContext() {
        // Given
        String originalSql = "SELECT * FROM users";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isEqualTo(originalSql);
    }

    @Test
    @DisplayName("SELECT 쿼리 필터링 - 테이블 별칭 있음")
    void testInspect_SelectQuery_WithTableAlias() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "SELECT u.* FROM users u";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("WHERE");
        assertThat(result).contains("tenant_id = 'test-tenant'");
    }

    @Test
    @DisplayName("UPDATE 쿼리 필터링 - 테넌트 컨텍스트 있음")
    void testInspect_UpdateQuery_WithTenantContext() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "UPDATE users SET username = 'newuser'";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("WHERE");
        assertThat(result).contains("tenant_id = 'test-tenant'");
        assertThat(result).contains("UPDATE users SET username = 'newuser'");
    }

    @Test
    @DisplayName("UPDATE 쿼리 필터링 - 테넌트 컨텍스트 없음")
    void testInspect_UpdateQuery_NoTenantContext() {
        // Given
        String originalSql = "UPDATE users SET username = 'newuser'";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isEqualTo(originalSql);
    }

    @Test
    @DisplayName("DELETE 쿼리 필터링 - 테넌트 컨텍스트 있음")
    void testInspect_DeleteQuery_WithTenantContext() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "DELETE FROM users";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("WHERE");
        assertThat(result).contains("tenant_id = 'test-tenant'");
        assertThat(result).contains("DELETE FROM users");
    }

    @Test
    @DisplayName("DELETE 쿼리 필터링 - 테넌트 컨텍스트 없음")
    void testInspect_DeleteQuery_NoTenantContext() {
        // Given
        String originalSql = "DELETE FROM users";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isEqualTo(originalSql);
    }

    @Test
    @DisplayName("INSERT 쿼리 - tenant_id 자동 주입")
    void testInspect_InsertQuery_WithTenantContext() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "INSERT INTO users (username, email) VALUES ('testuser', 'test@example.com')";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("tenant_id");
        assertThat(result).contains("'test-tenant'");
        assertThat(result).contains("INSERT INTO users");
    }

    @Test
    @DisplayName("INSERT 쿼리 - 테넌트 컨텍스트 없음")
    void testInspect_InsertQuery_NoTenantContext() {
        // Given
        String originalSql = "INSERT INTO users (username, email) VALUES ('testuser', 'test@example.com')";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isEqualTo(originalSql);
    }

    @Test
    @DisplayName("INSERT 쿼리 - 이미 tenant_id 포함")
    void testInspect_InsertQuery_AlreadyHasTenantId() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "INSERT INTO users (username, email, tenant_id) VALUES ('testuser', 'test@example.com', 'other-tenant')";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        // 기존 tenant_id 값이 유지되어야 함
        assertThat(result).contains("tenant_id");
        assertThat(result).contains("'other-tenant'");
    }

    @Test
    @DisplayName("대소문자 혼합 SQL 쿼리 처리")
    void testInspect_MixedCaseSQL() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "SeLeCt * FrOm UsErS";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("WHERE");
        assertThat(result).contains("tenant_id = 'test-tenant'");
    }

    @Test
    @DisplayName("복잡한 SELECT 쿼리 처리")
    void testInspect_ComplexSelectQuery() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "SELECT u.id, u.username, o.name FROM users u JOIN organizations o ON u.org_id = o.id WHERE u.status = 'ACTIVE'";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("WHERE");
        assertThat(result).contains("tenant_id = 'test-tenant'");
        assertThat(result).contains("u.status = 'ACTIVE'");
    }

    @Test
    @DisplayName("JOIN이 포함된 SELECT 쿼리 처리")
    void testInspect_SelectWithJoin() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "SELECT u.*, o.name FROM users u LEFT JOIN organizations o ON u.org_id = o.id";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("WHERE");
        assertThat(result).contains("tenant_id = 'test-tenant'");
    }

    @Test
    @DisplayName("서브쿼리가 포함된 SELECT 쿼리 처리")
    void testInspect_SelectWithSubquery() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM user_roles)";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("WHERE");
        assertThat(result).contains("tenant_id = 'test-tenant'");
    }

    @Test
    @DisplayName("null SQL 처리")
    void testInspect_NullSQL() {
        // Given
        TenantContextHolder.setTenant(testTenant);

        // When & Then
        assertThatCode(() -> interceptor.inspect(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빈 문자열 SQL 처리")
    void testInspect_EmptySQL() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isEqualTo(originalSql);
    }

    @Test
    @DisplayName("인식되지 않는 SQL 타입 처리")
    void testInspect_UnknownSQLType() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "CREATE TABLE test (id INT)";

        // When
        String result = interceptor.inspect(originalSql);

        // Then
        assertThat(result).isEqualTo(originalSql);
    }

    @Test
    @DisplayName("테넌트 컨텍스트 변경 시 다른 tenant_id 적용")
    void testInspect_DifferentTenantContext() {
        // Given
        TenantContextHolder.setTenant(testTenant);
        String originalSql = "SELECT * FROM users";
        String result1 = interceptor.inspect(originalSql);

        // When - 다른 테넌트로 변경
        Tenant otherTenant = Tenant.builder()
                .tenantKey("other-tenant")
                .tenantName("Other Tenant")
                .status(Status.ACTIVE)
                .build();
        otherTenant.setId(2L);
        TenantContextHolder.setTenant(otherTenant);
        String result2 = interceptor.inspect(originalSql);

        // Then
        assertThat(result1).contains("tenant_id = 'test-tenant'");
        assertThat(result2).contains("tenant_id = 'other-tenant'");
    }
}
