package com.agenticcp.core.domain.organization.dto;

import com.agenticcp.core.domain.organization.entity.TenantWorkerMap;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TenantWorkerMap 응답 DTO
 * 
 * <p>테넌트-Worker 매핑 정보를 표현하는 응답 DTO입니다.</p>
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
@Schema(description = "테넌트-Worker 매핑 응답")
public class TenantWorkerMapResponse {
    
    /** 테넌트 ID */
    @Schema(description = "테넌트 ID", example = "1")
    private Long tenantId;
    
    /** 테넌트 키 */
    @Schema(description = "테넌트 키", example = "tenant-dev")
    private String tenantKey;
    
    /** 테넌트명 */
    @Schema(description = "테넌트명", example = "개발 테넌트")
    private String tenantName;
    
    /** Worker ID */
    @Schema(description = "Worker ID", example = "1")
    private Long workerId;
    
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
    
    /** 접근 범위 */
    @Schema(description = "접근 범위", example = "FULL")
    private String accessScope;
    
    /** 가입일시 */
    @Schema(description = "가입일시", example = "2024-01-01T00:00:00")
    private LocalDateTime joinedAt;
    
    /** 생성일시 */
    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    
    /**
     * TenantWorkerMap 엔티티를 TenantWorkerMapResponse로 변환
     * 
     * @param map TenantWorkerMap 엔티티
     * @return TenantWorkerMap 응답 DTO
     */
    public static TenantWorkerMapResponse from(TenantWorkerMap map) {
        if (map == null) {
            return null;
        }
        
        return TenantWorkerMapResponse.builder()
                .tenantId(map.getTenant() != null ? map.getTenant().getId() : null)
                .tenantKey(map.getTenant() != null ? map.getTenant().getTenantKey() : null)
                .tenantName(map.getTenant() != null ? map.getTenant().getTenantName() : null)
                .workerId(map.getWorker() != null ? map.getWorker().getId() : null)
                .userId(map.getWorker() != null && map.getWorker().getUser() != null 
                        ? map.getWorker().getUser().getId() : null)
                .username(map.getWorker() != null && map.getWorker().getUser() != null 
                        ? map.getWorker().getUser().getUsername() : null)
                .userEmail(map.getWorker() != null && map.getWorker().getUser() != null 
                        ? map.getWorker().getUser().getEmail() : null)
                .userName(map.getWorker() != null && map.getWorker().getUser() != null 
                        ? map.getWorker().getUser().getName() : null)
                .accessScope(map.getAccessScope())
                .joinedAt(map.getJoinedAt())
                .createdAt(map.getCreatedAt())
                .build();
    }
}

