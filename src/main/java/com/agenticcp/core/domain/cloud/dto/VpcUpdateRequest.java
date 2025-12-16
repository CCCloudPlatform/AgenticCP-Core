package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * VPC 수정 요청 모델
 * 
 * VPC 수정을 위한 표준화된 요청 모델
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Value
@Builder
public class VpcUpdateRequest {
    ProviderType providerType;
    String accountScope;
    String region;
    String vpcName;
    String description;
    Map<String, String> tags;
    String tenantKey;
    Map<String, Object> providerSpecificConfig;
}
