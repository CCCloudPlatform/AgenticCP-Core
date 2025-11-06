package com.agenticcp.core.domain.cloud.port.model;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * AWS STS 세션 자격증명
 * 
 * AWS STS AssumeRole 또는 GetSessionToken으로 발급받은 임시 자격증명을 담습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Value
@Builder
public class AwsSessionCredential implements CloudSessionCredential {
    
    /**
     * AWS Access Key ID (임시)
     */
    @JsonProperty("accessKeyId")
    String accessKeyId;
    
    /**
     * AWS Secret Access Key (임시)
     */
    @JsonProperty("secretAccessKey")
    String secretAccessKey;
    
    /**
     * AWS Session Token
     */
    @JsonProperty("sessionToken")
    String sessionToken;
    
    /**
     * 리전
     */
    @JsonProperty("region")
    String region;
    
    /**
     * 세션 만료 시간
     */
    @JsonProperty("expiresAt")
    LocalDateTime expiresAt;
    
    /**
     * ARN (AssumeRole의 경우)
     */
    @JsonProperty("arn")
    String arn;
    
    @JsonCreator
    public AwsSessionCredential(
            @JsonProperty("accessKeyId") String accessKeyId,
            @JsonProperty("secretAccessKey") String secretAccessKey,
            @JsonProperty("sessionToken") String sessionToken,
            @JsonProperty("region") String region,
            @JsonProperty("expiresAt") LocalDateTime expiresAt,
            @JsonProperty("arn") String arn) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = sessionToken;
        this.region = region;
        this.expiresAt = expiresAt;
        this.arn = arn;
    }
    
    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
