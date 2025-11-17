package com.agenticcp.core.domain.security.service;

import java.util.Set;

/**
 * 권한 검증 서비스 인터페이스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-10
 */
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


