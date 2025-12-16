package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.Status;
// [DEPRECATED] 사용자 관련 테스트 제거됨
// import com.agenticcp.core.common.enums.UserRole;
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

    // [DEPRECATED] 사용자 관련 테스트 제거됨
    // private UserResponse testUserResponse;
    private OrganizationResponse testOrganizationResponse;

    @BeforeEach
    void setUp() {
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

    // ========== [DEPRECATED] 계층 구조 API 테스트 - ERD에 없음 ==========
    /*
    @Nested
    @DisplayName("[DEPRECATED] 조직 트리 조회 테스트")
    class GetOrganizationTreeTest {
        @Test
        void getOrganizationTree_WhenValid_ReturnsOk() {
            // API 주석처리됨 - 계층 구조 없음
        }
    }
    */

    // ========== [DEPRECATED] 사용자 관련 API 테스트 - OrganizationMember로 대체 예정 ==========
    /*
    @Nested
    @DisplayName("[DEPRECATED] 사용자 추가 테스트")
    class AddUserToOrganizationTest {
        @Test
        void addUserToOrganization_WhenValidRequest_ReturnsOk() {
            // API 주석처리됨 - OrganizationMember로 대체 예정
        }
    }

    @Nested
    @DisplayName("[DEPRECATED] 사용자 제거 테스트")
    class RemoveUserFromOrganizationTest {
        @Test
        void removeUserFromOrganization_WhenValidIds_ReturnsOk() {
            // API 주석처리됨 - OrganizationMember로 대체 예정
        }
    }

    @Nested
    @DisplayName("[DEPRECATED] 조직별 사용자 목록 조회 테스트")
    class GetOrganizationUsersTest {
        @Test
        void getOrganizationUsers_WhenValidId_ReturnsOk() {
            // API 주석처리됨 - OrganizationMember로 대체 예정
        }
    }
    */

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

    // ========== 테넌트 관련 테스트 (1:1 관계) ==========

    @Nested
    @DisplayName("조직 테넌트 존재 여부 조회 테스트")
    class CheckOrganizationTenantTest {
        @Test
        @DisplayName("테넌트 존재 시 200 반환")
        void checkOrganizationTenant_WhenTenantExists_ReturnsOk() {
            // Given
            Long organizationId = 1L;
            
            when(organizationService.hasTenant(organizationId)).thenReturn(true);
            when(organizationService.hasActiveTenant(organizationId)).thenReturn(true);

            // When
            ResponseEntity<ApiResponse<java.util.Map<String, Object>>> response = 
                organizationController.checkOrganizationTenant(organizationId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().get("hasTenant")).isEqualTo(true);
            assertThat(response.getBody().getData().get("hasActiveTenant")).isEqualTo(true);

            verify(organizationService).hasTenant(organizationId);
            verify(organizationService).hasActiveTenant(organizationId);
        }

        @Test
        @DisplayName("테넌트 없을 시 false 반환")
        void checkOrganizationTenant_WhenNoTenant_ReturnsFalse() {
            // Given
            Long organizationId = 1L;
            
            when(organizationService.hasTenant(organizationId)).thenReturn(false);
            when(organizationService.hasActiveTenant(organizationId)).thenReturn(false);

            // When
            ResponseEntity<ApiResponse<java.util.Map<String, Object>>> response = 
                organizationController.checkOrganizationTenant(organizationId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().get("hasTenant")).isEqualTo(false);
            assertThat(response.getBody().getData().get("hasActiveTenant")).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("조직 테넌트 정보 조회 테스트")
    class GetOrganizationTenantInfoTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getOrganizationTenantInfo_WhenValid_ReturnsOk() {
            // Given
            Long organizationId = 1L;
            java.util.Map<String, Object> tenantInfo = new java.util.HashMap<>();
            tenantInfo.put("organizationId", 1L);
            tenantInfo.put("organizationName", "테스트 조직");
            tenantInfo.put("hasTenant", true);
            tenantInfo.put("tenantId", 1L);
            tenantInfo.put("tenantKey", "tenant-test");
            
            when(organizationService.getOrganizationTenantInfo(organizationId))
                .thenReturn(tenantInfo);

            // When
            ResponseEntity<ApiResponse<java.util.Map<String, Object>>> response = 
                organizationController.getOrganizationTenantInfo(organizationId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().get("hasTenant")).isEqualTo(true);
            assertThat(response.getBody().getData().get("tenantKey")).isEqualTo("tenant-test");

            verify(organizationService).getOrganizationTenantInfo(organizationId);
        }
    }
}
