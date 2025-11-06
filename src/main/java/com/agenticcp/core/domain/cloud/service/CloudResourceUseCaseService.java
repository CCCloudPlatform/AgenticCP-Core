package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.ResourceQuery;
import com.agenticcp.core.domain.cloud.port.model.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
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
    private final CredentialProviderPort credentialProviderPort;
    private final CloudAccountRepository cloudAccountRepository;
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
        Long accountId = getAccountIdFromScope(id.getAccountScope(), id.getProviderType());
        CloudSessionCredential session = credentialProviderPort.getSession(tenantKey, accountId, id.getProviderType());
        
        // 세션을 Adapter에 전달
        router.lifecycle(id.getProviderType()).start(id, session);
        auditEventPort.record("START", "CloudResource", "SUCCESS", Map.of("id", id.getProviderResourceId()));
    }

    @Transactional
    public void stop(ResourceIdentity id, String serviceKey, String resourceType) {
        capabilityGuard.ensureSupported(id.getProviderType(), serviceKey, resourceType, CapabilityGuard.Operation.STOP);
        
        // JIT 세션 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        Long accountId = getAccountIdFromScope(id.getAccountScope(), id.getProviderType());
        CloudSessionCredential session = credentialProviderPort.getSession(tenantKey, accountId, id.getProviderType());
        
        // 세션을 Adapter에 전달
        router.lifecycle(id.getProviderType()).stop(id, session);
        auditEventPort.record("STOP", "CloudResource", "SUCCESS", Map.of("id", id.getProviderResourceId()));
    }

    @Transactional
    public void terminate(ResourceIdentity id, String serviceKey, String resourceType) {
        capabilityGuard.ensureSupported(id.getProviderType(), serviceKey, resourceType, CapabilityGuard.Operation.TERMINATE);
        
        // JIT 세션 획득
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        Long accountId = getAccountIdFromScope(id.getAccountScope(), id.getProviderType());
        CloudSessionCredential session = credentialProviderPort.getSession(tenantKey, accountId, id.getProviderType());
        
        // 세션을 Adapter에 전달
        router.lifecycle(id.getProviderType()).terminate(id, session);
        auditEventPort.record("TERMINATE", "CloudResource", "SUCCESS", Map.of("id", id.getProviderResourceId()));
    }
    
    /**
     * accountScope로 CloudAccount의 ID를 조회합니다.
     * 
     * @param accountScope 계정 범위 (AWS AccountId, Azure SubscriptionId, GCP ProjectId)
     * @param providerType 프로바이더 타입
     * @return CloudAccount ID
     */
    private Long getAccountIdFromScope(String accountScope, com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType providerType) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        return cloudAccountRepository.findByTenantKeyAndProviderType(tenantKey, providerType)
                .stream()
                .filter(account -> account.getAccountId() != null && account.getAccountId().equals(accountScope))
                .findFirst()
                .map(account -> account.getId())
                .orElseThrow(() -> new com.agenticcp.core.common.exception.BusinessException(
                    com.agenticcp.core.domain.cloud.exception.CloudErrorCode.ACCOUNT_NOT_FOUND,
                    "계정을 찾을 수 없습니다: " + accountScope
                ));
    }
}
