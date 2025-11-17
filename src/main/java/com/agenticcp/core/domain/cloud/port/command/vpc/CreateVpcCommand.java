package com.agenticcp.core.domain.cloud.port.command.vpc;

import java.util.Map;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.CloudSessionCredential;

import lombok.Builder;

/**
 * VPC 생성 시 아웃바운드 포트로 전달되는 명령 모델.
 */
@Builder
public record CreateVpcCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String serviceKey,
        String resourceType,
        String vpcName,
        String cidrBlock,
        String description,
        Map<String, String> tags,
        String tenantKey,
        Map<String, Object> providerSpecificConfig,
        CloudSessionCredential session
) {
}

