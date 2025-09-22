package com.agenticcp.core.common.repository;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * 테넌트 인식 Repository 인터페이스
 * 자동으로 현재 테넌트 컨텍스트를 적용하여 데이터를 필터링합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID> {
    
    /**
     * 현재 테넌트의 모든 엔티티 조회
     * 
     * @return 현재 테넌트의 엔티티 목록
     */
    default List<T> findAllForCurrentTenant() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return findByTenant(currentTenant);
    }
    
    /**
     * 테넌트별 엔티티 조회 (구현 필요)
     * 
     * @param tenant 테넌트
     * @return 엔티티 목록
     */
    List<T> findByTenant(Tenant tenant);
    
    /**
     * 현재 테넌트에서 ID로 엔티티 조회
     * 
     * @param id 엔티티 ID
     * @return 엔티티 (현재 테넌트에 속한 경우만)
     */
    default Optional<T> findByIdForCurrentTenant(ID id) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return findByIdAndTenant(id, currentTenant);
    }
    
    /**
     * 테넌트와 ID로 엔티티 조회 (구현 필요)
     * 
     * @param id 엔티티 ID
     * @param tenant 테넌트
     * @return 엔티티
     */
    Optional<T> findByIdAndTenant(ID id, Tenant tenant);
    
    /**
     * 현재 테넌트에서 엔티티 존재 여부 확인
     * 
     * @param id 엔티티 ID
     * @return 존재 여부
     */
    default boolean existsByIdForCurrentTenant(ID id) {
        return findByIdForCurrentTenant(id).isPresent();
    }
    
    /**
     * 현재 테넌트에서 엔티티 삭제
     * 
     * @param id 엔티티 ID
     */
    default void deleteByIdForCurrentTenant(ID id) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        deleteByIdAndTenant(id, currentTenant);
    }
    
    /**
     * 테넌트와 ID로 엔티티 삭제 (구현 필요)
     * 
     * @param id 엔티티 ID
     * @param tenant 테넌트
     */
    void deleteByIdAndTenant(ID id, Tenant tenant);
    
    /**
     * 현재 테넌트의 엔티티 수 조회
     * 
     * @return 엔티티 수
     */
    default long countForCurrentTenant() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return countByTenant(currentTenant);
    }
    
    /**
     * 테넌트별 엔티티 수 조회 (구현 필요)
     * 
     * @param tenant 테넌트
     * @return 엔티티 수
     */
    long countByTenant(Tenant tenant);
}
