package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.AssignWorkerRequest;
import com.agenticcp.core.domain.organization.dto.TenantWorkerMapResponse;
import com.agenticcp.core.domain.organization.entity.TenantWorkerMap;
import com.agenticcp.core.domain.organization.service.TenantWorkerService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.organization.entity.Worker;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TenantWorkerController 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantWorkerController 단위 테스트")
class TenantWorkerControllerTest {

    @Mock
    private TenantWorkerService tenantWorkerService;

    @InjectMocks
    private TenantWorkerController tenantWorkerController;

    private TenantWorkerMap testTenantWorkerMap;
    private TenantWorkerMapResponse testResponse;

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .build();
        testUser.setId(1L);

        Tenant testTenant = Tenant.builder()
                .tenantKey("tenant-shared")
                .tenantName("공유 테넌트")
                .build();
        testTenant.setId(1L);

        Worker testWorker = Worker.builder()
                .user(testUser)
                .organization(null)
                .build();
        testWorker.setId(1L);

        testTenantWorkerMap = TenantWorkerMap.builder()
                .tenant(testTenant)
                .worker(testWorker)
                .accessScope("FULL")
                .joinedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testResponse = TenantWorkerMapResponse.builder()
                .tenantId(1L)
                .tenantKey("tenant-shared")
                .tenantName("공유 테넌트")
                .workerId(1L)
                .userId(1L)
                .username("testuser")
                .userEmail("test@example.com")
                .userName("테스트 사용자")
                .accessScope("FULL")
                .joinedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("테넌트의 Worker 목록 조회 테스트")
    class GetWorkersTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getWorkers_WhenValidId_ReturnsOk() {
            // Given
            Long tenantId = 1L;
            List<TenantWorkerMap> maps = Arrays.asList(testTenantWorkerMap);

            when(tenantWorkerService.findByTenantId(tenantId))
                    .thenReturn(maps);

            // When
            ResponseEntity<ApiResponse<List<TenantWorkerMapResponse>>> response =
                    tenantWorkerController.getWorkers(tenantId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("Worker 목록을 성공적으로 조회했습니다.");
            assertThat(response.getBody().getData()).hasSize(1);

            verify(tenantWorkerService).findByTenantId(tenantId);
        }
    }

    @Nested
    @DisplayName("테넌트에 Worker 할당 테스트")
    class AssignWorkerTest {
        @Test
        @DisplayName("정상 할당 시 201 반환")
        void assignWorker_WhenValidRequest_ReturnsCreated() {
            // Given
            Long tenantId = 1L;
            AssignWorkerRequest request = AssignWorkerRequest.builder()
                    .workerId(1L)
                    .accessScope("FULL")
                    .build();

            when(tenantWorkerService.assignWorkerToTenant(anyLong(), anyLong(), anyString()))
                    .thenReturn(testTenantWorkerMap);

            // When
            ResponseEntity<ApiResponse<TenantWorkerMapResponse>> response =
                    tenantWorkerController.assignWorker(tenantId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("Worker가 성공적으로 할당되었습니다.");
            assertThat(response.getBody().getData().getWorkerId()).isEqualTo(1L);
            assertThat(response.getBody().getData().getTenantId()).isEqualTo(1L);

            verify(tenantWorkerService).assignWorkerToTenant(tenantId, request.getWorkerId(), request.getAccessScope());
        }
    }

    @Nested
    @DisplayName("테넌트에서 Worker 제거 테스트")
    class RemoveWorkerTest {
        @Test
        @DisplayName("정상 제거 시 204 반환")
        void removeWorker_WhenValidIds_ReturnsNoContent() {
            // Given
            Long tenantId = 1L;
            Long workerId = 1L;

            doNothing().when(tenantWorkerService).removeWorkerFromTenant(anyLong(), anyLong());

            // When
            ResponseEntity<Void> response =
                    tenantWorkerController.removeWorker(tenantId, workerId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();

            verify(tenantWorkerService).removeWorkerFromTenant(tenantId, workerId);
        }
    }
}

