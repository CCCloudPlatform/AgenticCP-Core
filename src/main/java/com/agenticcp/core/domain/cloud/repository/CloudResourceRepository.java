package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
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
     * 쿠버네티스 스타일: type 필드 (String) 사용
     * 
     * @param resourceType 리소스 타입 (String)
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "WHERE cr.type = :resourceType " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByResourceType(@Param("resourceType") String resourceType);
    
    /**
     * 테넌트 키 + 리소스 타입별 조회
     * 
     * @param tenantKey 테넌트 키
     * @param resourceType 리소스 타입 (String)
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "WHERE cr.tenant.tenantKey = :tenantKey " +
           "AND cr.type = :resourceType " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByTenantKeyAndResourceType(
            @Param("tenantKey") String tenantKey,
            @Param("resourceType") String resourceType);
    
    // ==================== 프로바이더(CSP)별 조회 ====================
    
    /**
     * 프로바이더별 조회 (AWS, Azure, GCP 등)
     * 쿠버네티스 스타일: provider 필드 (String) 사용
     * 
     * @param provider 프로바이더 (String, 예: "AWS", "AZURE", "GCP")
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "WHERE cr.provider = :provider " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByProvider(@Param("provider") String provider);
    
    /**
     * 테넌트 키 + 프로바이더별 조회
     * 
     * @param tenantKey 테넌트 키
     * @param provider 프로바이더 (String)
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "WHERE cr.tenant.tenantKey = :tenantKey " +
           "AND cr.provider = :provider " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByTenantKeyAndProvider(
            @Param("tenantKey") String tenantKey,
            @Param("provider") String provider);
    
    /**
     * 테넌트 키 + 프로바이더 + 리소스 타입별 조회
     * 
     * @param tenantKey 테넌트 키
     * @param provider 프로바이더 (String)
     * @param resourceType 리소스 타입 (String)
     * @return 클라우드 리소스 목록
     */
    @Query("SELECT cr FROM CloudResource cr " +
           "WHERE cr.tenant.tenantKey = :tenantKey " +
           "AND cr.provider = :provider " +
           "AND cr.type = :resourceType " +
           "AND cr.isDeleted = false")
    List<CloudResource> findByTenantKeyAndProviderAndResourceType(
            @Param("tenantKey") String tenantKey,
            @Param("provider") String provider,
            @Param("resourceType") String resourceType);
    
    // ==================== 상태 업데이트 (Modifying) ====================
    
    /**
     * 상태 업데이트 (쿠버네티스 스타일)
     * CSP에서 자원 시작/중지/종료 후 DB 상태 동기화에 사용
     * status 필드 (JSON)에 상태 정보를 저장합니다.
     * 
     * @param resourceId 리소스 ID
     * @param status 상태 정보 (JSON String)
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE CloudResource cr SET " +
           "cr.status = :status, " +
           "cr.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE cr.resourceId = :resourceId " +
           "AND cr.isDeleted = false")
    int updateStatus(
            @Param("resourceId") String resourceId,
            @Param("status") String status);
    
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
     * 프로바이더별 리소스 수 조회
     * 
     * @param provider 프로바이더 (String)
     * @return 리소스 수
     */
    @Query("SELECT COUNT(cr) FROM CloudResource cr " +
           "WHERE cr.provider = :provider " +
           "AND cr.isDeleted = false")
    long countByProvider(@Param("provider") String provider);
}

