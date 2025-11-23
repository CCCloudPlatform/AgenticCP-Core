package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.cloud.enums.AccountStatus;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 클라우드 계정 엔티티
 * 테넌트별 클라우드 프로바이더 계정 정보를 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Entity
@Table(name = "cloud_accounts", indexes = {
    @Index(name = "idx_cloud_accounts_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cloud_accounts_provider", columnList = "provider_id"),
    @Index(name = "idx_cloud_accounts_status", columnList = "account_status"),
    @Index(name = "idx_cloud_accounts_tenant_provider", columnList = "tenant_id, provider_id"),
    @Index(name = "idx_cloud_accounts_account_scope", columnList = "account_scope")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudAccount extends BaseEntity {

    /**
     * 소속 테넌트
     */
    @NotNull(message = "테넌트는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * 클라우드 프로바이더
     */
    @NotNull(message = "프로바이더는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private CloudProvider provider;

    /**
     * 계정 이름 (사용자 지정)
     */
    @NotBlank(message = "계정 이름은 필수입니다")
    @Size(min = 2, max = 100, message = "계정 이름은 2-100자 사이여야 합니다")
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    /**
     * 계정 범위 (Account Scope)
     * AWS: Account ID, Azure: Subscription ID, GCP: Project ID
     */
    @Column(name = "account_scope", length = 100)
    private String accountScope;

    /**
     * 자격증명 참조
     * OneToOne 관계로 암호화된 자격증명 저장
     */
    @NotNull(message = "자격증명은 필수입니다")
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "credential_id", nullable = false, unique = true)
    private CloudAccountCredential credential;

    /**
     * 계정 상태
     */
    @NotNull(message = "계정 상태는 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    /**
     * 기본 계정 여부
     * 프로바이더 타입별로 하나의 기본 계정 지정 가능
     */
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * 검증 완료 시간
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * 마지막 동기화 시간
     */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    /**
     * 추가 메타데이터 (JSON)
     * 리전, 계정 타입 등 추가 정보
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // 비즈니스 메서드

    /**
     * 계정을 활성화합니다.
     */
    public void activate() {
        this.accountStatus = AccountStatus.ACTIVE;
    }

    /**
     * 계정을 비활성화합니다.
     */
    public void deactivate() {
        this.accountStatus = AccountStatus.INACTIVE;
    }

    /**
     * 계정을 일시 중지합니다.
     */
    public void suspend() {
        this.accountStatus = AccountStatus.SUSPENDED;
    }

    /**
     * 계정이 활성 상태인지 확인합니다.
     * 
     * @return 활성 상태이면 true
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(this.accountStatus) || 
               AccountStatus.VERIFIED.equals(this.accountStatus);
    }

    /**
     * 계정 검증을 완료합니다.
     */
    public void markAsVerified() {
        this.accountStatus = AccountStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * 계정 검증 실패를 표시합니다.
     */
    public void markAsFailed() {
        this.accountStatus = AccountStatus.FAILED;
    }

    /**
     * 기본 계정으로 설정합니다.
     */
    public void setAsDefault() {
        this.isDefault = true;
    }

    /**
     * 기본 계정 설정을 해제합니다.
     */
    public void unsetAsDefault() {
        this.isDefault = false;
    }

    /**
     * 계정 정보를 동기화합니다.
     */
    public void updateLastSyncTime() {
        this.lastSyncAt = LocalDateTime.now();
    }
}

