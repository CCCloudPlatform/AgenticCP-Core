package com.agenticcp.core.common.repository;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TenantAwareRepositoryImpl 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantAwareRepositoryImpl 단위 테스트")
class TenantAwareRepositoryImplTest {

    @Mock
    private JpaEntityInformation<User, Long> entityInformation;
    
    @Mock
    private EntityManager entityManager;
    
    @Mock
    private CriteriaBuilder criteriaBuilder;
    
    @Mock
    private CriteriaQuery<User> criteriaQuery;
    
    @Mock
    private CriteriaQuery<Long> countQuery;
    
    @Mock
    private Root<User> root;
    
    @Mock
    private TypedQuery<User> typedQuery;
    
    @Mock
    private TypedQuery<Long> countTypedQuery;
    
    @Mock
    private Predicate predicate;
    
    @Mock
    private Expression<Long> countExpression;

    private TenantAwareRepositoryImpl<User, Long> repository;
    private Tenant testTenant1;
    private Tenant testTenant2;
    private User testUser1;
    private User testUser2;

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
        testUser1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .build();
        testUser1.setId(1L);
        testUser1.setTenant(testTenant1);

        testUser2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .build();
        testUser2.setId(2L);
        testUser2.setTenant(testTenant2);

        // Mock 설정
        when(entityInformation.getJavaType()).thenReturn(User.class);
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(User.class)).thenReturn(criteriaQuery);
        when(criteriaBuilder.createQuery(Long.class)).thenReturn(countQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(criteriaQuery.select(root)).thenReturn(criteriaQuery);
        when(countQuery.select(countExpression)).thenReturn(countQuery);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(entityManager.createQuery(countQuery)).thenReturn(countTypedQuery);
        when(criteriaBuilder.equal(any(), any())).thenReturn(predicate);
        when(criteriaBuilder.and(any(), any())).thenReturn(predicate);

        repository = new TenantAwareRepositoryImpl<>(entityInformation, entityManager);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("findAll - 테넌트 필터링 적용")
    void testFindAll_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        List<User> expectedUsers = List.of(testUser1);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);

        // When
        List<User> result = repository.findAll();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenant().getId()).isEqualTo(1L);
        
        // 테넌트 필터링이 적용되었는지 확인
        verify(criteriaBuilder).equal(root.get("tenant"), testTenant1);
    }

    @Test
    @DisplayName("findAll - 테넌트 컨텍스트 없음")
    void testFindAll_NoTenantContext() {
        // When & Then
        assertThatThrownBy(() -> repository.findAll())
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("findById - 테넌트 필터링 적용")
    void testFindById_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        when(typedQuery.getSingleResult()).thenReturn(testUser1);

        // When
        Optional<User> result = repository.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getTenant().getId()).isEqualTo(1L);
        
        // 테넌트 필터링이 적용되었는지 확인
        verify(criteriaBuilder).equal(root.get("tenant"), testTenant1);
    }

    @Test
    @DisplayName("findById - 다른 테넌트의 데이터 조회 시 빈 결과")
    void testFindById_DifferentTenant_EmptyResult() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        when(typedQuery.getSingleResult()).thenThrow(new jakarta.persistence.NoResultException());

        // When
        Optional<User> result = repository.findById(2L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsById - 테넌트 필터링 적용")
    void testExistsById_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        when(countTypedQuery.getSingleResult()).thenReturn(1L);

        // When
        boolean result = repository.existsById(1L);

        // Then
        assertThat(result).isTrue();
        
        // 테넌트 필터링이 적용되었는지 확인
        verify(criteriaBuilder).equal(root.get("tenant"), testTenant1);
    }

    @Test
    @DisplayName("count - 테넌트 필터링 적용")
    void testCount_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        when(countTypedQuery.getSingleResult()).thenReturn(1L);

        // When
        long result = repository.count();

        // Then
        assertThat(result).isEqualTo(1L);
        
        // 테넌트 필터링이 적용되었는지 확인
        verify(criteriaBuilder).equal(root.get("tenant"), testTenant1);
    }

    @Test
    @DisplayName("findAll with Sort - 테넌트 필터링 적용")
    void testFindAll_WithSort_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        Sort sort = Sort.by("username");
        List<User> expectedUsers = List.of(testUser1);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);

        // When
        List<User> result = repository.findAll(sort);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        // 테넌트 필터링이 적용되었는지 확인
        verify(criteriaBuilder).equal(root.get("tenant"), testTenant1);
    }

    @Test
    @DisplayName("findAll with Pageable - 테넌트 필터링 적용")
    void testFindAll_WithPageable_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        Pageable pageable = PageRequest.of(0, 10);
        List<User> expectedUsers = List.of(testUser1);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);
        when(countTypedQuery.getSingleResult()).thenReturn(1L);

        // When
        Page<User> result = repository.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        
        // 테넌트 필터링이 적용되었는지 확인
        verify(criteriaBuilder).equal(root.get("tenant"), testTenant1);
    }

    @Test
    @DisplayName("findAll with Specification - 테넌트 필터링과 결합")
    void testFindAll_WithSpecification_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        Specification<User> spec = (root, query, cb) -> cb.equal(root.get("username"), "user1");
        List<User> expectedUsers = List.of(testUser1);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);

        // When
        List<User> result = repository.findAll(spec);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        // 테넌트 필터링과 사용자 정의 스펙이 결합되었는지 확인
        verify(criteriaBuilder).and(any(), any());
    }

    @Test
    @DisplayName("findAll with Specification and Pageable - 테넌트 필터링과 결합")
    void testFindAll_WithSpecificationAndPageable_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        Specification<User> spec = (root, query, cb) -> cb.equal(root.get("username"), "user1");
        Pageable pageable = PageRequest.of(0, 10);
        List<User> expectedUsers = List.of(testUser1);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);
        when(countTypedQuery.getSingleResult()).thenReturn(1L);

        // When
        Page<User> result = repository.findAll(spec, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        
        // 테넌트 필터링과 사용자 정의 스펙이 결합되었는지 확인
        verify(criteriaBuilder).and(any(), any());
    }

    @Test
    @DisplayName("deleteById - 테넌트 필터링 적용")
    void testDeleteById_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        when(typedQuery.getSingleResult()).thenReturn(testUser1);

        // When
        repository.deleteById(1L);

        // Then
        // 테넌트 필터링이 적용되었는지 확인
        verify(criteriaBuilder).equal(root.get("tenant"), testTenant1);
        verify(entityManager).remove(testUser1);
    }

    @Test
    @DisplayName("deleteById - 다른 테넌트의 데이터 삭제 시 예외")
    void testDeleteById_DifferentTenant_ThrowsException() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        when(typedQuery.getSingleResult()).thenThrow(new jakarta.persistence.NoResultException());

        // When & Then
        assertThatThrownBy(() -> repository.deleteById(2L))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("delete - 테넌트 접근 권한 검증")
    void testDelete_WithTenantValidation() {
        // Given
        TenantContextHolder.setTenant(testTenant1);

        // When
        repository.delete(testUser1);

        // Then
        verify(entityManager).remove(testUser1);
    }

    @Test
    @DisplayName("delete - 다른 테넌트의 엔티티 삭제 시 예외")
    void testDelete_DifferentTenant_ThrowsException() {
        // Given
        TenantContextHolder.setTenant(testTenant1);

        // When & Then
        assertThatThrownBy(() -> repository.delete(testUser2))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("deleteAll - 테넌트 필터링 적용")
    void testDeleteAll_WithTenantFiltering() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        List<User> expectedUsers = List.of(testUser1);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);

        // When
        repository.deleteAll();

        // Then
        // 테넌트 필터링이 적용되었는지 확인
        verify(criteriaBuilder).equal(root.get("tenant"), testTenant1);
        verify(entityManager).remove(testUser1);
    }

    @Test
    @DisplayName("deleteAll with Iterable - 테넌트 접근 권한 검증")
    void testDeleteAll_WithIterable_WithTenantValidation() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        List<User> usersToDelete = List.of(testUser1);

        // When
        repository.deleteAll(usersToDelete);

        // Then
        verify(entityManager).remove(testUser1);
    }

    @Test
    @DisplayName("테넌트 컨텍스트 변경 시 다른 테넌트 데이터만 조회")
    void testTenantContextChange_DifferentTenantData() {
        // Given
        TenantContextHolder.setTenant(testTenant1);
        List<User> tenant1Users = List.of(testUser1);
        when(typedQuery.getResultList()).thenReturn(tenant1Users);
        
        List<User> result1 = repository.findAll();
        assertThat(result1).hasSize(1);
        assertThat(result1.get(0).getTenant().getId()).isEqualTo(1L);

        // When - 다른 테넌트로 변경
        TenantContextHolder.setTenant(testTenant2);
        List<User> tenant2Users = List.of(testUser2);
        when(typedQuery.getResultList()).thenReturn(tenant2Users);
        
        List<User> result2 = repository.findAll();

        // Then
        assertThat(result2).hasSize(1);
        assertThat(result2.get(0).getTenant().getId()).isEqualTo(2L);
    }
}
