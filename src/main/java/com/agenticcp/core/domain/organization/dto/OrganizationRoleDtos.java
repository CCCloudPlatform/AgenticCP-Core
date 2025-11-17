package com.agenticcp.core.domain.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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

    /**
     * 역할 할당 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Schema(description = "역할 할당 요청")
    public static class AssignRoleRequest {
        
        /** 역할 ID */
        @NotNull(message = "역할 ID는 필수입니다")
        @Positive(message = "역할 ID는 양수여야 합니다")
        @Schema(description = "역할 ID", example = "1", required = true)
        private Long roleId;
        
        /** 기본 역할로 설정 여부 */
        @Schema(description = "기본 역할로 설정 여부", example = "true")
        private Boolean makeDefault;
        
        /** 우선순위 */
        @Min(value = 0, message = "우선순위는 0 이상이어야 합니다")
        @Schema(description = "우선순위", example = "1")
        private Integer priority;
    }

    /**
     * 역할 설정 수정 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Schema(description = "역할 설정 수정 요청")
    public static class UpdateRoleSettingsRequest {
        
        /** 기본 역할로 설정 여부 */
        @Schema(description = "기본 역할로 설정 여부", example = "true")
        private Boolean makeDefault;
        
        /** 우선순위 */
        @Min(value = 0, message = "우선순위는 0 이상이어야 합니다")
        @Schema(description = "우선순위", example = "1")
        private Integer priority;
        
        /** 상태 */
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "상태는 ACTIVE 또는 INACTIVE여야 합니다")
        @Schema(description = "상태", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        private String status;
    }

    /**
     * 조직-역할 매핑 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Schema(description = "조직-역할 매핑 응답")
    public static class OrganizationRoleResponse {
        
        /** 매핑 ID */
        @Schema(description = "매핑 ID", example = "1")
        private Long mappingId;
        
        /** 역할 ID */
        @Schema(description = "역할 ID", example = "1")
        private Long roleId;
        
        /** 역할 키 */
        @Schema(description = "역할 키", example = "ADMIN")
        private String roleKey;
        
        /** 역할명 */
        @Schema(description = "역할명", example = "관리자")
        private String roleName;
        
        /** 기본 역할 여부 */
        @Schema(description = "기본 역할 여부", example = "true")
        private Boolean isDefault;
        
        /** 우선순위 */
        @Schema(description = "우선순위", example = "1")
        private Integer priority;
        
        /** 상태 */
        @Schema(description = "상태", example = "ACTIVE")
        private String status;
    }

    /**
     * 조직-역할 목록 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Schema(description = "조직-역할 목록 응답")
    public static class OrganizationRoleListResponse {
        
        /** 조직-역할 매핑 목록 */
        @Schema(description = "조직-역할 매핑 목록")
        private List<OrganizationRoleResponse> items;
        
        /** 목록 개수 */
        @Schema(description = "목록 개수", example = "5")
        private int count;
    }
}


