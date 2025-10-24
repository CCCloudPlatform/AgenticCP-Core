package com.agenticcp.core.domain.organization.repository;

import com.agenticcp.core.domain.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    /**
     * 테넌트별 조직 목록 조회
     * @param tenantId 테넌트 ID
     * @return 조직 목록
     */
    List<Organization> findByTenantId(Long tenantId);
    
    /**
     * 테넌트별 특정 조직 조회
     * @param id 조직 ID
     * @param tenantId 테넌트 ID
     * @return 조직 정보
     */
    Optional<Organization> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * 조직명 중복 검사 (같은 테넌트 내에서)
     * @param orgName 조직명
     * @param tenantId 테넌트 ID
     * @return 중복 여부
     */
    boolean existsByOrgNameAndTenantId(String orgName, Long tenantId);
    
    /**
     * 하위 조직 존재 여부 확인
     * @param parentOrgId 상위 조직 ID
     * @return 하위 조직 존재 여부
     */
    boolean existsByParentOrganizationId(Long parentOrgId);
    
    /**
     * 테넌트별 활성 조직 목록 조회
     * @param tenantId 테넌트 ID
     * @return 활성 조직 목록
     */
    @Query("SELECT o FROM Organization o WHERE o.tenant.id = :tenantId AND o.status = 'ACTIVE'")
    List<Organization> findActiveOrganizationsByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * 특정 조직의 하위 조직 목록 조회
     * @param parentOrgId 상위 조직 ID
     * @return 하위 조직 목록
     */
    List<Organization> findByParentOrganizationId(Long parentOrgId);
    
    /**
     * 테넌트별 조직 수 조회
     * @param tenantId 테넌트 ID
     * @return 조직 수
     */
    long countByTenantId(Long tenantId);
}
