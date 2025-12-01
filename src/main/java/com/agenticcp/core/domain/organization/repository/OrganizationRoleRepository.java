package com.agenticcp.core.domain.organization.repository;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationRole;
import com.agenticcp.core.domain.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 조직별 역할 매핑 리포지토리
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Repository
public interface OrganizationRoleRepository extends JpaRepository<OrganizationRole, Long> {

    /**
     * 조직과 역할로 매핑 조회
     * 
     * @param organization 조직
     * @param role 역할
     * @return 조직-역할 매핑 (Optional)
     */
    @Query("SELECT orl FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.role = :role AND orl.isDeleted = false")
    Optional<OrganizationRole> findByOrganizationAndRole(@Param("organization") Organization organization,
                                                         @Param("role") Role role);

    /**
     * 조직별 역할 목록 조회 (우선순위 순)
     * 
     * @param organization 조직
     * @return 조직-역할 매핑 목록 (우선순위 순)
     */
    @Query("SELECT orl FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.isDeleted = false ORDER BY orl.priority ASC, orl.id ASC")
    List<OrganizationRole> findByOrganizationOrderByPriority(@Param("organization") Organization organization);

    /**
     * 조직과 역할 매핑 존재 여부 확인
     * 
     * @param organization 조직
     * @param role 역할
     * @return 매핑 존재 여부
     */
    @Query("SELECT COUNT(orl) > 0 FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.role = :role AND orl.isDeleted = false")
    boolean existsByOrganizationAndRole(@Param("organization") Organization organization,
                                        @Param("role") Role role);

    /**
     * 조직의 기본 역할 개수 조회
     * 
     * @param organization 조직
     * @return 기본 역할 개수
     */
    @Query("SELECT COUNT(orl) FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.isDefault = true AND orl.isDeleted = false")
    long countDefaultByOrganization(@Param("organization") Organization organization);

    /**
     * 조직의 기본 역할 조회
     * 
     * @param organization 조직
     * @return 기본 역할 매핑 (Optional)
     */
    @Query("SELECT orl FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.isDefault = true AND orl.isDeleted = false")
    Optional<OrganizationRole> findDefaultByOrganization(@Param("organization") Organization organization);

    /**
     * 조직의 활성 역할 목록 조회 (우선순위 순)
     * 
     * @param organization 조직
     * @param status 상태
     * @return 활성 역할 매핑 목록 (우선순위 순)
     */
    @Query("SELECT orl FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.status = :status AND orl.isDeleted = false ORDER BY orl.priority ASC")
    List<OrganizationRole> findActiveByOrganization(@Param("organization") Organization organization,
                                                    @Param("status") Status status);
}


