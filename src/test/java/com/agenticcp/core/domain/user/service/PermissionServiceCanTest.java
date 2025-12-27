package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.entity.Worker;
import com.agenticcp.core.domain.user.entity.WorkerRoleAssignment;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import com.agenticcp.core.domain.user.repository.WorkerRoleAssignmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * PermissionService.can() 메서드 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService.can() 메서드 테스트")
class PermissionServiceCanTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private WorkerRoleAssignmentRepository workerRoleAssignmentRepository;

    @InjectMocks
    private PermissionService permissionService;

    private Tenant testTenant;
    private User testUser;
    private Worker testWorker;
    private Role testRole;
    private Permission testPermission;
    private CloudResource testResource;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .tenantKey("tenant-1")
                .tenantName("Tenant 1")
                .build();
        testTenant.setId(100L);
        testTenant.setIsDeleted(false);

        testUser = User.builder()
                .username("testuser")
                .build();
        testUser.setId(1L);

        testWorker = Worker.builder()
                .workerKey("worker-1")
                .user(testUser)
                .tenant(testTenant)
                .build();
        testWorker.setId(10L);
        testWorker.setIsDeleted(false);

        testPermission = Permission.builder()
                .permissionKey("vm:start")
                .permissionName("VM Start")
                .resource("INSTANCE")
                .action("START")
                .tenant(testTenant)
                .build();
        testPermission.setId(1000L);

        testRole = Role.builder()
                .roleKey("vm-admin")
                .roleName("VM Admin")
                .permissions(List.of(testPermission))
                .tenant(testTenant)
                .build();
        testRole.setId(100L);

        testResource = CloudResource.builder()
                .resourceId("vm-001")
                .name("Test VM")
                .type("INSTANCE")
                .provider("AWS")
                .region("us-east-1")
                .tenant(testTenant)
                .build();
        testResource.setId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("권한 체크 성공 테스트")
    class PermissionCheckSuccessTest {

        @Test
        @DisplayName("Worker가 리소스에 대한 권한이 있으면 true를 반환해야 한다")
        void can_WithValidPermission_ReturnsTrue() {
            // Given
            when(workerRoleAssignmentRepository.findRoleIdsByTenantIdAndWorkerId(100L, 10L, any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(100L));
            when(roleRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(testRole));
            when(permissionRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(testPermission));

            // When
            boolean result = permissionService.can(10L, 100L, "START", testResource);

            // Then
            assertThat(result).isTrue();
            verify(workerRoleAssignmentRepository).findRoleIdsByTenantIdAndWorkerId(100L, 10L, any(LocalDateTime.class));
            verify(roleRepository).findAllById(anyList());
            verify(permissionRepository).findAllById(anyList());
        }

        @Test
        @DisplayName("TenantContextHolder에서 workerId와 tenantId를 가져와서 권한 체크를 수행할 수 있어야 한다")
        void can_WithContextHolder_ReturnsTrue() {
            // Given
            TenantContextHolder.setCurrentTenantAndWorker(testTenant, testWorker);
            when(workerRoleAssignmentRepository.findRoleIdsByTenantIdAndWorkerId(100L, 10L, any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(100L));
            when(roleRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(testRole));
            when(permissionRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(testPermission));

            // When
            boolean result = permissionService.can(null, null, "START", testResource);

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Tenant 격리 실패 테스트")
    class TenantIsolationFailureTest {

        @Test
        @DisplayName("리소스의 Tenant가 현재 Tenant와 다르면 false를 반환해야 한다")
        void can_WithDifferentTenant_ReturnsFalse() {
            // Given
            Tenant otherTenant = Tenant.builder()
                    .tenantKey("tenant-2")
                    .build();
            otherTenant.setId(200L);
            CloudResource otherTenantResource = CloudResource.builder()
                    .resourceId("vm-002")
                    .type("INSTANCE")
                    .tenant(otherTenant)
                    .build();
            otherTenantResource.setId(2L);

            // When
            boolean result = permissionService.can(10L, 100L, "START", otherTenantResource);

            // Then
            assertThat(result).isFalse();
            verify(workerRoleAssignmentRepository, never()).findRoleIdsByTenantIdAndWorkerId(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("리소스에 Tenant가 없으면 false를 반환해야 한다")
        void can_WithNullTenant_ReturnsFalse() {
            // Given
            CloudResource resourceWithoutTenant = CloudResource.builder()
                    .resourceId("vm-003")
                    .type("INSTANCE")
                    .tenant(null)
                    .build();
            resourceWithoutTenant.setId(3L);

            // When
            boolean result = permissionService.can(10L, 100L, "START", resourceWithoutTenant);

            // Then
            assertThat(result).isFalse();
            verify(workerRoleAssignmentRepository, never()).findRoleIdsByTenantIdAndWorkerId(anyLong(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("Role 없음 테스트")
    class NoRoleTest {

        @Test
        @DisplayName("Worker에게 할당된 Role이 없으면 false를 반환해야 한다")
        void can_WithNoRoles_ReturnsFalse() {
            // Given
            when(workerRoleAssignmentRepository.findRoleIdsByTenantIdAndWorkerId(100L, 10L, any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            boolean result = permissionService.can(10L, 100L, "START", testResource);

            // Then
            assertThat(result).isFalse();
            verify(workerRoleAssignmentRepository).findRoleIdsByTenantIdAndWorkerId(100L, 10L, any(LocalDateTime.class));
            verify(roleRepository, never()).findAllById(anyList());
        }
    }

    @Nested
    @DisplayName("Permission 없음 테스트")
    class NoPermissionTest {

        @Test
        @DisplayName("Role에 Permission이 없으면 false를 반환해야 한다")
        void can_WithNoPermissions_ReturnsFalse() {
            // Given
            Role roleWithoutPermission = Role.builder()
                    .roleKey("vm-admin")
                    .permissions(Collections.emptyList())
                    .build();
            roleWithoutPermission.setId(100L);
            when(workerRoleAssignmentRepository.findRoleIdsByTenantIdAndWorkerId(100L, 10L, any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(100L));
            when(roleRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(roleWithoutPermission));

            // When
            boolean result = permissionService.can(10L, 100L, "START", testResource);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Permission이 리소스 타입과 일치하지 않으면 false를 반환해야 한다")
        void can_WithMismatchedResourceType_ReturnsFalse() {
            // Given
            Permission differentResourcePermission = Permission.builder()
                    .resource("BUCKET")
                    .action("START")
                    .build();
            differentResourcePermission.setId(1001L);
            Role roleWithDifferentPermission = Role.builder()
                    .permissions(List.of(differentResourcePermission))
                    .build();
            roleWithDifferentPermission.setId(100L);
            when(workerRoleAssignmentRepository.findRoleIdsByTenantIdAndWorkerId(100L, 10L, any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(100L));
            when(roleRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(roleWithDifferentPermission));
            when(permissionRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(differentResourcePermission));

            // When
            boolean result = permissionService.can(10L, 100L, "START", testResource);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Permission이 액션과 일치하지 않으면 false를 반환해야 한다")
        void can_WithMismatchedAction_ReturnsFalse() {
            // Given
            Permission differentActionPermission = Permission.builder()
                    .resource("INSTANCE")
                    .action("STOP")
                    .build();
            differentActionPermission.setId(1002L);
            Role roleWithDifferentAction = Role.builder()
                    .permissions(List.of(differentActionPermission))
                    .build();
            roleWithDifferentAction.setId(100L);
            when(workerRoleAssignmentRepository.findRoleIdsByTenantIdAndWorkerId(100L, 10L, any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(100L));
            when(roleRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(roleWithDifferentAction));
            when(permissionRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(differentActionPermission));

            // When
            boolean result = permissionService.can(10L, 100L, "START", testResource);

            // Then
            assertThat(result).isFalse();
        }
    }
}

