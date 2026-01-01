package com.agenticcp.core.domain.tenant.cloud.service.isolation;

import com.agenticcp.core.domain.tenant.adapter.dto.IsolationResult;
import com.agenticcp.core.domain.tenant.cloud.service.resource.CloudResourceCreator;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;

public interface IsolationStrategy {

    boolean supports(TenantIsolation.IsolationLevel isolationLevel);

    IsolationResult applyIsolation(String tenantKey, TenantIsolation.IsolationLevel isolation, CloudResourceCreator resourceCreator);

}
