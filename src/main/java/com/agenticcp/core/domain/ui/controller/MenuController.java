package com.agenticcp.core.domain.ui.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.logging.LogMaskingUtils;
import com.agenticcp.core.domain.ui.dto.MenuRequest;
import com.agenticcp.core.domain.ui.dto.MenuResponse;
import com.agenticcp.core.domain.ui.entity.Menu;
import com.agenticcp.core.domain.ui.entity.MenuPermission;
import com.agenticcp.core.domain.ui.service.MenuAuthorizationService;
import com.agenticcp.core.domain.ui.service.MenuCacheService;
import com.agenticcp.core.domain.ui.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 메뉴 관리 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Tag(name = "Menu Management", description = "메뉴 관리 API")
public class MenuController {

    private final MenuService menuService;
    private final MenuAuthorizationService menuAuthorizationService;
    private final MenuCacheService menuCacheService;

    /**
     * 사용자별 접근 가능한 메뉴 조회
     * 
     * @param authentication 인증 정보
     * @return 접근 가능한 메뉴 목록
     */
    @GetMapping
    @Operation(summary = "사용자 메뉴 조회", description = "현재 사용자가 접근 가능한 메뉴 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<MenuResponse.MenuTreeResponse>>> getUserMenus(
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("[MenuController] getUserMenus - username={}", username);

        List<Menu> authorizedMenus = menuAuthorizationService.getAuthorizedMenuTree(username);
        List<MenuResponse.MenuTreeResponse> response = authorizedMenus.stream()
                .map(MenuResponse.MenuTreeResponse::from)
                .collect(Collectors.toList());

        log.info("[MenuController] getUserMenus - success username={} count={}", username, response.size());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 최상위 메뉴 조회
     * 
     * @return 최상위 메뉴 목록
     */
    @GetMapping("/root")
    @Operation(summary = "최상위 메뉴 조회", description = "부모 메뉴가 없는 최상위 메뉴 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<MenuResponse.MenuInfo>>> getRootMenus() {
        log.info("[MenuController] getRootMenus");

        List<Menu> rootMenus = menuService.getRootMenus();
        List<MenuResponse.MenuInfo> response = rootMenus.stream()
                .map(MenuResponse.MenuInfo::from)
                .collect(Collectors.toList());

        log.info("[MenuController] getRootMenus - success count={}", response.size());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 하위 메뉴 조회
     * 
     * @param parentId 부모 메뉴 ID
     * @return 하위 메뉴 목록
     */
    @GetMapping("/parent/{parentId}")
    @Operation(summary = "하위 메뉴 조회", description = "특정 부모 메뉴의 하위 메뉴 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<MenuResponse.MenuInfo>>> getChildMenus(
            @Parameter(description = "부모 메뉴 ID") @PathVariable Long parentId) {
        
        log.info("[MenuController] getChildMenus - parentId={}", parentId);

        List<Menu> childMenus = menuService.getChildMenus(parentId);
        List<MenuResponse.MenuInfo> response = childMenus.stream()
                .map(MenuResponse.MenuInfo::from)
                .collect(Collectors.toList());

        log.info("[MenuController] getChildMenus - success parentId={} count={}", parentId, response.size());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 메뉴 상세 조회
     * 
     * @param menuId 메뉴 ID
     * @return 메뉴 상세 정보
     */
    @GetMapping("/{menuId}")
    @Operation(summary = "메뉴 상세 조회", description = "특정 메뉴의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<MenuResponse.MenuInfo>> getMenuById(
            @Parameter(description = "메뉴 ID") @PathVariable Long menuId) {
        
        log.info("[MenuController] getMenuById - menuId={}", menuId);

        Menu menu = menuService.getMenuById(menuId);
        MenuResponse.MenuInfo response = MenuResponse.MenuInfo.from(menu);

        log.info("[MenuController] getMenuById - success menuId={} menuKey={}", menuId, menu.getMenuKey());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 메뉴 키로 메뉴 조회
     * 
     * @param menuKey 메뉴 키
     * @return 메뉴 정보
     */
    @GetMapping("/key/{menuKey}")
    @Operation(summary = "메뉴 키로 조회", description = "메뉴 키로 메뉴 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<MenuResponse.MenuInfo>> getMenuByKey(
            @Parameter(description = "메뉴 키") @PathVariable String menuKey) {
        
        log.info("[MenuController] getMenuByKey - menuKey={}", menuKey);

        Menu menu = menuService.getMenuByKey(menuKey);
        MenuResponse.MenuInfo response = MenuResponse.MenuInfo.from(menu);

        log.info("[MenuController] getMenuByKey - success menuKey={} menuId={}", menuKey, menu.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 메뉴 검색
     * 
     * @param keyword 검색 키워드
     * @param pageable 페이지 정보
     * @return 검색된 메뉴 목록
     */
    @GetMapping("/search")
    @Operation(summary = "메뉴 검색", description = "메뉴명이나 설명으로 메뉴를 검색합니다.")
    public ResponseEntity<ApiResponse<Page<MenuResponse.MenuInfo>>> searchMenus(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            Pageable pageable) {
        
        log.info("[MenuController] searchMenus - keyword={} page={} size={}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        List<Menu> menus = menuService.searchMenus(keyword);
        
        // 수동 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), menus.size());
        List<Menu> pagedMenus = start >= menus.size() ? List.of() : menus.subList(start, end);
        
        List<MenuResponse.MenuInfo> response = pagedMenus.stream()
                .map(MenuResponse.MenuInfo::from)
                .collect(Collectors.toList());

        Page<MenuResponse.MenuInfo> pageResponse = new PageImpl<>(response, pageable, menus.size());

        log.info("[MenuController] searchMenus - success keyword={} total={} page={} size={}", 
                keyword, menus.size(), pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 메뉴 생성 (관리자용)
     * 
     * @param request 메뉴 생성 요청
     * @return 생성된 메뉴 정보
     */
    @PostMapping
    @Operation(summary = "메뉴 생성", description = "새로운 메뉴를 생성합니다. (관리자 권한 필요)")
    public ResponseEntity<ApiResponse<MenuResponse.MenuInfo>> createMenu(
            @Valid @RequestBody MenuRequest.CreateMenuRequest request) {
        
        log.info("[MenuController] createMenu - menuKey={} menuName={} parentId={}", 
                request.getMenuKey(), request.getMenuName(), request.getParentId());

        Menu menu = menuService.createMenu(
                request.getMenuKey(),
                request.getMenuName(),
                request.getDescription(),
                request.getUrl(),
                request.getIcon(),
                request.getParentId(),
                request.getSortOrder(),
                request.getIsSystem()
        );

        MenuResponse.MenuInfo response = MenuResponse.MenuInfo.from(menu);

        log.info("[MenuController] createMenu - success menuId={} menuKey={}", menu.getId(), menu.getMenuKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "메뉴가 성공적으로 생성되었습니다."));
    }

    /**
     * 메뉴 수정 (관리자용)
     * 
     * @param menuId 메뉴 ID
     * @param request 메뉴 수정 요청
     * @return 수정된 메뉴 정보
     */
    @PutMapping("/{menuId}")
    @Operation(summary = "메뉴 수정", description = "기존 메뉴 정보를 수정합니다. (관리자 권한 필요)")
    public ResponseEntity<ApiResponse<MenuResponse.MenuInfo>> updateMenu(
            @Parameter(description = "메뉴 ID") @PathVariable Long menuId,
            @Valid @RequestBody MenuRequest.UpdateMenuRequest request) {
        
        log.info("[MenuController] updateMenu - menuId={} menuKey={} menuName={}", 
                menuId, request.getMenuKey(), request.getMenuName());

        Menu menu = menuService.updateMenu(
                menuId,
                request.getMenuKey(),
                request.getMenuName(),
                request.getDescription(),
                request.getUrl(),
                request.getIcon(),
                request.getParentId(),
                request.getSortOrder(),
                request.getIsActive()
        );

        MenuResponse.MenuInfo response = MenuResponse.MenuInfo.from(menu);

        log.info("[MenuController] updateMenu - success menuId={} menuKey={}", menuId, menu.getMenuKey());
        return ResponseEntity.ok(ApiResponse.success(response, "메뉴가 성공적으로 수정되었습니다."));
    }

    /**
     * 메뉴 삭제 (관리자용)
     * 
     * @param menuId 메뉴 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{menuId}")
    @Operation(summary = "메뉴 삭제", description = "메뉴를 삭제합니다. (관리자 권한 필요)")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @Parameter(description = "메뉴 ID") @PathVariable Long menuId) {
        
        log.info("[MenuController] deleteMenu - menuId={}", menuId);

        menuService.deleteMenu(menuId);

        log.info("[MenuController] deleteMenu - success menuId={}", menuId);
        return ResponseEntity.ok(ApiResponse.success(null, "메뉴가 성공적으로 삭제되었습니다."));
    }

    /**
     * 메뉴 권한 할당 (관리자용)
     * 
     * @param menuId 메뉴 ID
     * @param request 권한 할당 요청
     * @return 할당된 권한 정보
     */
    @PostMapping("/{menuId}/permissions")
    @Operation(summary = "메뉴 권한 할당", description = "메뉴에 권한을 할당합니다. (관리자 권한 필요)")
    public ResponseEntity<ApiResponse<MenuResponse.MenuPermissionResponse>> assignMenuPermission(
            @Parameter(description = "메뉴 ID") @PathVariable Long menuId,
            @Valid @RequestBody MenuRequest.AssignMenuPermissionRequest request) {
        
        log.info("[MenuController] assignMenuPermission - menuId={} permissionId={} accessType={}", 
                menuId, request.getPermissionId(), request.getAccessType());

        MenuPermission menuPermission = menuService.assignMenuPermission(
                menuId, 
                request.getPermissionId(), 
                request.getAccessType()
        );

        MenuResponse.MenuPermissionResponse response = MenuResponse.MenuPermissionResponse.from(menuPermission);

        log.info("[MenuController] assignMenuPermission - success menuId={} permissionId={} accessType={}", 
                menuId, request.getPermissionId(), request.getAccessType());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "메뉴 권한이 성공적으로 할당되었습니다."));
    }

    /**
     * 메뉴 권한 제거 (관리자용)
     * 
     * @param menuId 메뉴 ID
     * @param permissionId 권한 ID
     * @return 제거 결과
     */
    @DeleteMapping("/{menuId}/permissions/{permissionId}")
    @Operation(summary = "메뉴 권한 제거", description = "메뉴에서 권한을 제거합니다. (관리자 권한 필요)")
    public ResponseEntity<ApiResponse<Void>> removeMenuPermission(
            @Parameter(description = "메뉴 ID") @PathVariable Long menuId,
            @Parameter(description = "권한 ID") @PathVariable Long permissionId) {
        
        log.info("[MenuController] removeMenuPermission - menuId={} permissionId={}", menuId, permissionId);

        menuService.removeMenuPermission(menuId, permissionId);

        log.info("[MenuController] removeMenuPermission - success menuId={} permissionId={}", menuId, permissionId);
        return ResponseEntity.ok(ApiResponse.success(null, "메뉴 권한이 성공적으로 제거되었습니다."));
    }

    /**
     * 메뉴 캐시 상태 조회
     * 
     * @return 캐시 상태 정보
     */
    @GetMapping("/cache/status")
    @Operation(summary = "메뉴 캐시 상태 조회", description = "메뉴 캐시의 현재 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<MenuResponse.MenuCacheStatusResponse>> getCacheStatus() {
        log.info("[MenuController] getCacheStatus");

        MenuCacheService.MenuCacheStatus cacheStatus = menuCacheService.getCacheStatus();
        MenuResponse.MenuCacheStatusResponse response = MenuResponse.MenuCacheStatusResponse.builder()
                .tenantKey(cacheStatus.getTenantKey())
                .menuTreeCached(cacheStatus.isMenuTreeCached())
                .systemMenusCached(cacheStatus.isSystemMenusCached())
                .userMenuCacheCount(cacheStatus.getUserMenuCacheCount())
                .menuPermissionCacheCount(cacheStatus.getMenuPermissionCacheCount())
                .build();

        log.info("[MenuController] getCacheStatus - success");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 메뉴 캐시 무효화
     * 
     * @param username 사용자명
     * @return 무효화 결과
     */
    @DeleteMapping("/cache/users/{username}")
    @Operation(summary = "사용자 메뉴 캐시 무효화", description = "특정 사용자의 메뉴 캐시를 무효화합니다.")
    public ResponseEntity<ApiResponse<Void>> evictUserMenuCache(
            @Parameter(description = "사용자명") @PathVariable String username) {
        
        log.info("[MenuController] evictUserMenuCache - username={}", username);

        menuAuthorizationService.evictUserMenuCache(username);
        menuCacheService.evictUserMenuCache(username);

        log.info("[MenuController] evictUserMenuCache - success username={}", username);
        return ResponseEntity.ok(ApiResponse.success(null, "사용자 메뉴 캐시가 성공적으로 무효화되었습니다."));
    }

    /**
     * 모든 메뉴 캐시 무효화
     * 
     * @return 무효화 결과
     */
    @DeleteMapping("/cache/all")
    @Operation(summary = "전체 메뉴 캐시 무효화", description = "모든 메뉴 관련 캐시를 무효화합니다.")
    public ResponseEntity<ApiResponse<Void>> evictAllMenuCache() {
        log.info("[MenuController] evictAllMenuCache");

        menuAuthorizationService.evictAllMenuCache();
        menuCacheService.evictAllMenuCache();

        log.info("[MenuController] evictAllMenuCache - success");
        return ResponseEntity.ok(ApiResponse.success(null, "모든 메뉴 캐시가 성공적으로 무효화되었습니다."));
    }
}
