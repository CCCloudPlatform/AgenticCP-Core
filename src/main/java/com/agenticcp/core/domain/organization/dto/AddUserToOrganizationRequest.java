package com.agenticcp.core.domain.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 사용자를 조직에 추가하는 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "사용자를 조직에 추가하는 요청")
public class AddUserToOrganizationRequest {

    /** 사용자 ID */
    @NotNull(message = "사용자 ID는 필수입니다")
    @Positive(message = "사용자 ID는 양수여야 합니다")
    @Schema(description = "사용자 ID", example = "1", required = true)
    private Long userId;
}
