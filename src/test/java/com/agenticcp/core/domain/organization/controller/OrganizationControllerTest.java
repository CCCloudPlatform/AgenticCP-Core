package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.organization.dto.*;
import com.agenticcp.core.domain.organization.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationController 테스트")
class OrganizationControllerTest {

    @Mock
    private OrganizationService organizationService;
    
    @InjectMocks
    private OrganizationController organizationController;

    private UserResponse testUserResponse;
    private OrganizationResponse testOrganizationResponse;

    @BeforeEach
    void setUp() {
        testUserResponse = UserResponse.builder()
            .id(1L)
            .username("testuser")
            .email("test@test.com")
            .name("Test User")
            .role(UserRole.VIEWER)
            .status(Status.ACTIVE)
            .department("개발팀")
            .jobTitle("개발자")
            .phoneNumber("010-1234-5678")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        testOrganizationResponse = OrganizationResponse.builder()
            .id(1L)
            .orgName("테스트 조직")
            .description("테스트 조직입니다")
            .status(Status.ACTIVE.name())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("조직별 사용자 목록 조회 성공")
    void 조직별_사용자_목록_조회_성공() {
        // Given
        Long organizationId = 1L;
        List<UserResponse> users = Arrays.asList(testUserResponse);
        
        when(organizationService.getOrganizationUsers(organizationId))
            .thenReturn(users);

        // When
        ResponseEntity<ApiResponse<List<UserResponse>>> response = organizationController.getOrganizationUsers(organizationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getData().get(0).getEmail()).isEqualTo("test@test.com");

        verify(organizationService).getOrganizationUsers(organizationId);
    }

    @Test
    @DisplayName("사용자를 조직에 추가 성공")
    void 사용자를_조직에_추가_성공() {
        // Given
        Long organizationId = 1L;
        AddUserToOrganizationRequest request = new AddUserToOrganizationRequest();
        request.setUserId(1L);
        
        when(organizationService.addUserToOrganization(organizationId, request))
            .thenReturn(testUserResponse);

        // When
        ResponseEntity<ApiResponse<UserResponse>> response = organizationController.addUserToOrganization(organizationId, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getData().getEmail()).isEqualTo("test@test.com");

        verify(organizationService).addUserToOrganization(organizationId, request);
    }

    @Test
    @DisplayName("사용자를 조직에서 제거 성공")
    void 사용자를_조직에서_제거_성공() {
        // Given
        Long organizationId = 1L;
        Long userId = 1L;
        
        doNothing().when(organizationService).removeUserFromOrganization(organizationId, userId);

        // When
        ResponseEntity<ApiResponse<Void>> response = organizationController.removeUserFromOrganization(organizationId, userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(organizationService).removeUserFromOrganization(organizationId, userId);
    }

    // ========== 기본 CRUD API 테스트 ==========

    @Test
    @DisplayName("조직 생성 성공")
    void 조직_생성_성공() {
        // Given
        CreateOrganizationRequest request = new CreateOrganizationRequest();
        request.setOrgName("새 조직");
        request.setDescription("새로운 조직입니다");
        
        when(organizationService.createOrganization(request))
            .thenReturn(testOrganizationResponse);

        // When
        ResponseEntity<ApiResponse<OrganizationResponse>> response = organizationController.createOrganization(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getOrgName()).isEqualTo("테스트 조직");
        assertThat(response.getBody().getMessage()).isEqualTo("조직이 성공적으로 생성되었습니다.");

        verify(organizationService).createOrganization(request);
    }

    @Test
    @DisplayName("조직 조회 성공")
    void 조직_조회_성공() {
        // Given
        Long organizationId = 1L;
        
        when(organizationService.getOrganization(organizationId))
            .thenReturn(testOrganizationResponse);

        // When
        ResponseEntity<ApiResponse<OrganizationResponse>> response = organizationController.getOrganization(organizationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getOrgName()).isEqualTo("테스트 조직");
        assertThat(response.getBody().getMessage()).isEqualTo("조직 정보를 성공적으로 조회했습니다.");

        verify(organizationService).getOrganization(organizationId);
    }

    @Test
    @DisplayName("조직 목록 조회 성공")
    void 조직_목록_조회_성공() {
        // Given
        List<OrganizationResponse> organizations = Arrays.asList(testOrganizationResponse);
        
        when(organizationService.getOrganizations())
            .thenReturn(organizations);

        // When
        ResponseEntity<ApiResponse<List<OrganizationResponse>>> response = organizationController.getOrganizations();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getOrgName()).isEqualTo("테스트 조직");
        assertThat(response.getBody().getMessage()).isEqualTo("조직 목록을 성공적으로 조회했습니다.");

        verify(organizationService).getOrganizations();
    }

    @Test
    @DisplayName("조직 수정 성공")
    void 조직_수정_성공() {
        // Given
        Long organizationId = 1L;
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setOrgName("수정된 조직");
        request.setDescription("수정된 조직입니다");
        
        when(organizationService.updateOrganization(organizationId, request))
            .thenReturn(testOrganizationResponse);

        // When
        ResponseEntity<ApiResponse<OrganizationResponse>> response = organizationController.updateOrganization(organizationId, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getOrgName()).isEqualTo("테스트 조직");
        assertThat(response.getBody().getMessage()).isEqualTo("조직 정보를 성공적으로 수정했습니다.");

        verify(organizationService).updateOrganization(organizationId, request);
    }

    @Test
    @DisplayName("조직 삭제 성공")
    void 조직_삭제_성공() {
        // Given
        Long organizationId = 1L;
        
        doNothing().when(organizationService).deleteOrganization(organizationId);

        // When
        ResponseEntity<ApiResponse<Void>> response = organizationController.deleteOrganization(organizationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo("조직이 성공적으로 삭제되었습니다.");

        verify(organizationService).deleteOrganization(organizationId);
    }

    // ========== 통계 API 테스트 ==========

    @Test
    @DisplayName("조직 수 조회 성공")
    void 조직_수_조회_성공() {
        // Given
        long count = 5L;
        
        when(organizationService.getOrganizationCount())
            .thenReturn(count);

        // When
        ResponseEntity<ApiResponse<Long>> response = organizationController.getOrganizationCount();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo(5L);
        assertThat(response.getBody().getMessage()).isEqualTo("조직 수를 성공적으로 조회했습니다.");

        verify(organizationService).getOrganizationCount();
    }
}