package com.agenticcp.core.domain.tenant.cloud.service.isolation;

import com.agenticcp.core.domain.tenant.adapter.dto.IsolationResult;
import com.agenticcp.core.domain.tenant.cloud.dto.CloudResourceResult;
import com.agenticcp.core.domain.tenant.cloud.dto.ResourceCreateRequest;
import com.agenticcp.core.domain.tenant.cloud.service.resource.CloudResourceCreator;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SharedIsolationStrategy implements IsolationStrategy{

    @Override
    public boolean supports(TenantIsolation.IsolationLevel isolationLevel) {
        return isolationLevel == TenantIsolation.IsolationLevel.SHARED;
    }

    @Override
    public IsolationResult applyIsolation(String tenantKey, TenantIsolation.IsolationLevel isolationLevel, CloudResourceCreator resourceCreator) {

        // VPC 요청 생성
        ResourceCreateRequest vpcRequest = ResourceCreateRequest.builder()
                .metadata(Map.of(
                        "tenantKey", tenantKey,
                        "isolationLevel", "SHARED"))
                .build();

        // ResourceCreator 에게 VPC 리소스 생성 위임
        CloudResourceResult vpcResult = resourceCreator.createVpc(tenantKey, vpcRequest, isolationLevel.SHARED);

        // 결과값(TODO: subnet, sg 생성 결과 포함)
        Map<String, Object> resourceDetails = Map.of(
                "vpcId", vpcResult.resourceId()
        );

        return IsolationResult.success(tenantKey, isolationLevel, resourceDetails);

    }
}
