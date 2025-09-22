package com.agenticcp.core.domain.user;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.dto.CreateRoleRequest;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import com.agenticcp.core.domain.user.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Feature 1 시나리오 테스트
 * 
 * 관련 이슈: #13 - https://github.com/your-org/AgenticCP-Core/issues/13
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Feature1ScenarioTest {
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    private Tenant testTenant;
    
    @BeforeEach
    void setUp() {
        // 테스트용 테넌트 생성
        testTenant = Tenant.builder()
                .tenantKey("test-tenant")
                .tenantName("Test Tenant")
                .description("Test tenant for feature 1 scenarios")
                .status(Status.ACTIVE)
                .build();
        
        // 테넌트 컨텍스트 설정
        TenantContextHolder.setTenant(testTenant);
        
        // 테스트용 권한 생성
        createTestPermissions();
    }
    
    /**
     * 시나리오 1: 역할 생성
     * Given: 관리자가 새로운 역할을 생성하려고 함
     * When: 역할명, 설명, 권한 목록을 입력하여 생성 요청
     * Then: 역할이 생성되고 지정된 권한들이 자동으로 매핑됨
     */
    @Test
    void scenario1_createRole() {
        // Given: 관리자가 새로운 역할을 생성하려고 함
        CreateRoleRequest request = CreateRoleRequest.builder()
                .roleKey("TEST_ROLE")
                .roleName("테스트 역할")
                .description("테스트용 역할입니다")
                .priority(50)
                .permissionKeys(Arrays.asList("USER_READ", "USER_CREATE"))
                .build();
        
        // When: 역할명, 설명, 권한 목록을 입력하여 생성 요청
        Role createdRole = roleService.createRole(request);
        
        // Then: 역할이 생성되고 지정된 권한들이 자동으로 매핑됨
        assertThat(createdRole).isNotNull();
        assertThat(createdRole.getRoleKey()).isEqualTo("TEST_ROLE");
        assertThat(createdRole.getRoleName()).isEqualTo("테스트 역할");
        assertThat(createdRole.getDescription()).isEqualTo("테스트용 역할입니다");
        assertThat(createdRole.getPriority()).isEqualTo(50);
        assertThat(createdRole.getTenant()).isEqualTo(testTenant);
        assertThat(createdRole.getIsSystem()).isFalse();
        assertThat(createdRole.getIsDefault()).isFalse();
        assertThat(createdRole.getStatus()).isEqualTo(Status.ACTIVE);
        
        // 권한 매핑 확인
        assertThat(createdRole.getPermissions()).isNotNull();
        assertThat(createdRole.getPermissions()).hasSize(2);
        assertThat(createdRole.getPermissions().stream()
                .map(Permission::getPermissionKey))
                .containsExactlyInAnyOrder("USER_READ", "USER_CREATE");
        
        // 데이터베이스에 저장되었는지 확인
        Optional<Role> savedRole = roleRepository.findByRoleKey("TEST_ROLE");
        assertThat(savedRole).isPresent();
        assertThat(savedRole.get().getRoleKey()).isEqualTo("TEST_ROLE");
    }
    
    /**
     * 시나리오 2: 권한 할당
     * Given: 기존 역할에 권한을 추가하려고 함
     * When: 역할에 새로운 권한을 할당
     * Then: 해당 역할을 가진 모든 사용자가 자동으로 새 권한을 획득함
     */
    @Test
    void scenario2_assignPermissions() {
        // Given: 기존 역할에 권한을 추가하려고 함
        Role existingRole = createTestRole();
        
        // When: 역할에 새로운 권한을 할당
        List<String> newPermissionKeys = Arrays.asList("USER_UPDATE", "USER_DELETE");
        roleService.assignPermissionsToRole(existingRole.getId(), newPermissionKeys);
        
        // Then: 해당 역할을 가진 모든 사용자가 자동으로 새 권한을 획득함
        Role updatedRole = roleService.getRoleByKeyOrThrow(existingRole.getRoleKey());
        assertThat(updatedRole.getPermissions()).isNotNull();
        assertThat(updatedRole.getPermissions()).hasSize(2);
        assertThat(updatedRole.getPermissions().stream()
                .map(Permission::getPermissionKey))
                .containsExactlyInAnyOrder("USER_UPDATE", "USER_DELETE");
        
        // 권한이 올바르게 저장되었는지 확인
        Optional<Role> savedRole = roleRepository.findByRoleKey(existingRole.getRoleKey());
        assertThat(savedRole).isPresent();
        assertThat(savedRole.get().getPermissions()).hasSize(2);
    }
    
    /**
     * 시나리오 3: 시스템 역할 보호
     * Given: 시스템 역할(SUPER_ADMIN)을 삭제하려고 함
     * When: 삭제 요청
     * Then: "시스템 역할은 삭제할 수 없습니다" 오류 반환
     */
    @Test
    void scenario3_systemRoleProtection() {
        // Given: 시스템 역할(SUPER_ADMIN)을 삭제하려고 함
        Role systemRole = createSystemRole();
        
        // When: 삭제 요청
        // Then: "시스템 역할은 삭제할 수 없습니다" 오류 반환
        assertThatThrownBy(() -> roleService.deleteRole(systemRole.getRoleKey()))
                .isInstanceOf(com.agenticcp.core.common.exception.BusinessException.class)
                .hasMessage("시스템 역할은 삭제할 수 없습니다");
        
        // 시스템 역할이 삭제되지 않았는지 확인
        Optional<Role> notDeletedRole = roleRepository.findByRoleKey(systemRole.getRoleKey());
        assertThat(notDeletedRole).isPresent();
        assertThat(notDeletedRole.get().getIsDeleted()).isFalse();
    }
    
    /**
     * 추가 테스트: 기본 역할 삭제 방지
     */
    @Test
    void test_defaultRoleProtection() {
        // Given: 기본 역할을 삭제하려고 함
        Role defaultRole = createDefaultRole();
        
        // When: 삭제 요청
        // Then: "기본 역할은 삭제할 수 없습니다" 오류 반환
        assertThatThrownBy(() -> roleService.deleteRole(defaultRole.getRoleKey()))
                .isInstanceOf(com.agenticcp.core.common.exception.BusinessException.class)
                .hasMessage("기본 역할은 삭제할 수 없습니다");
    }
    
    /**
     * 추가 테스트: 테넌트 격리 확인
     */
    @Test
    void test_tenantIsolation() {
        // Given: 다른 테넌트의 역할이 존재함
        Tenant otherTenant = Tenant.builder()
                .tenantKey("other-tenant")
                .tenantName("Other Tenant")
                .status(Status.ACTIVE)
                .build();
        
        Role otherTenantRole = Role.builder()
                .roleKey("OTHER_ROLE")
                .roleName("다른 테넌트 역할")
                .tenant(otherTenant)
                .isSystem(false)
                .isDefault(false)
                .priority(50)
                .status(Status.ACTIVE)
                .build();
        roleRepository.save(otherTenantRole);
        
        // When: 현재 테넌트에서 모든 역할을 조회
        List<Role> currentTenantRoles = roleService.getAllRoles();
        
        // Then: 다른 테넌트의 역할은 조회되지 않음
        assertThat(currentTenantRoles).isNotEmpty();
        assertThat(currentTenantRoles.stream()
                .anyMatch(role -> "OTHER_ROLE".equals(role.getRoleKey())))
                .isFalse();
    }
    
    /**
     * 테스트용 권한 생성
     */
    private void createTestPermissions() {
        List<Permission> permissions = Arrays.asList(
                createPermission("USER_READ", "사용자 조회", "사용자 정보를 조회할 수 있는 권한", "USER", "READ", "USER_MANAGEMENT", 100),
                createPermission("USER_CREATE", "사용자 생성", "사용자를 생성할 수 있는 권한", "USER", "CREATE", "USER_MANAGEMENT", 100),
                createPermission("USER_UPDATE", "사용자 수정", "사용자 정보를 수정할 수 있는 권한", "USER", "UPDATE", "USER_MANAGEMENT", 100),
                createPermission("USER_DELETE", "사용자 삭제", "사용자를 삭제할 수 있는 권한", "USER", "DELETE", "USER_MANAGEMENT", 100)
        );
        permissionRepository.saveAll(permissions);
    }
    
    /**
     * 테스트용 역할 생성
     */
    private Role createTestRole() {
        Role role = Role.builder()
                .roleKey("TEST_ROLE_2")
                .roleName("테스트 역할 2")
                .description("테스트용 역할 2입니다")
                .tenant(testTenant)
                .isSystem(false)
                .isDefault(false)
                .priority(50)
                .status(Status.ACTIVE)
                .build();
        return roleRepository.save(role);
    }
    
    /**
     * 테스트용 시스템 역할 생성
     */
    private Role createSystemRole() {
        Role role = Role.builder()
                .roleKey("SUPER_ADMIN")
                .roleName("최고 관리자")
                .description("모든 권한을 가진 최고 관리자 역할")
                .tenant(testTenant)
                .isSystem(true)
                .isDefault(false)
                .priority(100)
                .status(Status.ACTIVE)
                .build();
        return roleRepository.save(role);
    }
    
    /**
     * 테스트용 기본 역할 생성
     */
    private Role createDefaultRole() {
        Role role = Role.builder()
                .roleKey("DEFAULT_ROLE")
                .roleName("기본 역할")
                .description("기본 역할입니다")
                .tenant(testTenant)
                .isSystem(false)
                .isDefault(true)
                .priority(50)
                .status(Status.ACTIVE)
                .build();
        return roleRepository.save(role);
    }
    
    /**
     * 테스트용 권한 생성 헬퍼
     */
    private Permission createPermission(String key, String name, String description, 
                                      String resource, String action, String category, Integer priority) {
        return Permission.builder()
                .permissionKey(key)
                .permissionName(name)
                .description(description)
                .tenant(testTenant)
                .resource(resource)
                .action(action)
                .isSystem(false)
                .category(category)
                .priority(priority)
                .status(Status.ACTIVE)
                .build();
    }
}
