package com.agenticcp.core.domain.user.repository;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 권한 저장소 인터페이스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface PermissionRepository extends TenantAwareRepository<Permission, Long> {
    
    /**
     * 권한 키로 권한 조회
     * 
     * @param permissionKey 권한 키
     * @return 권한 정보
     */
    Optional<Permission> findByPermissionKey(String permissionKey);
    
    /**
     * 테넌트 포함 단건 조회 (LAZY 접근 없이 필터링)
     */
    @Query("SELECT p FROM Permission p WHERE p.permissionKey = :permissionKey AND p.tenant = :tenant AND p.isDeleted = false")
    Optional<Permission> findByPermissionKeyAndTenant(@Param("permissionKey") String permissionKey,
                                                     @Param("tenant") Tenant tenant);
    
    /**
     * 테넌트별 권한 목록 조회
     * 
     * @param tenant 테넌트
     * @return 권한 목록
     */
    List<Permission> findByTenant(Tenant tenant);
    
    /**
     * 테넌트 키로 권한 목록 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 권한 목록
     */
    @Query("SELECT p FROM Permission p WHERE p.tenant.tenantKey = :tenantKey AND p.isDeleted = false")
    List<Permission> findByTenantKey(@Param("tenantKey") String tenantKey);
    
    /**
     * 테넌트별 활성 권한 목록 조회
     * 
     * @param tenant 테넌트
     * @param status 상태
     * @return 활성 권한 목록
     */
    @Query("SELECT p FROM Permission p WHERE p.tenant = :tenant AND p.status = :status AND p.isDeleted = false")
    List<Permission> findActivePermissionsByTenant(@Param("tenant") Tenant tenant, @Param("status") Status status);
    
    /**
     * 테넌트 키로 활성 권한 목록 조회
     * 
     * @param tenantKey 테넌트 키
     * @param status 상태
     * @return 활성 권한 목록
     */
    @Query("SELECT p FROM Permission p WHERE p.tenant.tenantKey = :tenantKey AND p.status = :status AND p.isDeleted = false")
    List<Permission> findActivePermissionsByTenantKey(@Param("tenantKey") String tenantKey, @Param("status") Status status);
    
    /**
     * 시스템 권한 목록 조회
     * 
     * @param isSystem 시스템 권한 여부
     * @return 시스템 권한 목록
     */
    List<Permission> findByIsSystem(Boolean isSystem);
    
    /**
     * 카테고리별 권한 목록 조회
     * 
     * @param category 카테고리
     * @return 권한 목록
     */
    List<Permission> findByCategory(String category);
    
    /**
     * 테넌트별 카테고리별 권한 목록 조회
     * 
     * @param tenant 테넌트
     * @param category 카테고리
     * @return 권한 목록
     */
    @Query("SELECT p FROM Permission p WHERE p.tenant = :tenant AND p.category = :category AND p.isDeleted = false")
    List<Permission> findByTenantAndCategory(@Param("tenant") Tenant tenant, @Param("category") String category);
    
    /**
     * 테넌트 키로 카테고리별 권한 목록 조회
     * 
     * @param tenantKey 테넌트 키
     * @param category 카테고리
     * @return 권한 목록
     */
    @Query("SELECT p FROM Permission p WHERE p.tenant.tenantKey = :tenantKey AND p.category = :category AND p.isDeleted = false")
    List<Permission> findByTenantKeyAndCategory(@Param("tenantKey") String tenantKey, @Param("category") String category);
    
    /**
     * 리소스별 권한 목록 조회
     * 
     * @param resource 리소스
     * @return 권한 목록
     */
    List<Permission> findByResource(String resource);
    
    /**
     * 액션별 권한 목록 조회
     * 
     * @param action 액션
     * @return 권한 목록
     */
    List<Permission> findByAction(String action);
    
    /**
     * 리소스와 액션으로 권한 조회
     * 
     * @param resource 리소스
     * @param action 액션
     * @return 권한 목록
     */
    List<Permission> findByResourceAndAction(String resource, String action);
    
    /**
     * 테넌트별 리소스와 액션으로 권한 조회
     * 
     * @param tenant 테넌트
     * @param resource 리소스
     * @param action 액션
     * @return 권한 목록
     */
    @Query("SELECT p FROM Permission p WHERE p.tenant = :tenant AND p.resource = :resource AND p.action = :action AND p.isDeleted = false")
    List<Permission> findByTenantAndResourceAndAction(@Param("tenant") Tenant tenant, 
                                                     @Param("resource") String resource, 
                                                     @Param("action") String action);
    
    /**
     * 권한 키 목록으로 권한 조회
     * 
     * @param permissionKeys 권한 키 목록
     * @return 권한 목록
     */
    List<Permission> findByPermissionKeyIn(List<String> permissionKeys);
    
    /**
     * 테넌트별 권한 키 목록으로 권한 조회
     * 
     * @param permissionKeys 권한 키 목록
     * @param tenant 테넌트
     * @return 권한 목록
     */
    @Query("SELECT p FROM Permission p WHERE p.permissionKey IN :permissionKeys AND p.tenant = :tenant AND p.isDeleted = false")
    List<Permission> findByPermissionKeyInAndTenant(@Param("permissionKeys") List<String> permissionKeys, 
                                                   @Param("tenant") Tenant tenant);
    
    /**
     * 권한 키 중복 확인 (테넌트별)
     * 
     * @param permissionKey 권한 키
     * @param tenant 테넌트
     * @return 존재 여부
     */
    @Query("SELECT COUNT(p) > 0 FROM Permission p WHERE p.permissionKey = :permissionKey AND p.tenant = :tenant AND p.isDeleted = false")
    boolean existsByPermissionKeyAndTenant(@Param("permissionKey") String permissionKey, @Param("tenant") Tenant tenant);
    
    /**
     * 권한 키 중복 확인 (테넌트 키별)
     * 
     * @param permissionKey 권한 키
     * @param tenantKey 테넌트 키
     * @return 존재 여부
     */
    @Query("SELECT COUNT(p) > 0 FROM Permission p WHERE p.permissionKey = :permissionKey AND p.tenant.tenantKey = :tenantKey AND p.isDeleted = false")
    boolean existsByPermissionKeyAndTenantKey(@Param("permissionKey") String permissionKey, @Param("tenantKey") String tenantKey);
    
    /**
     * 권한을 사용하는 역할 수 조회
     * 
     * @param permission 권한
     * @return 역할 수
     */
    @Query("SELECT COUNT(r) FROM Role r JOIN r.permissions p WHERE p = :permission AND r.isDeleted = false")
    Long countRolesByPermission(@Param("permission") Permission permission);
    
    /**
     * 테넌트별 권한 수 조회
     * 
     * @param tenant 테넌트
     * @return 권한 수
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.tenant = :tenant AND p.isDeleted = false")
    Long countPermissionsByTenant(@Param("tenant") Tenant tenant);
    
    /**
     * 테넌트 키로 권한 수 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 권한 수
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.tenant.tenantKey = :tenantKey AND p.isDeleted = false")
    Long countPermissionsByTenantKey(@Param("tenantKey") String tenantKey);
    
    /**
     * 테넌트와 ID로 권한 조회
     * 
     * @param id 권한 ID
     * @param tenant 테넌트
     * @return 권한
     */
    @Query("SELECT p FROM Permission p WHERE p.id = :id AND p.tenant = :tenant AND p.isDeleted = false")
    Optional<Permission> findByIdAndTenant(@Param("id") Long id, @Param("tenant") Tenant tenant);
    
    /**
     * 테넌트와 ID로 권한 삭제
     * 
     * @param id 권한 ID
     * @param tenant 테넌트
     */
    @Query("UPDATE Permission p SET p.isDeleted = true WHERE p.id = :id AND p.tenant = :tenant")
    void deleteByIdAndTenant(@Param("id") Long id, @Param("tenant") Tenant tenant);
    
    /**
     * 권한 검색 (이름, 설명, 키로 검색)
     * 
     * @param keyword 검색 키워드
     * @param tenant 테넌트
     * @return 검색된 권한 목록
     */
    @Query("SELECT p FROM Permission p WHERE p.tenant = :tenant AND p.isDeleted = false AND " +
           "(p.permissionKey LIKE %:keyword% OR p.permissionName LIKE %:keyword% OR p.description LIKE %:keyword%)")
    List<Permission> searchPermissionsByTenant(@Param("keyword") String keyword, @Param("tenant") Tenant tenant);
    
    /**
     * 권한 검색 (이름, 설명, 키로 검색) - 테넌트 키로
     * 
     * @param keyword 검색 키워드
     * @param tenantKey 테넌트 키
     * @return 검색된 권한 목록
     */
    @Query("SELECT p FROM Permission p WHERE p.tenant.tenantKey = :tenantKey AND p.isDeleted = false AND " +
           "(p.permissionKey LIKE %:keyword% OR p.permissionName LIKE %:keyword% OR p.description LIKE %:keyword%)")
    List<Permission> searchPermissionsByTenantKey(@Param("keyword") String keyword, @Param("tenantKey") String tenantKey);
}
