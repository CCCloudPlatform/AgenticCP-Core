package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.dto.AddUserToOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.CreateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationResponse;
import com.agenticcp.core.domain.organization.dto.UpdateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.UserResponse;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
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
    
    @Nested
    @DisplayName("getOrganizationUsers 테스트")
    class GetOrganizationUsersTest {
        @Test
        @DisplayName("존재하는 조직 ID → 사용자 목록 반환")
        void getOrganizationUsers_존재하는조직ID_사용자목록반환() {
            // Given
            Long organizationId = 1L;
            User user1 = createTestUser(1L, "user1", "user1@test.com");
            User user2 = createTestUser(2L, "user2", "user2@test.com");
            List<User> users = Arrays.asList(user1, user2);
            
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(testOrganization));
            when(userRepository.findByOrganizationId(organizationId))
                .thenReturn(users);
            
            // When
            List<UserResponse> result = organizationService.getOrganizationUsers(organizationId);
            
            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUsername()).isEqualTo("user1");
            assertThat(result.get(1).getUsername()).isEqualTo("user2");
            
            verify(organizationRepository).findById(organizationId);
            verify(userRepository).findByOrganizationId(organizationId);
        }
        
        @Test
        @DisplayName("존재하지 않는 조직 ID → BusinessException 발생")
        void getOrganizationUsers_존재하지않는조직ID_BusinessException발생() {
            // Given
            Long organizationId = 999L;
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> organizationService.getOrganizationUsers(organizationId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("존재하지 않는 조직입니다");
            
            verify(organizationRepository).findById(organizationId);
            verify(userRepository, never()).findByOrganizationId(any());
        }
    }
    
    @Nested
    @DisplayName("addUserToOrganization 테스트")
    class AddUserToOrganizationTest {
        @Test
        @DisplayName("정상 요청 → 사용자 추가 성공")
        void addUserToOrganization_정상요청_사용자추가성공() {
            // Given
            Long organizationId = 1L;
            Long userId = 1L;
            User testUser = createTestUser(userId, "testuser", "test@test.com");
            AddUserToOrganizationRequest request = new AddUserToOrganizationRequest();
            request.setUserId(userId);
            
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(testOrganization));
            when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
            
            // When
            UserResponse result = organizationService.addUserToOrganization(organizationId, request);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            
            verify(organizationRepository).findById(organizationId);
            verify(userRepository).findById(userId);
            verify(userRepository).save(testUser);
        }
        
        @Test
        @DisplayName("이미 조직에 속한 사용자 → BusinessException 발생")
        void addUserToOrganization_이미조직에속한사용자_BusinessException발생() {
            // Given
            Long organizationId = 1L;
            Long userId = 1L;
            User testUser = createTestUser(userId, "testuser", "test@test.com");
            testUser.setOrganization(testOrganization); // 이미 조직에 속함
            AddUserToOrganizationRequest request = new AddUserToOrganizationRequest();
            request.setUserId(userId);
            
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(testOrganization));
            when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
            
            // When & Then
            assertThatThrownBy(() -> organizationService.addUserToOrganization(organizationId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 해당 조직에 속한 사용자입니다");
            
            verify(organizationRepository).findById(organizationId);
            verify(userRepository).findById(userId);
            verify(userRepository, never()).save(any());
        }
    }
    
    @Nested
    @DisplayName("removeUserFromOrganization 테스트")
    class RemoveUserFromOrganizationTest {
        @Test
        @DisplayName("정상 요청 → 사용자 제거 성공")
        void removeUserFromOrganization_정상요청_사용자제거성공() {
            // Given
            Long 
            organizationId = 1L;
            Long userId = 1L;
            User testUser = createTestUser(userId, "testuser", "test@test.com");
            testUser.setOrganization(testOrganization);
            
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(testOrganization));
            when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
            
            // When
            organizationService.removeUserFromOrganization(organizationId, userId);
            
            // Then
            verify(organizationRepository).findById(organizationId);
            verify(userRepository).findById(userId);
            verify(userRepository).save(testUser);
        }
        
        @Test
        @DisplayName("조직에 속하지 않은 사용자 → BusinessException 발생")
        void removeUserFromOrganization_조직에속하지않은사용자_BusinessException발생() {
            // Given
            Long organizationId = 1L;
            Long userId = 1L;
            User testUser = createTestUser(userId, "testuser", "test@test.com");
            // testUser.setOrganization(null); // 조직에 속하지 않음
            
            when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(testOrganization));
            when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
            
            // When & Then
            assertThatThrownBy(() -> organizationService.removeUserFromOrganization(organizationId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용자가 해당 조직에 속하지 않습니다");
            
            verify(organizationRepository).findById(organizationId);
            verify(userRepository).findById(userId);
            verify(userRepository, never()).save(any());
        }
    }
    
    // Helper method
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
}