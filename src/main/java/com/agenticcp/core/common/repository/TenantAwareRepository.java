package com.agenticcp.core.common.repository;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.entity.TenantAwareEntity;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * 테넌트 인식 Repository 인터페이스 - 테넌트별 데이터 격리
 * 
 * <p>
 * 멀티 테넌시 환경에서 테넌트별 데이터 격리를 지원하는 베이스 리포지토리입니다.
 * TenantAwareEntity를 상속받은 엔티티에 대한 데이터 액세스 계층을 제공하며,
 * 자동으로 현재 테넌트 컨텍스트를 적용하여 데이터를 필터링합니다.
 * </p>
 * 
 * <p>
 * 이 인터페이스는 현재 테넌트 컨텍스트 기반의 메서드들을 제공하며,
 * TenantContextHolder를 통해 자동으로 테넌트 정보를 가져와 적용합니다.
 * </p>
 * 
 * <p>
 * {@code @NoRepositoryBean} 어노테이션을 통해 Spring Data JPA가 
 * 이 인터페이스의 구현체를 생성하지 않도록 합니다.
 * </p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 * @see com.agenticcp.core.common.entity.TenantAwareEntity
 * @see com.agenticcp.core.common.context.TenantContextHolder
 * @see com.agenticcp.core.common.repository.BaseRepository
 */
@NoRepositoryBean
public interface TenantAwareRepository<T extends TenantAwareEntity, ID> extends BaseRepository<T, ID> {
    
    /**
     * 현재 테넌트의 모든 엔티티를 조회합니다.
     * 
     * <p>
     * TenantContextHolder에서 현재 테넌트를 가져와 해당 테넌트의 모든 엔티티를 반환합니다.
     * 테넌트 컨텍스트가 설정되지 않은 경우 예외가 발생합니다.
     * </p>
     * 
     * @return 현재 테넌트의 엔티티 목록
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    default List<T> findAllForCurrentTenant() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return findByTenant(currentTenant);
    }
    
    /**
     * 특정 테넌트의 모든 엔티티를 조회합니다.
     * 
     * <p>
     * 구현 클래스에서 반드시 구현해야 하는 메서드입니다.
     * Spring Data JPA의 메서드 이름 규칙을 사용하여 자동으로 쿼리가 생성됩니다.
     * </p>
     * 
     * @param tenant 조회할 테넌트
     * @return 해당 테넌트의 엔티티 목록
     */
    List<T> findByTenant(Tenant tenant);
    
    /**
     * 현재 테넌트에서 ID로 엔티티를 조회합니다.
     * 
     * <p>
     * TenantContextHolder에서 현재 테넌트를 가져와 해당 테넌트의 엔티티만 조회합니다.
     * 다른 테넌트의 데이터는 접근할 수 없으므로 데이터 격리를 보장합니다.
     * </p>
     * 
     * @param id 조회할 엔티티의 ID
     * @return 현재 테넌트에 속한 엔티티 (존재하지 않으면 Empty)
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    default Optional<T> findByIdForCurrentTenant(ID id) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return findByIdAndTenant(id, currentTenant);
    }
    
    /**
     * 특정 테넌트에서 ID로 엔티티를 조회합니다.
     * 
     * <p>
     * 구현 클래스에서 반드시 구현해야 하는 메서드입니다.
     * Spring Data JPA의 메서드 이름 규칙을 사용하여 자동으로 쿼리가 생성됩니다.
     * </p>
     * 
     * @param id 조회할 엔티티의 ID
     * @param tenant 조회할 테넌트
     * @return 해당 테넌트에 속한 엔티티 (존재하지 않으면 Empty)
     */
    Optional<T> findByIdAndTenant(ID id, Tenant tenant);
    
    /**
     * 현재 테넌트에서 엔티티의 존재 여부를 확인합니다.
     * 
     * <p>
     * 내부적으로 findByIdForCurrentTenant()를 호출하여 엔티티 존재 여부를 확인합니다.
     * </p>
     * 
     * @param id 확인할 엔티티의 ID
     * @return 존재하면 true, 존재하지 않으면 false
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    default boolean existsByIdForCurrentTenant(ID id) {
        return findByIdForCurrentTenant(id).isPresent();
    }
    
    /**
     * 현재 테넌트에서 엔티티를 삭제합니다.
     * 
     * <p>
     * TenantContextHolder에서 현재 테넌트를 가져와 해당 테넌트의 엔티티만 삭제합니다.
     * 다른 테넌트의 데이터는 삭제할 수 없으므로 데이터 격리를 보장합니다.
     * </p>
     * 
     * @param id 삭제할 엔티티의 ID
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    default void deleteByIdForCurrentTenant(ID id) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        deleteByIdAndTenant(id, currentTenant);
    }
    
    /**
     * 특정 테넌트에서 엔티티를 삭제합니다.
     * 
     * <p>
     * 구현 클래스에서 반드시 구현해야 하는 메서드입니다.
     * Spring Data JPA의 메서드 이름 규칙을 사용하여 자동으로 쿼리가 생성됩니다.
     * </p>
     * 
     * @param id 삭제할 엔티티의 ID
     * @param tenant 삭제할 테넌트
     */
    void deleteByIdAndTenant(ID id, Tenant tenant);
    
    /**
     * 현재 테넌트의 엔티티 개수를 조회합니다.
     * 
     * <p>
     * TenantContextHolder에서 현재 테넌트를 가져와 해당 테넌트의 엔티티 개수를 반환합니다.
     * </p>
     * 
     * @return 현재 테넌트의 엔티티 개수
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    default long countForCurrentTenant() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return countByTenant(currentTenant);
    }
    
    /**
     * 특정 테넌트의 엔티티 개수를 조회합니다.
     * 
     * <p>
     * 구현 클래스에서 반드시 구현해야 하는 메서드입니다.
     * Spring Data JPA의 메서드 이름 규칙을 사용하여 자동으로 쿼리가 생성됩니다.
     * </p>
     * 
     * @param tenant 조회할 테넌트
     * @return 해당 테넌트의 엔티티 개수
     */
    long countByTenant(Tenant tenant);
}
