package com.agenticcp.core.domain.tenant.cloud.service;


import com.agenticcp.core.domain.tenant.cloud.CloudProviderType;
import com.agenticcp.core.domain.tenant.cloud.dto.CloudResourceResult;
import com.agenticcp.core.domain.tenant.cloud.dto.ResourceCreateRequest;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;

public interface CloudResourceCreator {

    CloudProviderType getCloudProviderType();

    CloudResourceResult createVpc(String tenantKey, ResourceCreateRequest request, TenantIsolation.IsolationLevel level);

    CloudResourceResult createSubnets(String tenantKey, ResourceCreateRequest request, TenantIsolation.IsolationLevel level);

    CloudResourceResult createSecurityGroups(String tenantKey, ResourceCreateRequest request, TenantIsolation.IsolationLevel level);





}
