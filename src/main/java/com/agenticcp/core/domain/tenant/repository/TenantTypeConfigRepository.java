package com.agenticcp.core.domain.tenant.repository;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 테넌트 타입별 기본 설정 Repository
 * 
 * 테넌트 타입(ENTERPRISE, STANDARD, TRIAL)별 기본 설정값을 조회하고 관리하는
 * 데이터 액세스 레이어입니다. TenantTypeConfig 엔티티에 대한 CRUD 및
 * 커스텀 쿼리를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-23
 */
@Repository
public interface TenantTypeConfigRepository extends JpaRepository<TenantTypeConfig, Long> {

    /**
     * 테넌트 타입으로 모든 기본 설정 조회
     * 
     * 특정 테넌트 타입(ENTERPRISE, STANDARD, TRIAL)의 모든 기본 설정을 조회합니다.
     * 이 설정은 해당 타입의 모든 테넌트에 적용되는 기본값입니다.
     * 삭제되지 않은(isDeleted=false) 설정만 반환합니다.
     * 
     * @param tenantType 조회할 테넌트 타입 (ENTERPRISE, STANDARD, TRIAL)
     * @return 테넌트 타입의 기본 설정 목록 (삭제되지 않은 것만)
     */
    List<TenantTypeConfig> findByTenantTypeAndIsDeletedFalse(Tenant.TenantType tenantType);

    /**
     * 테넌트 타입과 설정 키로 특정 기본 설정 조회
     * 
     * 특정 테넌트 타입의 특정 설정 키에 해당하는 기본 설정을 조회합니다.
     * 해당 타입의 모든 테넌트에 적용되는 기본값을 확인할 때 사용합니다.
     * 삭제되지 않은(isDeleted=false) 설정만 반환합니다.
     * 
     * @param tenantType 조회할 테넌트 타입 (ENTERPRISE, STANDARD, TRIAL)
     * @param configKey 조회할 설정 키 (예: "max_users")
     * @return 테넌트 타입의 특정 기본 설정 (없으면 empty)
     */
    Optional<TenantTypeConfig> findByTenantTypeAndConfigKeyAndIsDeletedFalse(Tenant.TenantType tenantType, String configKey);

    /**
     * 모든 테넌트 타입의 기본 설정 일괄 조회
     * 
     * 모든 테넌트 타입(ENTERPRISE, STANDARD, TRIAL)의 기본 설정을
     * 한 번에 조회합니다. 전체 설정 비교, 초기 설정 로드, 관리자 페이지 등에서
     * 사용합니다. 삭제되지 않은(isDeleted=false) 설정만 반환합니다.
     * 
     * @return 모든 테넌트 타입의 기본 설정 목록 (삭제되지 않은 것만)
     */
    List<TenantTypeConfig> findByIsDeletedFalse();
}



