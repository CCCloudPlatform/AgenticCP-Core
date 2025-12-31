package com.agenticcp.core.domain.organization.dto;

import com.agenticcp.core.domain.organization.entity.WorkerRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WorkerRole 응답 DTO
 * 
 * <p>Worker 역할 정보를 표현하는 응답 DTO입니다.</p>
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
@Schema(description = "Worker 역할 응답")
public class WorkerRoleResponse {
    
    /** Worker ID */
    @Schema(description = "Worker ID", example = "1")
    private Long workerId;
    
    /** 사용자 ID */
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    /** 사용자명 */
    @Schema(description = "사용자명", example = "john_doe")
    private String username;
    
    /** 역할 ID */
    @Schema(description = "역할 ID", example = "1")
    private Long roleId;
    
    /** 역할 키 */
    @Schema(description = "역할 키", example = "ADMIN")
    private String roleKey;
    
    /** 역할명 */
    @Schema(description = "역할명", example = "관리자")
    private String roleName;
    
    /** 테넌트 ID */
    @Schema(description = "테넌트 ID", example = "1")
    private Long tenantId;
    
    /** 테넌트 키 */
    @Schema(description = "테넌트 키", example = "tenant-dev")
    private String tenantKey;
    
    /** 테넌트명 */
    @Schema(description = "테넌트명", example = "개발 테넌트")
    private String tenantName;
    
    /** 생성일시 */
    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    
    /** 수정일시 */
    @Schema(description = "수정일시", example = "2024-01-01T00:00:00")
    private LocalDateTime updatedAt;
    
    /**
     * WorkerRole 엔티티를 WorkerRoleResponse로 변환
     * 
     * @param workerRole WorkerRole 엔티티
     * @return WorkerRole 응답 DTO
     */
    public static WorkerRoleResponse from(WorkerRole workerRole) {
        if (workerRole == null) {
            return null;
        }
        
        return WorkerRoleResponse.builder()
                .workerId(workerRole.getWorker() != null ? workerRole.getWorker().getId() : null)
                .userId(workerRole.getWorker() != null && workerRole.getWorker().getUser() != null 
                        ? workerRole.getWorker().getUser().getId() : null)
                .username(workerRole.getWorker() != null && workerRole.getWorker().getUser() != null 
                        ? workerRole.getWorker().getUser().getUsername() : null)
                .roleId(workerRole.getRole() != null ? workerRole.getRole().getId() : null)
                .roleKey(workerRole.getRole() != null ? workerRole.getRole().getRoleKey() : null)
                .roleName(workerRole.getRole() != null ? workerRole.getRole().getRoleName() : null)
                .tenantId(workerRole.getRole() != null && workerRole.getRole().getTenant() != null 
                        ? workerRole.getRole().getTenant().getId() : null)
                .tenantKey(workerRole.getRole() != null && workerRole.getRole().getTenant() != null 
                        ? workerRole.getRole().getTenant().getTenantKey() : null)
                .tenantName(workerRole.getRole() != null && workerRole.getRole().getTenant() != null 
                        ? workerRole.getRole().getTenant().getTenantName() : null)
                .createdAt(workerRole.getCreatedAt())
                .updatedAt(workerRole.getUpdatedAt())
                .build();
    }
}

