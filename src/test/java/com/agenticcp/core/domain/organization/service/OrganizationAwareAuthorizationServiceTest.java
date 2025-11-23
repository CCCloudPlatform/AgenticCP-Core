package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationRole;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * OrganizationAwareAuthorizationService 단위 테스트
 * 
 * <p>조직 컨텍스트 기반 인가 확인 서비스의 핵심 기능을 검증합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationAwareAuthorizationService 단위 테스트")
class OrganizationAwareAuthorizationServiceTest {

    @Mock 
    private OrganizationRepository organizationRepository;
    
    @Mock 
    private UserService userService;
    
    @Mock 
    private OrganizationRoleService organizationRoleService;

    @InjectMocks 
    private OrganizationAwareAuthorizationService service;

    @Nested
    @DisplayName("hasRoleInOrganization 테스트")
    class HasRoleInOrganizationTest {
        
        @Test
        @DisplayName("동일 조직에서 매핑된 역할 보유 → true 반환")
        void hasRoleInOrganization_동일조직_역할보유_True반환() {
            // Given
            Organization org = Organization.builder().build();
            org.setId(1L);
            User user = User.builder().organization(org).build();
            Role role = Role.builder().roleKey("ROLE_ADMIN").build();
            OrganizationRole mapping = OrganizationRole.builder()
                    .organization(org)
                    .role(role)
                    .build();

            given(userService.getUserByUsernameOrThrow("alice")).willReturn(user);
            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(organizationRoleService.listRoles(1L)).willReturn(List.of(mapping));

            // When
            boolean result = service.hasRoleInOrganization("alice", 1L, "ROLE_ADMIN");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("다른 조직이면 → false 반환")
        void hasRoleInOrganization_다른조직_False반환() {
            // Given
            Organization orgA = Organization.builder().build();
            orgA.setId(1L);
            Organization orgB = Organization.builder().build();
            orgB.setId(2L);
            User user = User.builder().organization(orgA).build();

            given(userService.getUserByUsernameOrThrow("bob")).willReturn(user);
            given(organizationRepository.findById(2L)).willReturn(Optional.of(orgB));

            // When
            boolean result = service.hasRoleInOrganization("bob", 2L, "ROLE_ADMIN");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("조직이 존재하지 않으면 → false 반환")
        void hasRoleInOrganization_조직없음_False반환() {
            // Given
            Organization userOrg = Organization.builder().build();
            userOrg.setId(1L);
            User user = User.builder().organization(userOrg).build();

            given(userService.getUserByUsernameOrThrow("charlie")).willReturn(user);
            given(organizationRepository.findById(999L)).willReturn(Optional.empty());

            // When
            boolean result = service.hasRoleInOrganization("charlie", 999L, "ROLE_ADMIN");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("사용자의 조직이 없으면 → false 반환")
        void hasRoleInOrganization_사용자조직없음_False반환() {
            // Given
            Organization org = Organization.builder().build();
            org.setId(1L);
            User user = User.builder().organization(null).build();

            given(userService.getUserByUsernameOrThrow("david")).willReturn(user);
            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));

            // When
            boolean result = service.hasRoleInOrganization("david", 1L, "ROLE_ADMIN");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("역할이 매핑되지 않았으면 → false 반환")
        void hasRoleInOrganization_역할없음_False반환() {
            // Given
            Organization org = Organization.builder().build();
            org.setId(1L);
            User user = User.builder().organization(org).build();
            Role role = Role.builder().roleKey("ROLE_USER").build();
            OrganizationRole mapping = OrganizationRole.builder()
                    .organization(org)
                    .role(role)
                    .build();

            given(userService.getUserByUsernameOrThrow("eve")).willReturn(user);
            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(organizationRoleService.listRoles(1L)).willReturn(List.of(mapping));

            // When
            boolean result = service.hasRoleInOrganization("eve", 1L, "ROLE_ADMIN");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hasPermissionInOrganization 테스트")
    class HasPermissionInOrganizationTest {
        
        @Test
        @DisplayName("동일 조직에서 매핑된 권한 보유 → true 반환")
        void hasPermissionInOrganization_동일조직_권한보유_True반환() {
            // Given
            Organization org = Organization.builder().build();
            org.setId(1L);
            User user = User.builder().organization(org).build();
            Permission permission = Permission.builder().permissionKey("PERM_READ").build();
            Role role = Role.builder()
                    .roleKey("ROLE_ADMIN")
                    .permissions(List.of(permission))
                    .build();
            OrganizationRole mapping = OrganizationRole.builder()
                    .organization(org)
                    .role(role)
                    .build();

            given(userService.getUserByUsernameOrThrow("alice")).willReturn(user);
            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(organizationRoleService.listRoles(1L)).willReturn(List.of(mapping));

            // When
            boolean result = service.hasPermissionInOrganization("alice", 1L, "PERM_READ");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("다른 조직이면 → false 반환")
        void hasPermissionInOrganization_다른조직_False반환() {
            // Given
            Organization orgA = Organization.builder().build();
            orgA.setId(1L);
            Organization orgB = Organization.builder().build();
            orgB.setId(2L);
            User user = User.builder().organization(orgA).build();

            given(userService.getUserByUsernameOrThrow("bob")).willReturn(user);
            given(organizationRepository.findById(2L)).willReturn(Optional.of(orgB));

            // When
            boolean result = service.hasPermissionInOrganization("bob", 2L, "PERM_READ");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("조직이 존재하지 않으면 → false 반환")
        void hasPermissionInOrganization_조직없음_False반환() {
            // Given
            Organization userOrg = Organization.builder().build();
            userOrg.setId(1L);
            User user = User.builder().organization(userOrg).build();

            given(userService.getUserByUsernameOrThrow("charlie")).willReturn(user);
            given(organizationRepository.findById(999L)).willReturn(Optional.empty());

            // When
            boolean result = service.hasPermissionInOrganization("charlie", 999L, "PERM_READ");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("사용자의 조직이 없으면 → false 반환")
        void hasPermissionInOrganization_사용자조직없음_False반환() {
            // Given
            Organization org = Organization.builder().build();
            org.setId(1L);
            User user = User.builder().organization(null).build();

            given(userService.getUserByUsernameOrThrow("david")).willReturn(user);
            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));

            // When
            boolean result = service.hasPermissionInOrganization("david", 1L, "PERM_READ");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("권한이 매핑되지 않았으면 → false 반환")
        void hasPermissionInOrganization_권한없음_False반환() {
            // Given
            Organization org = Organization.builder().build();
            org.setId(1L);
            User user = User.builder().organization(org).build();
            Permission permission = Permission.builder().permissionKey("PERM_WRITE").build();
            Role role = Role.builder()
                    .roleKey("ROLE_USER")
                    .permissions(List.of(permission))
                    .build();
            OrganizationRole mapping = OrganizationRole.builder()
                    .organization(org)
                    .role(role)
                    .build();

            given(userService.getUserByUsernameOrThrow("eve")).willReturn(user);
            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(organizationRoleService.listRoles(1L)).willReturn(List.of(mapping));

            // When
            boolean result = service.hasPermissionInOrganization("eve", 1L, "PERM_READ");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("여러 역할 중 하나에 권한이 있으면 → true 반환")
        void hasPermissionInOrganization_여러역할중하나_True반환() {
            // Given
            Organization org = Organization.builder().build();
            org.setId(1L);
            User user = User.builder().organization(org).build();
            Permission permission1 = Permission.builder().permissionKey("PERM_READ").build();
            Permission permission2 = Permission.builder().permissionKey("PERM_WRITE").build();
            Role role1 = Role.builder()
                    .roleKey("ROLE_USER")
                    .permissions(List.of(permission1))
                    .build();
            Role role2 = Role.builder()
                    .roleKey("ROLE_ADMIN")
                    .permissions(List.of(permission2))
                    .build();
            OrganizationRole mapping1 = OrganizationRole.builder()
                    .organization(org)
                    .role(role1)
                    .build();
            OrganizationRole mapping2 = OrganizationRole.builder()
                    .organization(org)
                    .role(role2)
                    .build();

            given(userService.getUserByUsernameOrThrow("frank")).willReturn(user);
            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(organizationRoleService.listRoles(1L)).willReturn(List.of(mapping1, mapping2));

            // When
            boolean result = service.hasPermissionInOrganization("frank", 1L, "PERM_READ");

            // Then
            assertThat(result).isTrue();
        }
    }
}


