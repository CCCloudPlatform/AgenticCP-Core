package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.common.util.LogMaskingUtils;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.dto.PermissionResponse;
import com.agenticcp.core.domain.user.dto.RoleResponse;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import com.agenticcp.core.domain.user.service.PermissionService;
import com.agenticcp.core.domain.user.service.RoleService;
import com.agenticcp.core.domain.user.service.SystemRolePermissionInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantAuthorizationService {

    private static final String CACHE_KEY_TENANT_ROLES = "tenant_roles:";
    private static final String CACHE_KEY_TENANT_PERMISSIONS = "tenant_permissions:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final TenantService tenantService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final SystemRolePermissionInitializer systemRolePermissionInitializer;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void initializeTenantPermissions(String tenantKey) {
        Tenant tenant = tenantService.getTenantByKey(tenantKey)
                .orElseThrow(() -> new IllegalStateException("Invalid tenant key: " + tenantKey));
        log.info("[TenantAuthorizationService] initializeTenantPermissions - tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        systemRolePermissionInitializer.initializeForTenant(tenant);
        evictTenantCache(tenantKey);
    }

    public List<RoleResponse> getTenantRoles(String tenantKey) {
        String cacheKey = CACHE_KEY_TENANT_ROLES + tenantKey;
        @SuppressWarnings("unchecked")
        List<RoleResponse> cached = (List<RoleResponse>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Tenant tenant = tenantService.getTenantByKey(tenantKey)
                .orElseThrow(() -> new IllegalStateException("Invalid tenant key: " + tenantKey));
        List<Role> roles = roleRepository.findByTenantWithPermissions(tenant);
        List<RoleResponse> responses = roles.stream()
                .map(roleService::toRoleResponse)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, responses, CACHE_TTL);
        return responses;
    }

    public List<PermissionResponse> getTenantPermissions(String tenantKey) {
        String cacheKey = CACHE_KEY_TENANT_PERMISSIONS + tenantKey;
        @SuppressWarnings("unchecked")
        List<PermissionResponse> cached = (List<PermissionResponse>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Permission> permissions = permissionRepository.findByTenantKey(tenantKey);
        List<PermissionResponse> responses = permissions.stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, responses, CACHE_TTL);
        return responses;
    }

    @Transactional
    public void evictTenantCache(String tenantKey) {
        String rolesKey = CACHE_KEY_TENANT_ROLES + tenantKey;
        String permissionsKey = CACHE_KEY_TENANT_PERMISSIONS + tenantKey;
        redisTemplate.delete(rolesKey);
        redisTemplate.delete(permissionsKey);
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .permissionKey(permission.getPermissionKey())
                .permissionName(permission.getPermissionName())
                .description(permission.getDescription())
                .tenantKey(permission.getTenant() != null ? permission.getTenant().getTenantKey() : null)
                .tenantName(permission.getTenant() != null ? permission.getTenant().getTenantName() : null)
                .status(permission.getStatus())
                .resource(permission.getResource())
                .action(permission.getAction())
                .isSystem(permission.getIsSystem())
                .category(permission.getCategory())
                .priority(permission.getPriority())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .createdBy(permission.getCreatedBy())
                .updatedBy(permission.getUpdatedBy())
                .build();
    }
}


