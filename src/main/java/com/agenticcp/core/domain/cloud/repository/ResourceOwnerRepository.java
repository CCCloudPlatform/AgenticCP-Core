package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.ResourceOwner;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 리소스 소유자 Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface ResourceOwnerRepository extends JpaRepository<ResourceOwner, Long> {

    /**
     * 사용자가 소유한 리소스 목록 조회 (JOIN 최적화)
     * 
     * @param user 사용자
     * @param tenant 테넌트
     * @return 소유한 리소스 목록
     */
    @Query("SELECT DISTINCT ro.resource FROM ResourceOwner ro " +
           "LEFT JOIN FETCH ro.resource.provider " +
           "LEFT JOIN FETCH ro.resource.region " +
           "LEFT JOIN FETCH ro.resource.service " +
           "LEFT JOIN FETCH ro.resource.tenant " +
           "WHERE ro.user = :user AND ro.tenant = :tenant AND ro.isDeleted = false " +
           "AND ro.resource.isDeleted = false")
    List<CloudResource> findResourcesByOwner(@Param("user") User user, 
                                            @Param("tenant") Tenant tenant);

    /**
     * 리소스 소유권 확인
     * 
     * @param resource 리소스
     * @param user 사용자
     * @return 소유권 존재 여부
     */
    boolean existsByResourceAndUserAndIsDeletedFalse(CloudResource resource, User user);

    /**
     * 리소스 소유권 조회
     * 
     * @param resource 리소스
     * @param user 사용자
     * @return 리소스 소유권 정보
     */
    Optional<ResourceOwner> findByResourceAndUserAndIsDeletedFalse(CloudResource resource, User user);

    /**
     * 리소스의 모든 소유자 조회
     * 
     * @param resource 리소스
     * @return 소유자 목록
     */
    List<ResourceOwner> findByResourceAndIsDeletedFalse(CloudResource resource);

    /**
     * 사용자와 테넌트로 소유권 목록 조회
     * 
     * @param user 사용자
     * @param tenant 테넌트
     * @return 소유권 목록
     */
    List<ResourceOwner> findByUserAndTenantAndIsDeletedFalse(User user, Tenant tenant);

    /**
     * 사용자가 소유한 리소스 ID 목록 조회 (배치 최적화)
     * 
     * @param user 사용자
     * @param tenant 테넌트
     * @return 소유한 리소스 ID 목록
     */
    @Query("SELECT ro.resource.id FROM ResourceOwner ro " +
           "WHERE ro.user = :user AND ro.tenant = :tenant AND ro.isDeleted = false")
    List<Long> findResourceIdsByOwner(@Param("user") User user, 
                                     @Param("tenant") Tenant tenant);

    /**
     * 여러 리소스에 대한 사용자 소유권 일괄 확인 (배치 최적화)
     * 
     * @param user 사용자
     * @param resourceIds 리소스 ID 목록
     * @param tenant 테넌트
     * @return 소유한 리소스 ID 목록
     */
    @Query("SELECT ro.resource.id FROM ResourceOwner ro " +
           "WHERE ro.user = :user " +
           "AND ro.resource.id IN :resourceIds " +
           "AND ro.tenant = :tenant " +
           "AND ro.isDeleted = false")
    List<Long> findOwnedResourceIds(@Param("user") User user,
                                   @Param("resourceIds") List<Long> resourceIds,
                                   @Param("tenant") Tenant tenant);
}

