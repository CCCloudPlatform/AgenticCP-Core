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
 * 조직 멤버 추가 요청 DTO
 * 
 * <p>조직에 사용자를 멤버로 추가하기 위한 요청 DTO입니다.</p>
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
@Schema(description = "조직 멤버 추가 요청")
public class AddMemberRequest {
    
    /** 사용자 ID */
    @NotNull(message = "사용자 ID는 필수입니다")
    @Positive(message = "사용자 ID는 양수여야 합니다")
    @Schema(description = "사용자 ID", example = "1", required = true)
    private Long userId;
    
    /** 조직 내 역할 (선택적) */
    @Size(max = 50, message = "역할은 50자를 초과할 수 없습니다")
    @Schema(description = "조직 내 역할", example = "ADMIN")
    private String role;
}

