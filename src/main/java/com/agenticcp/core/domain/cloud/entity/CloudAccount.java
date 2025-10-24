package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 클라우드 계정 엔티티
 * 
 * 테넌트와 클라우드 프로바이더 계정의 연결 정보를 관리합니다.
 * 멀티 클라우드 환경을 지원하며, AWS, GCP, Azure의 계정을 통합 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-25
 */
@Entity
@Table(name = "cloud_accounts", indexes = {
    @Index(name = "idx_cloud_accounts_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cloud_accounts_provider", columnList = "provider_id"),
    @Index(name = "idx_cloud_accounts_tenant_provider", columnList = "tenant_id, provider_id"),
    @Index(name = "idx_cloud_accounts_status", columnList = "status"),
    @Index(name = "idx_cloud_accounts_account_id", columnList = "account_id"),
    @Index(name = "idx_cloud_accounts_default", columnList = "tenant_id, provider_id, is_default")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudAccount extends BaseEntity {

    // ==================== 기본 정보 ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private CloudProvider provider;

    @NotBlank(message = "계정 ID는 필수입니다")
    @Size(max = 100, message = "계정 ID는 100자를 초과할 수 없습니다")
    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;  // AWS: Account ID, GCP: Project ID, Azure: Subscription ID

    @NotBlank(message = "계정명은 필수입니다")
    @Size(max = 200, message = "계정명은 200자를 초과할 수 없습니다")
    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", length = 50)
    private AuthMethod authMethod;

    // ==================== AWS 전용 필드 ====================
    
    @Column(name = "role_arn", length = 255)
    private String roleArn;  // AWS IAM Role ARN

    @Column(name = "external_id", length = 255)
    private String externalId;  // AWS Cross-Account Access External ID

    // ==================== GCP 전용 필드 ====================
    
    @Column(name = "service_account_email", length = 255)
    private String serviceAccountEmail;  // GCP Service Account Email

    // ==================== Azure 전용 필드 ====================
    
    @Column(name = "azure_tenant_id", length = 100)
    private String azureTenantId;  // Azure Tenant ID

    @Column(name = "azure_client_id", length = 100)
    private String azureClientId;  // Azure Client ID (Service Principal)

    // ==================== 공통 설정 ====================
    
    @Column(name = "default_region", length = 50)
    private String defaultRegion;  // AWS: us-east-1, GCP: us-central1, Azure: eastus

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;  // 프로바이더별 기본 계정 여부

    @Column(name = "last_verified")
    private LocalDateTime lastVerified;  // 마지막 검증 시간

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  // JSON for additional metadata (resourceCount, monthlyCost, etc.)

    /**
     * 인증 방식 Enum
     */
    public enum AuthMethod {
        // AWS
        IAM_ROLE,
        ACCESS_KEY,
        INSTANCE_PROFILE,
        
        // GCP
        SERVICE_ACCOUNT,
        OAUTH2,
        
        // Azure
        SERVICE_PRINCIPAL,
        MANAGED_IDENTITY,
        
        // 공통
        API_KEY,
        CERTIFICATE,
        TOKEN
    }

    // ==================== 비즈니스 메서드 ====================

    /**
     * 계정을 활성화합니다.
     */
    public void activate() {
        this.status = Status.ACTIVE;
    }

    /**
     * 계정을 일시정지합니다.
     */
    public void suspend() {
        this.status = Status.SUSPENDED;
    }

    /**
     * 계정을 만료 상태로 변경합니다.
     * (만료는 INACTIVE 상태로 매핑됩니다)
     */
    public void expire() {
        this.status = Status.INACTIVE;
    }

    /**
     * 계정이 활성화 상태인지 확인합니다.
     *
     * @return 활성화 상태이면 true
     */
    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }

    /**
     * 계정이 일시정지 상태인지 확인합니다.
     *
     * @return 일시정지 상태이면 true
     */
    public boolean isSuspended() {
        return Status.SUSPENDED.equals(this.status);
    }

    /**
     * 계정이 만료되었는지 확인합니다.
     * (만료는 INACTIVE 상태로 매핑됩니다)
     *
     * @return 만료 상태이면 true
     */
    public boolean isExpired() {
        return Status.INACTIVE.equals(this.status);
    }

    /**
     * 기본 계정으로 설정합니다.
     */
    public void setAsDefault() {
        this.isDefault = true;
    }

    /**
     * 기본 계정을 해제합니다.
     */
    public void unsetAsDefault() {
        this.isDefault = false;
    }

    /**
     * 마지막 검증 시간을 업데이트합니다.
     */
    public void updateLastVerified() {
        this.lastVerified = LocalDateTime.now();
    }

    /**
     * AWS 계정인지 확인합니다.
     *
     * @return AWS 프로바이더이면 true
     */
    public boolean isAwsAccount() {
        return provider != null && 
               CloudProvider.ProviderType.AWS.equals(provider.getProviderType());
    }

    /**
     * GCP 계정인지 확인합니다.
     *
     * @return GCP 프로바이더이면 true
     */
    public boolean isGcpAccount() {
        return provider != null && 
               CloudProvider.ProviderType.GCP.equals(provider.getProviderType());
    }

    /**
     * Azure 계정인지 확인합니다.
     *
     * @return Azure 프로바이더이면 true
     */
    public boolean isAzureAccount() {
        return provider != null && 
               CloudProvider.ProviderType.AZURE.equals(provider.getProviderType());
    }

    /**
     * 계정이 검증이 필요한지 확인합니다.
     * 마지막 검증 후 7일이 지났거나, 한 번도 검증되지 않았으면 true를 반환합니다.
     *
     * @return 검증이 필요하면 true
     */
    public boolean needsVerification() {
        if (lastVerified == null) {
            return true;
        }
        return lastVerified.isBefore(LocalDateTime.now().minusDays(7));
    }
}

