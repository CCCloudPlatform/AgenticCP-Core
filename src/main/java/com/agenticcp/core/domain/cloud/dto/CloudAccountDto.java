package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.cloud.entity.CloudAccount.AuthMethod;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 클라우드 계정 응답 DTO
 * 
 * 클라우드 계정 정보를 클라이언트에 반환할 때 사용합니다.
 * 민감한 정보(Role ARN, Secret 등)는 제외됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "클라우드 계정 정보")
public class CloudAccountDto {
    
    @Schema(description = "계정 ID", example = "1")
    private Long id;
    
    @Schema(description = "테넌트 ID", example = "1")
    private Long tenantId;
    
    @Schema(description = "테넌트 키", example = "tenant-samsung")
    private String tenantKey;
    
    @Schema(description = "테넌트명", example = "Samsung Electronics")
    private String tenantName;
    
    @Schema(description = "프로바이더 ID", example = "1")
    private Long providerId;
    
    @Schema(description = "프로바이더 타입", example = "AWS")
    private String providerType;
    
    @Schema(description = "프로바이더명", example = "Amazon Web Services")
    private String providerName;
    
    @Schema(description = "계정 ID (마스킹됨)", example = "123456******")
    private String accountId;
    
    @Schema(description = "계정명", example = "aws-prod-account")
    private String accountName;
    
    @Schema(description = "계정 설명", example = "프로덕션 환경 AWS 계정")
    private String description;
    
    @Schema(description = "계정 상태", example = "ACTIVE")
    private Status status;
    
    @Schema(description = "인증 방식", example = "IAM_ROLE")
    private AuthMethod authMethod;
    
    @Schema(description = "기본 리전/존/로케이션", example = "us-east-1")
    private String defaultRegion;
    
    @Schema(description = "기본 계정 여부", example = "true")
    private Boolean isDefault;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "마지막 검증 시간", example = "2025-10-24 10:30:00")
    private LocalDateTime lastVerified;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "생성 시간", example = "2025-10-24 09:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "생성자", example = "admin@samsung.com")
    private String createdBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "수정 시간", example = "2025-10-24 10:00:00")
    private LocalDateTime updatedAt;
    
    @Schema(description = "수정자", example = "admin@samsung.com")
    private String updatedBy;
    
    /**
     * 계정 ID 마스킹 처리
     * AWS: 123456789012 → 123456******
     * GCP: my-gcp-project → my-gcp-******
     * Azure: 12345678-1234-1234-1234-123456789012 → 12345678-****-****-****-************
     */
    public static String maskAccountId(String accountId, String providerType) {
        if (accountId == null || accountId.length() <= 6) {
            return accountId;
        }
        
        if ("AZURE".equalsIgnoreCase(providerType) && accountId.contains("-")) {
            // UUID 형식: 첫 8자만 표시
            return accountId.substring(0, 8) + "-****-****-****-************";
        } else if (accountId.length() == 12 && accountId.matches("\\d{12}")) {
            // AWS 계정 ID: 앞 6자리만 표시
            return accountId.substring(0, 6) + "******";
        } else {
            // GCP 등: 앞 6자만 표시
            String visible = accountId.substring(0, Math.min(6, accountId.length()));
            return visible + "******";
        }
    }
}

