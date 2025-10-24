package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.organization.dto.AddUserToOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.UserResponse;
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
        ResponseEntity<List<UserResponse>> response = organizationController.getOrganizationUsers(organizationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().get(0).getEmail()).isEqualTo("test@test.com");

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
        ResponseEntity<UserResponse> response = organizationController.addUserToOrganization(organizationId, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getEmail()).isEqualTo("test@test.com");

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
        ResponseEntity<Void> response = organizationController.removeUserFromOrganization(organizationId, userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(organizationService).removeUserFromOrganization(organizationId, userId);
    }
}