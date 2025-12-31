package com.agenticcp.core.domain.cloud.port.model.cdn;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * CDN 캐시 동작 설정 VO
 * 
 * Distribution의 캐시 동작(Cache Behavior) 설정을 표현합니다.
 * 경로 패턴별로 캐시 TTL, 허용 메서드, 압축 등을 설정할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record CacheBehaviorConfig(
    @JsonProperty("pathPattern")
    @Pattern(
            regexp = "^/.*|^\\*$",
            message = "경로 패턴은 '/'로 시작하거나 '*'이어야 합니다"
    )
    String pathPattern,
    
    @JsonProperty("ttl")
    @Min(value = 0, message = "TTL은 0 이상이어야 합니다")
    Long ttl,  // Time to Live (초)
    
    @JsonProperty("allowedMethods")
    @Size(min = 1, message = "최소 하나 이상의 HTTP 메서드가 필요합니다")
    List<@Pattern(
            regexp = "^(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH)$",
            message = "허용된 HTTP 메서드는 GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH 중 하나여야 합니다"
    ) String> allowedMethods,  // GET, POST, PUT, DELETE 등
    
    @JsonProperty("compress")
    Boolean compress,  // Gzip 압축 여부
    
    @JsonProperty("viewerProtocolPolicy")
    @jakarta.validation.constraints.Size(max = 50, message = "Viewer Protocol Policy는 최대 50자까지 가능합니다")
    String viewerProtocolPolicy,  // CSP별로 다른 값 사용 가능 (예: AWS: allow-all/https-only/redirect-to-https, GCP: ALLOW_ALL/HTTPS_ONLY 등)
    
    @JsonProperty("cachePolicyId")
    @jakarta.validation.constraints.Size(max = 255, message = "Cache Policy ID는 최대 255자까지 가능합니다")
    String cachePolicyId,  // CSP별 Managed Cache Policy ID (선택, AWS/GCP/Azure 각각 다른 형식)
    
    @JsonProperty("originRequestPolicyId")
    @jakarta.validation.constraints.Size(max = 255, message = "Origin Request Policy ID는 최대 255자까지 가능합니다")
    String originRequestPolicyId  // CSP별 Managed Origin Request Policy ID (선택, AWS/GCP/Azure 각각 다른 형식)
) {
}

