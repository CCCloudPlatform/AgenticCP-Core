package com.agenticcp.core.domain.ui.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.logging.LogMaskingUtils;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.ui.entity.Menu;
import com.agenticcp.core.domain.ui.entity.MenuPermission;
import com.agenticcp.core.domain.ui.repository.MenuRepository;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 메뉴 권한 기반 조회 서비스
 * 사용자의 권한에 따라 접근 가능한 메뉴를 필터링하여 반환
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuAuthorizationService {

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String MENU_CACHE_KEY_PREFIX = "user_menus:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * 사용자별 접근 가능한 메뉴 조회 (캐시 적용)
     * 
     * @param username 사용자명
     * @return 접근 가능한 메뉴 목록
     */
    public List<Menu> getAuthorizedMenus(String username) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = MENU_CACHE_KEY_PREFIX + currentTenant.getTenantKey() + ":" + username;
        
        log.info("[MenuAuthorizationService] getAuthorizedMenus - username={} tenantKey={}", 
                username, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));

        // 캐시에서 조회
        @SuppressWarnings("unchecked")
        List<Menu> cachedMenus = (List<Menu>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedMenus != null) {
            log.info("[MenuAuthorizationService] getAuthorizedMenus - cache hit username={} count={}", username, cachedMenus.size());
            return cachedMenus;
        }

        // 데이터베이스에서 조회
        List<Menu> authorizedMenus = getAuthorizedMenusFromDatabase(username, currentTenant);
        
        // 캐시에 저장
        redisTemplate.opsForValue().set(cacheKey, authorizedMenus, CACHE_TTL);
        
        log.info("[MenuAuthorizationService] getAuthorizedMenus - success username={} count={} tenantKey={}", 
                username, authorizedMenus.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        return authorizedMenus;
    }

    /**
     * 사용자별 접근 가능한 메뉴 트리 구조 조회
     * 
     * @param username 사용자명
     * @return 접근 가능한 메뉴 트리 구조
     */
    public List<Menu> getAuthorizedMenuTree(String username) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[MenuAuthorizationService] getAuthorizedMenuTree - username={} tenantKey={}", 
                username, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));

        List<Menu> allMenus = menuRepository.findMenuTreeWithPermissions(currentTenant);
        List<Menu> authorizedMenus = filterMenusByUserPermissions(allMenus, username);
        
        log.info("[MenuAuthorizationService] getAuthorizedMenuTree - success username={} count={} tenantKey={}", 
                username, authorizedMenus.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
        return authorizedMenus;
    }

    /**
     * 특정 메뉴에 대한 사용자 접근 권한 확인
     * 
     * @param username 사용자명
     * @param menuId 메뉴 ID
     * @param accessType 접근 타입
     * @return 접근 권한 여부
     */
    public boolean hasMenuAccess(String username, Long menuId, MenuPermission.AccessType accessType) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[MenuAuthorizationService] hasMenuAccess - username={} menuId={} accessType={} tenantKey={}", 
                username, menuId, accessType, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));

        User user = getUserByUsername(username);
        Set<String> userPermissions = getUserPermissions(user);

        // 메뉴의 권한 요구사항 조회
        List<MenuPermission> menuPermissions = menuRepository.findById(menuId)
                .map(Menu::getPermissions)
                .orElse(new ArrayList<>())
                .stream()
                .filter(mp -> mp.getAccessType() == accessType && !mp.getIsDeleted())
                .collect(Collectors.toList());

        // 권한이 설정되지 않은 메뉴는 접근 허용
        if (menuPermissions.isEmpty()) {
            log.info("[MenuAuthorizationService] hasMenuAccess - no permissions required menuId={} accessType={}", menuId, accessType);
            return true;
        }

        // 사용자가 필요한 권한 중 하나라도 가지고 있는지 확인
        boolean hasAccess = menuPermissions.stream()
                .anyMatch(mp -> userPermissions.contains(mp.getPermission().getPermissionKey()));

        log.info("[MenuAuthorizationService] hasMenuAccess - result={} username={} menuId={} accessType={}", 
                hasAccess, username, menuId, accessType);
        
        return hasAccess;
    }

    /**
     * 사용자별 메뉴 캐시 무효화
     * 
     * @param username 사용자명
     */
    public void evictUserMenuCache(String username) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = MENU_CACHE_KEY_PREFIX + currentTenant.getTenantKey() + ":" + username;
        
        redisTemplate.delete(cacheKey);
        log.info("[MenuAuthorizationService] evictUserMenuCache - username={} tenantKey={}", 
                username, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
    }

    /**
     * 테넌트별 모든 메뉴 캐시 무효화
     */
    public void evictAllMenuCache() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String pattern = MENU_CACHE_KEY_PREFIX + currentTenant.getTenantKey() + ":*";
        
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        
        log.info("[MenuAuthorizationService] evictAllMenuCache - tenantKey={} deletedKeys={}", 
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()), keys != null ? keys.size() : 0);
    }

    /**
     * 데이터베이스에서 사용자별 접근 가능한 메뉴 조회
     * 
     * @param username 사용자명
     * @param tenant 테넌트
     * @return 접근 가능한 메뉴 목록
     */
    private List<Menu> getAuthorizedMenusFromDatabase(String username, Tenant tenant) {
        User user = getUserByUsername(username);
        Set<String> userPermissions = getUserPermissions(user);

        // 모든 활성화된 메뉴 조회
        List<Menu> allMenus = menuRepository.findByTenantAndActive(tenant);
        
        // 사용자 권한에 따라 필터링
        return filterMenusByUserPermissions(allMenus, username);
    }

    /**
     * 사용자 권한에 따라 메뉴 필터링
     * 
     * @param menus 메뉴 목록
     * @param username 사용자명
     * @return 필터링된 메뉴 목록
     */
    private List<Menu> filterMenusByUserPermissions(List<Menu> menus, String username) {
        User user = getUserByUsername(username);
        Set<String> userPermissions = getUserPermissions(user);

        return menus.stream()
                .filter(menu -> hasMenuAccess(menu, userPermissions))
                .collect(Collectors.toList());
    }

    /**
     * 메뉴 접근 권한 확인
     * 
     * @param menu 메뉴
     * @param userPermissions 사용자 권한 목록
     * @return 접근 권한 여부
     */
    private boolean hasMenuAccess(Menu menu, Set<String> userPermissions) {
        // 메뉴에 권한이 설정되지 않은 경우 접근 허용
        if (menu.getPermissions() == null || menu.getPermissions().isEmpty()) {
            return true;
        }

        // 사용자가 메뉴에 필요한 권한 중 하나라도 가지고 있는지 확인
        return menu.getPermissions().stream()
                .filter(mp -> !mp.getIsDeleted())
                .anyMatch(mp -> userPermissions.contains(mp.getPermission().getPermissionKey()));
    }

    /**
     * 사용자명으로 사용자 조회
     * 
     * @param username 사용자명
     * @return 사용자 정보
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * 사용자의 모든 권한 조회 (역할 기반 권한 + 직접 할당된 권한)
     * 
     * @param user 사용자
     * @return 권한 키 목록
     */
    private Set<String> getUserPermissions(User user) {
        Set<String> permissions = new HashSet<>();

        // 역할 기반 권한
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role.getPermissions() != null) {
                    permissions.addAll(role.getPermissions().stream()
                            .map(Permission::getPermissionKey)
                            .collect(Collectors.toSet()));
                }
            }
        }

        // 직접 할당된 권한
        if (user.getPermissions() != null) {
            permissions.addAll(user.getPermissions().stream()
                    .map(Permission::getPermissionKey)
                    .collect(Collectors.toSet()));
        }

        return permissions;
    }
}
