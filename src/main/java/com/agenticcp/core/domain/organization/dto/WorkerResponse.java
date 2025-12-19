package com.agenticcp.core.domain.organization.dto;

import com.agenticcp.core.domain.organization.entity.Worker;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Worker 응답 DTO
 * 
 * <p>Worker 정보를 표현하는 응답 DTO입니다.</p>
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
@Schema(description = "Worker 응답")
public class WorkerResponse {
    
    /** Worker ID */
    @Schema(description = "Worker ID", example = "1")
    private Long id;
    
    /** 사용자 ID */
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    /** 사용자명 */
    @Schema(description = "사용자명", example = "john_doe")
    private String username;
    
    /** 사용자 이메일 */
    @Schema(description = "사용자 이메일", example = "john@example.com")
    private String userEmail;
    
    /** 사용자 이름 */
    @Schema(description = "사용자 이름", example = "John Doe")
    private String userName;
    
    /** 조직 ID (Organization 기반 Worker인 경우) */
    @Schema(description = "조직 ID", example = "1")
    private Long organizationId;
    
    /** 조직명 (Organization 기반 Worker인 경우) */
    @Schema(description = "조직명", example = "개발팀")
    private String organizationName;
    
    /** 생성일시 */
    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    
    /** 수정일시 */
    @Schema(description = "수정일시", example = "2024-01-01T00:00:00")
    private LocalDateTime updatedAt;
    
    /**
     * Worker 엔티티를 WorkerResponse로 변환
     * 
     * @param worker Worker 엔티티
     * @return Worker 응답 DTO
     */
    public static WorkerResponse from(Worker worker) {
        if (worker == null) {
            return null;
        }
        
        return WorkerResponse.builder()
                .id(worker.getId())
                .userId(worker.getUser() != null ? worker.getUser().getId() : null)
                .username(worker.getUser() != null ? worker.getUser().getUsername() : null)
                .userEmail(worker.getUser() != null ? worker.getUser().getEmail() : null)
                .userName(worker.getUser() != null ? worker.getUser().getName() : null)
                .organizationId(worker.getOrganization() != null ? worker.getOrganization().getId() : null)
                .organizationName(worker.getOrganization() != null ? worker.getOrganization().getName() : null)
                .createdAt(worker.getCreatedAt())
                .updatedAt(worker.getUpdatedAt())
                .build();
    }
}

