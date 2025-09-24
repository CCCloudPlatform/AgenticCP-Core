package com.agenticcp.core.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 역할 생성 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {
    
    @NotBlank(message = "역할 키는 필수입니다")
    @Size(min = 2, max = 50, message = "역할 키는 2-50자 사이여야 합니다")
    private String roleKey;
    
    @NotBlank(message = "역할명은 필수입니다")
    @Size(min = 2, max = 100, message = "역할명은 2-100자 사이여야 합니다")
    private String roleName;
    
    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;
    
    @NotNull(message = "우선순위는 필수입니다")
    private Integer priority = 0;
    
    private List<String> permissionKeys;

    private Boolean isSystem;
}
