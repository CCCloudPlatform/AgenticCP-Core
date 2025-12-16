package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudResource.LifecycleState;
import com.agenticcp.core.domain.cloud.entity.CloudResource.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 클라우드 리소스 Repository
 * <p>
 * 멀티 클라우드(AWS, Azure, GCP 등) 자원의 생명주기 관리를 위한 Repository입니다.
 * CSP 독립적인 자원 조회, 상태 업데이트, 소프트 삭제 기능을 제공합니다.
 * </p>
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 * @since 2025-10-06
 */
@Repository
public interface CloudResourceRepository extends JpaRepository<CloudResource, Long> {
    
    // ==================== 기본 조회 ====================
    
    /**
     * 테넌트 키로 클라우드 리소스 목록 조회
     * 
     * @param tenantKey 테넌트 키 (tenantKey)
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider " +
           "WHERE cr.tenant.tenantKey = :tenantKey " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByTenantKey(@Param("tenantKey") String tenantKey);
    
    /**
     * 리소스 ID로 조회
     * 
     * @param resourceId 리소스 ID (AWS instanceId, Azure vmId 등)
     * @return 클라우드 리소스
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "WHERE cr.resourceId = :resourceId " +
           "AND cr.isDeleted = false")
    CloudResource findByResourceId(@Param("resourceId") String resourceId);
    
    /**
     * 리소스 ID로 조회 (Optional 반환)
     * 
     * @param resourceId 리소스 ID (AWS instanceId, Azure vmId 등)
     * @return 클라우드 리소스 (Optional)
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider " +
           "WHERE cr.resourceId = :resourceId " +
           "AND cr.isDeleted = false")
    Optional<CloudResource> findOptionalByResourceId(@Param("resourceId") String resourceId);
    
    /**
     * 리소스 ID 존재 여부 확인
     * 
     * @param resourceId 리소스 ID
     * @return 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END " +
           "FROM CloudResource cr " +
           "WHERE cr.resourceId = :resourceId " +
           "AND cr.isDeleted = false")
    boolean existsByResourceId(@Param("resourceId") String resourceId);
    
    // ==================== 리소스 타입별 조회 ====================
    
    /**
     * 리소스 타입별 조회 (INSTANCE, BUCKET, NETWORK 등)
     * 
     * @param resourceType 리소스 타입
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider " +
           "WHERE cr.resourceType = :resourceType " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByResourceType(@Param("resourceType") ResourceType resourceType);
    
    /**
     * 테넌트 키 + 리소스 타입별 조회
     * 
     * @param tenantKey 테넌트 키
     * @param resourceType 리소스 타입
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider " +
           "WHERE cr.tenant.tenantKey = :tenantKey " +
           "AND cr.resourceType = :resourceType " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByTenantKeyAndResourceType(
            @Param("tenantKey") String tenantKey,
            @Param("resourceType") ResourceType resourceType);
    
    // ==================== 프로바이더(CSP)별 조회 ====================
    
    /**
     * 프로바이더 타입별 조회 (AWS, Azure, GCP 등)
     * 
     * @param providerType 프로바이더 타입
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider p " +
           "WHERE p.providerType = :providerType " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByProviderType(@Param("providerType") ProviderType providerType);
    
    /**
     * 테넌트 키 + 프로바이더 타입별 조회
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider p " +
           "WHERE cr.tenant.tenantKey = :tenantKey " +
           "AND p.providerType = :providerType " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByTenantKeyAndProviderType(
            @Param("tenantKey") String tenantKey,
            @Param("providerType") ProviderType providerType);
    
    // ==================== 생명주기 상태별 조회 ====================
    
    /**
     * 생명주기 상태별 조회
     * 
     * @param lifecycleState 생명주기 상태 (RUNNING, STOPPED 등)
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider " +
           "WHERE cr.lifecycleState = :lifecycleState " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByLifecycleState(@Param("lifecycleState") LifecycleState lifecycleState);
    
    /**
     * 특정 생명주기 상태를 제외한 조회 (동기화용)
     * TERMINATED 상태를 제외한 활성 리소스 조회 등에 활용
     * 
     * @param excludeStates 제외할 상태 목록
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider " +
           "WHERE cr.lifecycleState NOT IN :excludeStates " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByLifecycleStateNotIn(@Param("excludeStates") List<LifecycleState> excludeStates);
    
    /**
     * 테넌트 키 + 프로바이더 타입 + 리소스 타입별 조회
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @param resourceType 리소스 타입
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "JOIN FETCH cr.provider p " +
           "WHERE cr.tenant.tenantKey = :tenantKey " +
           "AND p.providerType = :providerType " +
           "AND cr.resourceType = :resourceType " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByTenantKeyAndProviderTypeAndResourceType(
            @Param("tenantKey") String tenantKey,
            @Param("providerType") ProviderType providerType,
            @Param("resourceType") ResourceType resourceType);
    
    // ==================== 상태 업데이트 (Modifying) ====================
    
    /**
     * 생명주기 상태 업데이트
     * CSP에서 자원 시작/중지/종료 후 DB 상태 동기화에 사용
     * 
     * @param resourceId 리소스 ID
     * @param lifecycleState 새로운 생명주기 상태
     * @param lastModifiedInCloud 클라우드에서 수정된 시간
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE CloudResource cr SET " +
           "cr.lifecycleState = :lifecycleState, " +
           "cr.lastModifiedInCloud = :lastModifiedInCloud, " +
           "cr.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE cr.resourceId = :resourceId " +
           "AND cr.isDeleted = false")
    int updateLifecycleState(
            @Param("resourceId") String resourceId,
            @Param("lifecycleState") LifecycleState lifecycleState,
            @Param("lastModifiedInCloud") LocalDateTime lastModifiedInCloud);
    
    /**
     * 소프트 삭제 처리
     * CSP에서 자원 삭제 후 DB에서 논리적 삭제 처리에 사용
     * 
     * @param resourceId 리소스 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE CloudResource cr SET " +
           "cr.isDeleted = true, " +
           "cr.lifecycleState = 'TERMINATED', " +
           "cr.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE cr.resourceId = :resourceId")
    int softDeleteByResourceId(@Param("resourceId") String resourceId);
    
    /**
     * 마지막 동기화 시간 업데이트
     * 
     * @param resourceId 리소스 ID
     * @param lastSync 마지막 동기화 시간
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE CloudResource cr SET " +
           "cr.lastSync = :lastSync, " +
           "cr.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE cr.resourceId = :resourceId " +
           "AND cr.isDeleted = false")
    int updateLastSync(
            @Param("resourceId") String resourceId,
            @Param("lastSync") LocalDateTime lastSync);
    
    // ==================== 통계 조회 ====================
    
    /**
     * 테넌트별 리소스 수 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 리소스 수
     */
    @Query("SELECT COUNT(cr) FROM CloudResource cr " +
           "WHERE cr.tenant.tenantKey = :tenantKey " +
           "AND cr.isDeleted = false")
    long countByTenantKey(@Param("tenantKey") String tenantKey);
    
    /**
     * 프로바이더 타입별 리소스 수 조회
     * 
     * @param providerType 프로바이더 타입
     * @return 리소스 수
     */
    @Query("SELECT COUNT(cr) FROM CloudResource cr " +
           "JOIN cr.provider p " +
           "WHERE p.providerType = :providerType " +
           "AND cr.isDeleted = false")
    long countByProviderType(@Param("providerType") ProviderType providerType);
}

