package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.ResourceQuery;
import com.agenticcp.core.domain.cloud.port.model.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CloudResourceUseCaseService {

    private final ResourcePortRouter router;
    private final CapabilityGuard capabilityGuard;
    private final AccountCredentialManagementPort accountCredentialManagementPort;
    private final AuditEventPort auditEventPort;
    private final TracingPort tracingPort;

    public Page<CloudResource> list(ResourceQuery query) {
        try (AutoCloseable span = tracingPort.startSpan("cloud.listResources", Map.of(
                "provider", query.getProviderType().name()))) {
            Page<CloudResource> page = router.discovery(query.getProviderType()).listResources(query);
            auditEventPort.record("LIST_RESOURCES", "CloudResource", "SUCCESS",
                    Map.of("provider", query.getProviderType().name(), "count", page.getTotalElements()));
            return page;
        } catch (RuntimeException ex) {
            auditEventPort.record("LIST_RESOURCES", "CloudResource", "FAILURE",
                    Map.of("provider", query.getProviderType().name(), "error", ex.getMessage()));
            throw ex;
        } catch (Exception ex) {
            auditEventPort.record("LIST_RESOURCES", "CloudResource", "FAILURE",
                    Map.of("provider", query.getProviderType().name(), "error", ex.getMessage()));
            throw new RuntimeException(ex);
        }
    }

    @Transactional
    public void start(ResourceIdentity id, String serviceKey, String resourceType) {
        capabilityGuard.ensureSupported(id.getProviderType(), serviceKey, resourceType, CapabilityGuard.Operation.START);
        
        // JIT 세션 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(tenantKey, id.getAccountScope(), id.getProviderType());
        
        // 세션을 Adapter에 전달
        router.lifecycle(id.getProviderType()).start(id, session);
        auditEventPort.record("START", "CloudResource", "SUCCESS", Map.of("id", id.getProviderResourceId()));
    }

    @Transactional
    public void stop(ResourceIdentity id, String serviceKey, String resourceType) {
        capabilityGuard.ensureSupported(id.getProviderType(), serviceKey, resourceType, CapabilityGuard.Operation.STOP);
        
        // JIT 세션 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(tenantKey, id.getAccountScope(), id.getProviderType());
        
        // 세션을 Adapter에 전달
        router.lifecycle(id.getProviderType()).stop(id, session);
        auditEventPort.record("STOP", "CloudResource", "SUCCESS", Map.of("id", id.getProviderResourceId()));
    }

    @Transactional
    public void terminate(ResourceIdentity id, String serviceKey, String resourceType) {
        capabilityGuard.ensureSupported(id.getProviderType(), serviceKey, resourceType, CapabilityGuard.Operation.TERMINATE);
        
        // JIT 세션 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = accountCredentialManagementPort.getSession(tenantKey, id.getAccountScope(), id.getProviderType());
        
        // 세션을 Adapter에 전달
        router.lifecycle(id.getProviderType()).terminate(id, session);
        auditEventPort.record("TERMINATE", "CloudResource", "SUCCESS", Map.of("id", id.getProviderResourceId()));
    }
}
