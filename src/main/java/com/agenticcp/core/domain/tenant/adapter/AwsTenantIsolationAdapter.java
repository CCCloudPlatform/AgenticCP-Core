package com.agenticcp.core.domain.tenant.adapter;

import com.agenticcp.core.domain.tenant.adapter.dto.IsolationResult;
import com.agenticcp.core.domain.tenant.adapter.dto.IsolationStatus;
import com.agenticcp.core.domain.tenant.cloud.CloudProviderType;
import com.agenticcp.core.domain.tenant.cloud.service.resource.AwsCloudResourceCreator;
import com.agenticcp.core.domain.tenant.cloud.service.isolation.IsolationStrategy;
import com.agenticcp.core.domain.tenant.cloud.service.isolation.IsolationStrategyFactory;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AwsTenantIsolationAdapter implements TenantIsolationAdapter{

    private final IsolationStrategyFactory strategyFactory;
    private final AwsCloudResourceCreator resourceCreator;

    @Override
    public IsolationResult applyIsolationPolicy(String tenantKey, TenantIsolation isolation) {
        // Strategy(격리 수준) 가져오기
        IsolationStrategy strategy = strategyFactory.getStrategy(isolation.getIsolationLevel());

        // Strategy 에 ResourceCreator 전달
        return strategy.applyIsolation(tenantKey, isolation.getIsolationLevel(), resourceCreator);

    }

    @Override
    public void removeIsolationPolicy(String tenantKey) {

    }
    @Override
    public IsolationStatus getIsolationStatus(String tenantKey) {
        return null;
    }
    @Override
    public boolean supportsIsolationLevel(TenantIsolation.IsolationLevel isolationLevel) {
        return isolationLevel == TenantIsolation.IsolationLevel.SHARED;
    }

    @Override
    public CloudProviderType getSupportedCloudProvider(){
        return CloudProviderType.AWS;
    }
}
