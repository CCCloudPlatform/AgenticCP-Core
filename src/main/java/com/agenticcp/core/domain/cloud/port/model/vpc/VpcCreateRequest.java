package com.agenticcp.core.domain.cloud.port.model.vpc;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * VPC 생성 요청 모델
 * 
 * 모든 클라우드 프로바이더에서 VPC 생성을 위한 표준화된 요청 모델
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Value
@Builder
public class VpcCreateRequest {
    ProviderType providerType;
    String accountScope;
    String region;
    String vpcName;
    String cidrBlock;
    String description;
    Map<String, String> tags;
    String tenantKey;
    Map<String, Object> providerSpecificConfig;
}
