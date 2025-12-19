package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.AssignRoleRequest;
import com.agenticcp.core.domain.organization.dto.WorkerRoleResponse;
import com.agenticcp.core.domain.organization.entity.WorkerRole;
import com.agenticcp.core.domain.organization.service.WorkerRoleService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.organization.entity.Worker;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * WorkerRoleController 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerRoleController 단위 테스트")
class WorkerRoleControllerTest {

    @Mock
    private WorkerRoleService workerRoleService;

    @InjectMocks
    private WorkerRoleController workerRoleController;

    private WorkerRole testWorkerRole;
    private WorkerRoleResponse testResponse;

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .build();
        testUser.setId(1L);

        Tenant testTenant = Tenant.builder()
                .tenantKey("tenant-dev")
                .tenantName("개발 테넌트")
                .build();
        testTenant.setId(1L);

        Worker testWorker = Worker.builder()
                .user(testUser)
                .organization(null)
                .build();
        testWorker.setId(1L);

        Role testRole = Role.builder()
                .roleKey("ADMIN")
                .roleName("관리자")
                .tenant(testTenant)
                .build();
        testRole.setId(1L);

        testWorkerRole = WorkerRole.builder()
                .worker(testWorker)
                .role(testRole)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testResponse = WorkerRoleResponse.builder()
                .workerId(1L)
                .userId(1L)
                .username("testuser")
                .roleId(1L)
                .roleKey("ADMIN")
                .roleName("관리자")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Worker의 역할 목록 조회 테스트")
    class GetRolesTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getRoles_WhenValidId_ReturnsOk() {
            // Given
            Long workerId = 1L;
            List<WorkerRole> roles = Arrays.asList(testWorkerRole);

            when(workerRoleService.findByWorkerId(workerId))
                    .thenReturn(roles);

            // When
            ResponseEntity<ApiResponse<List<WorkerRoleResponse>>> response =
                    workerRoleController.getRoles(workerId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("역할 목록을 성공적으로 조회했습니다.");
            assertThat(response.getBody().getData()).hasSize(1);

            verify(workerRoleService).findByWorkerId(workerId);
        }
    }

    @Nested
    @DisplayName("Worker에게 역할 부여 테스트")
    class AssignRoleTest {
        @Test
        @DisplayName("정상 부여 시 200 반환")
        void assignRole_WhenValidRequest_ReturnsOk() {
            // Given
            Long workerId = 1L;
            AssignRoleRequest request = AssignRoleRequest.builder()
                    .roleId(1L)
                    .build();

            when(workerRoleService.assignRole(anyLong(), anyLong()))
                    .thenReturn(testWorkerRole);

            // When
            ResponseEntity<ApiResponse<WorkerRoleResponse>> response =
                    workerRoleController.assignRole(workerId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("역할이 성공적으로 부여되었습니다.");
            assertThat(response.getBody().getData().getWorkerId()).isEqualTo(1L);
            assertThat(response.getBody().getData().getRoleId()).isEqualTo(1L);

            verify(workerRoleService).assignRole(workerId, request.getRoleId());
        }
    }

    @Nested
    @DisplayName("Worker에서 역할 제거 테스트")
    class RemoveRoleTest {
        @Test
        @DisplayName("정상 제거 시 204 반환")
        void removeRole_WhenValidParams_ReturnsNoContent() {
            // Given
            Long workerId = 1L;
            Long roleId = 1L;

            doNothing().when(workerRoleService).removeRole(anyLong(), anyLong());

            // When
            ResponseEntity<Void> response =
                    workerRoleController.removeRole(workerId, roleId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();

            verify(workerRoleService).removeRole(workerId, roleId);
        }
    }
}

