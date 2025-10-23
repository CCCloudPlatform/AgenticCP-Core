package com.agenticcp.core.domain.user.repository;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.Organization;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 조직 저장소 인터페이스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Repository
public interface OrganizationRepository extends TenantAwareRepository<Organization, Long> {
    
    /**
     * 조직 키로 조직 조회
     * 
     * @param orgKey 조직 키
     * @return 조직 정보
     */
    Optional<Organization> findByOrgKey(String orgKey);
    
    /**
     * 테넌트별 조직 목록 조회
     * 
     * @param tenant 테넌트
     * @return 조직 목록
     */
    List<Organization> findByTenant(Tenant tenant);
    
    /**
     * 상위 조직별 하위 조직 목록 조회
     * 
     * @param parentOrganization 상위 조직
     * @return 하위 조직 목록
     */
    List<Organization> findByParentOrganization(Organization parentOrganization);
    
    /**
     * 조직 타입별 조직 목록 조회
     * 
     * @param orgType 조직 타입
     * @return 조직 목록
     */
    List<Organization> findByOrgType(Organization.OrganizationType orgType);
    
    /**
     * 상태별 조직 목록 조회
     * 
     * @param status 상태
     * @return 조직 목록
     */
    List<Organization> findByStatus(Status status);
    
    /**
     * 테넌트별 활성 조직 목록 조회
     * 
     * @param tenant 테넌트
     * @param status 상태
     * @return 활성 조직 목록
     */
    @Query("SELECT o FROM Organization o WHERE o.tenant = :tenant AND o.status = :status AND o.isDeleted = false")
    List<Organization> findActiveOrganizationsByTenant(@Param("tenant") Tenant tenant, @Param("status") Status status);
    
    /**
     * 테넌트별 최상위 조직 목록 조회 (상위 조직이 없는 조직들)
     * 
     * @param tenant 테넌트
     * @return 최상위 조직 목록
     */
    @Query("SELECT o FROM Organization o WHERE o.tenant = :tenant AND o.parentOrganization IS NULL AND o.isDeleted = false")
    List<Organization> findTopLevelOrganizationsByTenant(@Param("tenant") Tenant tenant);
    
    /**
     * 조직의 하위 조직 수 조회
     * 
     * @param parentOrganization 상위 조직
     * @return 하위 조직 수
     */
    @Query("SELECT COUNT(o) FROM Organization o WHERE o.parentOrganization = :parentOrganization AND o.isDeleted = false")
    Long countSubOrganizations(@Param("parentOrganization") Organization parentOrganization);
}
