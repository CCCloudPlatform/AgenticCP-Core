package com.agenticcp.core.domain.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 조직별 역할 DTO 집합
 * 요청/응답 전송 객체를 모아 관리합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-28
 */
public class OrganizationRoleDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignRoleRequest {
        @NotNull
        private Long roleId;
        private Boolean makeDefault;
        private Integer priority;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRoleSettingsRequest {
        private Boolean makeDefault;
        private Integer priority;
        private String status; // ACTIVE, INACTIVE 등 (문자열로 수신)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationRoleResponse {
        private Long mappingId;
        private Long roleId;
        private String roleKey;
        private String roleName;
        private Boolean isDefault;
        private Integer priority;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationRoleListResponse {
        private List<OrganizationRoleResponse> items;
        private int count;
    }
}


