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
 * Worker 역할 부여 요청 DTO
 * 
 * <p>Worker에게 역할을 부여하기 위한 요청 DTO입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "Worker 역할 부여 요청")
public class AssignRoleRequest {
    
    /** 역할 ID */
    @NotNull(message = "역할 ID는 필수입니다")
    @Positive(message = "역할 ID는 양수여야 합니다")
    @Schema(description = "역할 ID", example = "1", required = true)
    private Long roleId;
    
    /** 테넌트 ID */
    @NotNull(message = "테넌트 ID는 필수입니다")
    @Positive(message = "테넌트 ID는 양수여야 합니다")
    @Schema(description = "테넌트 ID", example = "1", required = true)
    private Long tenantId;
}

