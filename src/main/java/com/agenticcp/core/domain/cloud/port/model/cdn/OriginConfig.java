package com.agenticcp.core.domain.cloud.port.model.cdn;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Builder;

/**
 * CDN Origin 설정 VO
 * 
 * Distribution의 Origin 설정을 표현합니다.
 * PUBLIC_S3, CUSTOM(ELB, EC2 등) Origin 타입을 지원합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record OriginConfig(
    @JsonProperty("id")
    @NotBlank(message = "Origin ID는 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Origin ID는 영문자, 숫자, 하이픈(-), 언더스코어(_)만 사용할 수 있습니다")
    String id,
    
    @JsonProperty("domainName")
    @NotBlank(message = "Origin 도메인 이름은 필수입니다")
    @Pattern(
            regexp = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$|^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$",
            message = "유효한 도메인 형식이어야 합니다"
    )
    String domainName,
    
    @JsonProperty("type")
    @NotNull(message = "Origin 타입은 필수입니다")
    OriginType type,
    
    // Custom Origin 설정 (ELB, EC2 등)
    @JsonProperty("httpPort")
    @Min(value = 1, message = "HTTP 포트는 1 이상이어야 합니다")
    @Max(value = 65535, message = "HTTP 포트는 65535 이하여야 합니다")
    Integer httpPort,  // 기본 80
    
    @JsonProperty("httpsPort")
    @Min(value = 1, message = "HTTPS 포트는 1 이상이어야 합니다")
    @Max(value = 65535, message = "HTTPS 포트는 65535 이하여야 합니다")
    Integer httpsPort,  // 기본 443
    
    @JsonProperty("originProtocolPolicy")
    @jakarta.validation.constraints.Size(max = 50, message = "Origin Protocol Policy는 최대 50자까지 가능합니다")
    String originProtocolPolicy  // CSP별로 다른 값 사용 가능 (예: AWS: http-only/https-only/match-viewer, GCP: HTTP_ONLY/HTTPS_ONLY 등)
) {
    
    /**
     * Origin 타입
     */
    public enum OriginType {
        PUBLIC_S3,    // 퍼블릭 S3 버킷
        CUSTOM        // 커스텀 Origin (ELB, EC2, 외부 웹서버 등)
    }
}

