package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 클라우드 계정 Repository
 * 클라우드 계정 데이터의 영속성을 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface CloudAccountRepository extends JpaRepository<CloudAccount, Long> {

    /**
     * 테넌트 ID로 모든 계정을 조회합니다.
     * 
     * @param tenantId 테넌트 ID
     * @return CloudAccount 리스트
     */
    @Query("SELECT ca FROM CloudAccount ca WHERE ca.tenant.id = :tenantId AND ca.isDeleted = false")
    List<CloudAccount> findByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 테넌트 키로 모든 계정을 조회합니다.
     * 
     * @param tenantKey 테넌트 키
     * @return CloudAccount 리스트
     */
    @Query("SELECT ca FROM CloudAccount ca WHERE ca.tenant.tenantKey = :tenantKey AND ca.isDeleted = false")
    List<CloudAccount> findByTenantKey(@Param("tenantKey") String tenantKey);

    /**
     * 테넌트 키와 프로바이더 타입으로 계정을 조회합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @return CloudAccount 리스트
     */
    @Query("SELECT ca FROM CloudAccount ca WHERE ca.tenant.tenantKey = :tenantKey " +
           "AND ca.provider.providerType = :providerType AND ca.isDeleted = false")
    List<CloudAccount> findByTenantKeyAndProviderType(
        @Param("tenantKey") String tenantKey, 
        @Param("providerType") ProviderType providerType
    );

    /**
     * 테넌트 ID와 계정 ID로 계정 존재 여부를 확인합니다.
     * 
     * @param tenantId 테넌트 ID
     * @param accountId 계정 ID (AWS Account ID, Azure Subscription ID 등)
     * @return 존재하면 true
     */
    @Query("SELECT COUNT(ca) > 0 FROM CloudAccount ca WHERE ca.tenant.id = :tenantId " +
           "AND ca.accountId = :accountId AND ca.isDeleted = false")
    boolean existsByTenantIdAndAccountId(
        @Param("tenantId") Long tenantId, 
        @Param("accountId") String accountId
    );

    /**
     * 테넌트 키와 프로바이더 타입으로 기본 계정을 조회합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @return 기본 계정 Optional
     */
    @Query("SELECT ca FROM CloudAccount ca WHERE ca.tenant.tenantKey = :tenantKey " +
           "AND ca.provider.providerType = :providerType AND ca.isDefault = true AND ca.isDeleted = false")
    Optional<CloudAccount> findDefaultByTenantKeyAndProviderType(
        @Param("tenantKey") String tenantKey, 
        @Param("providerType") ProviderType providerType
    );

    /**
     * 프로바이더 타입으로 계정을 조회합니다.
     * 
     * @param providerType 프로바이더 타입
     * @return CloudAccount 리스트
     */
    @Query("SELECT ca FROM CloudAccount ca WHERE ca.provider.providerType = :providerType AND ca.isDeleted = false")
    List<CloudAccount> findByProviderType(@Param("providerType") ProviderType providerType);

    /**
     * 계정 상태로 계정을 조회합니다.
     * 
     * @param status 계정 상태
     * @return CloudAccount 리스트
     */
    @Query("SELECT ca FROM CloudAccount ca WHERE ca.accountStatus = :status AND ca.isDeleted = false")
    List<CloudAccount> findByAccountStatus(@Param("status") AccountStatus status);

    /**
     * 테넌트 ID와 프로바이더 ID로 모든 기본 계정을 조회합니다.
     * (기본 계정 해제 시 사용)
     * 
     * @param tenantId 테넌트 ID
     * @param providerId 프로바이더 ID
     * @return CloudAccount 리스트
     */
    @Query("SELECT ca FROM CloudAccount ca WHERE ca.tenant.id = :tenantId " +
           "AND ca.provider.id = :providerId AND ca.isDefault = true AND ca.isDeleted = false")
    List<CloudAccount> findDefaultAccountsByTenantAndProvider(
        @Param("tenantId") Long tenantId, 
        @Param("providerId") Long providerId
    );

    /**
     * 테넌트 ID와 프로바이더 타입으로 모든 기본 계정을 조회합니다.
     * (기본 계정 해제 시 사용)
     * 
     * @param tenantId 테넌트 ID
     * @param providerType 프로바이더 타입
     * @return CloudAccount 리스트
     */
    @Query("SELECT ca FROM CloudAccount ca WHERE ca.tenant.id = :tenantId " +
           "AND ca.provider.providerType = :providerType AND ca.isDefault = true AND ca.isDeleted = false")
    List<CloudAccount> findDefaultAccountsByTenantAndProviderType(
        @Param("tenantId") Long tenantId, 
        @Param("providerType") ProviderType providerType
    );

    /**
     * 계정 ID와 테넌트 ID로 계정을 조회합니다.
     * 
     * @param id 계정 ID
     * @param tenantId 테넌트 ID
     * @return CloudAccount Optional
     */
    @Query("SELECT ca FROM CloudAccount ca WHERE ca.id = :id AND ca.tenant.id = :tenantId AND ca.isDeleted = false")
    Optional<CloudAccount> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}

