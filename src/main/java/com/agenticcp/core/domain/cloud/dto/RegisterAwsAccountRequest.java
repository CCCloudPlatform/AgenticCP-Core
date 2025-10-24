package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.validation.ValidIamRoleArn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * AWS 클라우드 계정 등록 요청 DTO
 * 
 * AWS 전용 필드(Role ARN, External ID 등)를 포함합니다.
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
@Schema(description = "AWS 클라우드 계정 등록 요청")
public class RegisterAwsAccountRequest extends RegisterCloudAccountRequest {
    
    @ValidIamRoleArn
    @Size(max = 255, message = "Role ARN은 255자를 초과할 수 없습니다")
    @Schema(description = "AWS IAM Role ARN", 
            example = "arn:aws:iam::123456789012:role/AgenticCPRole")
    private String roleArn;
    
    @Size(max = 255, message = "External ID는 255자를 초과할 수 없습니다")
    @Schema(description = "AWS Cross-Account Access External ID", 
            example = "external-id-12345")
    private String externalId;
    
    @Pattern(regexp = "^[a-z]{2}-[a-z]+-\\d{1}$", 
             message = "올바른 AWS 리전 형식이 아닙니다 (예: us-east-1, ap-northeast-2)")
    @Schema(description = "AWS 리전", 
            example = "us-east-1")
    private String region;
}

