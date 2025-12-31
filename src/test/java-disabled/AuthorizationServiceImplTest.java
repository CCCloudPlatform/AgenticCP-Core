package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.PermissionService;
import com.agenticcp.core.domain.user.service.RoleService;
import com.agenticcp.core.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AuthorizationServiceImpl authorizationService;

    @Nested
    @DisplayName("hasPermission")
    class HasPermissionTest {
        @Test
        @DisplayName("사용자의 역할/직접 권한에 키가 존재하면 true 반환")
        void hasPermission_Exists_ReturnsTrue() {
            String username = "alice";
            User user = mock(User.class);
            Role role = mock(Role.class);
            Permission permRole = mock(Permission.class);

            when(userService.getUserByUsernameOrThrow(username)).thenReturn(user);
            when(user.getRoles()).thenReturn(List.of(role));
            when(role.getPermissions()).thenReturn(List.of(permRole));
            when(permRole.getPermissionKey()).thenReturn("user.read");
            when(user.getPermissions()).thenReturn(List.of());

            boolean result = authorizationService.hasPermission(username, "user.read");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("사용자의 권한에 키가 없으면 false 반환")
        void hasPermission_NotExists_ReturnsFalse() {
            String username = "bob";
            User user = mock(User.class);
            when(userService.getUserByUsernameOrThrow(username)).thenReturn(user);
            when(user.getRoles()).thenReturn(List.of());
            when(user.getPermissions()).thenReturn(List.of());

            boolean result = authorizationService.hasPermission(username, "user.write");
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hasRole / hasAnyRole / hasAllRoles")
    class RoleChecksTest {
        @Test
        void hasRole_UserHasRole_ReturnsTrue() {
            String username = "alice";
            User user = mock(User.class);
            Role role = mock(Role.class);
            when(userService.getUserByUsernameOrThrow(username)).thenReturn(user);
            when(user.getRoles()).thenReturn(List.of(role));
            when(role.getRoleKey()).thenReturn("ADMIN");

            boolean result = authorizationService.hasRole(username, "ADMIN");
            assertThat(result).isTrue();
        }

        @Test
        void hasAnyRole_AnyMatches_ReturnsTrue() {
            String username = "alice";
            User user = mock(User.class);
            Role role = mock(Role.class);
            when(userService.getUserByUsernameOrThrow(username)).thenReturn(user);
            when(user.getRoles()).thenReturn(List.of(role));
            when(role.getRoleKey()).thenReturn("MANAGER");

            boolean result = authorizationService.hasAnyRole(username, "ADMIN", "MANAGER");
            assertThat(result).isTrue();
        }

        @Test
        void hasAllRoles_AllMatch_ReturnsTrue() {
            String username = "alice";
            User user = mock(User.class);
            Role role1 = mock(Role.class);
            Role role2 = mock(Role.class);
            when(userService.getUserByUsernameOrThrow(username)).thenReturn(user);
            when(user.getRoles()).thenReturn(List.of(role1, role2));
            when(role1.getRoleKey()).thenReturn("ADMIN");
            when(role2.getRoleKey()).thenReturn("AUDITOR");

            boolean result = authorizationService.hasAllRoles(username, "ADMIN", "AUDITOR");
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("hasPermissionForResource")
    class HasPermissionForResourceTest {
        @Test
        void hasPermissionForResource_Match_ReturnsTrue() {
            String username = "alice";
            User user = mock(User.class);
            Permission permDirect = mock(Permission.class);
            Permission permMeta = mock(Permission.class);

            when(userService.getUserByUsernameOrThrow(username)).thenReturn(user);
            when(user.getRoles()).thenReturn(List.of());
            when(user.getPermissions()).thenReturn(List.of(permDirect));
            when(permDirect.getPermissionKey()).thenReturn("user.read");

            when(permissionService.getPermissionByKey("user.read")).thenReturn(Optional.of(permMeta));
            when(permMeta.getResource()).thenReturn("user");
            when(permMeta.getAction()).thenReturn("read");

            boolean result = authorizationService.hasPermissionForResource(username, "user", "read");
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("validateTenantAccess")
    class ValidateTenantAccessTest {
        @Test
        void validateTenantAccess_TenantMatches_DoesNotThrow() {
            String username = "alice";
            User user = mock(User.class);
            Tenant tenant = mock(Tenant.class);
            when(userService.getUserByUsernameOrThrow(username)).thenReturn(user);
            when(user.getTenant()).thenReturn(tenant);
            when(tenant.getTenantKey()).thenReturn("tnt-a");

            authorizationService.validateTenantAccess(username, "tnt-a");
        }

        @Test
        void validateTenantAccess_TenantMismatch_ThrowsAccessDenied() {
            String username = "alice";
            User user = mock(User.class);
            Tenant tenant = mock(Tenant.class);
            when(userService.getUserByUsernameOrThrow(username)).thenReturn(user);
            when(user.getTenant()).thenReturn(tenant);
            when(tenant.getTenantKey()).thenReturn("tnt-a");

            assertThatThrownBy(() -> authorizationService.validateTenantAccess(username, "tnt-b"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("해당 테넌트에 대한 접근 권한이 없습니다");
        }
    }

    @Nested
    @DisplayName("getUserPermissions")
    class GetUserPermissionsTest {
        @Test
        void getUserPermissions_MergesRoleAndDirectPermissions() {
            String username = "alice";
            User user = mock(User.class);
            Role role = mock(Role.class);
            Permission permRole = mock(Permission.class);
            Permission permDirect = mock(Permission.class);
            when(userService.getUserByUsernameOrThrow(username)).thenReturn(user);
            when(user.getRoles()).thenReturn(List.of(role));
            when(role.getPermissions()).thenReturn(List.of(permRole));
            when(permRole.getPermissionKey()).thenReturn("user.read");
            when(user.getPermissions()).thenReturn(List.of(permDirect));
            when(permDirect.getPermissionKey()).thenReturn("user.write");

            Set<String> result = authorizationService.getUserPermissions(username);
            assertThat(result).containsExactlyInAnyOrder("user.read", "user.write");
        }
    }
}


