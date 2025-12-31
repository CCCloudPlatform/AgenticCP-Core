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
 * <p>설계 C 기준: User/Organization 기반 Worker 생성 및 리소스 단위 접근 권한 관리 시나리오를 검증합니다.</p>
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
        @DisplayName("User 기반 Worker 생성 성공")
        void createWorkerFromUser_Success() {
            // When
            var worker = workerService.createWorkerFromUser(testUser.getId());

            // Then
            assertThat(worker).isNotNull();
            assertThat(worker.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(worker.getOrganization()).isNull();
            assertThat(worker.getId()).isNotNull();
        }

        @Test
        @DisplayName("같은 User로 중복 생성 시 예외 발생")
        void createWorkerFromUser_Duplicate_ThrowsException() {
            // Given
            workerService.createWorkerFromUser(testUser.getId());

            // When & Then
            assertThatThrownBy(() -> workerService.createWorkerFromUser(testUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 Worker가 생성되었습니다");
        }

        @Test
        @DisplayName("존재하지 않는 User로 Worker 생성 시 예외 발생")
        void createWorkerFromUser_WithNonExistentUser_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> workerService.createWorkerFromUser(999L))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Organization 기반 Worker 생성 성공")
        void createWorkerFromOrganization_Success() {
            // When
            var worker = workerService.createWorkerFromOrganization(testOrganization.getId());

            // Then
            assertThat(worker).isNotNull();
            assertThat(worker.getOrganization().getId()).isEqualTo(testOrganization.getId());
            assertThat(worker.getUser()).isNull();
            assertThat(worker.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("시나리오 2: Worker에게 역할 부여 (WorkerRole) - C안 기준")
    class AssignRoleToWorkerScenario {

        @Test
        @DisplayName("Worker에게 역할 부여 성공")
        void assignRoleToWorker_Success() {
            // Given
            var worker = workerService.createWorkerFromUser(testUser.getId());

            // When
            var workerRole = workerRoleService.assignRole(worker.getId(), adminRole.getId());

            // Then
            assertThat(workerRole).isNotNull();
            assertThat(workerRole.getWorker().getId()).isEqualTo(worker.getId());
            assertThat(workerRole.getRole().getId()).isEqualTo(adminRole.getId());
        }

        @Test
        @DisplayName("Worker에게 여러 역할 부여 가능")
        void assignRoleToWorker_MultipleRoles_Success() {
            // Given
            var worker = workerService.createWorkerFromUser(testUser.getId());

            // When
            var workerRole1 = workerRoleService.assignRole(worker.getId(), adminRole.getId());
            var workerRole2 = workerRoleService.assignRole(worker.getId(), viewerRole.getId());

            // Then
            assertThat(workerRole1).isNotNull();
            assertThat(workerRole2).isNotNull();
            
            var roles = workerRoleService.findByWorkerId(worker.getId());
            assertThat(roles).hasSize(2);
        }

        @Test
        @DisplayName("같은 Worker에게 같은 역할 중복 부여 시 예외 발생")
        void assignRoleToWorker_DuplicateRole_ThrowsException() {
            // Given
            var worker = workerService.createWorkerFromUser(testUser.getId());
            workerRoleService.assignRole(worker.getId(), adminRole.getId());

            // When & Then
            assertThatThrownBy(() -> workerRoleService.assignRole(worker.getId(), adminRole.getId()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("이미 부여된 역할입니다");
        }

        @Test
        @DisplayName("Worker 역할 제거 성공")
        void removeRoleFromWorker_Success() {
            // Given
            var worker = workerService.createWorkerFromUser(testUser.getId());
            workerRoleService.assignRole(worker.getId(), adminRole.getId());

            // When
            workerRoleService.removeRole(worker.getId(), adminRole.getId());

            // Then
            var roles = workerRoleService.findByWorkerId(worker.getId());
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
            var worker = workerService.createWorkerFromUser(testUser.getId());
            assertThat(worker).isNotNull();

            // Step 3: Worker에게 역할 부여
            var workerRole = workerRoleService.assignRole(worker.getId(), adminRole.getId());
            assertThat(workerRole).isNotNull();

            // Step 4: Worker 조회
            var workers = workerService.findByUserId(testUser.getId());
            assertThat(workers).hasSize(1);
        }
    }
}

