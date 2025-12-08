package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.dto.CreateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationResponse;
import com.agenticcp.core.domain.organization.dto.UpdateOrganizationRequest;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
// [DEPRECATED] OrganizationMember로 대체 예정
// import com.agenticcp.core.domain.organization.dto.AddUserToOrganizationRequest;
// import com.agenticcp.core.domain.organization.dto.UserResponse;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrganizationService 단위 테스트
 * 
 * <p>조직 관리 서비스의 핵심 기능을 검증합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationService 단위 테스트")
class OrganizationServiceTest {
    
    @Mock
    private OrganizationRepository organizationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private OrganizationService organizationService;
    
    private Organization testOrganization;
    
    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .orgKey("TEST_ORG")
                .orgName("Test Organization")
                .description("Test Description")
                .status(Status.ACTIVE)
                .build();
        testOrganization.setId(1L);
    }
    
    @Nested
    @DisplayName("createOrganization 테스트")
    class CreateOrganizationTest {
        @Test
        @DisplayName("정상 요청 → 조직 생성 성공")
        void createOrganization_정상요청_조직생성성공() {
            // Given
            CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                    .orgName("New Organization")
                    .description("New Description")
                    .build();
            
            when(organizationRepository.existsByOrgName("New Organization")).thenReturn(false);
            when(organizationRepository.existsByOrgKey(anyString())).thenReturn(false);
            when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
            
            // When
            OrganizationResponse response = organizationService.createOrganization(request);
            
            // Then
            assertThat(response.getOrgName()).isEqualTo("Test Organization");
            assertThat(response.getDescription()).isEqualTo("Test Description");
            
            verify(organizationRepository).existsByOrgName("New Organization");
            verify(organizationRepository).save(any(Organization.class));
        }
        
        @Test
        @DisplayName("중복된 조직명 → BusinessException 발생")
        void createOrganization_중복된조직명_BusinessException발생() {
            // Given
            CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                    .orgName("Existing Organization")
                    .description("New Description")
                    .build();
            
            when(organizationRepository.existsByOrgName("Existing Organization")).thenReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> organizationService.createOrganization(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 존재하는 조직명입니다");
        }
    }
    
    @Nested
    @DisplayName("getOrganization 테스트")
    class GetOrganizationTest {
        @Test
        @DisplayName("존재하는 조직 ID → 조직 정보 반환")
        void getOrganization_존재하는조직ID_조직정보반환() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            
            // When
            OrganizationResponse response = organizationService.getOrganization(1L);
            
            // Then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getOrgName()).isEqualTo("Test Organization");
            
            verify(organizationRepository).findById(1L);
        }
        
        @Test
        @DisplayName("존재하지 않는 조직 ID → BusinessException 발생")
        void getOrganization_존재하지않는조직ID_BusinessException발생() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> organizationService.getOrganization(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("존재하지 않는 조직입니다");
        }
    }
    
    @Nested
    @DisplayName("getOrganizations 테스트")
    class GetOrganizationsTest {
        @Test
        @DisplayName("정상 요청 → 조직 목록 반환")
        void getOrganizations_정상요청_조직목록반환() {
            // Given
            List<Organization> organizations = Arrays.asList(testOrganization);
            when(organizationRepository.findAll()).thenReturn(organizations);
            
            // When
            List<OrganizationResponse> responses = organizationService.getOrganizations();
            
            // Then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getOrgName()).isEqualTo("Test Organization");
            
            verify(organizationRepository).findAll();
        }
    }
    
    @Nested
    @DisplayName("updateOrganization 테스트")
    class UpdateOrganizationTest {
        @Test
        @DisplayName("정상 요청 → 조직 수정 성공")
        void updateOrganization_정상요청_조직수정성공() {
            // Given
            UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                    .orgName("Updated Organization")
                    .description("Updated Description")
                    .build();
            
            Organization updatedOrganization = Organization.builder()
                    .orgKey("UPDATED_ORG")
                    .orgName("Updated Organization")
                    .description("Updated Description")
                    .status(Status.ACTIVE)
                    .build();
            updatedOrganization.setId(1L);
            
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(organizationRepository.existsByOrgName("Updated Organization")).thenReturn(false);
            when(organizationRepository.save(any(Organization.class))).thenReturn(updatedOrganization);
            
            // When
            OrganizationResponse response = organizationService.updateOrganization(1L, request);
            
            // Then
            assertThat(response.getOrgName()).isEqualTo("Updated Organization");
            
            verify(organizationRepository).findById(1L);
            verify(organizationRepository).existsByOrgName("Updated Organization");
            verify(organizationRepository).save(any(Organization.class));
        }
    }
    
    @Nested
    @DisplayName("deleteOrganization 테스트")
    class DeleteOrganizationTest {
        @Test
        @DisplayName("하위 조직 없음 → 조직 삭제 성공")
        void deleteOrganization_하위조직없음_조직삭제성공() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(organizationRepository.existsByParentOrganizationId(1L)).thenReturn(false);
            
            // When
            organizationService.deleteOrganization(1L);
            
            // Then
            verify(organizationRepository).findById(1L);
            verify(organizationRepository).existsByParentOrganizationId(1L);
            verify(organizationRepository).delete(testOrganization);
        }
        
        @Test
        @DisplayName("하위 조직 존재 → BusinessException 발생")
        void deleteOrganization_하위조직존재_BusinessException발생() {
            // Given
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(organizationRepository.existsByParentOrganizationId(1L)).thenReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> organizationService.deleteOrganization(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("하위 조직이 존재하는 조직은 삭제할 수 없습니다");
        }
    }
    
    @Nested
    @DisplayName("getOrganizationCount 테스트")
    class GetOrganizationCountTest {
        @Test
        @DisplayName("정상 요청 → 조직 수 반환")
        void getOrganizationCount_정상요청_조직수반환() {
            // Given
            when(organizationRepository.count()).thenReturn(5L);
            
            // When
            long count = organizationService.getOrganizationCount();
            
            // Then
            assertThat(count).isEqualTo(5L);
            
            verify(organizationRepository).count();
        }
    }
    
    // ========== [DEPRECATED] 사용자 관련 테스트 - OrganizationMember로 대체 예정 ==========
    /*
    @Nested
    @DisplayName("[DEPRECATED] getOrganizationUsers 테스트")
    class GetOrganizationUsersTest {
        // OrganizationMember로 대체 예정
    }
    
    @Nested
    @DisplayName("[DEPRECATED] addUserToOrganization 테스트")
    class AddUserToOrganizationTest {
        // OrganizationMember로 대체 예정
    }
    
    @Nested
    @DisplayName("[DEPRECATED] removeUserFromOrganization 테스트")
    class RemoveUserFromOrganizationTest {
        // OrganizationMember로 대체 예정
    }
    */

    // ========== 테넌트 관련 테스트 (1:1 관계) ==========

    @Nested
    @DisplayName("hasTenant 테스트")
    class HasTenantTest {
        @Test
        @DisplayName("테넌트 존재 시 true 반환")
        void hasTenant_테넌트존재시_true반환() {
            // Given
            Long organizationId = 1L;
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(testOrganization));
            when(organizationRepository.existsTenantByOrganizationId(organizationId))
                .thenReturn(true);
            
            // When
            boolean result = organizationService.hasTenant(organizationId);
            
            // Then
            assertThat(result).isTrue();
            verify(organizationRepository).existsTenantByOrganizationId(organizationId);
        }
        
        @Test
        @DisplayName("테넌트 없을 시 false 반환")
        void hasTenant_테넌트없을시_false반환() {
            // Given
            Long organizationId = 1L;
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(testOrganization));
            when(organizationRepository.existsTenantByOrganizationId(organizationId))
                .thenReturn(false);
            
            // When
            boolean result = organizationService.hasTenant(organizationId);
            
            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hasActiveTenant 테스트")
    class HasActiveTenantTest {
        @Test
        @DisplayName("활성 테넌트 존재 시 true 반환")
        void hasActiveTenant_활성테넌트존재시_true반환() {
            // Given
            Long organizationId = 1L;
            com.agenticcp.core.domain.tenant.entity.Tenant activeTenant = 
                com.agenticcp.core.domain.tenant.entity.Tenant.builder()
                    .tenantKey("TEST_TENANT")
                    .tenantName("Test Tenant")
                    .status(Status.ACTIVE)
                    .build();
            activeTenant.setId(1L);
            
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(testOrganization));
            when(organizationRepository.findTenantByOrganizationId(organizationId))
                .thenReturn(Optional.of(activeTenant));
            
            // When
            boolean result = organizationService.hasActiveTenant(organizationId);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("비활성 테넌트 시 false 반환")
        void hasActiveTenant_비활성테넌트시_false반환() {
            // Given
            Long organizationId = 1L;
            com.agenticcp.core.domain.tenant.entity.Tenant inactiveTenant = 
                com.agenticcp.core.domain.tenant.entity.Tenant.builder()
                    .tenantKey("TEST_TENANT")
                    .tenantName("Test Tenant")
                    .status(Status.INACTIVE)
                    .build();
            inactiveTenant.setId(1L);
            
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(testOrganization));
            when(organizationRepository.findTenantByOrganizationId(organizationId))
                .thenReturn(Optional.of(inactiveTenant));
            
            // When
            boolean result = organizationService.hasActiveTenant(organizationId);
            
            // Then
            assertThat(result).isFalse();
        }
    }
    
    // [DEPRECATED] Helper method - 사용자 관련 테스트 제거됨
    /*
    private User createTestUser(Long id, String username, String email) {
        User user = User.builder()
            .username(username)
            .email(email)
            .name("Test User")
            .role(UserRole.VIEWER)
            .status(Status.ACTIVE)
            .build();
        user.setId(id);
        return user;
    }
    */
}