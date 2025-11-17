package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.OrganizationRole;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.organization.repository.OrganizationRoleRepository;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrganizationRoleService 단위 테스트
 * 
 * <p>스프링 컨텍스트 없이 Mock을 사용한 순수 단위 테스트입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationRoleService 단위 테스트")
class OrganizationRoleServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    
    @Mock
    private OrganizationRoleRepository organizationRoleRepository;
    
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private OrganizationRoleService organizationRoleService;

    private Organization testOrganization;
    private Role testRole;
    private OrganizationRole testOrganizationRole;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
            .orgName("테스트 조직")
            .build();
        testOrganization.setId(1L);

        testRole = Role.builder()
            .roleKey("ROLE_DEV")
            .roleName("개발자")
            .build();
        testRole.setId(10L);

        testOrganizationRole = OrganizationRole.builder()
            .organization(testOrganization)
            .role(testRole)
            .isDefault(false)
            .priority(5)
            .status(Status.ACTIVE)
            .build();
        testOrganizationRole.setId(1L);
    }

    @Nested
    @DisplayName("역할 목록 조회 테스트")
    class ListRolesTest {
        @Test
        @DisplayName("정상 조회 시 역할 목록 반환")
        void listRoles_WhenValidOrganizationId_ReturnsRoleList() {
            // Given
            List<OrganizationRole> roles = Arrays.asList(testOrganizationRole);
            
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(organizationRoleRepository.findByOrganizationOrderByPriority(testOrganization))
                .thenReturn(roles);

            // When
            List<OrganizationRole> result = organizationRoleService.listRoles(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRole().getRoleKey()).isEqualTo("ROLE_DEV");
            
            verify(organizationRepository).findById(1L);
            verify(organizationRoleRepository).findByOrganizationOrderByPriority(testOrganization);
        }
    }

    @Nested
    @DisplayName("역할 할당 테스트")
    class AssignRoleTest {
        @Test
        @DisplayName("정상 할당 시 매핑 반환")
        void assignRole_WhenValidRequest_ReturnsMapping() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(roleRepository.findById(10L)).thenReturn(Optional.of(testRole));
            when(organizationRoleRepository.existsByOrganizationAndRole(testOrganization, testRole))
                .thenReturn(false);
            when(organizationRoleRepository.save(any(OrganizationRole.class)))
                .thenReturn(testOrganizationRole);

            // When
            OrganizationRole result = organizationRoleService.assignRole(1L, 10L, false, 5);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPriority()).isEqualTo(5);
            
            verify(organizationRepository).findById(1L);
            verify(roleRepository).findById(10L);
            verify(organizationRoleRepository).existsByOrganizationAndRole(testOrganization, testRole);
            verify(organizationRoleRepository).save(any(OrganizationRole.class));
        }

        @Test
        @DisplayName("기본 역할로 설정 시 기본 역할 설정됨")
        void assignRole_WhenMakeDefault_ReturnsMappingWithDefault() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(roleRepository.findById(10L)).thenReturn(Optional.of(testRole));
            when(organizationRoleRepository.existsByOrganizationAndRole(testOrganization, testRole))
                .thenReturn(false);
            when(organizationRoleRepository.save(any(OrganizationRole.class)))
                .thenReturn(testOrganizationRole);
            when(organizationRoleRepository.findDefaultByOrganization(testOrganization))
                .thenReturn(Optional.empty());

            // When
            OrganizationRole result = organizationRoleService.assignRole(1L, 10L, true, 5);

            // Then
            assertThat(result).isNotNull();
            verify(organizationRoleRepository, times(2)).save(any(OrganizationRole.class));
        }

        @Test
        @DisplayName("이미 할당된 역할인 경우 예외 발생")
        void assignRole_WhenAlreadyAssigned_ThrowsException() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(roleRepository.findById(10L)).thenReturn(Optional.of(testRole));
            when(organizationRoleRepository.existsByOrganizationAndRole(testOrganization, testRole))
                .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> organizationRoleService.assignRole(1L, 10L, false, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 조직에 할당된 역할입니다");
            
            verify(organizationRoleRepository, never()).save(any(OrganizationRole.class));
        }
    }

    @Nested
    @DisplayName("역할 제거 테스트")
    class RemoveRoleTest {
        @Test
        @DisplayName("정상 제거 시 소프트 삭제 확인")
        void removeRole_WhenValidIds_SoftDeletesMapping() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(roleRepository.findById(10L)).thenReturn(Optional.of(testRole));
            when(organizationRoleRepository.findByOrganizationAndRole(testOrganization, testRole))
                .thenReturn(Optional.of(testOrganizationRole));
            when(organizationRoleRepository.save(any(OrganizationRole.class)))
                .thenReturn(testOrganizationRole);

            // When
            organizationRoleService.removeRole(1L, 10L);

            // Then
            assertThat(testOrganizationRole.getIsDeleted()).isTrue();
            verify(organizationRoleRepository).findByOrganizationAndRole(testOrganization, testRole);
            verify(organizationRoleRepository).save(testOrganizationRole);
        }

        @Test
        @DisplayName("할당되지 않은 역할인 경우 예외 발생")
        void removeRole_WhenNotAssigned_ThrowsException() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(roleRepository.findById(10L)).thenReturn(Optional.of(testRole));
            when(organizationRoleRepository.findByOrganizationAndRole(testOrganization, testRole))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationRoleService.removeRole(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("조직에 할당되지 않은 역할입니다");
            
            verify(organizationRoleRepository, never()).save(any(OrganizationRole.class));
        }
    }

    @Nested
    @DisplayName("기본 역할 설정 테스트")
    class SetDefaultRoleTest {
        @Test
        @DisplayName("정상 설정 시 기본 역할 변경 확인")
        void setDefaultRole_WhenValidIds_ChangesDefaultRole() {
            // Given
            Role oldRole = Role.builder().roleKey("ROLE_OLD").build();
            OrganizationRole existingDefault = OrganizationRole.builder()
                .organization(testOrganization)
                .role(oldRole)
                .isDefault(true)
                .build();
            existingDefault.setId(2L);

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(roleRepository.findById(10L)).thenReturn(Optional.of(testRole));
            when(organizationRoleRepository.findByOrganizationAndRole(testOrganization, testRole))
                .thenReturn(Optional.of(testOrganizationRole));
            when(organizationRoleRepository.findDefaultByOrganization(testOrganization))
                .thenReturn(Optional.of(existingDefault));
            when(organizationRoleRepository.save(any(OrganizationRole.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            OrganizationRole result = organizationRoleService.setDefaultRole(1L, 10L);

            // Then
            assertThat(result.getIsDefault()).isTrue();
            verify(organizationRoleRepository, times(2)).save(any(OrganizationRole.class));
        }

        @Test
        @DisplayName("할당되지 않은 역할인 경우 예외 발생")
        void setDefaultRole_WhenNotAssigned_ThrowsException() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(roleRepository.findById(10L)).thenReturn(Optional.of(testRole));
            when(organizationRoleRepository.findByOrganizationAndRole(testOrganization, testRole))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationRoleService.setDefaultRole(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("조직에 할당되지 않은 역할입니다");
        }
    }

    @Nested
    @DisplayName("우선순위 업데이트 테스트")
    class UpdatePriorityTest {
        @Test
        @DisplayName("정상 업데이트 시 우선순위 변경 확인")
        void updatePriority_WhenValidRequest_UpdatesPriority() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(roleRepository.findById(10L)).thenReturn(Optional.of(testRole));
            when(organizationRoleRepository.findByOrganizationAndRole(testOrganization, testRole))
                .thenReturn(Optional.of(testOrganizationRole));
            when(organizationRoleRepository.save(any(OrganizationRole.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            OrganizationRole result = organizationRoleService.updatePriority(1L, 10L, 7);

            // Then
            assertThat(result.getPriority()).isEqualTo(7);
            verify(organizationRoleRepository).findByOrganizationAndRole(testOrganization, testRole);
            verify(organizationRoleRepository).save(testOrganizationRole);
        }

        @Test
        @DisplayName("할당되지 않은 역할인 경우 예외 발생")
        void updatePriority_WhenNotAssigned_ThrowsException() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(roleRepository.findById(10L)).thenReturn(Optional.of(testRole));
            when(organizationRoleRepository.findByOrganizationAndRole(testOrganization, testRole))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationRoleService.updatePriority(1L, 10L, 7))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("조직에 할당되지 않은 역할입니다");
            
            verify(organizationRoleRepository, never()).save(any(OrganizationRole.class));
        }
    }
}
