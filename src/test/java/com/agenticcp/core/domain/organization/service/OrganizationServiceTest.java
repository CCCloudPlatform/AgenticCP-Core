package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationService 테스트")
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
    
    @Test
    @DisplayName("조직 생성 성공")
    void 조직_생성_성공() {
        // Given
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .orgName("New Organization")
                .description("New Description")
                .build();
        
        when(organizationRepository.existsByOrgName("New Organization")).thenReturn(false);
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
    @DisplayName("조직 생성 실패 - 중복된 조직명")
    void 조직_생성_실패_중복된_조직명() {
        // Given
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .orgName("Existing Organization")
                .description("New Description")
                .build();
        
        when(organizationRepository.existsByOrgName("Existing Organization")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> organizationService.createOrganization(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 조직명입니다: Existing Organization");
    }
    
    @Test
    @DisplayName("조직 조회 성공")
    void 조직_조회_성공() {
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
    @DisplayName("조직 조회 실패 - 존재하지 않는 조직")
    void 조직_조회_실패_존재하지_않는_조직() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> organizationService.getOrganization(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 조직입니다: 1");
    }
    
    @Test
    @DisplayName("조직 목록 조회 성공")
    void 조직_목록_조회_성공() {
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
    
    @Test
    @DisplayName("조직 수정 성공")
    void 조직_수정_성공() {
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
    
    @Test
    @DisplayName("조직 삭제 성공")
    void 조직_삭제_성공() {
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
    @DisplayName("조직 삭제 실패 - 하위 조직 존재")
    void 조직_삭제_실패_하위_조직_존재() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.existsByParentOrganizationId(1L)).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> organizationService.deleteOrganization(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("하위 조직이 존재하는 조직은 삭제할 수 없습니다: 1");
    }
    
    @Test
    @DisplayName("조직 수 조회 성공")
    void 조직_수_조회_성공() {
        // Given
        when(organizationRepository.count()).thenReturn(5L);
        
        // When
        long count = organizationService.getOrganizationCount();
        
        // Then
        assertThat(count).isEqualTo(5L);
        
        verify(organizationRepository).count();
    }
    
    // ========== 조직-사용자 관계 관리 테스트 ==========
    
    @Test
    @DisplayName("조직별 사용자 목록 조회 성공")
    void 조직별_사용자_목록_조회_성공() {
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
    @DisplayName("조직별 사용자 목록 조회 - 조직이 존재하지 않음")
    void 조직별_사용자_목록_조회_조직_존재하지_않음() {
        // Given
        Long organizationId = 999L;
        when(organizationRepository.findById(organizationId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> organizationService.getOrganizationUsers(organizationId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Organization");
        
        verify(organizationRepository).findById(organizationId);
        verify(userRepository, never()).findByOrganizationId(any());
    }
    
    @Test
    @DisplayName("사용자를 조직에 추가 성공")
    void 사용자를_조직에_추가_성공() {
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
    @DisplayName("사용자를 조직에 추가 - 이미 조직에 속한 사용자")
    void 사용자를_조직에_추가_이미_속한_사용자() {
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
    
    @Test
    @DisplayName("사용자를 조직에서 제거 성공")
    void 사용자를_조직에서_제거_성공() {
        // Given
        Long organizationId = 1L;
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
    @DisplayName("사용자를 조직에서 제거 - 해당 조직에 속하지 않은 사용자")
    void 사용자를_조직에서_제거_속하지_않은_사용자() {
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
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User");
        
        verify(organizationRepository).findById(organizationId);
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
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