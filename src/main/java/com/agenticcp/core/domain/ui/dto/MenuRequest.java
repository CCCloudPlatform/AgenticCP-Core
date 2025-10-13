package com.agenticcp.core.domain.ui.dto;

import com.agenticcp.core.domain.ui.entity.MenuPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메뉴 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-11
 */
public class MenuRequest {

    /**
     * 메뉴 생성 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateMenuRequest {
        
        @NotBlank(message = "메뉴 키는 필수입니다.")
        @Size(max = 100, message = "메뉴 키는 100자 이하여야 합니다.")
        private String menuKey;

        @NotBlank(message = "메뉴명은 필수입니다.")
        @Size(max = 100, message = "메뉴명은 100자 이하여야 합니다.")
        private String menuName;

        @Size(max = 500, message = "메뉴 설명은 500자 이하여야 합니다.")
        private String description;

        @Size(max = 200, message = "메뉴 URL은 200자 이하여야 합니다.")
        private String url;

        @Size(max = 50, message = "메뉴 아이콘은 50자 이하여야 합니다.")
        private String icon;

        private Long parentId;

        private Integer sortOrder;

        private Boolean isSystem;
    }

    /**
     * 메뉴 수정 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateMenuRequest {
        
        @NotBlank(message = "메뉴 키는 필수입니다.")
        @Size(max = 100, message = "메뉴 키는 100자 이하여야 합니다.")
        private String menuKey;

        @NotBlank(message = "메뉴명은 필수입니다.")
        @Size(max = 100, message = "메뉴명은 100자 이하여야 합니다.")
        private String menuName;

        @Size(max = 500, message = "메뉴 설명은 500자 이하여야 합니다.")
        private String description;

        @Size(max = 200, message = "메뉴 URL은 200자 이하여야 합니다.")
        private String url;

        @Size(max = 50, message = "메뉴 아이콘은 50자 이하여야 합니다.")
        private String icon;

        private Long parentId;

        private Integer sortOrder;

        private Boolean isActive;
    }

    /**
     * 메뉴 권한 할당 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignMenuPermissionRequest {
        
        @NotNull(message = "권한 ID는 필수입니다.")
        private Long permissionId;

        @NotNull(message = "접근 타입은 필수입니다.")
        private MenuPermission.AccessType accessType;
    }

    /**
     * 메뉴 검색 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMenuRequest {
        
        @NotBlank(message = "검색 키워드는 필수입니다.")
        @Size(max = 100, message = "검색 키워드는 100자 이하여야 합니다.")
        private String keyword;
    }
}
