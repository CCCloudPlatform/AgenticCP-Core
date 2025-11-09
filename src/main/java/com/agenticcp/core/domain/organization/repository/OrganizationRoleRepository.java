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

    @Query("SELECT orl FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.role = :role AND orl.isDeleted = false")
    Optional<OrganizationRole> findByOrganizationAndRole(@Param("organization") Organization organization,
                                                         @Param("role") Role role);

    @Query("SELECT orl FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.isDeleted = false ORDER BY orl.priority ASC, orl.id ASC")
    List<OrganizationRole> findByOrganizationOrderByPriority(@Param("organization") Organization organization);

    @Query("SELECT COUNT(orl) > 0 FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.role = :role AND orl.isDeleted = false")
    boolean existsByOrganizationAndRole(@Param("organization") Organization organization,
                                        @Param("role") Role role);

    @Query("SELECT COUNT(orl) FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.isDefault = true AND orl.isDeleted = false")
    long countDefaultByOrganization(@Param("organization") Organization organization);

    @Query("SELECT orl FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.isDefault = true AND orl.isDeleted = false")
    Optional<OrganizationRole> findDefaultByOrganization(@Param("organization") Organization organization);

    @Query("SELECT orl FROM OrganizationRole orl WHERE orl.organization = :organization AND orl.status = :status AND orl.isDeleted = false ORDER BY orl.priority ASC")
    List<OrganizationRole> findActiveByOrganization(@Param("organization") Organization organization,
                                                    @Param("status") Status status);
}


