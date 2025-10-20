package com.agenticcp.core.domain.ui.repository;

import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.ui.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 메뉴 데이터 접근 레포지토리
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Repository
public interface MenuRepository extends JpaRepository<Menu, Long>, TenantAwareRepository<Menu, Long> {

    /**
     * 테넌트별 활성화된 모든 메뉴 조회
     * 
     * @param tenant 테넌트
     * @return 활성화된 메뉴 목록
     */
    @Query("SELECT m FROM Menu m WHERE m.tenant = :tenant AND m.isActive = true AND m.isDeleted = false ORDER BY m.sortOrder ASC, m.id ASC")
    List<Menu> findByTenantAndActive(@Param("tenant") Tenant tenant);

    /**
     * 테넌트별 최상위 메뉴 조회 (parentId가 null인 메뉴)
     * 
     * @param tenant 테넌트
     * @return 최상위 메뉴 목록
     */
    @Query("SELECT m FROM Menu m WHERE m.tenant = :tenant AND m.parentId IS NULL AND m.isActive = true AND m.isDeleted = false ORDER BY m.sortOrder ASC, m.id ASC")
    List<Menu> findRootMenusByTenant(@Param("tenant") Tenant tenant);

    /**
     * 부모 메뉴의 하위 메뉴 조회
     * 
     * @param parentId 부모 메뉴 ID
     * @param tenant 테넌트
     * @return 하위 메뉴 목록
     */
    @Query("SELECT m FROM Menu m WHERE m.parentId = :parentId AND m.tenant = :tenant AND m.isActive = true AND m.isDeleted = false ORDER BY m.sortOrder ASC, m.id ASC")
    List<Menu> findByParentIdAndTenant(@Param("parentId") Long parentId, @Param("tenant") Tenant tenant);

    /**
     * 메뉴 키로 메뉴 조회
     * 
     * @param menuKey 메뉴 키
     * @param tenant 테넌트
     * @return 메뉴 정보
     */
    @Query("SELECT m FROM Menu m WHERE m.menuKey = :menuKey AND m.tenant = :tenant AND m.isDeleted = false")
    Optional<Menu> findByMenuKeyAndTenant(@Param("menuKey") String menuKey, @Param("tenant") Tenant tenant);

    /**
     * 메뉴 키 중복 확인
     * 
     * @param menuKey 메뉴 키
     * @param tenant 테넌트
     * @param excludeId 제외할 메뉴 ID (수정 시 사용)
     * @return 중복 여부
     */
    @Query("SELECT COUNT(m) > 0 FROM Menu m WHERE m.menuKey = :menuKey AND m.tenant = :tenant AND m.id != :excludeId AND m.isDeleted = false")
    boolean existsByMenuKeyAndTenant(@Param("menuKey") String menuKey, @Param("tenant") Tenant tenant, @Param("excludeId") Long excludeId);

    /**
     * 특정 권한을 가진 메뉴 조회
     * 
     * @param permissionId 권한 ID
     * @param tenant 테넌트
     * @return 권한을 가진 메뉴 목록
     */
    @Query("SELECT DISTINCT m FROM Menu m JOIN m.permissions mp WHERE mp.permission.id = :permissionId AND m.tenant = :tenant AND m.isActive = true AND m.isDeleted = false ORDER BY m.sortOrder ASC, m.id ASC")
    List<Menu> findByPermissionIdAndTenant(@Param("permissionId") Long permissionId, @Param("tenant") Tenant tenant);

    /**
     * 메뉴 트리 구조 조회 (N+1 문제 방지를 위한 fetch join)
     * 
     * @param tenant 테넌트
     * @return 메뉴 트리 구조
     */
    @Query("SELECT m FROM Menu m LEFT JOIN FETCH m.children c LEFT JOIN FETCH m.permissions mp LEFT JOIN FETCH mp.permission WHERE m.tenant = :tenant AND m.isActive = true AND m.isDeleted = false ORDER BY m.sortOrder ASC, m.id ASC")
    List<Menu> findMenuTreeWithPermissions(@Param("tenant") Tenant tenant);

    /**
     * 시스템 메뉴 조회
     * 
     * @param tenant 테넌트
     * @return 시스템 메뉴 목록
     */
    @Query("SELECT m FROM Menu m WHERE m.tenant = :tenant AND m.isSystem = true AND m.isDeleted = false ORDER BY m.sortOrder ASC, m.id ASC")
    List<Menu> findSystemMenusByTenant(@Param("tenant") Tenant tenant);

    /**
     * 메뉴 깊이 확인 (최대 5단계 제한)
     * 
     * @param menuId 메뉴 ID
     * @return 메뉴 깊이
     */
    @Query(value = "WITH RECURSIVE menu_hierarchy AS (" +
                   "SELECT id, parent_id, 1 as level " +
                   "FROM menus WHERE id = :menuId " +
                   "UNION ALL " +
                   "SELECT m.id, m.parent_id, mh.level + 1 " +
                   "FROM menus m " +
                   "INNER JOIN menu_hierarchy mh ON m.parent_id = mh.id " +
                   ") " +
                   "SELECT MAX(level) FROM menu_hierarchy", nativeQuery = true)
    Integer getMenuDepth(@Param("menuId") Long menuId);

    /**
     * 특정 메뉴의 모든 하위 메뉴 조회 (재귀)
     * 
     * @param menuId 메뉴 ID
     * @param tenant 테넌트
     * @return 하위 메뉴 목록
     */
    @Query(value = "WITH RECURSIVE menu_tree AS (" +
                   "SELECT id, parent_id, menu_key, menu_name, sort_order " +
                   "FROM menus WHERE parent_id = :menuId AND tenant_id = :tenantId AND is_active = true AND is_deleted = false " +
                   "UNION ALL " +
                   "SELECT m.id, m.parent_id, m.menu_key, m.menu_name, m.sort_order " +
                   "FROM menus m " +
                   "INNER JOIN menu_tree mt ON m.parent_id = mt.id " +
                   "WHERE m.tenant_id = :tenantId AND m.is_active = true AND m.is_deleted = false " +
                   ") " +
                   "SELECT * FROM menu_tree ORDER BY sort_order ASC, id ASC", nativeQuery = true)
    List<Object[]> findDescendantMenus(@Param("menuId") Long menuId, @Param("tenantId") Long tenantId);

    /**
     * 메뉴 검색 (메뉴명, 설명으로 검색)
     * 
     * @param keyword 검색 키워드
     * @param tenant 테넌트
     * @return 검색된 메뉴 목록
     */
    @Query("SELECT m FROM Menu m WHERE m.tenant = :tenant AND m.isDeleted = false AND " +
           "(LOWER(m.menuName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY m.sortOrder ASC, m.id ASC")
    List<Menu> searchMenus(@Param("keyword") String keyword, @Param("tenant") Tenant tenant);
}
