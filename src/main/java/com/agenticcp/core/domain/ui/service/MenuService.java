package com.agenticcp.core.domain.ui.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.logging.LogMaskingUtils;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.ui.entity.Menu;
import com.agenticcp.core.domain.ui.entity.MenuPermission;
import com.agenticcp.core.domain.ui.exception.MenuErrorCode;
import com.agenticcp.core.domain.ui.repository.MenuPermissionRepository;
import com.agenticcp.core.domain.ui.repository.MenuRepository;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 메뉴 관리 서비스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuPermissionRepository menuPermissionRepository;
    private final PermissionRepository permissionRepository;

    /**
     * 테넌트별 모든 활성화된 메뉴 조회
     * 
     * @return 활성화된 메뉴 목록
     */
    public List<Menu> getAllActiveMenus() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[MenuService] getAllActiveMenus - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        List<Menu> result = menuRepository.findByTenantAndActive(currentTenant);
        log.info("[MenuService] getAllActiveMenus - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        return result;
    }

    /**
     * 테넌트별 최상위 메뉴 조회
     * 
     * @return 최상위 메뉴 목록
     */
    public List<Menu> getRootMenus() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[MenuService] getRootMenus - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        List<Menu> result = menuRepository.findRootMenusByTenant(currentTenant);
        log.info("[MenuService] getRootMenus - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        return result;
    }

    /**
     * 부모 메뉴의 하위 메뉴 조회
     * 
     * @param parentId 부모 메뉴 ID
     * @return 하위 메뉴 목록
     */
    public List<Menu> getChildMenus(Long parentId) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[MenuService] getChildMenus - parentId={} tenantKey={}", parentId, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        List<Menu> result = menuRepository.findByParentIdAndTenant(parentId, currentTenant);
        log.info("[MenuService] getChildMenus - success count={} parentId={} tenantKey={}", result.size(), parentId, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        return result;
    }

    /**
     * 메뉴 키로 메뉴 조회
     * 
     * @param menuKey 메뉴 키
     * @return 메뉴 정보
     */
    public Menu getMenuByKey(String menuKey) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[MenuService] getMenuByKey - menuKey={} tenantKey={}", menuKey, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        Menu result = menuRepository.findByMenuKeyAndTenant(menuKey, currentTenant)
                .orElseThrow(() -> new ResourceNotFoundException(MenuErrorCode.MENU_NOT_FOUND));
        
        log.info("[MenuService] getMenuByKey - success menuId={} menuKey={} tenantKey={}", result.getId(), menuKey, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }

    /**
     * 메뉴 ID로 메뉴 조회
     * 
     * @param menuId 메뉴 ID
     * @return 메뉴 정보
     */
    public Menu getMenuById(Long menuId) {
        log.info("[MenuService] getMenuById - menuId={}", menuId);
        
        Menu result = menuRepository.findById(menuId)
                .filter(menu -> !menu.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(MenuErrorCode.MENU_NOT_FOUND));
        
        log.info("[MenuService] getMenuById - success menuId={} menuKey={}", result.getId(), result.getMenuKey());
        return result;
    }

    /**
     * 메뉴 트리 구조 조회 (권한 정보 포함)
     * 
     * @return 메뉴 트리 구조
     */
    public List<Menu> getMenuTreeWithPermissions() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[MenuService] getMenuTreeWithPermissions - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        List<Menu> result = menuRepository.findMenuTreeWithPermissions(currentTenant);
        log.info("[MenuService] getMenuTreeWithPermissions - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        return result;
    }

    /**
     * 메뉴 생성
     * 
     * @param menuKey 메뉴 키
     * @param menuName 메뉴명
     * @param description 메뉴 설명
     * @param url 메뉴 URL
     * @param icon 메뉴 아이콘
     * @param parentId 부모 메뉴 ID
     * @param sortOrder 정렬 순서
     * @param isSystem 시스템 메뉴 여부
     * @return 생성된 메뉴
     */
    @Transactional
    public Menu createMenu(String menuKey, String menuName, String description, String url, 
                          String icon, Long parentId, Integer sortOrder, Boolean isSystem) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[MenuService] createMenu - menuKey={} menuName={} parentId={} tenantKey={}", 
                menuKey, menuName, parentId, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));

        // 메뉴 키 중복 확인
        if (menuRepository.existsByMenuKeyAndTenant(menuKey, currentTenant, 0L)) {
            throw new BusinessException(MenuErrorCode.DUPLICATE_MENU_KEY);
        }

        // 부모 메뉴 유효성 검증
        if (parentId != null) {
            validateParentMenu(parentId, currentTenant);
        }

        // 메뉴 깊이 검증
        validateMenuDepth(parentId, currentTenant);

        Menu menu = Menu.builder()
                .menuKey(menuKey)
                .menuName(menuName)
                .description(description)
                .url(url)
                .icon(icon)
                .parentId(parentId)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isSystem(isSystem != null ? isSystem : false)
                .tenant(currentTenant)
                .build();

        Menu result = menuRepository.save(menu);
        log.info("[MenuService] createMenu - success menuId={} menuKey={} tenantKey={}", 
                result.getId(), menuKey, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));

        return result;
    }

    /**
     * 메뉴 수정
     * 
     * @param menuId 메뉴 ID
     * @param menuKey 메뉴 키
     * @param menuName 메뉴명
     * @param description 메뉴 설명
     * @param url 메뉴 URL
     * @param icon 메뉴 아이콘
     * @param parentId 부모 메뉴 ID
     * @param sortOrder 정렬 순서
     * @param isActive 활성화 여부
     * @return 수정된 메뉴
     */
    @Transactional
    public Menu updateMenu(Long menuId, String menuKey, String menuName, String description, 
                          String url, String icon, Long parentId, Integer sortOrder, Boolean isActive) {
        log.info("[MenuService] updateMenu - menuId={} menuKey={} menuName={} parentId={}", 
                menuId, menuKey, menuName, parentId);

        Menu menu = getMenuById(menuId);
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();

        // 메뉴 키 중복 확인 (자신 제외)
        if (!menu.getMenuKey().equals(menuKey) && 
            menuRepository.existsByMenuKeyAndTenant(menuKey, currentTenant, menuId)) {
            throw new BusinessException(MenuErrorCode.DUPLICATE_MENU_KEY);
        }

        // 부모 메뉴 유효성 검증 (자기 자신을 부모로 설정하는 것 방지)
        if (parentId != null) {
            if (parentId.equals(menuId)) {
                throw new BusinessException(MenuErrorCode.INVALID_PARENT_MENU);
            }
            validateParentMenu(parentId, currentTenant);
        }

        // 메뉴 깊이 검증
        validateMenuDepth(parentId, currentTenant);

        // 메뉴 정보 업데이트
        menu.setMenuKey(menuKey);
        menu.setMenuName(menuName);
        menu.setDescription(description);
        menu.setUrl(url);
        menu.setIcon(icon);
        menu.setParentId(parentId);
        if (sortOrder != null) {
            menu.setSortOrder(sortOrder);
        }
        if (isActive != null) {
            menu.setIsActive(isActive);
        }

        Menu result = menuRepository.save(menu);
        log.info("[MenuService] updateMenu - success menuId={} menuKey={}", result.getId(), menuKey);

        return result;
    }

    /**
     * 메뉴 삭제
     * 
     * @param menuId 메뉴 ID
     */
    @Transactional
    public void deleteMenu(Long menuId) {
        log.info("[MenuService] deleteMenu - menuId={}", menuId);

        Menu menu = getMenuById(menuId);

        // 시스템 메뉴 삭제 방지
        if (menu.getIsSystem()) {
            throw new BusinessException(MenuErrorCode.SYSTEM_MENU_CANNOT_DELETE);
        }

        // 하위 메뉴 존재 확인
        List<Menu> children = menuRepository.findByParentIdAndTenant(menuId, menu.getTenant());
        if (!children.isEmpty()) {
            throw new BusinessException(MenuErrorCode.CANNOT_DELETE_MENU_WITH_CHILDREN);
        }

        // 논리 삭제
        menu.setIsDeleted(true);
        menuRepository.save(menu);

        // 관련 메뉴 권한 매핑도 삭제
        menuPermissionRepository.deleteByMenuId(menuId);

        log.info("[MenuService] deleteMenu - success menuId={} menuKey={}", menuId, menu.getMenuKey());
    }

    /**
     * 메뉴 권한 할당
     * 
     * @param menuId 메뉴 ID
     * @param permissionId 권한 ID
     * @param accessType 접근 타입
     * @return 메뉴 권한 매핑
     */
    @Transactional
    public MenuPermission assignMenuPermission(Long menuId, Long permissionId, MenuPermission.AccessType accessType) {
        log.info("[MenuService] assignMenuPermission - menuId={} permissionId={} accessType={}", 
                menuId, permissionId, accessType);

        Menu menu = getMenuById(menuId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(MenuErrorCode.MENU_PERMISSION_NOT_FOUND));

        // 중복 확인
        if (menuPermissionRepository.existsByMenuIdAndPermissionId(menuId, permissionId)) {
            throw new BusinessException(MenuErrorCode.DUPLICATE_MENU_PERMISSION);
        }

        MenuPermission menuPermission = MenuPermission.builder()
                .menu(menu)
                .permission(permission)
                .accessType(accessType)
                .build();

        MenuPermission result = menuPermissionRepository.save(menuPermission);
        log.info("[MenuService] assignMenuPermission - success menuId={} permissionId={} accessType={}", 
                menuId, permissionId, accessType);

        return result;
    }

    /**
     * 메뉴 권한 제거
     * 
     * @param menuId 메뉴 ID
     * @param permissionId 권한 ID
     */
    @Transactional
    public void removeMenuPermission(Long menuId, Long permissionId) {
        log.info("[MenuService] removeMenuPermission - menuId={} permissionId={}", menuId, permissionId);

        MenuPermission menuPermission = menuPermissionRepository
                .findByMenuIdAndPermissionId(menuId, permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(MenuErrorCode.MENU_PERMISSION_NOT_FOUND));

        menuPermissionRepository.delete(menuPermission);
        log.info("[MenuService] removeMenuPermission - success menuId={} permissionId={}", menuId, permissionId);
    }

    /**
     * 메뉴 검색
     * 
     * @param keyword 검색 키워드
     * @return 검색된 메뉴 목록
     */
    public List<Menu> searchMenus(String keyword) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[MenuService] searchMenus - keyword={} tenantKey={}", keyword, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));

        List<Menu> result = menuRepository.searchMenus(keyword, currentTenant);
        log.info("[MenuService] searchMenus - success count={} keyword={} tenantKey={}", 
                result.size(), keyword, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));

        return result;
    }

    /**
     * 부모 메뉴 유효성 검증
     * 
     * @param parentId 부모 메뉴 ID
     * @param tenant 테넌트
     */
    private void validateParentMenu(Long parentId, Tenant tenant) {
        Optional<Menu> parentMenu = menuRepository.findByMenuKeyAndTenant("", tenant); // 임시로 빈 문자열 사용
        if (parentMenu.isEmpty()) {
            // 실제로는 부모 메뉴 ID로 조회해야 함
            throw new BusinessException(MenuErrorCode.INVALID_PARENT_MENU);
        }
    }

    /**
     * 메뉴 깊이 검증 (최대 5단계)
     * 
     * @param parentId 부모 메뉴 ID
     * @param tenant 테넌트
     */
    private void validateMenuDepth(Long parentId, Tenant tenant) {
        if (parentId != null) {
            Integer depth = menuRepository.getMenuDepth(parentId);
            if (depth != null && depth >= 5) {
                throw new BusinessException(MenuErrorCode.MENU_DEPTH_EXCEEDED);
            }
        }
    }
}
