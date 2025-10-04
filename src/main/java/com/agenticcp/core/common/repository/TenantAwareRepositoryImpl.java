package com.agenticcp.core.common.repository;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 테넌트 인식 Repository 구현체
 * 모든 기본 JPA 메서드를 테넌트 필터링 버전으로 오버라이드
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
public class TenantAwareRepositoryImpl<T extends BaseEntity, ID extends Serializable> 
        extends SimpleJpaRepository<T, ID> implements TenantAwareRepository<T, ID> {

    private final EntityManager entityManager;
    private final Class<T> domainClass;

    public TenantAwareRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.domainClass = entityInformation.getJavaType();
    }

    /**
     * 현재 테넌트의 모든 엔티티 조회 (기본 findAll 오버라이드)
     */
    @Override
    @NonNull
    public List<T> findAll() {
        return findAllForCurrentTenant();
    }

    /**
     * 현재 테넌트의 모든 엔티티 조회 (정렬 포함)
     */
    @Override
    @NonNull
    public List<T> findAll(@NonNull Sort sort) {
        Tenant currentTenant = getCurrentTenantOrThrow();
        return findByTenantWithSort(currentTenant, sort);
    }

    /**
     * 현재 테넌트의 엔티티 조회 (페이징 포함)
     */
    @Override
    @NonNull
    public Page<T> findAll(@NonNull Pageable pageable) {
        Tenant currentTenant = getCurrentTenantOrThrow();
        return findByTenantWithPageable(currentTenant, pageable);
    }

    /**
     * 현재 테넌트에서 ID로 엔티티 조회 (기본 findById 오버라이드)
     */
    @Override
    @NonNull
    public Optional<T> findById(@NonNull ID id) {
        return findByIdForCurrentTenant(id);
    }

    /**
     * 현재 테넌트에서 엔티티 존재 여부 확인 (기본 existsById 오버라이드)
     */
    @Override
    public boolean existsById(@NonNull ID id) {
        return existsByIdForCurrentTenant(id);
    }

    /**
     * 현재 테넌트의 엔티티 수 조회 (기본 count 오버라이드)
     */
    @Override
    public long count() {
        return countForCurrentTenant();
    }

    /**
     * 현재 테넌트에서 엔티티 삭제 (기본 deleteById 오버라이드)
     */
    @Override
    public void deleteById(@NonNull ID id) {
        deleteByIdForCurrentTenant(id);
    }

    /**
     * 현재 테넌트에서 엔티티 삭제 (기본 delete 오버라이드)
     */
    @Override
    public void delete(@NonNull T entity) {
        validateTenantAccess(entity);
        super.delete(entity);
    }

    /**
     * 현재 테넌트에서 엔티티들 삭제 (기본 deleteAll 오버라이드)
     */
    @Override
    public void deleteAll(@NonNull Iterable<? extends T> entities) {
        for (T entity : entities) {
            validateTenantAccess(entity);
        }
        super.deleteAll(entities);
    }

    /**
     * 현재 테넌트의 모든 엔티티 삭제 (기본 deleteAll 오버라이드)
     */
    @Override
    public void deleteAll() {
        Tenant currentTenant = getCurrentTenantOrThrow();
        deleteAllByTenant(currentTenant);
    }

    // ========== TenantAwareRepository 인터페이스 구현 ==========

    @Override
    public List<T> findByTenant(Tenant tenant) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        query.select(root).where(cb.equal(root.get("tenant"), tenant));
        
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public Optional<T> findByIdAndTenant(ID id, Tenant tenant) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        Predicate idPredicate = cb.equal(root.get("id"), id);
        Predicate tenantPredicate = cb.equal(root.get("tenant"), tenant);
        
        query.select(root).where(cb.and(idPredicate, tenantPredicate));
        
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        List<T> results = typedQuery.getResultList();
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public void deleteByIdAndTenant(ID id, Tenant tenant) {
        Optional<T> entity = findByIdAndTenant(id, tenant);
        if (entity.isPresent()) {
            entityManager.remove(entity.get());
        }
    }

    @Override
    public long countByTenant(Tenant tenant) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(domainClass);
        
        query.select(cb.count(root)).where(cb.equal(root.get("tenant"), tenant));
        
        return entityManager.createQuery(query).getSingleResult();
    }

    // ========== 추가 헬퍼 메서드들 ==========

    /**
     * 테넌트별 엔티티 조회 (정렬 포함)
     */
    private List<T> findByTenantWithSort(Tenant tenant, Sort sort) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        query.select(root).where(cb.equal(root.get("tenant"), tenant));
        
        // 정렬 적용
        if (sort != null && sort.isSorted()) {
            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
            for (Sort.Order order : sort) {
                if (order.getDirection().isAscending()) {
                    orders.add(cb.asc(root.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(order.getProperty())));
                }
            }
            query.orderBy(orders);
        }
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * 테넌트별 엔티티 조회 (페이징 포함)
     */
    private Page<T> findByTenantWithPageable(Tenant tenant, Pageable pageable) {
        // 전체 개수 조회
        long total = countByTenant(tenant);
        
        // 페이징된 데이터 조회
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        query.select(root).where(cb.equal(root.get("tenant"), tenant));
        
        // 정렬 적용
        if (pageable.getSort().isSorted()) {
            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
            for (Sort.Order order : pageable.getSort()) {
                if (order.getDirection().isAscending()) {
                    orders.add(cb.asc(root.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(order.getProperty())));
                }
            }
            query.orderBy(orders);
        }
        
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<T> content = typedQuery.getResultList();
        
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 테넌트별 모든 엔티티 삭제
     */
    private void deleteAllByTenant(Tenant tenant) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        query.select(root).where(cb.equal(root.get("tenant"), tenant));
        
        List<T> entities = entityManager.createQuery(query).getResultList();
        for (T entity : entities) {
            entityManager.remove(entity);
        }
    }

    /**
     * 엔티티의 테넌트 접근 권한 검증
     */
    private void validateTenantAccess(T entity) {
        if (entity == null) {
            return;
        }
        
        Tenant currentTenant = getCurrentTenantOrThrow();
        Tenant entityTenant = entity.getTenant();
        
        if (entityTenant == null || !entityTenant.getId().equals(currentTenant.getId())) {
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET, 
                "Access denied: Entity does not belong to current tenant");
        }
    }

    /**
     * 현재 테넌트 조회 (예외 포함)
     */
    private Tenant getCurrentTenantOrThrow() {
        try {
            return TenantContextHolder.getCurrentTenantOrThrow();
        } catch (Exception e) {
            log.error("Failed to get current tenant context: {}", e.getMessage());
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET, 
                "Tenant context is required for repository operations");
        }
    }

    /**
     * Specification을 사용한 테넌트 필터링 조회
     */
    @NonNull
    public List<T> findAll(@Nullable Specification<T> spec) {
        Tenant currentTenant = getCurrentTenantOrThrow();
        Specification<T> tenantSpec = (root, query, cb) -> cb.equal(root.get("tenant"), currentTenant);
        Specification<T> combinedSpec = spec != null ? spec.and(tenantSpec) : tenantSpec;
        
        return super.findAll(combinedSpec);
    }

    /**
     * Specification을 사용한 테넌트 필터링 조회 (페이징 포함)
     */
    @NonNull
    public Page<T> findAll(@Nullable Specification<T> spec, @NonNull Pageable pageable) {
        Tenant currentTenant = getCurrentTenantOrThrow();
        Specification<T> tenantSpec = (root, query, cb) -> cb.equal(root.get("tenant"), currentTenant);
        Specification<T> combinedSpec = spec != null ? spec.and(tenantSpec) : tenantSpec;
        
        return super.findAll(combinedSpec, pageable);
    }

    /**
     * Specification을 사용한 테넌트 필터링 조회 (정렬 포함)
     */
    @NonNull
    public List<T> findAll(@Nullable Specification<T> spec, @NonNull Sort sort) {
        Tenant currentTenant = getCurrentTenantOrThrow();
        Specification<T> tenantSpec = (root, query, cb) -> cb.equal(root.get("tenant"), currentTenant);
        Specification<T> combinedSpec = spec != null ? spec.and(tenantSpec) : tenantSpec;
        
        return super.findAll(combinedSpec, sort);
    }
}
