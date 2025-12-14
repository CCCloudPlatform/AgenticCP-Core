package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.organization.dto.AddMemberRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * OrganizationMemberController 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationMemberController 단위 테스트")
class OrganizationMemberControllerTest {

    @Mock
    private OrganizationMemberService organizationMemberService;

    @InjectMocks
    private OrganizationMemberController organizationMemberController;

    private OrganizationMember testMember;
    private OrganizationMemberResponse testMemberResponse;

    @BeforeEach
    void setUp() {
        testMember = OrganizationMember.builder()
                .joinedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testMemberResponse = OrganizationMemberResponse.builder()
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
    @DisplayName("조직 멤버 목록 조회 테스트")
    class GetMembersTest {
        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getMembers_WhenValidId_ReturnsOk() {
            // Given
            Long organizationId = 1L;
            List<OrganizationMember> members = Arrays.asList(testMember);

            when(organizationMemberService.getMembers(organizationId))
                    .thenReturn(members);

            // When
            ResponseEntity<ApiResponse<List<OrganizationMemberResponse>>> response =
                    organizationMemberController.getMembers(organizationId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("멤버 목록을 성공적으로 조회했습니다.");

            verify(organizationMemberService).getMembers(organizationId);
        }
    }

    @Nested
    @DisplayName("조직에 멤버 추가 테스트")
    class AddMemberTest {
        @Test
        @DisplayName("정상 추가 시 201 반환")
        void addMember_WhenValidRequest_ReturnsCreated() {
            // Given
            Long organizationId = 1L;
            AddMemberRequest request = AddMemberRequest.builder()
                    .userId(1L)
                    .role("ADMIN")
                    .build();

            when(organizationMemberService.addMember(anyLong(), anyLong(), any()))
                    .thenReturn(testMember);

            // When
            ResponseEntity<ApiResponse<OrganizationMemberResponse>> response =
                    organizationMemberController.addMember(organizationId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("멤버가 성공적으로 추가되었습니다.");

            verify(organizationMemberService).addMember(organizationId, request.getUserId(), request.getRole());
        }
    }

    @Nested
    @DisplayName("조직에서 멤버 제거 테스트")
    class RemoveMemberTest {
        @Test
        @DisplayName("정상 제거 시 204 반환")
        void removeMember_WhenValidIds_ReturnsNoContent() {
            // Given
            Long organizationId = 1L;
            Long userId = 1L;

            doNothing().when(organizationMemberService).removeMember(anyLong(), anyLong());

            // When
            ResponseEntity<Void> response =
                    organizationMemberController.removeMember(organizationId, userId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();

            verify(organizationMemberService).removeMember(organizationId, userId);
        }
    }
}

