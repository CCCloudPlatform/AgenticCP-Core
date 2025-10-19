package com.agenticcp.core.domain.ui.repository;

import com.agenticcp.core.domain.ui.entity.MenuPermission;
import com.agenticcp.core.domain.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 메뉴 권한 매핑 데이터 접근 레포지토리
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Repository
public interface MenuPermissionRepository extends JpaRepository<MenuPermission, Long> {

    /**
     * 메뉴 ID로 메뉴 권한 매핑 조회
     * 
     * @param menuId 메뉴 ID
     * @return 메뉴 권한 매핑 목록
     */
    @Query("SELECT mp FROM MenuPermission mp WHERE mp.menu.id = :menuId AND mp.isDeleted = false")
    List<MenuPermission> findByMenuId(@Param("menuId") Long menuId);

    /**
     * 권한 ID로 메뉴 권한 매핑 조회
     * 
     * @param permissionId 권한 ID
     * @return 메뉴 권한 매핑 목록
     */
    @Query("SELECT mp FROM MenuPermission mp WHERE mp.permission.id = :permissionId AND mp.isDeleted = false")
    List<MenuPermission> findByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 메뉴와 권한으로 특정 매핑 조회
     * 
     * @param menuId 메뉴 ID
     * @param permissionId 권한 ID
     * @return 메뉴 권한 매핑
     */
    @Query("SELECT mp FROM MenuPermission mp WHERE mp.menu.id = :menuId AND mp.permission.id = :permissionId AND mp.isDeleted = false")
    Optional<MenuPermission> findByMenuIdAndPermissionId(@Param("menuId") Long menuId, @Param("permissionId") Long permissionId);

    /**
     * 메뉴와 권한 매핑 존재 여부 확인
     * 
     * @param menuId 메뉴 ID
     * @param permissionId 권한 ID
     * @return 매핑 존재 여부
     */
    @Query("SELECT COUNT(mp) > 0 FROM MenuPermission mp WHERE mp.menu.id = :menuId AND mp.permission.id = :permissionId AND mp.isDeleted = false")
    boolean existsByMenuIdAndPermissionId(@Param("menuId") Long menuId, @Param("permissionId") Long permissionId);

    /**
     * 특정 접근 타입의 메뉴 권한 매핑 조회
     * 
     * @param menuId 메뉴 ID
     * @param accessType 접근 타입
     * @return 메뉴 권한 매핑 목록
     */
    @Query("SELECT mp FROM MenuPermission mp WHERE mp.menu.id = :menuId AND mp.accessType = :accessType AND mp.isDeleted = false")
    List<MenuPermission> findByMenuIdAndAccessType(@Param("menuId") Long menuId, @Param("accessType") MenuPermission.AccessType accessType);

    /**
     * 메뉴 ID 목록으로 메뉴 권한 매핑 일괄 조회
     * 
     * @param menuIds 메뉴 ID 목록
     * @return 메뉴 권한 매핑 목록
     */
    @Query("SELECT mp FROM MenuPermission mp WHERE mp.menu.id IN :menuIds AND mp.isDeleted = false")
    List<MenuPermission> findByMenuIdIn(@Param("menuIds") List<Long> menuIds);

    /**
     * 권한 ID 목록으로 메뉴 권한 매핑 일괄 조회
     * 
     * @param permissionIds 권한 ID 목록
     * @return 메뉴 권한 매핑 목록
     */
    @Query("SELECT mp FROM MenuPermission mp WHERE mp.permission.id IN :permissionIds AND mp.isDeleted = false")
    List<MenuPermission> findByPermissionIdIn(@Param("permissionIds") List<Long> permissionIds);

    /**
     * 메뉴의 특정 접근 타입 권한 목록 조회
     * 
     * @param menuId 메뉴 ID
     * @param accessType 접근 타입
     * @return 권한 목록
     */
    @Query("SELECT mp.permission FROM MenuPermission mp WHERE mp.menu.id = :menuId AND mp.accessType = :accessType AND mp.isDeleted = false")
    List<Permission> findPermissionsByMenuIdAndAccessType(@Param("menuId") Long menuId, @Param("accessType") MenuPermission.AccessType accessType);

    /**
     * 메뉴 삭제 시 관련 권한 매핑도 함께 삭제 (논리 삭제)
     * 
     * @param menuId 메뉴 ID
     */
    @Query("UPDATE MenuPermission mp SET mp.isDeleted = true, mp.updatedAt = CURRENT_TIMESTAMP WHERE mp.menu.id = :menuId")
    void deleteByMenuId(@Param("menuId") Long menuId);

    /**
     * 권한 삭제 시 관련 메뉴 권한 매핑도 함께 삭제 (논리 삭제)
     * 
     * @param permissionId 권한 ID
     */
    @Query("UPDATE MenuPermission mp SET mp.isDeleted = true, mp.updatedAt = CURRENT_TIMESTAMP WHERE mp.permission.id = :permissionId")
    void deleteByPermissionId(@Param("permissionId") Long permissionId);
}
