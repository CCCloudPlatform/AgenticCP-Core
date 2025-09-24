package com.agenticcp.core.domain.user.repository;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 역할 저장소 인터페이스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface RoleRepository extends TenantAwareRepository<Role, Long> {
    
    /**
     * 역할 키로 역할 조회
     * 
     * @param roleKey 역할 키
     * @return 역할 정보
     */
    Optional<Role> findByRoleKey(String roleKey);
    
    /**
     * 테넌트별 역할 목록 조회
     * 
     * @param tenant 테넌트
     * @return 역할 목록
     */
    List<Role> findByTenant(Tenant tenant);
    
    /**
     * 현재 테넌트의 모든 역할을 권한까지 함께 로드
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN FETCH r.tenant LEFT JOIN FETCH r.permissions WHERE r.tenant = :tenant AND r.isDeleted = false")
    List<Role> findByTenantWithPermissions(@Param("tenant") Tenant tenant);
    
    /**
     * 테넌트 키로 역할 목록 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.tenant.tenantKey = :tenantKey AND r.isDeleted = false")
    List<Role> findByTenantKey(@Param("tenantKey") String tenantKey);
    
    /**
     * 테넌트별 활성 역할 목록 조회
     * 
     * @param tenant 테넌트
     * @param status 상태
     * @return 활성 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.tenant = :tenant AND r.status = :status AND r.isDeleted = false")
    List<Role> findActiveRolesByTenant(@Param("tenant") Tenant tenant, @Param("status") Status status);
    
    /**
     * 테넌트 키로 활성 역할 목록 조회
     * 
     * @param tenantKey 테넌트 키
     * @param status 상태
     * @return 활성 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.tenant.tenantKey = :tenantKey AND r.status = :status AND r.isDeleted = false")
    List<Role> findActiveRolesByTenantKey(@Param("tenantKey") String tenantKey, @Param("status") Status status);
    
    /**
     * 시스템 역할 목록 조회
     * 
     * @param isSystem 시스템 역할 여부
     * @return 시스템 역할 목록
     */
    List<Role> findByIsSystem(Boolean isSystem);
    
    /**
     * 기본 역할 목록 조회
     * 
     * @param isDefault 기본 역할 여부
     * @return 기본 역할 목록
     */
    List<Role> findByIsDefault(Boolean isDefault);
    
    /**
     * 테넌트별 시스템 역할 조회
     * 
     * @param tenant 테넌트
     * @param isSystem 시스템 역할 여부
     * @return 시스템 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.tenant = :tenant AND r.isSystem = :isSystem AND r.isDeleted = false")
    List<Role> findSystemRolesByTenant(@Param("tenant") Tenant tenant, @Param("isSystem") Boolean isSystem);
    
    /**
     * 테넌트별 기본 역할 조회
     * 
     * @param tenant 테넌트
     * @param isDefault 기본 역할 여부
     * @return 기본 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.tenant = :tenant AND r.isDefault = :isDefault AND r.isDeleted = false")
    List<Role> findDefaultRolesByTenant(@Param("tenant") Tenant tenant, @Param("isDefault") Boolean isDefault);
    
    /**
     * 역할 키 중복 확인 (테넌트별)
     * 
     * @param roleKey 역할 키
     * @param tenant 테넌트
     * @return 존재 여부
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.roleKey = :roleKey AND r.tenant = :tenant AND r.isDeleted = false")
    boolean existsByRoleKeyAndTenant(@Param("roleKey") String roleKey, @Param("tenant") Tenant tenant);
    
    /**
     * 역할 키 중복 확인 (테넌트 키별)
     * 
     * @param roleKey 역할 키
     * @param tenantKey 테넌트 키
     * @return 존재 여부
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.roleKey = :roleKey AND r.tenant.tenantKey = :tenantKey AND r.isDeleted = false")
    boolean existsByRoleKeyAndTenantKey(@Param("roleKey") String roleKey, @Param("tenantKey") String tenantKey);
    
    /**
     * 역할을 사용하는 사용자 수 조회
     * 
     * @param role 역할
     * @return 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role AND u.isDeleted = false")
    Long countUsersByRole(@Param("role") Role role);
    
    /**
     * 테넌트별 역할 수 조회
     * 
     * @param tenant 테넌트
     * @return 역할 수
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.tenant = :tenant AND r.isDeleted = false")
    Long countRolesByTenant(@Param("tenant") Tenant tenant);
    
    /**
     * 테넌트와 ID로 역할 조회
     * 
     * @param id 역할 ID
     * @param tenant 테넌트
     * @return 역할
     */
    @Query("SELECT r FROM Role r WHERE r.id = :id AND r.tenant = :tenant AND r.isDeleted = false")
    Optional<Role> findByIdAndTenant(@Param("id") Long id, @Param("tenant") Tenant tenant);

    /**
     * 역할 키와 테넌트로 권한까지 함께 로드하여 단건 조회
     *
     * @param roleKey 역할 키
     * @param tenant 테넌트
     * @return 역할 (permissions fetch join)
     */
    @Query("SELECT r FROM Role r JOIN FETCH r.tenant LEFT JOIN FETCH r.permissions WHERE r.roleKey = :roleKey AND r.tenant = :tenant AND r.isDeleted = false")
    Optional<Role> findByRoleKeyAndTenantWithPermissions(@Param("roleKey") String roleKey, @Param("tenant") Tenant tenant);

    /**
     * ID와 테넌트로 권한까지 함께 로드하여 단건 조회
     *
     * @param id 역할 ID
     * @param tenant 테넌트
     * @return 역할 (permissions fetch join)
     */
    @Query("SELECT r FROM Role r JOIN FETCH r.tenant LEFT JOIN FETCH r.permissions WHERE r.id = :id AND r.tenant = :tenant AND r.isDeleted = false")
    Optional<Role> findByIdAndTenantWithPermissions(@Param("id") Long id, @Param("tenant") Tenant tenant);
    
    /**
     * 테넌트와 ID로 역할 삭제
     * 
     * @param id 역할 ID
     * @param tenant 테넌트
     */
    @Query("UPDATE Role r SET r.isDeleted = true WHERE r.id = :id AND r.tenant = :tenant")
    void deleteByIdAndTenant(@Param("id") Long id, @Param("tenant") Tenant tenant);
    
    /**
     * 테넌트 키로 역할 수 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 역할 수
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.tenant.tenantKey = :tenantKey AND r.isDeleted = false")
    Long countRolesByTenantKey(@Param("tenantKey") String tenantKey);
    
    /**
     * 역할 검색 (이름, 설명, 키로 검색)
     * 
     * @param keyword 검색 키워드
     * @param tenant 테넌트
     * @return 검색된 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.tenant = :tenant AND r.isDeleted = false AND " +
           "(r.roleKey LIKE %:keyword% OR r.roleName LIKE %:keyword% OR r.description LIKE %:keyword%)")
    List<Role> searchRolesByTenant(@Param("keyword") String keyword, @Param("tenant") Tenant tenant);
    
    /**
     * 역할 검색 (이름, 설명, 키로 검색) - 테넌트 키로
     * 
     * @param keyword 검색 키워드
     * @param tenantKey 테넌트 키
     * @return 검색된 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.tenant.tenantKey = :tenantKey AND r.isDeleted = false AND " +
           "(r.roleKey LIKE %:keyword% OR r.roleName LIKE %:keyword% OR r.description LIKE %:keyword%)")
    List<Role> searchRolesByTenantKey(@Param("keyword") String keyword, @Param("tenantKey") String tenantKey);
}
