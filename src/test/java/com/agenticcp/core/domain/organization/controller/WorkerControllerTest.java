package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.CreateWorkerRequest;
import com.agenticcp.core.domain.organization.dto.WorkerResponse;
import com.agenticcp.core.domain.organization.entity.Worker;
import com.agenticcp.core.domain.organization.service.WorkerService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
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
 * WorkerController 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerController 단위 테스트")
class WorkerControllerTest {

    @Mock
    private WorkerService workerService;

    @InjectMocks
    private WorkerController workerController;

    private Worker testWorker;
    private WorkerResponse testWorkerResponse;

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

        testWorker = Worker.builder()
                .user(testUser)
                .organization(null)
                .build();
        testWorker.setId(1L);
        testWorker.setCreatedAt(LocalDateTime.now());
        testWorker.setUpdatedAt(LocalDateTime.now());

        testWorkerResponse = WorkerResponse.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .userEmail("test@example.com")
                .userName("테스트 사용자")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Worker 생성 테스트")
    class CreateWorkerTest {
        @Test
        @DisplayName("정상 생성 시 201 반환")
        void createWorkerFromUser_WhenValidRequest_ReturnsCreated() {
            // Given
            Long userId = 1L;

            when(workerService.createWorkerFromUser(anyLong()))
                    .thenReturn(testWorker);

            // When
            ResponseEntity<ApiResponse<WorkerResponse>> response =
                    workerController.createWorkerFromUser(userId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("Worker가 성공적으로 생성되었습니다.");
            assertThat(response.getBody().getData().getUserId()).isEqualTo(1L);

            verify(workerService).createWorkerFromUser(userId);
        }
    }

    @Nested
    @DisplayName("사용자의 Worker 목록 조회 테스트")
    class GetWorkersByUserIdTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getWorkersByUserId_WhenValidId_ReturnsOk() {
            // Given
            Long userId = 1L;
            List<Worker> workers = Arrays.asList(testWorker);

            when(workerService.findByUserId(userId))
                    .thenReturn(workers);

            // When
            ResponseEntity<ApiResponse<List<WorkerResponse>>> response =
                    workerController.getWorkersByUserId(userId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("Worker 목록을 성공적으로 조회했습니다.");
            assertThat(response.getBody().getData()).hasSize(1);

            verify(workerService).findByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Worker 조회 테스트")
    class GetWorkerTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getWorker_WhenValidIds_ReturnsOk() {
            // Given
            Long userId = 1L;
            Long workerId = 1L;

            when(workerService.findById(workerId))
                    .thenReturn(testWorker);

            // When
            ResponseEntity<ApiResponse<WorkerResponse>> response =
                    workerController.getWorker(userId, workerId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("Worker 정보를 성공적으로 조회했습니다.");
            assertThat(response.getBody().getData().getId()).isEqualTo(1L);

            verify(workerService).findById(workerId);
        }
    }
}

