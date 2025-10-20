package com.agenticcp.core.domain.ui.dto;

import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;
import com.agenticcp.core.domain.ui.entity.Menu;
import com.agenticcp.core.domain.ui.entity.MenuPermission;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 메뉴 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
public class MenuResponse {

    /**
     * 메뉴 기본 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuInfo {
        private Long id;
        @Masked(type = MaskingType.DEFAULT)
        private String menuKey;
        private String menuName;
        private String description;
        private String url;
        private String icon;
        private Long parentId;
        private Integer sortOrder;
        private Boolean isActive;
        private Boolean isSystem;
        private int level;
        private boolean isRoot;
        private boolean isLeaf;
        private String menuPath;
        private List<MenuPermissionInfo> permissions;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;

        public static MenuInfo from(Menu menu) {
            return MenuInfo.builder()
                    .id(menu.getId())
                    .menuKey(menu.getMenuKey())
                    .menuName(menu.getMenuName())
                    .description(menu.getDescription())
                    .url(menu.getUrl())
                    .icon(menu.getIcon())
                    .parentId(menu.getParentId())
                    .sortOrder(menu.getSortOrder())
                    .isActive(menu.getIsActive())
                    .isSystem(menu.getIsSystem())
                    .level(menu.getLevel())
                    .isRoot(menu.isRoot())
                    .isLeaf(menu.isLeaf())
                    .menuPath(menu.getMenuPath())
                    .permissions(menu.getPermissions() != null ? 
                            menu.getPermissions().stream()
                                    .map(MenuPermissionInfo::from)
                                    .collect(Collectors.toList()) : null)
                    .createdAt(menu.getCreatedAt())
                    .updatedAt(menu.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 메뉴 트리 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuTreeResponse {
        private Long id;
        @Masked(type = MaskingType.DEFAULT)
        private String menuKey;
        private String menuName;
        private String description;
        private String url;
        private String icon;
        private Long parentId;
        private Integer sortOrder;
        private Boolean isActive;
        private Boolean isSystem;
        private int level;
        private boolean isRoot;
        private boolean isLeaf;
        private String menuPath;
        private List<MenuTreeResponse> children;

        public static MenuTreeResponse from(Menu menu) {
            MenuTreeResponse response = MenuTreeResponse.builder()
                    .id(menu.getId())
                    .menuKey(menu.getMenuKey())
                    .menuName(menu.getMenuName())
                    .description(menu.getDescription())
                    .url(menu.getUrl())
                    .icon(menu.getIcon())
                    .parentId(menu.getParentId())
                    .sortOrder(menu.getSortOrder())
                    .isActive(menu.getIsActive())
                    .isSystem(menu.getIsSystem())
                    .level(menu.getLevel())
                    .isRoot(menu.isRoot())
                    .isLeaf(menu.isLeaf())
                    .menuPath(menu.getMenuPath())
                    .build();

            // 하위 메뉴 재귀적으로 변환
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                response.setChildren(menu.getChildren().stream()
                        .map(MenuTreeResponse::from)
                        .collect(Collectors.toList()));
            }

            return response;
        }
    }

    /**
     * 메뉴 권한 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuPermissionInfo {
        private Long id;
        private Long permissionId;
        @Masked(type = MaskingType.DEFAULT)
        private String permissionKey;
        private String permissionName;
        private MenuPermission.AccessType accessType;
        private String accessTypeDescription;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static MenuPermissionInfo from(MenuPermission menuPermission) {
            return MenuPermissionInfo.builder()
                    .id(menuPermission.getId())
                    .permissionId(menuPermission.getPermission().getId())
                    .permissionKey(menuPermission.getPermission().getPermissionKey())
                    .permissionName(menuPermission.getPermission().getPermissionName())
                    .accessType(menuPermission.getAccessType())
                    .accessTypeDescription(menuPermission.getAccessTypeDescription())
                    .createdAt(menuPermission.getCreatedAt())
                    .build();
        }
    }

    /**
     * 메뉴 권한 할당 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuPermissionResponse {
        private Long id;
        private Long menuId;
        @Masked(type = MaskingType.DEFAULT)
        private String menuKey;
        private String menuName;
        private Long permissionId;
        @Masked(type = MaskingType.DEFAULT)
        private String permissionKey;
        private String permissionName;
        private MenuPermission.AccessType accessType;
        private String accessTypeDescription;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static MenuPermissionResponse from(MenuPermission menuPermission) {
            return MenuPermissionResponse.builder()
                    .id(menuPermission.getId())
                    .menuId(menuPermission.getMenu().getId())
                    .menuKey(menuPermission.getMenu().getMenuKey())
                    .menuName(menuPermission.getMenu().getMenuName())
                    .permissionId(menuPermission.getPermission().getId())
                    .permissionKey(menuPermission.getPermission().getPermissionKey())
                    .permissionName(menuPermission.getPermission().getPermissionName())
                    .accessType(menuPermission.getAccessType())
                    .accessTypeDescription(menuPermission.getAccessTypeDescription())
                    .createdAt(menuPermission.getCreatedAt())
                    .build();
        }
    }

    /**
     * 메뉴 캐시 상태 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuCacheStatusResponse {
        @Masked(type = MaskingType.TENANT_KEY)
        private String tenantKey;
        private boolean menuTreeCached;
        private boolean systemMenusCached;
        private int userMenuCacheCount;
        private int menuPermissionCacheCount;
    }
}
