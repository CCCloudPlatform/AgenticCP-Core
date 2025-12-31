package com.agenticcp.core.domain.organization.repository;

import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 조직 Repository
 * 
 * <p>조직 엔티티에 대한 데이터 접근을 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    /**
     * 조직명 중복 검사 (설계 B: name 필드 사용)
     * @param name 조직명
     * @return 중복 여부
     */
    boolean existsByName(String name);
    
    /**
     * 조직명 중복 검사 (Deprecated - 호환성 유지)
     * @deprecated existsByName 사용 권장
     */
    @Deprecated
    @Query("SELECT COUNT(o) > 0 FROM Organization o WHERE o.name = :orgName")
    boolean existsByOrgName(@Param("orgName") String orgName);
    
    /**
     * 조직 키 중복 검사 (Deprecated - 설계 B에 없음)
     * @deprecated 설계 B에는 orgKey 필드가 없음
     */
    @Deprecated
    @Query("SELECT false FROM Organization o WHERE 1=0")
    boolean existsByOrgKey(String orgKey);
    
    /**
     * 하위 조직 존재 여부 확인 (Deprecated - 설계 B에 계층 구조 없음)
     * @deprecated 설계 B에는 계층 구조가 없음
     */
    @Deprecated
    @Query("SELECT false FROM Organization o WHERE 1=0")
    boolean existsByParentOrganizationId(Long parentOrgId);
    
    /**
     * 활성 조직 목록 조회 (Deprecated - 설계 B에 status 필드 없음)
     * @deprecated 설계 B에는 status 필드가 없음
     */
    @Deprecated
    @Query("SELECT o FROM Organization o")
    List<Organization> findActiveOrganizations();
    
    /**
     * 특정 조직의 하위 조직 목록 조회 (Deprecated - 설계 B에 계층 구조 없음)
     * @deprecated 설계 B에는 계층 구조가 없음
     */
    @Deprecated
    @Query("SELECT o FROM Organization o WHERE 1=0")
    List<Organization> findByParentOrganizationId(Long parentOrgId);
    
    /**
     * 루트 조직 목록 조회 (Deprecated - 설계 B에 계층 구조 없음)
     * @deprecated 설계 B에는 계층 구조가 없음
     */
    @Deprecated
    @Query("SELECT o FROM Organization o")
    List<Organization> findRootOrganizations();
    
    /**
     * 조직 수 조회
     * @return 조직 수
     */
    long count();
    
    /**
     * 특정 조직에 연결된 테넌트 조회 (1:1 관계)
     * @param organizationId 조직 ID
     * @return 테넌트 (Optional)
     */
    @Query("SELECT t FROM Tenant t WHERE t.organization.id = :organizationId")
    Optional<Tenant> findTenantByOrganizationId(@Param("organizationId") Long organizationId);
    
    /**
     * 조직에 테넌트가 존재하는지 확인 (1:1 관계)
     * @param organizationId 조직 ID
     * @return 테넌트 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Tenant t WHERE t.organization.id = :organizationId")
    boolean existsTenantByOrganizationId(@Param("organizationId") Long organizationId);
}
