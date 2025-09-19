package com.agenticcp.core.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 권한 생성 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {
    
    @NotBlank(message = "권한 키는 필수입니다")
    @Size(min = 2, max = 50, message = "권한 키는 2-50자 사이여야 합니다")
    private String permissionKey;
    
    @NotBlank(message = "권한명은 필수입니다")
    @Size(min = 2, max = 100, message = "권한명은 2-100자 사이여야 합니다")
    private String permissionName;
    
    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;
    
    @NotBlank(message = "리소스는 필수입니다")
    @Size(max = 100, message = "리소스는 100자를 초과할 수 없습니다")
    private String resource;
    
    @NotBlank(message = "액션은 필수입니다")
    @Size(max = 50, message = "액션은 50자를 초과할 수 없습니다")
    private String action;
    
    @Size(max = 50, message = "카테고리는 50자를 초과할 수 없습니다")
    private String category;
    
    @NotNull(message = "우선순위는 필수입니다")
    private Integer priority = 0;
}
