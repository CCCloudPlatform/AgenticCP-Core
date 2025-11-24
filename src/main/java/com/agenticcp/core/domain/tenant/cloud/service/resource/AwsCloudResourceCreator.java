package com.agenticcp.core.domain.tenant.cloud.service.resource;

import com.agenticcp.core.domain.tenant.cloud.dto.CloudResourceResult;
import com.agenticcp.core.domain.tenant.cloud.dto.ResourceCreateRequest;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

// 샘플
@Component
public class AwsCloudResourceCreator implements CloudResourceCreator {

    @Override
    public com.agenticcp.core.domain.tenant.cloud.CloudProviderType getCloudProviderType() {
        return com.agenticcp.core.domain.tenant.cloud.CloudProviderType.AWS;
    }

    @Override
    public CloudResourceResult createVpc(String tenantKey, ResourceCreateRequest request, TenantIsolation.IsolationLevel level) {
        // Strategy 의 추상적 요청을 AWS 형식으로 변환
        Map<String, Object> awsMetadata = getAwsMetadata(request);

        // TODO: 실제 AWS API 호출 구현
        return null;
    }

    @Override
    public CloudResourceResult createSubnets(String tenantKey, ResourceCreateRequest request, TenantIsolation.IsolationLevel level) {
        // TODO: 실제 AWS API 호출 구현
        return null;
    }

    @Override
    public CloudResourceResult createSecurityGroups(String tenantKey, ResourceCreateRequest request, TenantIsolation.IsolationLevel level) {
        // TODO: 실제 AWS API 호출 구현
        return null;
    }

    private Map<String, Object> getAwsMetadata(ResourceCreateRequest request) {
        return new HashMap<>(request.metadata());
    }
}
