package com.agenticcp.core.domain.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 테넌트에 Worker 할당 요청 DTO
 * 
 * <p>Shared Tenant에 Worker를 할당하기 위한 요청 DTO입니다.</p>
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
@Schema(description = "테넌트에 Worker 할당 요청")
public class AssignWorkerRequest {
    
    /** Worker ID */
    @NotNull(message = "Worker ID는 필수입니다")
    @Positive(message = "Worker ID는 양수여야 합니다")
    @Schema(description = "Worker ID", example = "1", required = true)
    private Long workerId;
    
    /** 접근 범위 (선택적) */
    @Size(max = 30, message = "접근 범위는 30자를 초과할 수 없습니다")
    @Schema(description = "접근 범위", example = "FULL")
    private String accessScope;
}

