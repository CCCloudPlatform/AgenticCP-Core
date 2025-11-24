package com.agenticcp.core.domain.cloud.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
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
 * 클라우드 프로바이더 엔티티
 * <p>
 * 다양한 클라우드 프로바이더(AWS, Azure, GCP 등)의 정보를 관리하는 엔티티입니다.
 * 프로바이더별 지원 리전, 서비스, 인증 방식, 가격 모델 등의 정보를 저장합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Entity
@Table(name = "cloud_providers", indexes = {
    @Index(name = "idx_cloud_providers_provider_key", columnList = "provider_key"),
    @Index(name = "idx_cloud_providers_provider_type", columnList = "provider_type"),
    @Index(name = "idx_cloud_providers_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudProvider extends BaseEntity {

    /**
     * 프로바이더 키 (고유 식별자)
     */
    @NotBlank(message = "프로바이더 키는 필수입니다")
    @Size(max = 100, message = "프로바이더 키는 100자를 초과할 수 없습니다")
    @Column(name = "provider_key", nullable = false, unique = true, length = 100)
    private String providerKey;

    /**
     * 프로바이더 이름
     */
    @NotBlank(message = "프로바이더 이름은 필수입니다")
    @Size(max = 255, message = "프로바이더 이름은 255자를 초과할 수 없습니다")
    @Column(name = "provider_name", nullable = false, length = 255)
    private String providerName;

    /**
     * 프로바이더 설명
     */
    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type")
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Column(name = "api_version")
    private String apiVersion;

    @Column(name = "authentication_type")
    @Enumerated(EnumType.STRING)
    private AuthenticationType authenticationType;

    @Column(name = "supported_regions", columnDefinition = "TEXT")
    private String supportedRegions; // JSON array of supported regions

    @Column(name = "supported_services", columnDefinition = "TEXT")
    private String supportedServices; // JSON array of supported services

    @Column(name = "pricing_model")
    @Enumerated(EnumType.STRING)
    private PricingModel pricingModel;

    @Column(name = "is_global")
    @Builder.Default
    private Boolean isGlobal = false;

    @Column(name = "is_government")
    @Builder.Default
    private Boolean isGovernment = false;

    @Column(name = "compliance_certifications", columnDefinition = "TEXT")
    private String complianceCertifications; // JSON array of compliance certifications

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional provider metadata

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    /**
     * 클라우드 프로바이더 타입 열거형
     */
    public enum ProviderType {
        AWS,
        AZURE,
        GCP,
        ALIBABA_CLOUD,
        IBM_CLOUD,
        ORACLE_CLOUD,
        VMWARE,
        OPENSTACK,
        KUBERNETES,
        DOCKER,
        ON_PREMISE
    }

    /**
     * 인증 타입 열거형
     */
    public enum AuthenticationType {
        API_KEY,
        OAUTH2,
        IAM_ROLE,
        SERVICE_ACCOUNT,
        CERTIFICATE,
        TOKEN
    }

    /**
     * 가격 모델 열거형
     */
    public enum PricingModel {
        PAY_AS_YOU_GO,
        RESERVED_INSTANCE,
        SPOT_INSTANCE,
        SAVINGS_PLANS,
        COMMITTED_USE,
        PREPAID
    }
}
