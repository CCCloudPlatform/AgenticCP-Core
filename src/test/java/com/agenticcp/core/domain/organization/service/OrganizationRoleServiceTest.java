package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationRole;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.organization.repository.OrganizationRoleRepository;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.repository.RoleRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationRoleService 단위 테스트")
class OrganizationRoleServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationRoleRepository organizationRoleRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks private OrganizationRoleService organizationRoleService;

    @Nested
    @DisplayName("역할 할당")
    class AssignRoleTest {
        @Test
        @DisplayName("정상 할당 및 기본 역할 설정")
        void assignRole_WithMakeDefault_Success() {
            Organization org = Organization.builder().build();
            Role role = Role.builder().roleKey("ROLE_DEV").roleName("개발자").build();
            OrganizationRole saved = OrganizationRole.builder()
                    .organization(org).role(role)
                    .isDefault(false).priority(5).status(Status.ACTIVE).build();

            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(roleRepository.findById(10L)).willReturn(Optional.of(role));
            given(organizationRoleRepository.existsByOrganizationAndRole(org, role)).willReturn(false);
            given(organizationRoleRepository.save(any(OrganizationRole.class))).willReturn(saved);
            given(organizationRoleRepository.findDefaultByOrganization(org)).willReturn(Optional.empty());

            OrganizationRole result = organizationRoleService.assignRole(1L, 10L, true, 5);

            assertThat(result).isNotNull();
            then(organizationRoleRepository).should(times(2)).save(any(OrganizationRole.class));
        }

        @Test
        @DisplayName("중복 할당 시 예외")
        void assignRole_Duplicate_Throws() {
            Organization org = Organization.builder().build();
            Role role = Role.builder().build();
            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(roleRepository.findById(10L)).willReturn(Optional.of(role));
            given(organizationRoleRepository.existsByOrganizationAndRole(org, role)).willReturn(true);

            assertThatThrownBy(() -> organizationRoleService.assignRole(1L, 10L, false, 0))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("기본 역할 설정")
    class DefaultRoleTest {
        @Test
        @DisplayName("기본 역할 교체")
        void setDefaultRole_SwitchesDefault() {
            Organization org = Organization.builder().build();
            Role roleA = Role.builder().roleKey("ROLE_A").build();
            Role roleB = Role.builder().roleKey("ROLE_B").build();
            OrganizationRole mappingA = OrganizationRole.builder().organization(org).role(roleA).isDefault(true).build();
            OrganizationRole mappingB = OrganizationRole.builder().organization(org).role(roleB).isDefault(false).build();

            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(roleRepository.findById(11L)).willReturn(Optional.of(roleB));
            given(organizationRoleRepository.findByOrganizationAndRole(org, roleB)).willReturn(Optional.of(mappingB));
            given(organizationRoleRepository.findDefaultByOrganization(org)).willReturn(Optional.of(mappingA));

            OrganizationRole result = organizationRoleService.setDefaultRole(1L, 11L);
            assertThat(result.getIsDefault()).isTrue();
            then(organizationRoleRepository).should(times(2)).save(any(OrganizationRole.class));
        }
    }

    @Nested
    @DisplayName("우선순위")
    class PriorityTest {
        @Test
        @DisplayName("우선순위 변경")
        void updatePriority_UpdatesValue() {
            Organization org = Organization.builder().build();
            Role role = Role.builder().build();
            OrganizationRole mapping = OrganizationRole.builder().organization(org).role(role).priority(1).build();

            given(organizationRepository.findById(1L)).willReturn(Optional.of(org));
            given(roleRepository.findById(10L)).willReturn(Optional.of(role));
            given(organizationRoleRepository.findByOrganizationAndRole(org, role)).willReturn(Optional.of(mapping));
            given(organizationRoleRepository.save(any(OrganizationRole.class))).willAnswer(inv -> inv.getArgument(0));

            OrganizationRole updated = organizationRoleService.updatePriority(1L, 10L, 7);
            assertThat(updated.getPriority()).isEqualTo(7);
        }
    }
}


