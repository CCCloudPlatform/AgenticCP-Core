package com.agenticcp.core.domain.organization.repository;

import com.agenticcp.core.domain.organization.entity.OrganizationUser;
import com.agenticcp.core.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Organization User Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, Long> {

    /**
     * Organization ID로 OrganizationUser 목록 조회
     * 
     * @param organizationId Organization ID
     * @return OrganizationUser 목록
     */
    List<OrganizationUser> findByOrganizationIdAndIsDeletedFalse(Long organizationId);

    /**
     * User ID로 OrganizationUser 목록 조회
     * 
     * @param userId User ID
     * @return OrganizationUser 목록
     */
    List<OrganizationUser> findByUserIdAndIsDeletedFalse(Long userId);

    /**
     * Organization ID와 User ID로 OrganizationUser 조회
     * 
     * @param organizationId Organization ID
     * @param userId User ID
     * @return OrganizationUser (Optional)
     */
    Optional<OrganizationUser> findByOrganizationIdAndUserIdAndIsDeletedFalse(Long organizationId, Long userId);

    /**
     * User가 속한 Organization ID 목록 조회
     * 
     * @param userId User ID
     * @return Organization ID 목록
     */
    @Query("SELECT DISTINCT ou.organization.id FROM OrganizationUser ou " +
           "WHERE ou.user.id = :userId AND ou.isDeleted = false")
    List<Long> findOrganizationIdsByUserId(@Param("userId") Long userId);

    /**
     * User가 특정 Organization에 속하는지 확인
     * 
     * @param userId User ID
     * @param organizationId Organization ID
     * @return 존재 여부
     */
    boolean existsByUserIdAndOrganizationIdAndIsDeletedFalse(Long userId, Long organizationId);

    /**
     * Organization ID와 Status로 OrganizationUser 목록 조회
     * 
     * @param organizationId Organization ID
     * @param status Status
     * @return OrganizationUser 목록
     */
    @Query("SELECT ou FROM OrganizationUser ou " +
           "WHERE ou.organization.id = :organizationId " +
           "AND ou.status = :status " +
           "AND ou.isDeleted = false")
    List<OrganizationUser> findByOrganizationIdAndStatus(
        @Param("organizationId") Long organizationId,
        @Param("status") Status status
    );
}

