package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantAwareAuthorizationService {

    private final TenantService tenantService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public boolean hasRoleInTenant(String tenantKey, String roleKey) {
        Tenant currentTenant = getAndValidateTenantContext(tenantKey);
        return roleRepository.existsByRoleKeyAndTenant(roleKey, currentTenant);
    }

    public boolean hasPermissionInTenant(String tenantKey, String permissionKey) {
        Tenant currentTenant = getAndValidateTenantContext(tenantKey);
        return permissionRepository.existsByPermissionKeyAndTenant(permissionKey, currentTenant);
    }

    private Tenant getAndValidateTenantContext(String tenantKey) {
        Tenant contextTenant = TenantContextHolder.getCurrentTenant();
        if (contextTenant == null || contextTenant.getTenantKey() == null) {
            throw new IllegalStateException("Tenant context is not set");
        }
        if (!contextTenant.getTenantKey().equals(tenantKey)) {
            throw new IllegalStateException("Tenant context mismatch");
        }
        return tenantService.getTenantByKey(tenantKey)
                .orElseThrow(() -> new IllegalStateException("Invalid tenant key: " + tenantKey));
    }
}


