package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.cdn.CacheBehaviorConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * CDN Distribution 수정 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CDNDistributionUpdateRequest {

    /**
     * 클라우드 프로바이더 타입
     * Controller에서 PathVariable로 주입됩니다.
     */
    private CloudProvider.ProviderType providerType;
    
    /**
     * 계정 스코프
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String accountScope;

    /**
     * Distribution 설명
     * 최대 128자
     */
    @Size(max = 128, message = "설명은 최대 128자까지 가능합니다")
    private String comment;

    /**
     * Distribution 활성화 여부
     */
    private Boolean enabled;

    /**
     * Cache Behavior 설정 목록
     */
    @Valid
    private List<CacheBehaviorConfig> cacheBehaviors;

    /**
     * CNAME 목록 (커스텀 도메인)
     * 최대 100개, 각각 유효한 도메인 형식이어야 함
     */
    @Size(max = 100, message = "CNAME 목록은 최대 100개까지 가능합니다")
    private List<@jakarta.validation.constraints.Pattern(
            regexp = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$",
            message = "유효한 도메인 형식이어야 합니다"
    ) String> aliases;

    /**
     * SSL/TLS 인증서 ID
     * CSP별로 다른 형식 사용 가능:
     * - AWS: ACM ARN (arn:aws:acm:region:account:certificate/certificate-id) 또는 IAM Certificate ID
     * - GCP: Certificate Manager 리소스 이름
     * - Azure: Key Vault 인증서 ID
     */
    @jakarta.validation.constraints.Size(max = 512, message = "SSL 인증서 ID는 최대 512자까지 가능합니다")
    private String sslCertificateId;

    /**
     * 태그
     */
    private Map<String, String> tags;
}

