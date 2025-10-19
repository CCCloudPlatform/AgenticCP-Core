package com.agenticcp.core.domain.ui.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.util.LogMaskingUtils;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.ui.entity.Menu;
import com.agenticcp.core.domain.ui.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 메뉴 캐싱 관리 서비스
 * 메뉴 구조와 권한 정보를 효율적으로 캐싱하여 성능을 최적화
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuCacheService {

    private final MenuRepository menuRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 캐시 키 상수
    private static final String MENU_TREE_CACHE_KEY_PREFIX = "menu_tree:";
    private static final String USER_MENU_CACHE_KEY_PREFIX = "user_menus:";
    private static final String MENU_PERMISSIONS_CACHE_KEY_PREFIX = "menu_permissions:";
    private static final String SYSTEM_MENUS_CACHE_KEY_PREFIX = "system_menus:";

    // 캐시 TTL 설정
    private static final Duration MENU_TREE_TTL = Duration.ofMinutes(30);      // 메뉴 트리 구조
    private static final Duration USER_MENU_TTL = Duration.ofMinutes(5);       // 사용자별 메뉴
    private static final Duration MENU_PERMISSIONS_TTL = Duration.ofMinutes(15); // 메뉴 권한 정보
    private static final Duration SYSTEM_MENUS_TTL = Duration.ofHours(1);       // 시스템 메뉴 (변경 빈도 낮음)

    /**
     * 메뉴 트리 구조 캐싱
     * 
     * @return 캐시된 메뉴 트리 구조
     */
    @SuppressWarnings("unchecked")
    public List<Menu> getCachedMenuTree() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = MENU_TREE_CACHE_KEY_PREFIX + currentTenant.getTenantKey();
        
        log.info("[MenuCacheService] getCachedMenuTree - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));

        List<Menu> cachedMenuTree = (List<Menu>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedMenuTree != null) {
            log.info("[MenuCacheService] getCachedMenuTree - cache hit tenantKey={} count={}", 
                    LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()), cachedMenuTree.size());
            return cachedMenuTree;
        }

        // 캐시 미스 시 데이터베이스에서 조회하여 캐싱
        List<Menu> menuTree = menuRepository.findMenuTreeWithPermissions(currentTenant);
        redisTemplate.opsForValue().set(cacheKey, menuTree, MENU_TREE_TTL);
        
        log.info("[MenuCacheService] getCachedMenuTree - cache miss and stored tenantKey={} count={}", 
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()), menuTree.size());
        
        return menuTree;
    }

    /**
     * 사용자별 메뉴 캐싱
     * 
     * @param username 사용자명
     * @param menus 메뉴 목록
     */
    public void cacheUserMenus(String username, List<Menu> menus) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = USER_MENU_CACHE_KEY_PREFIX + currentTenant.getTenantKey() + ":" + username;
        
        redisTemplate.opsForValue().set(cacheKey, menus, USER_MENU_TTL);
        
        log.info("[MenuCacheService] cacheUserMenus - username={} count={} tenantKey={}", 
                LogMaskingUtils.maskUsername(username), menus.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
    }

    /**
     * 사용자별 메뉴 캐시 조회
     * 
     * @param username 사용자명
     * @return 캐시된 메뉴 목록
     */
    @SuppressWarnings("unchecked")
    public List<Menu> getCachedUserMenus(String username) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = USER_MENU_CACHE_KEY_PREFIX + currentTenant.getTenantKey() + ":" + username;
        
        List<Menu> cachedMenus = (List<Menu>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedMenus != null) {
            log.info("[MenuCacheService] getCachedUserMenus - cache hit username={} count={}", LogMaskingUtils.maskUsername(username), cachedMenus.size());
        } else {
            log.info("[MenuCacheService] getCachedUserMenus - cache miss username={}", LogMaskingUtils.maskUsername(username));
        }
        
        return cachedMenus;
    }

    /**
     * 시스템 메뉴 캐싱
     * 
     * @return 캐시된 시스템 메뉴 목록
     */
    @SuppressWarnings("unchecked")
    public List<Menu> getCachedSystemMenus() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = SYSTEM_MENUS_CACHE_KEY_PREFIX + currentTenant.getTenantKey();
        
        log.info("[MenuCacheService] getCachedSystemMenus - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));

        List<Menu> cachedSystemMenus = (List<Menu>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedSystemMenus != null) {
            log.info("[MenuCacheService] getCachedSystemMenus - cache hit tenantKey={} count={}", 
                    LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()), cachedSystemMenus.size());
            return cachedSystemMenus;
        }

        // 캐시 미스 시 데이터베이스에서 조회하여 캐싱
        List<Menu> systemMenus = menuRepository.findSystemMenusByTenant(currentTenant);
        redisTemplate.opsForValue().set(cacheKey, systemMenus, SYSTEM_MENUS_TTL);
        
        log.info("[MenuCacheService] getCachedSystemMenus - cache miss and stored tenantKey={} count={}", 
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()), systemMenus.size());
        
        return systemMenus;
    }

    /**
     * 메뉴 트리 구조 캐시 무효화
     */
    public void evictMenuTreeCache() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = MENU_TREE_CACHE_KEY_PREFIX + currentTenant.getTenantKey();
        
        redisTemplate.delete(cacheKey);
        log.info("[MenuCacheService] evictMenuTreeCache - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
    }

    /**
     * 특정 사용자 메뉴 캐시 무효화
     * 
     * @param username 사용자명
     */
    public void evictUserMenuCache(String username) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = USER_MENU_CACHE_KEY_PREFIX + currentTenant.getTenantKey() + ":" + username;
        
        redisTemplate.delete(cacheKey);
        log.info("[MenuCacheService] evictUserMenuCache - username={} tenantKey={}", 
                LogMaskingUtils.maskUsername(username), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
    }

    /**
     * 모든 사용자 메뉴 캐시 무효화
     */
    public void evictAllUserMenuCache() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String pattern = USER_MENU_CACHE_KEY_PREFIX + currentTenant.getTenantKey() + ":*";
        
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        
        log.info("[MenuCacheService] evictAllUserMenuCache - tenantKey={} deletedKeys={}", 
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()), keys != null ? keys.size() : 0);
    }

    /**
     * 시스템 메뉴 캐시 무효화
     */
    public void evictSystemMenuCache() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = SYSTEM_MENUS_CACHE_KEY_PREFIX + currentTenant.getTenantKey();
        
        redisTemplate.delete(cacheKey);
        log.info("[MenuCacheService] evictSystemMenuCache - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
    }

    /**
     * 메뉴 권한 정보 캐시 무효화
     * 
     * @param menuId 메뉴 ID
     */
    public void evictMenuPermissionsCache(Long menuId) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String cacheKey = MENU_PERMISSIONS_CACHE_KEY_PREFIX + currentTenant.getTenantKey() + ":" + menuId;
        
        redisTemplate.delete(cacheKey);
        log.info("[MenuCacheService] evictMenuPermissionsCache - menuId={} tenantKey={}", 
                menuId, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
    }

    /**
     * 테넌트별 모든 메뉴 캐시 무효화
     */
    public void evictAllMenuCache() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String tenantKey = currentTenant.getTenantKey();
        
        // 각종 캐시 키 패턴으로 일괄 삭제
        String[] patterns = {
            MENU_TREE_CACHE_KEY_PREFIX + tenantKey,
            USER_MENU_CACHE_KEY_PREFIX + tenantKey + ":*",
            MENU_PERMISSIONS_CACHE_KEY_PREFIX + tenantKey + ":*",
            SYSTEM_MENUS_CACHE_KEY_PREFIX + tenantKey
        };
        
        int totalDeleted = 0;
        for (String pattern : patterns) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                totalDeleted += keys.size();
            }
        }
        
        log.info("[MenuCacheService] evictAllMenuCache - tenantKey={} totalDeleted={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), totalDeleted);
    }

    /**
     * 캐시 상태 정보 조회
     * 
     * @return 캐시 상태 정보
     */
    public MenuCacheStatus getCacheStatus() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        String tenantKey = currentTenant.getTenantKey();
        
        // 각 캐시의 존재 여부 확인
        boolean menuTreeExists = redisTemplate.hasKey(MENU_TREE_CACHE_KEY_PREFIX + tenantKey);
        boolean systemMenusExist = redisTemplate.hasKey(SYSTEM_MENUS_CACHE_KEY_PREFIX + tenantKey);
        
        // 사용자 메뉴 캐시 개수 확인
        Set<String> userMenuKeys = redisTemplate.keys(USER_MENU_CACHE_KEY_PREFIX + tenantKey + ":*");
        int userMenuCacheCount = userMenuKeys != null ? userMenuKeys.size() : 0;
        
        // 메뉴 권한 캐시 개수 확인
        Set<String> menuPermissionKeys = redisTemplate.keys(MENU_PERMISSIONS_CACHE_KEY_PREFIX + tenantKey + ":*");
        int menuPermissionCacheCount = menuPermissionKeys != null ? menuPermissionKeys.size() : 0;
        
        return MenuCacheStatus.builder()
                .tenantKey(tenantKey)
                .menuTreeCached(menuTreeExists)
                .systemMenusCached(systemMenusExist)
                .userMenuCacheCount(userMenuCacheCount)
                .menuPermissionCacheCount(menuPermissionCacheCount)
                .build();
    }

    /**
     * 메뉴 캐시 상태 정보 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MenuCacheStatus {
        private String tenantKey;
        private boolean menuTreeCached;
        private boolean systemMenusCached;
        private int userMenuCacheCount;
        private int menuPermissionCacheCount;
    }
}
