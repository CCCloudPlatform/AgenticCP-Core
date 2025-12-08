package com.agenticcp.core.common.repository;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.entity.TenantAwareEntity;
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
 * 
 * <p>
 * SimpleJpaRepository를 상속받아 모든 기본 JPA 메서드를 테넌트 필터링 버전으로 오버라이드합니다.
 * 멀티 테넌시 환경에서 데이터 격리를 자동으로 보장하며, Criteria API를 사용하여 
 * 동적 쿼리를 생성합니다.
 * </p>
 * 
 * <p>
 * 이 구현체는 Spring Data JPA의 RepositoryFactoryBean을 통해 자동으로 생성되며,
 * 모든 CRUD 작업에 현재 테넌트 필터링을 자동으로 적용합니다.
 * </p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 * @see com.agenticcp.core.common.repository.TenantAwareRepository
 * @see org.springframework.data.jpa.repository.support.SimpleJpaRepository
 */
@Slf4j
public class TenantAwareRepositoryImpl<T extends TenantAwareEntity, ID extends Serializable> 
        extends SimpleJpaRepository<T, ID> implements TenantAwareRepository<T, ID> {

    private final EntityManager entityManager;
    private final Class<T> domainClass;

    /**
     * TenantAwareRepositoryImpl 생성자
     * 
     * <p>
     * Spring Data JPA의 RepositoryFactoryBean에 의해 자동으로 호출됩니다.
     * EntityManager와 엔티티 정보를 주입받아 초기화합니다.
     * </p>
     * 
     * @param entityInformation JPA 엔티티 메타정보
     * @param entityManager JPA EntityManager
     */
    public TenantAwareRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.domainClass = entityInformation.getJavaType();
        
        log.debug("TenantAwareRepositoryImpl 초기화: domainClass={}", domainClass.getSimpleName());
    }

    /**
     * 현재 테넌트의 모든 엔티티를 조회합니다 (기본 findAll 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 findAll()을 오버라이드하여 현재 테넌트 필터링을 적용합니다.
     * TenantAwareRepository의 findAllForCurrentTenant()를 내부적으로 호출합니다.
     * </p>
     * 
     * @return 현재 테넌트의 모든 엔티티 목록
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @Override
    @NonNull
    public List<T> findAll() {
        log.debug("현재 테넌트의 모든 엔티티 조회: domainClass={}", domainClass.getSimpleName());
        return findAllForCurrentTenant();
    }

    /**
     * 현재 테넌트의 모든 엔티티를 정렬하여 조회합니다 (기본 findAll 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 findAll(Sort)을 오버라이드하여 현재 테넌트 필터링을 적용합니다.
     * </p>
     * 
     * @param sort 정렬 조건
     * @return 현재 테넌트의 정렬된 엔티티 목록
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @Override
    @NonNull
    public List<T> findAll(@NonNull Sort sort) {
        log.debug("현재 테넌트의 엔티티 조회 (정렬): domainClass={}, sort={}", 
            domainClass.getSimpleName(), sort);
        Tenant currentTenant = getCurrentTenantOrThrow();
        return findByTenantWithSort(currentTenant, sort);
    }

    /**
     * 현재 테넌트의 엔티티를 페이징하여 조회합니다 (기본 findAll 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 findAll(Pageable)을 오버라이드하여 현재 테넌트 필터링을 적용합니다.
     * </p>
     * 
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 현재 테넌트의 페이징된 엔티티
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @Override
    @NonNull
    public Page<T> findAll(@NonNull Pageable pageable) {
        log.debug("현재 테넌트의 엔티티 조회 (페이징): domainClass={}, page={}, size={}", 
            domainClass.getSimpleName(), pageable.getPageNumber(), pageable.getPageSize());
        Tenant currentTenant = getCurrentTenantOrThrow();
        return findByTenantWithPageable(currentTenant, pageable);
    }

    /**
     * 현재 테넌트에서 ID로 엔티티를 조회합니다 (기본 findById 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 findById()를 오버라이드하여 현재 테넌트 필터링을 적용합니다.
     * 다른 테넌트의 데이터는 조회할 수 없으므로 데이터 격리를 보장합니다.
     * </p>
     * 
     * @param id 조회할 엔티티의 ID
     * @return 현재 테넌트에 속한 엔티티 (존재하지 않으면 Empty)
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @Override
    @NonNull
    public Optional<T> findById(@NonNull ID id) {
        log.debug("현재 테넌트에서 엔티티 조회: domainClass={}, id={}", 
            domainClass.getSimpleName(), id);
        return findByIdForCurrentTenant(id);
    }

    /**
     * 현재 테넌트에서 엔티티의 존재 여부를 확인합니다 (기본 existsById 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 existsById()를 오버라이드하여 현재 테넌트 필터링을 적용합니다.
     * </p>
     * 
     * @param id 확인할 엔티티의 ID
     * @return 존재하면 true, 존재하지 않으면 false
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @Override
    public boolean existsById(@NonNull ID id) {
        log.debug("현재 테넌트에서 엔티티 존재 확인: domainClass={}, id={}", 
            domainClass.getSimpleName(), id);
        return existsByIdForCurrentTenant(id);
    }

    /**
     * 현재 테넌트의 엔티티 개수를 조회합니다 (기본 count 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 count()를 오버라이드하여 현재 테넌트 필터링을 적용합니다.
     * </p>
     * 
     * @return 현재 테넌트의 엔티티 개수
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @Override
    public long count() {
        log.debug("현재 테넌트의 엔티티 개수 조회: domainClass={}", domainClass.getSimpleName());
        return countForCurrentTenant();
    }

    /**
     * 현재 테넌트에서 엔티티를 삭제합니다 (기본 deleteById 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 deleteById()를 오버라이드하여 현재 테넌트 필터링을 적용합니다.
     * 다른 테넌트의 데이터는 삭제할 수 없으므로 데이터 격리를 보장합니다.
     * </p>
     * 
     * @param id 삭제할 엔티티의 ID
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @Override
    public void deleteById(@NonNull ID id) {
        log.debug("현재 테넌트에서 엔티티 삭제: domainClass={}, id={}", 
            domainClass.getSimpleName(), id);
        deleteByIdForCurrentTenant(id);
    }

    /**
     * 현재 테넌트에서 엔티티를 삭제합니다 (기본 delete 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 delete()를 오버라이드하여 현재 테넌트 접근 권한을 검증합니다.
     * 엔티티가 현재 테넌트에 속하지 않으면 예외가 발생합니다.
     * </p>
     * 
     * @param entity 삭제할 엔티티
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않았거나 엔티티가 현재 테넌트에 속하지 않은 경우
     */
    @Override
    public void delete(@NonNull T entity) {
        log.debug("엔티티 삭제: domainClass={}", domainClass.getSimpleName());
        validateTenantAccess(entity);
        super.delete(entity);
    }

    /**
     * 현재 테넌트에서 여러 엔티티를 삭제합니다 (기본 deleteAll 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 deleteAll(Iterable)을 오버라이드하여 
     * 각 엔티티에 대해 현재 테넌트 접근 권한을 검증합니다.
     * </p>
     * 
     * @param entities 삭제할 엔티티들
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않았거나 엔티티가 현재 테넌트에 속하지 않은 경우
     */
    @Override
    public void deleteAll(@NonNull Iterable<? extends T> entities) {
        log.debug("여러 엔티티 삭제: domainClass={}", domainClass.getSimpleName());
        // 모든 엔티티에 대해 테넌트 접근 권한 검증
        for (T entity : entities) {
            validateTenantAccess(entity);
        }
        super.deleteAll(entities);
    }

    /**
     * 현재 테넌트의 모든 엔티티를 삭제합니다 (기본 deleteAll 오버라이드).
     * 
     * <p>
     * SimpleJpaRepository의 deleteAll()을 오버라이드하여 현재 테넌트 필터링을 적용합니다.
     * 다른 테넌트의 데이터는 삭제되지 않으므로 데이터 격리를 보장합니다.
     * </p>
     * 
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @Override
    public void deleteAll() {
        log.debug("현재 테넌트의 모든 엔티티 삭제: domainClass={}", domainClass.getSimpleName());
        Tenant currentTenant = getCurrentTenantOrThrow();
        deleteAllByTenant(currentTenant);
    }

    // ========== TenantAwareRepository 인터페이스 구현 ==========

    /**
     * 특정 테넌트의 모든 엔티티를 조회합니다.
     * 
     * <p>
     * Criteria API를 사용하여 동적으로 쿼리를 생성하고 테넌트 필터링을 적용합니다.
     * </p>
     * 
     * @param tenant 조회할 테넌트
     * @return 해당 테넌트의 모든 엔티티 목록
     */
    @Override
    public List<T> findByTenant(Tenant tenant) {
        log.debug("테넌트별 엔티티 조회: domainClass={}, tenantId={}", 
            domainClass.getSimpleName(), tenant.getId());
        
        // Criteria API를 사용한 동적 쿼리 생성
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        // WHERE tenant = :tenant 조건 추가
        query.select(root).where(cb.equal(root.get("tenant"), tenant));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * 특정 테넌트에서 ID로 엔티티를 조회합니다.
     * 
     * <p>
     * Criteria API를 사용하여 ID와 테넌트 조건을 모두 만족하는 엔티티를 조회합니다.
     * 데이터 격리를 보장하기 위해 반드시 두 조건을 AND로 결합합니다.
     * </p>
     * 
     * @param id 조회할 엔티티의 ID
     * @param tenant 조회할 테넌트
     * @return 해당 테넌트에 속한 엔티티 (존재하지 않으면 Empty)
     */
    @Override
    public Optional<T> findByIdAndTenant(ID id, Tenant tenant) {
        log.debug("테넌트와 ID로 엔티티 조회: domainClass={}, id={}, tenantId={}", 
            domainClass.getSimpleName(), id, tenant.getId());
        
        // Criteria API를 사용한 동적 쿼리 생성
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        // WHERE id = :id AND tenant = :tenant 조건 생성
        Predicate idPredicate = cb.equal(root.get("id"), id);
        Predicate tenantPredicate = cb.equal(root.get("tenant"), tenant);
        
        query.select(root).where(cb.and(idPredicate, tenantPredicate));
        
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        List<T> results = typedQuery.getResultList();
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 특정 테넌트에서 엔티티를 삭제합니다.
     * 
     * <p>
     * 먼저 엔티티를 조회한 후 존재하는 경우에만 삭제합니다.
     * 다른 테넌트의 데이터는 삭제되지 않으므로 데이터 격리를 보장합니다.
     * </p>
     * 
     * @param id 삭제할 엔티티의 ID
     * @param tenant 삭제할 테넌트
     */
    @Override
    public void deleteByIdAndTenant(ID id, Tenant tenant) {
        log.debug("테넌트와 ID로 엔티티 삭제: domainClass={}, id={}, tenantId={}", 
            domainClass.getSimpleName(), id, tenant.getId());
        
        // 엔티티 존재 여부 확인 후 삭제
        Optional<T> entity = findByIdAndTenant(id, tenant);
        if (entity.isPresent()) {
            entityManager.remove(entity.get());
            log.debug("엔티티 삭제 완료: domainClass={}, id={}", domainClass.getSimpleName(), id);
        } else {
            log.debug("삭제할 엔티티가 존재하지 않음: domainClass={}, id={}", 
                domainClass.getSimpleName(), id);
        }
    }

    /**
     * 특정 테넌트의 엔티티 개수를 조회합니다.
     * 
     * <p>
     * Criteria API를 사용하여 COUNT 쿼리를 생성하고 테넌트 필터링을 적용합니다.
     * </p>
     * 
     * @param tenant 조회할 테넌트
     * @return 해당 테넌트의 엔티티 개수
     */
    @Override
    public long countByTenant(Tenant tenant) {
        log.debug("테넌트별 엔티티 개수 조회: domainClass={}, tenantId={}", 
            domainClass.getSimpleName(), tenant.getId());
        
        // Criteria API를 사용한 COUNT 쿼리 생성
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(domainClass);
        
        // SELECT COUNT(*) WHERE tenant = :tenant
        query.select(cb.count(root)).where(cb.equal(root.get("tenant"), tenant));
        
        return entityManager.createQuery(query).getSingleResult();
    }

    // ========== 추가 헬퍼 메서드들 ==========

    /**
     * 테넌트별 엔티티를 정렬하여 조회합니다 (private 헬퍼).
     * 
     * <p>
     * Criteria API를 사용하여 테넌트 필터링과 정렬을 동시에 적용합니다.
     * 정렬 조건이 없는 경우 기본 순서로 조회됩니다.
     * </p>
     * 
     * @param tenant 조회할 테넌트
     * @param sort 정렬 조건
     * @return 정렬된 엔티티 목록
     */
    private List<T> findByTenantWithSort(Tenant tenant, Sort sort) {
        // Criteria API를 사용한 동적 쿼리 생성
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        // WHERE tenant = :tenant 조건 추가
        query.select(root).where(cb.equal(root.get("tenant"), tenant));
        
        // 정렬 조건 적용
        if (sort != null && sort.isSorted()) {
            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
            for (Sort.Order order : sort) {
                // ASC 또는 DESC 방향에 따라 정렬 추가
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
     * 테넌트별 엔티티를 페이징하여 조회합니다 (private 헬퍼).
     * 
     * <p>
     * 먼저 전체 개수를 조회한 후 페이징된 데이터를 조회합니다.
     * Pageable에 포함된 정렬 조건도 함께 적용됩니다.
     * </p>
     * 
     * @param tenant 조회할 테넌트
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 페이징된 엔티티
     */
    private Page<T> findByTenantWithPageable(Tenant tenant, Pageable pageable) {
        // 1단계: 전체 개수 조회 (페이지 정보 계산을 위해 필요)
        long total = countByTenant(tenant);
        
        // 2단계: 페이징된 데이터 조회
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        // WHERE tenant = :tenant 조건 추가
        query.select(root).where(cb.equal(root.get("tenant"), tenant));
        
        // 정렬 조건 적용
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
        
        // 페이징 파라미터 설정
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());  // 시작 위치
        typedQuery.setMaxResults(pageable.getPageSize());        // 페이지 크기
        
        List<T> content = typedQuery.getResultList();
        
        // Page 객체 생성 (content, pageable, total)
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 테넌트의 모든 엔티티를 삭제합니다 (private 헬퍼).
     * 
     * <p>
     * 먼저 해당 테넌트의 모든 엔티티를 조회한 후 하나씩 삭제합니다.
     * 다른 테넌트의 데이터는 삭제되지 않으므로 데이터 격리를 보장합니다.
     * </p>
     * 
     * @param tenant 삭제할 테넌트
     */
    private void deleteAllByTenant(Tenant tenant) {
        log.debug("테넌트의 모든 엔티티 삭제: domainClass={}, tenantId={}", 
            domainClass.getSimpleName(), tenant.getId());
        
        // Criteria API를 사용한 동적 쿼리 생성
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(domainClass);
        Root<T> root = query.from(domainClass);
        
        // WHERE tenant = :tenant 조건으로 조회
        query.select(root).where(cb.equal(root.get("tenant"), tenant));
        
        List<T> entities = entityManager.createQuery(query).getResultList();
        
        // 조회된 모든 엔티티 삭제
        for (T entity : entities) {
            entityManager.remove(entity);
        }
        
        log.debug("테넌트의 엔티티 삭제 완료: domainClass={}, count={}", 
            domainClass.getSimpleName(), entities.size());
    }

    /**
     * 엔티티의 테넌트 접근 권한을 검증합니다 (private 헬퍼).
     * 
     * <p>
     * 엔티티가 현재 테넌트에 속하는지 확인합니다.
     * 다른 테넌트의 데이터에 접근하려고 하면 BusinessException이 발생합니다.
     * </p>
     * 
     * @param entity 검증할 엔티티
     * @throws BusinessException 엔티티가 현재 테넌트에 속하지 않은 경우
     */
    private void validateTenantAccess(T entity) {
        if (entity == null) {
            return;
        }
        
        Tenant currentTenant = getCurrentTenantOrThrow();
        Tenant entityTenant = entity.getTenant();
        
        // 엔티티의 테넌트가 없거나 현재 테넌트와 다른 경우 예외 발생
        if (entityTenant == null || !entityTenant.getId().equals(currentTenant.getId())) {
            log.error("테넌트 접근 권한 없음: domainClass={}, currentTenantId={}, entityTenantId={}", 
                domainClass.getSimpleName(), currentTenant.getId(), 
                entityTenant != null ? entityTenant.getId() : "null");
            
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET, 
                "접근 거부: 엔티티가 현재 테넌트에 속하지 않습니다");
        }
    }

    /**
     * 현재 테넌트를 조회합니다 (private 헬퍼).
     * 
     * <p>
     * TenantContextHolder에서 현재 테넌트를 가져옵니다.
     * 테넌트 컨텍스트가 설정되지 않은 경우 예외가 발생합니다.
     * </p>
     * 
     * @return 현재 테넌트
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    private Tenant getCurrentTenantOrThrow() {
        try {
            return TenantContextHolder.getCurrentTenantOrThrow();
        } catch (Exception e) {
            log.error("테넌트 컨텍스트 조회 실패: {}", e.getMessage());
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET, 
                "리포지토리 작업을 위해 테넌트 컨텍스트가 필요합니다");
        }
    }

    /**
     * Specification을 사용하여 현재 테넌트의 엔티티를 조회합니다.
     * 
     * <p>
     * 사용자가 제공한 Specification과 테넌트 필터링 Specification을 AND로 결합합니다.
     * 이를 통해 동적 쿼리 구성과 테넌트 격리를 동시에 보장합니다.
     * </p>
     * 
     * <p>
     * 사용 예시:
     * <pre>
     * Specification<User> spec = (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");
     * List<User> users = repository.findAll(spec); // 현재 테넌트의 ACTIVE 사용자만 조회
     * </pre>
     * </p>
     * 
     * @param spec 사용자 정의 Specification (null 가능)
     * @return 조건을 만족하는 현재 테넌트의 엔티티 목록
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @NonNull
    public List<T> findAll(@Nullable Specification<T> spec) {
        log.debug("Specification을 사용한 엔티티 조회: domainClass={}", domainClass.getSimpleName());
        
        Tenant currentTenant = getCurrentTenantOrThrow();
        
        // 테넌트 필터링 Specification 생성
        Specification<T> tenantSpec = (root, query, cb) -> cb.equal(root.get("tenant"), currentTenant);
        
        // 사용자 Specification과 AND로 결합
        Specification<T> combinedSpec = spec != null ? spec.and(tenantSpec) : tenantSpec;
        
        return super.findAll(combinedSpec);
    }

    /**
     * Specification을 사용하여 현재 테넌트의 엔티티를 페이징 조회합니다.
     * 
     * <p>
     * 사용자가 제공한 Specification과 테넌트 필터링 Specification을 AND로 결합합니다.
     * 페이징과 정렬 정보도 함께 적용됩니다.
     * </p>
     * 
     * <p>
     * 사용 예시:
     * <pre>
     * Specification<User> spec = (root, query, cb) -> cb.like(root.get("name"), "%John%");
     * PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("createdAt").descending());
     * Page<User> users = repository.findAll(spec, pageRequest);
     * </pre>
     * </p>
     * 
     * @param spec 사용자 정의 Specification (null 가능)
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 조건을 만족하는 현재 테넌트의 페이징된 엔티티
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @NonNull
    public Page<T> findAll(@Nullable Specification<T> spec, @NonNull Pageable pageable) {
        log.debug("Specification을 사용한 엔티티 조회 (페이징): domainClass={}, page={}, size={}", 
            domainClass.getSimpleName(), pageable.getPageNumber(), pageable.getPageSize());
        
        Tenant currentTenant = getCurrentTenantOrThrow();
        
        // 테넌트 필터링 Specification 생성
        Specification<T> tenantSpec = (root, query, cb) -> cb.equal(root.get("tenant"), currentTenant);
        
        // 사용자 Specification과 AND로 결합
        Specification<T> combinedSpec = spec != null ? spec.and(tenantSpec) : tenantSpec;
        
        return super.findAll(combinedSpec, pageable);
    }

    /**
     * Specification을 사용하여 현재 테넌트의 엔티티를 정렬 조회합니다.
     * 
     * <p>
     * 사용자가 제공한 Specification과 테넌트 필터링 Specification을 AND로 결합합니다.
     * 정렬 조건도 함께 적용됩니다.
     * </p>
     * 
     * <p>
     * 사용 예시:
     * <pre>
     * Specification<User> spec = (root, query, cb) -> cb.greaterThan(root.get("age"), 18);
     * Sort sort = Sort.by("name").ascending();
     * List<User> users = repository.findAll(spec, sort);
     * </pre>
     * </p>
     * 
     * @param spec 사용자 정의 Specification (null 가능)
     * @param sort 정렬 조건
     * @return 조건을 만족하는 현재 테넌트의 정렬된 엔티티 목록
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @NonNull
    public List<T> findAll(@Nullable Specification<T> spec, @NonNull Sort sort) {
        log.debug("Specification을 사용한 엔티티 조회 (정렬): domainClass={}, sort={}", 
            domainClass.getSimpleName(), sort);
        
        Tenant currentTenant = getCurrentTenantOrThrow();
        
        // 테넌트 필터링 Specification 생성
        Specification<T> tenantSpec = (root, query, cb) -> cb.equal(root.get("tenant"), currentTenant);
        
        // 사용자 Specification과 AND로 결합
        Specification<T> combinedSpec = spec != null ? spec.and(tenantSpec) : tenantSpec;
        
        return super.findAll(combinedSpec, sort);
    }
}
