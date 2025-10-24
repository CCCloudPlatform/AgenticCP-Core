package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.validation.ValidGcpProjectId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * GCP 클라우드 계정 등록 요청 DTO
 * 
 * GCP 전용 필드(Service Account Email, Project ID 등)를 포함합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "GCP 클라우드 계정 등록 요청")
public class RegisterGcpAccountRequest extends RegisterCloudAccountRequest {
    
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "Service Account Email은 255자를 초과할 수 없습니다")
    @Schema(description = "GCP Service Account Email", 
            example = "service-account@my-gcp-project.iam.gserviceaccount.com")
    private String serviceAccountEmail;
    
    @ValidGcpProjectId
    @Schema(description = "GCP Project ID", 
            example = "my-gcp-project")
    private String projectId;
    
    @Pattern(regexp = "^[a-z]+-[a-z]+\\d+(-[a-z])?$", 
             message = "올바른 GCP Zone 형식이 아닙니다 (예: us-central1-a, asia-northeast3-a)")
    @Schema(description = "GCP Zone", 
            example = "us-central1-a")
    private String zone;
}

