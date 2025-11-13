package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.organization.dto.*;
import com.agenticcp.core.domain.organization.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/**
 * OrganizationController 단위 테스트
 * 
 * <p>스프링 컨텍스트 없이 Mock을 사용한 순수 단위 테스트입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationController 단위 테스트")
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

    @Nested
    @DisplayName("조직 생성 테스트")
    class CreateOrganizationTest {
        @Test
        @DisplayName("정상 생성 시 201 반환")
        void createOrganization_WhenValidRequest_ReturnsCreated() {
            // Given
            CreateOrganizationRequest request = new CreateOrganizationRequest();
            request.setOrgName("새 조직");
            request.setDescription("새로운 조직입니다");
            
            when(organizationService.createOrganization(request))
                .thenReturn(testOrganizationResponse);

            // When
            ResponseEntity<ApiResponse<OrganizationResponse>> response = 
                organizationController.createOrganization(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getData().getOrgName()).isEqualTo("테스트 조직");
            assertThat(response.getBody().getMessage()).isEqualTo("조직이 성공적으로 생성되었습니다.");

            verify(organizationService).createOrganization(request);
        }
    }

    @Nested
    @DisplayName("조직 조회 테스트")
    class GetOrganizationTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getOrganization_WhenValidId_ReturnsOk() {
            // Given
            Long organizationId = 1L;
            
            when(organizationService.getOrganization(organizationId))
                .thenReturn(testOrganizationResponse);

            // When
            ResponseEntity<ApiResponse<OrganizationResponse>> response = 
                organizationController.getOrganization(organizationId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getOrgName()).isEqualTo("테스트 조직");
            assertThat(response.getBody().getMessage()).isEqualTo("조직 정보를 성공적으로 조회했습니다.");

            verify(organizationService).getOrganization(organizationId);
        }
    }

    @Nested
    @DisplayName("조직 목록 조회 테스트")
    class GetOrganizationsTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getOrganizations_WhenValid_ReturnsOk() {
            // Given
            List<OrganizationResponse> organizations = Arrays.asList(testOrganizationResponse);
            
            when(organizationService.getOrganizations())
                .thenReturn(organizations);

            // When
            ResponseEntity<ApiResponse<List<OrganizationResponse>>> response = 
                organizationController.getOrganizations();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getOrgName()).isEqualTo("테스트 조직");
            assertThat(response.getBody().getMessage()).isEqualTo("조직 목록을 성공적으로 조회했습니다.");

            verify(organizationService).getOrganizations();
        }
    }

    @Nested
    @DisplayName("조직 수정 테스트")
    class UpdateOrganizationTest {
        @Test
        @DisplayName("정상 수정 시 200 반환")
        void updateOrganization_WhenValidRequest_ReturnsOk() {
            // Given
            Long organizationId = 1L;
            UpdateOrganizationRequest request = new UpdateOrganizationRequest();
            request.setOrgName("수정된 조직");
            request.setDescription("수정된 조직입니다");
            
            when(organizationService.updateOrganization(organizationId, request))
                .thenReturn(testOrganizationResponse);

            // When
            ResponseEntity<ApiResponse<OrganizationResponse>> response = 
                organizationController.updateOrganization(organizationId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getOrgName()).isEqualTo("테스트 조직");
            assertThat(response.getBody().getMessage()).isEqualTo("조직 정보를 성공적으로 수정했습니다.");

            verify(organizationService).updateOrganization(organizationId, request);
        }
    }

    @Nested
    @DisplayName("조직 삭제 테스트")
    class DeleteOrganizationTest {
        @Test
        @DisplayName("정상 삭제 시 204 반환")
        void deleteOrganization_WhenValidId_ReturnsNoContent() {
            // Given
            Long organizationId = 1L;
            
            doNothing().when(organizationService).deleteOrganization(organizationId);

            // When
            ResponseEntity<Void> response = organizationController.deleteOrganization(organizationId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            verify(organizationService).deleteOrganization(organizationId);
        }
    }

    @Nested
    @DisplayName("조직 트리 조회 테스트")
    class GetOrganizationTreeTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getOrganizationTree_WhenValid_ReturnsOk() {
            // Given
            List<OrganizationHierarchyResponse> tree = Arrays.asList();
            
            when(organizationService.getOrganizationTree())
                .thenReturn(tree);

            // When
            ResponseEntity<ApiResponse<List<OrganizationHierarchyResponse>>> response = 
                organizationController.getOrganizationTree();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getMessage()).isEqualTo("조직 트리를 성공적으로 조회했습니다.");

            verify(organizationService).getOrganizationTree();
        }
    }

    @Nested
    @DisplayName("사용자 추가 테스트")
    class AddUserToOrganizationTest {
        @Test
        @DisplayName("정상 추가 시 200 반환")
        void addUserToOrganization_WhenValidRequest_ReturnsOk() {
            // Given
            Long organizationId = 1L;
            AddUserToOrganizationRequest request = new AddUserToOrganizationRequest();
            request.setUserId(1L);
            
            when(organizationService.addUserToOrganization(organizationId, request))
                .thenReturn(testUserResponse);

            // When
            ResponseEntity<ApiResponse<UserResponse>> response = 
                organizationController.addUserToOrganization(organizationId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getUsername()).isEqualTo("testuser");
            assertThat(response.getBody().getData().getEmail()).isEqualTo("test@test.com");

            verify(organizationService).addUserToOrganization(organizationId, request);
        }
    }

    @Nested
    @DisplayName("사용자 제거 테스트")
    class RemoveUserFromOrganizationTest {
        @Test
        @DisplayName("정상 제거 시 200 반환")
        void removeUserFromOrganization_WhenValidIds_ReturnsOk() {
            // Given
            Long organizationId = 1L;
            Long userId = 1L;
            
            doNothing().when(organizationService).removeUserFromOrganization(organizationId, userId);

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                organizationController.removeUserFromOrganization(organizationId, userId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            verify(organizationService).removeUserFromOrganization(organizationId, userId);
        }
    }

    @Nested
    @DisplayName("조직별 사용자 목록 조회 테스트")
    class GetOrganizationUsersTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getOrganizationUsers_WhenValidId_ReturnsOk() {
            // Given
            Long organizationId = 1L;
            List<UserResponse> users = Arrays.asList(testUserResponse);
            
            when(organizationService.getOrganizationUsers(organizationId))
                .thenReturn(users);

            // When
            ResponseEntity<ApiResponse<List<UserResponse>>> response = 
                organizationController.getOrganizationUsers(organizationId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getUsername()).isEqualTo("testuser");
            assertThat(response.getBody().getData().get(0).getEmail()).isEqualTo("test@test.com");

            verify(organizationService).getOrganizationUsers(organizationId);
        }
    }

    @Nested
    @DisplayName("조직 수 조회 테스트")
    class GetOrganizationCountTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getOrganizationCount_WhenValid_ReturnsOk() {
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
}
