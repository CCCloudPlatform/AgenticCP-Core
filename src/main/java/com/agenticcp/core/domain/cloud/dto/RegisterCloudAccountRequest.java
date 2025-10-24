package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudAccount.AuthMethod;
import com.agenticcp.core.domain.cloud.validation.ValidCloudAccountRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 클라우드 계정 등록 요청 DTO (공통)
 * 
 * 모든 CSP(AWS, GCP, Azure)에 공통적으로 사용되는 기본 필드를 포함합니다.
 * CSP별 전용 필드는 각 상속 클래스에서 정의됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ValidCloudAccountRequest
@Schema(description = "클라우드 계정 등록 요청")
public class RegisterCloudAccountRequest {
    
    @NotNull(message = "테넌트 ID는 필수입니다")
    @Schema(description = "테넌트 ID", example = "1", required = true)
    private Long tenantId;
    
    @NotNull(message = "프로바이더 ID는 필수입니다")
    @Schema(description = "클라우드 프로바이더 ID", example = "1", required = true)
    private Long providerId;
    
    @NotBlank(message = "계정 ID는 필수입니다")
    @Size(max = 100, message = "계정 ID는 100자를 초과할 수 없습니다")
    @Schema(description = "클라우드 계정 ID (AWS: 12자리 숫자, GCP: Project ID, Azure: Subscription ID)", 
            example = "123456789012", 
            required = true)
    private String accountId;
    
    @NotBlank(message = "계정명은 필수입니다")
    @Size(max = 200, message = "계정명은 200자를 초과할 수 없습니다")
    @Schema(description = "계정 별칭", example = "aws-prod-account", required = true)
    private String accountName;
    
    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    @Schema(description = "계정 설명", example = "프로덕션 환경 AWS 계정")
    private String description;
    
    @NotNull(message = "인증 방식은 필수입니다")
    @Schema(description = "인증 방식", example = "IAM_ROLE", required = true)
    private AuthMethod authMethod;
    
    @Size(max = 50, message = "기본 리전은 50자를 초과할 수 없습니다")
    @Schema(description = "기본 리전/존/로케이션", example = "us-east-1")
    private String defaultRegion;
    
    @Builder.Default
    @Schema(description = "기본 계정 여부", example = "false")
    private Boolean isDefault = false;
}

