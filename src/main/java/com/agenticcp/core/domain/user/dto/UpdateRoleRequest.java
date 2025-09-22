package com.agenticcp.core.domain.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 역할 수정 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {
    
    @Size(min = 2, max = 100, message = "역할명은 2-100자 사이여야 합니다")
    private String roleName;
    
    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;
    
    private Integer priority;
    
    private List<String> permissionKeys;
    
    private Boolean isSystem;
}
