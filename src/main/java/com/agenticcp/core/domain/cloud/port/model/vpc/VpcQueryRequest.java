package com.agenticcp.core.domain.cloud.port.model.vpc;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * VPC 조회 쿼리 모델
 * 
 * VPC 목록 조회를 위한 표준화된 쿼리 모델
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Value
@Builder
public class VpcQueryRequest {
    CloudProvider.ProviderType providerType;
    String accountScope;
    String region;
    String vpcName;
    String cidrBlock;
    Map<String, String> tags;
    String tenantKey;
    Integer page;
    Integer size;
    String sortBy;
    String sortDirection;
}
