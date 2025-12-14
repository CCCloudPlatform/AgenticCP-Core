package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationMember;
import com.agenticcp.core.domain.organization.repository.OrganizationMemberRepository;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import com.agenticcp.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Worker 서비스 통합 테스트
 * 
 * <p>설계 B 기준: User 기반 Worker 생성 및 테넌트 멤버십 관리 시나리오를 검증합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Worker 서비스 통합 테스트")
class WorkerServiceIntegrationTest {

    @Autowired
    private WorkerService workerService;

    @Autowired
    private TenantWorkerService tenantWorkerService;

    @Autowired
    private WorkerRoleService workerRoleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private OrganizationMemberService organizationMemberService;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private Tenant dedicatedTenant;
    private Tenant sharedTenant;
    private Organization testOrganization;
    private Role adminRole;
    private Role viewerRole;
    private Role sharedAdminRole;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .status(Status.ACTIVE)
                .build();
        testUser = userRepository.save(testUser);

        // Dedicated Tenant용 Organization
        Organization dedicatedOrg = Organization.builder()
                .name("전용 테넌트 조직")
                .build();
        dedicatedOrg = organizationRepository.save(dedicatedOrg);

        // Shared Tenant용 Organization
        Organization sharedOrg = Organization.builder()
                .name("공유 테넌트 조직")
                .build();
        sharedOrg = organizationRepository.save(sharedOrg);

        testOrganization = dedicatedOrg; // 기본 조직으로 사용

        // 설계 B: Tenant는 organization 필드로 1:1 관계
        dedicatedTenant = Tenant.builder()
                .tenantKey("dedicated-tenant")
                .tenantName("전용 테넌트")
                .organization(dedicatedOrg) // Dedicated Tenant는 organization 사용 (1:1 관계)
                .tenantType(Tenant.TenantType.DEDICATED)
                .status(Status.ACTIVE)
                .build();
        dedicatedTenant = tenantRepository.save(dedicatedTenant);

        // Shared Tenant도 organization을 가짐 (1:1 관계)
        sharedTenant = Tenant.builder()
                .tenantKey("shared-tenant")
                .tenantName("공유 테넌트")
                .organization(sharedOrg) // Shared Tenant도 organization 필요 (1:1 관계)
                .tenantType(Tenant.TenantType.SHARED)
                .status(Status.ACTIVE)
                .build();
        sharedTenant = tenantRepository.save(sharedTenant);

        adminRole = Role.builder()
                .roleKey("admin")
                .roleName("관리자")
                .tenant(dedicatedTenant)
                .status(Status.ACTIVE)
                .build();
        adminRole = roleRepository.save(adminRole);

        viewerRole = Role.builder()
                .roleKey("viewer")
                .roleName("조회자")
                .tenant(dedicatedTenant)
                .status(Status.ACTIVE)
                .build();
        viewerRole = roleRepository.save(viewerRole);

        // Shared Tenant용 Role도 생성
        sharedAdminRole = Role.builder()
                .roleKey("admin")
                .roleName("관리자")
                .tenant(sharedTenant)
                .status(Status.ACTIVE)
                .build();
        sharedAdminRole = roleRepository.save(sharedAdminRole);
    }

    @Nested
    @DisplayName("시나리오 1: User가 Worker로 생성되는 경우")
    class CreateWorkerScenario {

        @Test
        @DisplayName("User와 Tenant로 Worker 생성 성공")
        void createWorker_WithUserAndTenant_Success() {
            // When
            var worker = workerService.createWorker(testUser.getId(), dedicatedTenant.getId());

            // Then
            assertThat(worker).isNotNull();
            assertThat(worker.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(worker.getTenant().getId()).isEqualTo(dedicatedTenant.getId());
            assertThat(worker.getId()).isNotNull();
        }

        @Test
        @DisplayName("같은 User와 Tenant로 중복 생성 시 예외 발생")
        void createWorker_DuplicateUserAndTenant_ThrowsException() {
            // Given
            workerService.createWorker(testUser.getId(), dedicatedTenant.getId());

            // When & Then
            assertThatThrownBy(() -> workerService.createWorker(testUser.getId(), dedicatedTenant.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("같은 User와 Tenant로 이미 Worker가 생성되었습니다");
        }

        @Test
        @DisplayName("존재하지 않는 User로 Worker 생성 시 예외 발생")
        void createWorker_WithNonExistentUser_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> workerService.createWorker(999L, dedicatedTenant.getId()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 Tenant로 Worker 생성 시 예외 발생")
        void createWorker_WithNonExistentTenant_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> workerService.createWorker(testUser.getId(), 999L))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("테넌트를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("같은 User가 여러 Tenant에서 Worker로 생성 가능")
        void createWorker_SameUserDifferentTenants_Success() {
            // When
            var worker1 = workerService.createWorker(testUser.getId(), dedicatedTenant.getId());
            var worker2 = workerService.createWorker(testUser.getId(), sharedTenant.getId());

            // Then
            assertThat(worker1.getId()).isNotEqualTo(worker2.getId());
            assertThat(worker1.getUser().getId()).isEqualTo(worker2.getUser().getId());
            assertThat(worker1.getTenant().getId()).isNotEqualTo(worker2.getTenant().getId());
        }
    }

    @Nested
    @DisplayName("시나리오 2: Worker가 테넌트에 할당되는 경우 (TenantWorkerMap)")
    class AssignWorkerToTenantScenario {

        @Test
        @DisplayName("Shared Tenant에 Worker 할당 성공")
        void assignWorkerToTenant_SharedTenant_Success() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), sharedTenant.getId());

            // When
            var tenantWorkerMap = tenantWorkerService.assignWorkerToTenant(
                    sharedTenant.getId(), worker.getId(), "FULL_ACCESS");

            // Then
            assertThat(tenantWorkerMap).isNotNull();
            assertThat(tenantWorkerMap.getTenant().getId()).isEqualTo(sharedTenant.getId());
            assertThat(tenantWorkerMap.getWorker().getId()).isEqualTo(worker.getId());
            assertThat(tenantWorkerMap.getAccessScope()).isEqualTo("FULL_ACCESS");
            assertThat(tenantWorkerMap.getJoinedAt()).isNotNull();
        }

        @Test
        @DisplayName("Dedicated Tenant는 자동으로 Worker가 할당됨 (TenantWorkerMap 불필요)")
        void assignWorkerToTenant_DedicatedTenant_NotRequired() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), dedicatedTenant.getId());

            // When & Then
            // Dedicated Tenant는 Worker 생성 시 자동으로 소속되므로 별도 할당 불필요
            var workers = workerService.findByTenantId(dedicatedTenant.getId());
            assertThat(workers).hasSize(1);
            assertThat(workers.get(0).getId()).isEqualTo(worker.getId());
        }

        @Test
        @DisplayName("Shared Tenant에 Worker 중복 할당 시 예외 발생")
        void assignWorkerToTenant_Duplicate_ThrowsException() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), sharedTenant.getId());
            tenantWorkerService.assignWorkerToTenant(sharedTenant.getId(), worker.getId(), "FULL_ACCESS");

            // When & Then
            assertThatThrownBy(() -> tenantWorkerService.assignWorkerToTenant(
                    sharedTenant.getId(), worker.getId(), "READ_ONLY"))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("이미 할당된 Worker입니다");
        }

        @Test
        @DisplayName("Shared Tenant에서 Worker 제거 성공")
        void removeWorkerFromTenant_Success() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), sharedTenant.getId());
            tenantWorkerService.assignWorkerToTenant(sharedTenant.getId(), worker.getId(), "FULL_ACCESS");

            // When
            tenantWorkerService.removeWorkerFromTenant(sharedTenant.getId(), worker.getId());

            // Then
            var tenantWorkerMaps = tenantWorkerService.findByTenantId(sharedTenant.getId());
            assertThat(tenantWorkerMaps).isEmpty();
        }
    }

    @Nested
    @DisplayName("시나리오 3: Worker에게 역할이 부여되는 경우 (WorkerRole)")
    class AssignRoleToWorkerScenario {

        @Test
        @DisplayName("Worker에게 역할 부여 성공")
        void assignRoleToWorker_Success() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), dedicatedTenant.getId());

            // When
            var workerRole = workerRoleService.assignRole(
                    worker.getId(), adminRole.getId(), dedicatedTenant.getId());

            // Then
            assertThat(workerRole).isNotNull();
            assertThat(workerRole.getWorker().getId()).isEqualTo(worker.getId());
            assertThat(workerRole.getRole().getId()).isEqualTo(adminRole.getId());
            assertThat(workerRole.getTenant().getId()).isEqualTo(dedicatedTenant.getId());
        }

        @Test
        @DisplayName("Worker에게 여러 역할 부여 가능")
        void assignRoleToWorker_MultipleRoles_Success() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), dedicatedTenant.getId());

            // When
            var workerRole1 = workerRoleService.assignRole(
                    worker.getId(), adminRole.getId(), dedicatedTenant.getId());
            var workerRole2 = workerRoleService.assignRole(
                    worker.getId(), viewerRole.getId(), dedicatedTenant.getId());

            // Then
            assertThat(workerRole1).isNotNull();
            assertThat(workerRole2).isNotNull();
            
            var roles = workerRoleService.findByWorkerIdAndTenantId(worker.getId(), dedicatedTenant.getId());
            assertThat(roles).hasSize(2);
        }

        @Test
        @DisplayName("같은 Worker에게 같은 역할 중복 부여 시 예외 발생")
        void assignRoleToWorker_DuplicateRole_ThrowsException() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), dedicatedTenant.getId());
            workerRoleService.assignRole(worker.getId(), adminRole.getId(), dedicatedTenant.getId());

            // When & Then
            assertThatThrownBy(() -> workerRoleService.assignRole(
                    worker.getId(), adminRole.getId(), dedicatedTenant.getId()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("이미 부여된 역할입니다");
        }

        @Test
        @DisplayName("Worker 역할 제거 성공")
        void removeRoleFromWorker_Success() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), dedicatedTenant.getId());
            workerRoleService.assignRole(worker.getId(), adminRole.getId(), dedicatedTenant.getId());

            // When
            workerRoleService.removeRole(worker.getId(), adminRole.getId(), dedicatedTenant.getId());

            // Then
            var roles = workerRoleService.findByWorkerIdAndTenantId(worker.getId(), dedicatedTenant.getId());
            assertThat(roles).isEmpty();
        }
    }

    @Nested
    @DisplayName("시나리오 4: OrganizationMember를 통한 User-Organization 관계 관리")
    class OrganizationMemberScenario {

        @Test
        @DisplayName("User를 Organization에 멤버로 추가 성공")
        void addUserToOrganization_Success() {
            // When
            var member = organizationMemberService.addMember(
                    testOrganization.getId(), testUser.getId(), "ADMIN");

            // Then
            assertThat(member).isNotNull();
            assertThat(member.getOrganization().getId()).isEqualTo(testOrganization.getId());
            assertThat(member.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(member.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("같은 User를 같은 Organization에 중복 추가 시 예외 발생")
        void addUserToOrganization_Duplicate_ThrowsException() {
            // Given
            organizationMemberService.addMember(testOrganization.getId(), testUser.getId(), "ADMIN");

            // When & Then
            assertThatThrownBy(() -> organizationMemberService.addMember(
                    testOrganization.getId(), testUser.getId(), "VIEWER"))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("이미 멤버로 등록된 사용자입니다");
        }
    }

    @Nested
    @DisplayName("시나리오 5: Shared Tenant 접근 시 Worker 멤버십 + 역할 검증")
    class SharedTenantAccessScenario {

        @Test
        @DisplayName("Shared Tenant 접근 시 Worker 멤버십과 역할 모두 필요")
        void accessSharedTenant_RequiresWorkerAndRole_Success() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), sharedTenant.getId());
            tenantWorkerService.assignWorkerToTenant(sharedTenant.getId(), worker.getId(), "FULL_ACCESS");
            workerRoleService.assignRole(worker.getId(), sharedAdminRole.getId(), sharedTenant.getId());

            // When
            var hasAccess = tenantWorkerService.hasAccessToTenant(
                    testUser.getId(), sharedTenant.getId(), "admin");

            // Then
            assertThat(hasAccess).isTrue();
        }

        @Test
        @DisplayName("Worker 멤버십 없이 Shared Tenant 접근 시 실패")
        void accessSharedTenant_WithoutWorkerMembership_Fails() {
            // Given - Worker는 생성했지만 TenantWorkerMap에 할당하지 않음
            var worker = workerService.createWorker(testUser.getId(), sharedTenant.getId());

            // When
            var hasAccess = tenantWorkerService.hasAccessToTenant(
                    testUser.getId(), sharedTenant.getId(), "admin");

            // Then
            assertThat(hasAccess).isFalse();
        }

        @Test
        @DisplayName("역할 없이 Shared Tenant 접근 시 실패")
        void accessSharedTenant_WithoutRole_Fails() {
            // Given
            var worker = workerService.createWorker(testUser.getId(), sharedTenant.getId());
            tenantWorkerService.assignWorkerToTenant(sharedTenant.getId(), worker.getId(), "FULL_ACCESS");
            // 역할 부여하지 않음

            // When
            var hasAccess = tenantWorkerService.hasAccessToTenant(
                    testUser.getId(), sharedTenant.getId(), "admin");

            // Then
            assertThat(hasAccess).isFalse();
        }
    }

    @Nested
    @DisplayName("시나리오 6: 복합 시나리오 - 전체 플로우")
    class ComplexScenario {

        @Test
        @DisplayName("User → Worker 생성 → Tenant 할당 → 역할 부여 전체 플로우")
        void completeFlow_Success() {
            // Step 1: User를 Organization에 멤버로 추가
            var member = organizationMemberService.addMember(
                    testOrganization.getId(), testUser.getId(), "ADMIN");
            assertThat(member).isNotNull();

            // Step 2: User 기반으로 Worker 생성
            var worker = workerService.createWorker(testUser.getId(), dedicatedTenant.getId());
            assertThat(worker).isNotNull();

            // Step 3: Shared Tenant에 Worker 할당
            var worker2 = workerService.createWorker(testUser.getId(), sharedTenant.getId());
            var tenantWorkerMap = tenantWorkerService.assignWorkerToTenant(
                    sharedTenant.getId(), worker2.getId(), "FULL_ACCESS");
            assertThat(tenantWorkerMap).isNotNull();

            // Step 4: Worker에게 역할 부여
            var workerRole = workerRoleService.assignRole(
                    worker2.getId(), sharedAdminRole.getId(), sharedTenant.getId());
            assertThat(workerRole).isNotNull();

            // Step 5: 접근 권한 검증
            var hasAccess = tenantWorkerService.hasAccessToTenant(
                    testUser.getId(), sharedTenant.getId(), "admin");
            assertThat(hasAccess).isTrue();

            // Step 6: Worker 조회
            var workers = workerService.findByUserId(testUser.getId());
            assertThat(workers).hasSize(2);

            var tenantWorkers = workerService.findByTenantId(sharedTenant.getId());
            assertThat(tenantWorkers).hasSize(1);
        }
    }
}

