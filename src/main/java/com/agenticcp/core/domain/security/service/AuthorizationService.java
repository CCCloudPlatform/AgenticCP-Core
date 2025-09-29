package com.agenticcp.core.domain.security.service;

import java.util.Set;

public interface AuthorizationService {

    boolean hasPermission(String username, String permissionKey);

    boolean hasRole(String username, String roleKey);

    boolean hasAnyRole(String username, String... roleKeys);

    boolean hasAllRoles(String username, String... roleKeys);

    boolean hasPermissionForResource(String username, String resource, String action);

    void validateTenantAccess(String username, String tenantKey);

    void evictUserPermissionCache(String username);

    void warmUserPermissionCache(String username);

    Set<String> getUserPermissions(String username);
}


