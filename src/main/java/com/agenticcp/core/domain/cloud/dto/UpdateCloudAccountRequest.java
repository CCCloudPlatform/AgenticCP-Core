package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.common.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 클라우드 계정 수정 요청 DTO
 * 
 * 계정 정보 중 수정 가능한 필드만 포함합니다.
 * (계정 ID, Provider ID 등은 수정 불가)
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "클라우드 계정 수정 요청")
public class UpdateCloudAccountRequest {
    
    @Size(max = 200, message = "계정명은 200자를 초과할 수 없습니다")
    @Schema(description = "계정 별칭", example = "aws-prod-account-updated")
    private String accountName;
    
    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    @Schema(description = "계정 설명", example = "업데이트된 프로덕션 환경 AWS 계정")
    private String description;
    
    @Size(max = 50, message = "기본 리전은 50자를 초과할 수 없습니다")
    @Schema(description = "기본 리전/존/로케이션", example = "us-west-2")
    private String defaultRegion;
    
    @Schema(description = "계정 상태", example = "ACTIVE")
    private Status status;
    
    @Schema(description = "기본 계정 여부", example = "true")
    private Boolean isDefault;
}

