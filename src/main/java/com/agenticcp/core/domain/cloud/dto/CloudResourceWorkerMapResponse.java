package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudResourceWorkerMap;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CloudResourceWorkerMap 응답 DTO
 * 
 * <p>클라우드 리소스-Worker 매핑 정보를 표현하는 응답 DTO입니다.
 * 설계 C 기준: 리소스 단위로 Worker 접근 권한을 관리합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "클라우드 리소스-Worker 매핑 응답")
public class CloudResourceWorkerMapResponse {
    
    /** 클라우드 리소스 ID */
    @Schema(description = "클라우드 리소스 ID", example = "1")
    private Long cloudResourceId;
    
    /** 클라우드 리소스명 */
    @Schema(description = "클라우드 리소스명", example = "my-resource")
    private String cloudResourceName;
    
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
    
    /** 조직 ID */
    @Schema(description = "조직 ID", example = "1")
    private Long organizationId;
    
    /** 조직명 */
    @Schema(description = "조직명", example = "개발팀")
    private String organizationName;
    
    /** 생성일시 */
    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    
    /** 수정일시 */
    @Schema(description = "수정일시", example = "2024-01-01T00:00:00")
    private LocalDateTime updatedAt;
    
    /**
     * CloudResourceWorkerMap 엔티티를 CloudResourceWorkerMapResponse로 변환
     * 
     * @param map CloudResourceWorkerMap 엔티티
     * @return CloudResourceWorkerMap 응답 DTO
     */
    public static CloudResourceWorkerMapResponse from(CloudResourceWorkerMap map) {
        if (map == null) {
            return null;
        }
        
        return CloudResourceWorkerMapResponse.builder()
                .cloudResourceId(map.getCloudResource() != null ? map.getCloudResource().getId() : null)
                .cloudResourceName(map.getCloudResource() != null ? map.getCloudResource().getResourceName() : null)
                .workerId(map.getWorker() != null ? map.getWorker().getId() : null)
                .userId(map.getWorker() != null && map.getWorker().getUser() != null 
                        ? map.getWorker().getUser().getId() : null)
                .username(map.getWorker() != null && map.getWorker().getUser() != null 
                        ? map.getWorker().getUser().getUsername() : null)
                .userEmail(map.getWorker() != null && map.getWorker().getUser() != null 
                        ? map.getWorker().getUser().getEmail() : null)
                .userName(map.getWorker() != null && map.getWorker().getUser() != null 
                        ? map.getWorker().getUser().getName() : null)
                .organizationId(map.getWorker() != null && map.getWorker().getOrganization() != null 
                        ? map.getWorker().getOrganization().getId() : null)
                .organizationName(map.getWorker() != null && map.getWorker().getOrganization() != null 
                        ? map.getWorker().getOrganization().getName() : null)
                .createdAt(map.getCreatedAt())
                .updatedAt(map.getUpdatedAt())
                .build();
    }
}

