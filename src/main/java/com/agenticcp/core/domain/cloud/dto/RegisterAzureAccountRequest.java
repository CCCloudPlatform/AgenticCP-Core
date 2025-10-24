package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.validation.ValidAzureSubscriptionId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Azure 클라우드 계정 등록 요청 DTO
 * 
 * Azure 전용 필드(Tenant ID, Client ID, Subscription ID 등)를 포함합니다.
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
@Schema(description = "Azure 클라우드 계정 등록 요청")
public class RegisterAzureAccountRequest extends RegisterCloudAccountRequest {
    
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
             message = "올바른 UUID 형식이 아닙니다")
    @Size(max = 100, message = "Azure Tenant ID는 100자를 초과할 수 없습니다")
    @Schema(description = "Azure Tenant ID (UUID 형식)", 
            example = "87654321-4321-4321-4321-210987654321")
    private String azureTenantId;
    
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
             message = "올바른 UUID 형식이 아닙니다")
    @Size(max = 100, message = "Azure Client ID는 100자를 초과할 수 없습니다")
    @Schema(description = "Azure Client ID / Service Principal ID (UUID 형식)", 
            example = "abcdef12-3456-7890-abcd-ef1234567890")
    private String azureClientId;
    
    @ValidAzureSubscriptionId
    @Schema(description = "Azure Subscription ID (UUID 형식)", 
            example = "12345678-1234-1234-1234-123456789012")
    private String subscriptionId;
    
    @Pattern(regexp = "^[a-z]+$", 
             message = "올바른 Azure Location 형식이 아닙니다 (예: eastus, westeurope)")
    @Schema(description = "Azure Location", 
            example = "eastus")
    private String location;
}
