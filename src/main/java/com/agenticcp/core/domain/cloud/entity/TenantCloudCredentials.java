package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 테넌트별 클라우드 자격증명 엔티티
 * 
 * 멀티 테넌트 환경에서 각 테넌트의 클라우드 프로바이더별 자격증명 정보를 관리합니다.
 * STS AssumeRole 패턴을 사용하여 IAM Role ARN을 저장합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Entity
@Table(name = "tenant_cloud_credentials", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_key", "provider_type", "account_scope"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantCloudCredentials extends BaseEntity {
    
    /**
     * 테넌트 키
     */
    @Column(name = "tenant_key", nullable = false, length = 100)
    private String tenantKey;
    
    /**
     * 클라우드 프로바이더 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    private CloudProvider.ProviderType providerType;
    
    /**
     * 계정 스코프 (AWS Account ID, Azure Subscription ID, GCP Project ID 등)
     */
    @Column(name = "account_scope", nullable = false, length = 100)
    private String accountScope;
    
    /**
     * IAM Role ARN (AWS AssumeRole용)
     */
    @Column(name = "role_arn", length = 500)
    private String roleArn;
    
    /**
     * 외부 ID (AssumeRole 시 추가 보안용)
     */
    @Column(name = "external_id", length = 100)
    private String externalId;
    
    /**
     * 세션 이름 (AssumeRole 시 식별용)
     */
    @Column(name = "session_name", length = 100)
    private String sessionName;
    
    /**
     * 자격증명 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * 마지막 사용 시간
     */
    @Column(name = "last_used_at")
    private java.time.LocalDateTime lastUsedAt;
    
    /**
     * 마지막 오류 메시지
     */
    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;
    
    @Builder
    public TenantCloudCredentials(String tenantKey, 
                                CloudProvider.ProviderType providerType, 
                                String accountScope,
                                String roleArn, 
                                String externalId, 
                                String sessionName,
                                Boolean isActive) {
        this.tenantKey = tenantKey;
        this.providerType = providerType;
        this.accountScope = accountScope;
        this.roleArn = roleArn;
        this.externalId = externalId;
        this.sessionName = sessionName;
        this.isActive = isActive != null ? isActive : true;
    }
    
    /**
     * 자격증명 사용 기록 업데이트
     */
    public void updateLastUsed() {
        this.lastUsedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * 오류 정보 업데이트
     */
    public void updateError(String errorMessage) {
        this.lastErrorMessage = errorMessage;
        this.lastUsedAt = java.time.LocalDateTime.now();
    }
    
    /**
     * 자격증명 활성화
     */
    public void activate() {
        this.isActive = true;
        this.lastErrorMessage = null;
    }
    
    /**
     * 자격증명 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * AWS AssumeRole용 캐시 키 생성
     */
    public String generateCacheKey() {
        return String.format("%s:%s:%s", tenantKey, providerType, accountScope);
    }
    
    /**
     * AWS AssumeRole용 세션 이름 생성 (기본값)
     */
    public String getDefaultSessionName() {
        if (sessionName != null && !sessionName.isEmpty()) {
            return sessionName;
        }
        return String.format("AgenticCP-%s-%s", tenantKey, accountScope);
    }
}
