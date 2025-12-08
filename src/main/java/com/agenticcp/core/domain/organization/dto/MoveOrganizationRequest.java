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
 * 조직 이동 요청 DTO
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
@Schema(description = "조직 이동 요청")
public class MoveOrganizationRequest {
    
    /** 새로운 상위 조직 ID */
    @NotNull(message = "새로운 상위 조직 ID는 필수입니다")
    @Positive(message = "새로운 상위 조직 ID는 양수여야 합니다")
    @Schema(description = "새로운 상위 조직 ID", example = "1", required = true)
    private Long newParentId;
}
