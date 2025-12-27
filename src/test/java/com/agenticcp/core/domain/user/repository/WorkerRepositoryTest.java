package com.agenticcp.core.domain.user.repository;

import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.entity.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * WorkerRepository 통합 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("WorkerRepository 통합 테스트")
class WorkerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WorkerRepository workerRepository;

    private User testUser;
    private Tenant testTenant1;
    private Tenant testTenant2;
    private Organization testOrganization;
    private Worker testWorker1;
    private Worker testWorker2;

    @BeforeEach
    void setUp() {
        // User 생성
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        // Organization 생성
        testOrganization = Organization.builder()
                .name("Test Organization")
                .build();
        testOrganization = entityManager.persistAndFlush(testOrganization);

        // Tenant 생성
        testTenant1 = Tenant.builder()
                .tenantKey("tenant-1")
                .tenantName("Tenant 1")
                .build();
        testTenant1.setIsDeleted(false);
        testTenant1 = entityManager.persistAndFlush(testTenant1);

        testTenant2 = Tenant.builder()
                .tenantKey("tenant-2")
                .tenantName("Tenant 2")
                .build();
        testTenant2.setIsDeleted(false);
        testTenant2 = entityManager.persistAndFlush(testTenant2);

        // Worker 생성
        testWorker1 = Worker.builder()
                .workerKey("worker-1")
                .user(testUser)
                .tenant(testTenant1)
                .organization(testOrganization)
                .build();
        testWorker1.setIsDeleted(false);
        testWorker1 = entityManager.persistAndFlush(testWorker1);

        testWorker2 = Worker.builder()
                .workerKey("worker-2")
                .user(testUser)
                .tenant(testTenant2)
                .organization(testOrganization)
                .build();
        testWorker2.setIsDeleted(false);
        testWorker2 = entityManager.persistAndFlush(testWorker2);
    }

    @Nested
    @DisplayName("findByUserIdAndIsDeletedFalse 테스트")
    class FindByUserIdTest {

        @Test
        @DisplayName("User ID로 Worker 목록을 조회할 수 있어야 한다")
        void findByUserIdAndIsDeletedFalse_WithValidUserId_ReturnsWorkers() {
            // When
            List<Worker> result = workerRepository.findByUserIdAndIsDeletedFalse(testUser.getId());

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Worker::getWorkerKey)
                    .containsExactlyInAnyOrder("worker-1", "worker-2");
        }

        @Test
        @DisplayName("존재하지 않는 User ID로 조회하면 빈 목록을 반환해야 한다")
        void findByUserIdAndIsDeletedFalse_WithNonExistentUserId_ReturnsEmpty() {
            // When
            List<Worker> result = workerRepository.findByUserIdAndIsDeletedFalse(999L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndTenantIdAndIsDeletedFalse 테스트")
    class FindByUserIdAndTenantIdTest {

        @Test
        @DisplayName("User ID와 Tenant ID로 Worker를 조회할 수 있어야 한다")
        void findByUserIdAndTenantIdAndIsDeletedFalse_WithValidIds_ReturnsWorker() {
            // When
            Optional<Worker> result = workerRepository.findByUserIdAndTenantIdAndIsDeletedFalse(
                    testUser.getId(), testTenant1.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getWorkerKey()).isEqualTo("worker-1");
            assertThat(result.get().getTenant().getId()).isEqualTo(testTenant1.getId());
        }

        @Test
        @DisplayName("존재하지 않는 조합으로 조회하면 빈 Optional을 반환해야 한다")
        void findByUserIdAndTenantIdAndIsDeletedFalse_WithNonExistentIds_ReturnsEmpty() {
            // When
            Optional<Worker> result = workerRepository.findByUserIdAndTenantIdAndIsDeletedFalse(
                    999L, 999L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTenantIdAndIsDeletedFalse 테스트")
    class FindByTenantIdTest {

        @Test
        @DisplayName("Tenant ID로 Worker 목록을 조회할 수 있어야 한다")
        void findByTenantIdAndIsDeletedFalse_WithValidTenantId_ReturnsWorkers() {
            // When
            List<Worker> result = workerRepository.findByTenantIdAndIsDeletedFalse(testTenant1.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWorkerKey()).isEqualTo("worker-1");
        }
    }

    @Nested
    @DisplayName("findByWorkerKeyAndIsDeletedFalse 테스트")
    class FindByWorkerKeyTest {

        @Test
        @DisplayName("Worker Key로 Worker를 조회할 수 있어야 한다")
        void findByWorkerKeyAndIsDeletedFalse_WithValidWorkerKey_ReturnsWorker() {
            // When
            Optional<Worker> result = workerRepository.findByWorkerKeyAndIsDeletedFalse("worker-1");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getWorkerKey()).isEqualTo("worker-1");
            assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("존재하지 않는 Worker Key로 조회하면 빈 Optional을 반환해야 한다")
        void findByWorkerKeyAndIsDeletedFalse_WithNonExistentWorkerKey_ReturnsEmpty() {
            // When
            Optional<Worker> result = workerRepository.findByWorkerKeyAndIsDeletedFalse("non-existent");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findTenantIdsByUserId 테스트")
    class FindTenantIdsByUserIdTest {

        @Test
        @DisplayName("User가 속한 모든 Tenant ID 목록을 조회할 수 있어야 한다")
        void findTenantIdsByUserId_WithValidUserId_ReturnsTenantIds() {
            // When
            List<Long> result = workerRepository.findTenantIdsByUserId(testUser.getId());

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(testTenant1.getId(), testTenant2.getId());
        }

        @Test
        @DisplayName("존재하지 않는 User ID로 조회하면 빈 목록을 반환해야 한다")
        void findTenantIdsByUserId_WithNonExistentUserId_ReturnsEmpty() {
            // When
            List<Long> result = workerRepository.findTenantIdsByUserId(999L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndTenantIdAndIsDeletedFalse 테스트")
    class ExistsByUserIdAndTenantIdTest {

        @Test
        @DisplayName("User가 Tenant에 속하면 true를 반환해야 한다")
        void existsByUserIdAndTenantIdAndIsDeletedFalse_WithValidIds_ReturnsTrue() {
            // When
            boolean result = workerRepository.existsByUserIdAndTenantIdAndIsDeletedFalse(
                    testUser.getId(), testTenant1.getId());

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("User가 Tenant에 속하지 않으면 false를 반환해야 한다")
        void existsByUserIdAndTenantIdAndIsDeletedFalse_WithInvalidIds_ReturnsFalse() {
            // When
            boolean result = workerRepository.existsByUserIdAndTenantIdAndIsDeletedFalse(
                    999L, 999L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("삭제된 Worker 필터링 테스트")
    class DeletedWorkerTest {

        @Test
        @DisplayName("삭제된 Worker는 조회되지 않아야 한다")
        void findByUserIdAndIsDeletedFalse_WithDeletedWorker_ExcludesDeleted() {
            // Given
            testWorker1.setIsDeleted(true);
            entityManager.persistAndFlush(testWorker1);

            // When
            List<Worker> result = workerRepository.findByUserIdAndIsDeletedFalse(testUser.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWorkerKey()).isEqualTo("worker-2");
        }
    }
}

