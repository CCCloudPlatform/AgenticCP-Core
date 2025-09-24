package com.agenticcp.core.domain.user.dto;

import com.agenticcp.core.common.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 권한 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    
    private Long id;
    private String permissionKey;
    private String permissionName;
    private String description;
    private String tenantKey;
    private String tenantName;
    private Status status;
    private String resource;
    private String action;
    private Boolean isSystem;
    private String category;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
