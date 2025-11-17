package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.TenantCloudCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 테넌트별 클라우드 자격증명 리포지토리
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface TenantCloudCredentialsRepository extends JpaRepository<TenantCloudCredentials, Long> {
    
    /**
     * 테넌트 키, 프로바이더 타입, 계정 스코프로 자격증명 조회
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 스코프
     * @return 자격증명 정보
     */
    Optional<TenantCloudCredentials> findByTenantKeyAndProviderTypeAndAccountScopeAndIsActiveTrue(
            String tenantKey, CloudProvider.ProviderType providerType, String accountScope);
    
    /**
     * 테넌트별 활성화된 자격증명 목록 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 자격증명 목록
     */
    List<TenantCloudCredentials> findByTenantKeyAndIsActiveTrue(String tenantKey);
    
    /**
     * 프로바이더별 활성화된 자격증명 목록 조회
     * 
     * @param providerType 프로바이더 타입
     * @return 자격증명 목록
     */
    List<TenantCloudCredentials> findByProviderTypeAndIsActiveTrue(CloudProvider.ProviderType providerType);
    
    /**
     * 특정 계정 스코프의 자격증명 존재 여부 확인
     * 
     * @param accountScope 계정 스코프
     * @param providerType 프로바이더 타입
     * @return 존재 여부
     */
    boolean existsByAccountScopeAndProviderTypeAndIsActiveTrue(String accountScope, CloudProvider.ProviderType providerType);
    
    /**
     * 오류가 발생한 자격증명 목록 조회 (모니터링용)
     * 
     * @param hours 지난 N시간 이내
     * @return 오류 자격증명 목록
     */
    @Query("SELECT tcc FROM TenantCloudCredentials tcc " +
           "WHERE tcc.lastErrorMessage IS NOT NULL " +
           "AND tcc.lastUsedAt >= :since " +
           "AND tcc.isActive = true")
    List<TenantCloudCredentials> findCredentialsWithRecentErrors(@Param("since") java.time.LocalDateTime since);
    
    /**
     * 오래 사용되지 않은 자격증명 목록 조회 (정리용)
     * 
     * @param days 지난 N일 이내
     * @return 미사용 자격증명 목록
     */
    @Query("SELECT tcc FROM TenantCloudCredentials tcc " +
           "WHERE (tcc.lastUsedAt IS NULL OR tcc.lastUsedAt < :since) " +
           "AND tcc.isActive = true")
    List<TenantCloudCredentials> findUnusedCredentials(@Param("since") java.time.LocalDateTime since);
}
