package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.dto.CreateRoleRequest;
import com.agenticcp.core.domain.user.dto.UpdateRoleRequest;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.enums.RoleErrorCode;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import com.agenticcp.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleService roleService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .tenantKey("tenant-001")
                .tenantName("테넌트001")
                .build();
        TenantContextHolder.setTenant(tenant);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("시나리오 1: 역할 생성")
    class CreateRoleScenario {
        @Test
        @DisplayName("Given 관리자가 새 역할 생성 When 역할명/설명/권한 입력 Then 역할 생성 및 권한 매핑")
        void createRole_assignsPermissions() {
            // Given
            List<String> permissionKeys = Arrays.asList("perm.read", "perm.write");
            CreateRoleRequest request = CreateRoleRequest.builder()
                    .roleKey("ROLE_DEV")
                    .roleName("개발자")
                    .description("개발자 권한")
                    .isSystem(false)
                    .priority(10)
                    .permissionKeys(permissionKeys)
                    .build();

            given(roleRepository.existsByRoleKeyAndTenant("ROLE_DEV", tenant)).willReturn(false);

            // save 시 ID가 부여되도록 스텁
            willAnswer(invocation -> {
                Role arg = invocation.getArgument(0);
                arg.setTenant(tenant);
                arg.setIsDefault(false);
                arg.setIsSystem(false);
                arg.setStatus(Status.ACTIVE);
                arg.setId(1L);
                return arg;
            }).given(roleRepository).save(any(Role.class));

            Permission p1 = Permission.builder().permissionKey("perm.read").tenant(tenant).build();
            Permission p2 = Permission.builder().permissionKey("perm.write").tenant(tenant).build();
            given(permissionRepository.findByPermissionKeyInAndTenant(permissionKeys, tenant))
                    .willReturn(Arrays.asList(p1, p2));

            // assignPermissionsToRole 에서 호출되는 findById(1L)
            given(roleRepository.findById(1L)).willReturn(Optional.of(Role.builder()
                    .roleKey("ROLE_DEV").tenant(tenant).build()));

            // createRole 마지막 반환에서 호출되는 findByIdAndTenantWithPermissions(1L, tenant)
            given(roleRepository.findByIdAndTenantWithPermissions(1L, tenant))
                    .willReturn(Optional.of(Role.builder()
                            .roleKey("ROLE_DEV")
                            .roleName("개발자")
                            .description("개발자 권한")
                            .tenant(tenant)
                            .status(Status.ACTIVE)
                            .build()));

            // When
            Role result = roleService.createRole(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRoleKey()).isEqualTo("ROLE_DEV");
            then(permissionRepository).should().findByPermissionKeyInAndTenant(permissionKeys, tenant);
            then(roleRepository).should().findById(1L);
            then(roleRepository).should().findByIdAndTenantWithPermissions(1L, tenant);
            // 생성 시 1회, 권한 매핑 시 1회 총 2회 저장 호출
            then(roleRepository).should(times(2)).save(any(Role.class));
        }
    }

    @Nested
    @DisplayName("시나리오 2: 권한 할당")
    class AssignPermissionScenario {
        @Test
        @DisplayName("Given 기존 역할 When 새로운 권한을 할당 Then 역할 권한 목록이 업데이트")
        void assignPermissionsToRole_updatesRolePermissions() {
            // Given
            Role role = Role.builder()
                    .roleKey("ROLE_USER")
                    .tenant(tenant)
                    .permissions(new ArrayList<>())
                    .build();
            role.setId(1L);

            List<String> newKeys = Arrays.asList("perm.export", "perm.audit");
            Permission px = Permission.builder().permissionKey("perm.export").tenant(tenant).build();
            Permission py = Permission.builder().permissionKey("perm.audit").tenant(tenant).build();

            given(roleRepository.findById(1L)).willReturn(Optional.of(role));
            given(permissionRepository.findByPermissionKeyInAndTenant(newKeys, tenant))
                    .willReturn(Arrays.asList(px, py));

            // When
            roleService.assignPermissionsToRole(1L, newKeys);

            // Then
            then(roleRepository).should().save(argThat(r -> {
                List<Permission> perms = r.getPermissions();
                return perms != null && perms.size() == 2 &&
                        perms.stream().anyMatch(p -> "perm.export".equals(p.getPermissionKey())) &&
                        perms.stream().anyMatch(p -> "perm.audit".equals(p.getPermissionKey()));
            }));
        }
    }

    @Nested
    @DisplayName("시나리오 3: 시스템 역할 보호")
    class SystemRoleProtectionScenario {
        @Test
        @DisplayName("Given 시스템 역할 삭제 시도 When 삭제 요청 Then SYSTEM_ROLE_NOT_DELETABLE 예외")
        void deleteRole_systemRole_throws() {
            // Given
            Role systemRole = Role.builder()
                    .roleKey("SUPER_ADMIN")
                    .tenant(tenant)
                    .isSystem(true)
                    .isDefault(false)
                    .build();

            given(roleRepository.findByRoleKeyAndTenantWithPermissions("SUPER_ADMIN", tenant))
                    .willReturn(Optional.of(systemRole));

            // When & Then
            assertThatThrownBy(() -> roleService.deleteRole("SUPER_ADMIN"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(RoleErrorCode.SYSTEM_ROLE_NOT_DELETABLE.getMessage());
        }
    }
}
