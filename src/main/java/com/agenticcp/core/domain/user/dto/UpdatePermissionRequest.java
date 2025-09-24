package com.agenticcp.core.domain.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 권한 수정 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermissionRequest {
    
    @Size(min = 2, max = 100, message = "권한명은 2-100자 사이여야 합니다")
    private String permissionName;
    
    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;
    
    @Size(max = 100, message = "리소스는 100자를 초과할 수 없습니다")
    private String resource;
    
    @Size(max = 50, message = "액션은 50자를 초과할 수 없습니다")
    private String action;
    
    @Size(max = 50, message = "카테고리는 50자를 초과할 수 없습니다")
    private String category;
    
    private Integer priority;
    
    private Boolean isSystem;
}
