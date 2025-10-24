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
     * 조직명 중복 검사
     * @param orgName 조직명
     * @return 중복 여부
     */
    boolean existsByOrgName(String orgName);
    
    /**
     * 조직 키 중복 검사
     * @param orgKey 조직 키
     * @return 중복 여부
     */
    boolean existsByOrgKey(String orgKey);
    
    /**
     * 하위 조직 존재 여부 확인
     * @param parentOrgId 상위 조직 ID
     * @return 하위 조직 존재 여부
     */
    boolean existsByParentOrganizationId(Long parentOrgId);
    
    /**
     * 활성 조직 목록 조회
     * @return 활성 조직 목록
     */
    @Query("SELECT o FROM Organization o WHERE o.status = 'ACTIVE'")
    List<Organization> findActiveOrganizations();
    
    /**
     * 특정 조직의 하위 조직 목록 조회
     * @param parentOrgId 상위 조직 ID
     * @return 하위 조직 목록
     */
    List<Organization> findByParentOrganizationId(Long parentOrgId);
    
    /**
     * 루트 조직 목록 조회 (상위 조직이 없는 조직들)
     * @return 루트 조직 목록
     */
    @Query("SELECT o FROM Organization o WHERE o.parentOrganization IS NULL")
    List<Organization> findRootOrganizations();
    
    /**
     * 조직 수 조회
     * @return 조직 수
     */
    long count();
}
