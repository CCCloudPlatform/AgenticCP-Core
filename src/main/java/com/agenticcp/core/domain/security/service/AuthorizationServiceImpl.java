package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.user.service.PermissionService;
import com.agenticcp.core.domain.user.service.RoleService;
import com.agenticcp.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    @Override
    public boolean hasPermission(String username, String permissionKey) {
        return getUserPermissions(username).contains(permissionKey);
    }

    @Override
    public boolean hasRole(String username, String roleKey) {
        return userService.getUserByUsernameOrThrow(username)
                .getRoles().stream()
                .anyMatch(role -> role.getRoleKey().equals(roleKey));
    }

    @Override
    public boolean hasAnyRole(String username, String... roleKeys) {
        Set<String> userRoleKeys = userService.getUserByUsernameOrThrow(username)
                .getRoles().stream()
                .map(role -> role.getRoleKey())
                .collect(java.util.stream.Collectors.toSet());
        return Arrays.stream(roleKeys).anyMatch(userRoleKeys::contains);
    }

    @Override
    public boolean hasAllRoles(String username, String... roleKeys) {
        Set<String> userRoleKeys = userService.getUserByUsernameOrThrow(username)
                .getRoles().stream()
                .map(role -> role.getRoleKey())
                .collect(java.util.stream.Collectors.toSet());
        return Arrays.stream(roleKeys).allMatch(userRoleKeys::contains);
    }

    @Override
    public boolean hasPermissionForResource(String username, String resource, String action) {
        return getUserPermissions(username).stream().anyMatch(permissionKey -> {
            Optional<com.agenticcp.core.domain.user.entity.Permission> optional = permissionService.getPermissionByKey(permissionKey);
            var permission = optional.orElse(null);
            return permission != null
                    && resource.equals(permission.getResource())
                    && action.equals(permission.getAction());
        });
    }

    @Override
    public void validateTenantAccess(String username, String tenantKey) {
        var user = userService.getUserByUsernameOrThrow(username);
        if (!user.getTenant().getTenantKey().equals(tenantKey)) {
            throw new AccessDeniedException("해당 테넌트에 대한 접근 권한이 없습니다");
        }
    }

    @Override
    public void evictUserPermissionCache(String username) {
        // no-op placeholder for now; will be implemented with cache backend
    }

    @Override
    public void warmUserPermissionCache(String username) {
        // no-op placeholder for now; will be implemented with cache backend
        getUserPermissions(username);
    }

    @Override
    public Set<String> getUserPermissions(String username) {
        var user = userService.getUserByUsernameOrThrow(username);
        Set<String> permissions = new HashSet<>();

        user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getPermissionKey())));
        user.getPermissions().forEach(p -> permissions.add(p.getPermissionKey()));

        return permissions;
    }
}


