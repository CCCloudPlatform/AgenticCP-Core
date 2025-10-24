package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 클라우드 계정 리포지토리
 * 
 * 테넌트별 클라우드 계정 관리를 위한 데이터 액세스 레이어입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-25
 */
@Repository
public interface CloudAccountRepository extends JpaRepository<CloudAccount, Long> {

    /**
     * 테넌트 ID로 모든 클라우드 계정을 조회합니다.
     * 
     * @param tenantId 테넌트 ID
     * @return 클라우드 계정 목록
     */
    @Query("SELECT ca FROM CloudAccount ca " +
           "WHERE ca.tenant.id = :tenantId " +
           "AND ca.isDeleted = false " +
           "ORDER BY ca.createdAt DESC")
    List<CloudAccount> findByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 테넌트 ID와 프로바이더 ID로 클라우드 계정 목록을 조회합니다.
     * 
     * @param tenantId 테넌트 ID
     * @param providerId 프로바이더 ID
     * @return 클라우드 계정 목록
     */
    @Query("SELECT ca FROM CloudAccount ca " +
           "WHERE ca.tenant.id = :tenantId " +
           "AND ca.provider.id = :providerId " +
           "AND ca.isDeleted = false " +
           "ORDER BY ca.createdAt DESC")
    List<CloudAccount> findByTenantIdAndProviderId(@Param("tenantId") Long tenantId, 
                                                    @Param("providerId") Long providerId);

    /**
     * 테넌트 ID와 프로바이더 ID로 기본 계정을 조회합니다.
     * 
     * @param tenantId 테넌트 ID
     * @param providerId 프로바이더 ID
     * @return 기본 계정 (Optional)
     */
    @Query("SELECT ca FROM CloudAccount ca " +
           "WHERE ca.tenant.id = :tenantId " +
           "AND ca.provider.id = :providerId " +
           "AND ca.isDefault = true " +
           "AND ca.isDeleted = false")
    Optional<CloudAccount> findByTenantIdAndProviderIdAndIsDefaultTrue(@Param("tenantId") Long tenantId, 
                                                                        @Param("providerId") Long providerId);

    /**
     * 테넌트 ID, 프로바이더 ID, 계정 ID로 계정이 존재하는지 확인합니다.
     * 중복 계정 검증에 사용됩니다.
     * 
     * @param tenantId 테넌트 ID
     * @param providerId 프로바이더 ID
     * @param accountId 계정 ID
     * @return 존재하면 true
     */
    @Query("SELECT CASE WHEN COUNT(ca) > 0 THEN true ELSE false END " +
           "FROM CloudAccount ca " +
           "WHERE ca.tenant.id = :tenantId " +
           "AND ca.provider.id = :providerId " +
           "AND ca.accountId = :accountId " +
           "AND ca.isDeleted = false")
    boolean existsByTenantIdAndProviderIdAndAccountId(@Param("tenantId") Long tenantId, 
                                                       @Param("providerId") Long providerId, 
                                                       @Param("accountId") String accountId);

    /**
     * 테넌트 ID로 클라우드 계정 개수를 조회합니다.
     * 
     * @param tenantId 테넌트 ID
     * @return 계정 개수
     */
    @Query("SELECT COUNT(ca) FROM CloudAccount ca " +
           "WHERE ca.tenant.id = :tenantId " +
           "AND ca.isDeleted = false")
    long countByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 프로바이더 ID로 클라우드 계정 목록을 조회합니다.
     * 
     * @param providerId 프로바이더 ID
     * @return 클라우드 계정 목록
     */
    @Query("SELECT ca FROM CloudAccount ca " +
           "WHERE ca.provider.id = :providerId " +
           "AND ca.isDeleted = false " +
           "ORDER BY ca.createdAt DESC")
    List<CloudAccount> findByProviderId(@Param("providerId") Long providerId);

    /**
     * 계정 ID로 클라우드 계정을 조회합니다.
     * 
     * @param accountId 계정 ID
     * @return 클라우드 계정 목록
     */
    @Query("SELECT ca FROM CloudAccount ca " +
           "WHERE ca.accountId = :accountId " +
           "AND ca.isDeleted = false")
    List<CloudAccount> findByAccountId(@Param("accountId") String accountId);

    /**
     * 삭제되지 않은 클라우드 계정을 ID로 조회합니다.
     * 
     * @param id 계정 PK
     * @return 클라우드 계정 (Optional)
     */
    @Query("SELECT ca FROM CloudAccount ca " +
           "WHERE ca.id = :id " +
           "AND ca.isDeleted = false")
    Optional<CloudAccount> findByIdAndIsDeletedFalse(@Param("id") Long id);

    /**
     * 테넌트 ID와 계정명으로 클라우드 계정을 조회합니다.
     * 
     * @param tenantId 테넌트 ID
     * @param accountName 계정명
     * @return 클라우드 계정 (Optional)
     */
    @Query("SELECT ca FROM CloudAccount ca " +
           "WHERE ca.tenant.id = :tenantId " +
           "AND ca.accountName = :accountName " +
           "AND ca.isDeleted = false")
    Optional<CloudAccount> findByTenantIdAndAccountName(@Param("tenantId") Long tenantId, 
                                                         @Param("accountName") String accountName);

    /**
     * 테넌트 ID와 상태로 클라우드 계정 목록을 조회합니다.
     * 
     * @param tenantId 테넌트 ID
     * @param status 상태
     * @return 클라우드 계정 목록
     */
    @Query("SELECT ca FROM CloudAccount ca " +
           "WHERE ca.tenant.id = :tenantId " +
           "AND ca.status = :status " +
           "AND ca.isDeleted = false " +
           "ORDER BY ca.createdAt DESC")
    List<CloudAccount> findByTenantIdAndStatus(@Param("tenantId") Long tenantId, 
                                                @Param("status") com.agenticcp.core.common.enums.Status status);

    /**
     * 기본 계정 목록을 조회합니다 (테넌트 ID 기준).
     * 
     * @param tenantId 테넌트 ID
     * @return 기본 계정 목록
     */
    @Query("SELECT ca FROM CloudAccount ca " +
           "WHERE ca.tenant.id = :tenantId " +
           "AND ca.isDefault = true " +
           "AND ca.isDeleted = false " +
           "ORDER BY ca.provider.id")
    List<CloudAccount> findDefaultAccountsByTenantId(@Param("tenantId") Long tenantId);
}

