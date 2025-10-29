package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationRole;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.organization.repository.OrganizationRoleRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationAwareAuthorizationService 단위 테스트")
class OrganizationAwareAuthorizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationRoleRepository organizationRoleRepository; // 현재 직접 사용은 안하지만 주입 구조 유지
    @Mock private UserService userService;
    @Mock private OrganizationRoleService organizationRoleService;

    @InjectMocks private OrganizationAwareAuthorizationService service;

    @Nested
    @DisplayName("역할 인가")
    class RoleAuthTest {
        @Test
        @DisplayName("동일 조직에서 매핑된 역할 보유 → true")
        void hasRoleInOrganization_SameOrgAndMappedRole_ReturnsTrue() {
            Organization org = Organization.builder().build();
            User user = User.builder().organization(org).build();
            OrganizationRole mapping = OrganizationRole.builder().organization(org)
                    .role(com.agenticcp.core.domain.user.entity.Role.builder().roleKey("ROLE_ADMIN").build())
                    .build();

            given(userService.getUserByUsernameOrThrow("alice")).willReturn(user);
            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(organizationRoleService.listRoles(1L)).willReturn(List.of(mapping));

            boolean result = service.hasRoleInOrganization("alice", 1L, "ROLE_ADMIN");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("다른 조직이면 → false")
        void hasRoleInOrganization_DifferentOrg_ReturnsFalse() {
            Organization orgA = Organization.builder().build();
            Organization orgB = Organization.builder().build();
            User user = User.builder().organization(orgA).build();

            given(userService.getUserByUsernameOrThrow("bob")).willReturn(user);
            given(organizationRepository.findById(2L)).willReturn(Optional.of(orgB));

            boolean result = service.hasRoleInOrganization("bob", 2L, "ROLE_ADMIN");
            assertThat(result).isFalse();
        }
    }
}


