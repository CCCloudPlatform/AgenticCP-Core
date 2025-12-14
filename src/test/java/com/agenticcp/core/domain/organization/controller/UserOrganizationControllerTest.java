package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.OrganizationMemberResponse;
import com.agenticcp.core.domain.organization.entity.OrganizationMember;
import com.agenticcp.core.domain.organization.service.OrganizationMemberService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * UserOrganizationController 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserOrganizationController 단위 테스트")
class UserOrganizationControllerTest {

    @Mock
    private OrganizationMemberService organizationMemberService;

    @InjectMocks
    private UserOrganizationController userOrganizationController;

    private OrganizationMember testMember;
    private OrganizationMemberResponse testResponse;

    @BeforeEach
    void setUp() {
        testMember = OrganizationMember.builder()
                .joinedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testResponse = OrganizationMemberResponse.builder()
                .organizationId(1L)
                .organizationName("테스트 조직")
                .userId(1L)
                .username("testuser")
                .userEmail("test@example.com")
                .userName("테스트 사용자")
                .role("ADMIN")
                .joinedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("사용자의 조직 목록 조회 테스트")
    class GetOrganizationsByUserIdTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getOrganizationsByUserId_WhenValidId_ReturnsOk() {
            // Given
            Long userId = 1L;
            List<OrganizationMember> members = Arrays.asList(testMember);

            when(organizationMemberService.getOrganizationsByUserId(userId))
                    .thenReturn(members);

            // When
            ResponseEntity<ApiResponse<List<OrganizationMemberResponse>>> response =
                    userOrganizationController.getOrganizationsByUserId(userId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("조직 목록을 성공적으로 조회했습니다.");
            assertThat(response.getBody().getData()).hasSize(1);

            verify(organizationMemberService).getOrganizationsByUserId(userId);
        }

        @Test
        @DisplayName("조직이 없는 사용자 조회 시 빈 리스트 반환")
        void getOrganizationsByUserId_WhenNoOrganizations_ReturnsEmptyList() {
            // Given
            Long userId = 1L;
            List<OrganizationMember> emptyList = Arrays.asList();

            when(organizationMemberService.getOrganizationsByUserId(userId))
                    .thenReturn(emptyList);

            // When
            ResponseEntity<ApiResponse<List<OrganizationMemberResponse>>> response =
                    userOrganizationController.getOrganizationsByUserId(userId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEmpty();

            verify(organizationMemberService).getOrganizationsByUserId(userId);
        }
    }
}

