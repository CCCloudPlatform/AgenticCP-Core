package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.organization.dto.*;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Organization 계층 구조 관리 테스트")
class OrganizationHierarchyServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Tenant testTenant;
    private Organization rootOrganization;
    private Organization childOrganization1;
    private Organization childOrganization2;
    private Organization grandChildOrganization;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .tenantName("Test Tenant")
                .build();
        testTenant.setId(1L);

        rootOrganization = Organization.builder()
                .orgName("Root Organization")
                .description("Root Description")
                .tenant(testTenant)
                .status(Status.ACTIVE)
                .build();
        rootOrganization.setId(1L);

        childOrganization1 = Organization.builder()
                .orgName("Child Organization 1")
                .description("Child 1 Description")
                .tenant(testTenant)
                .parentOrganization(rootOrganization)
                .status(Status.ACTIVE)
                .build();
        childOrganization1.setId(2L);

        childOrganization2 = Organization.builder()
                .orgName("Child Organization 2")
                .description("Child 2 Description")
                .tenant(testTenant)
                .parentOrganization(rootOrganization)
                .status(Status.ACTIVE)
                .build();
        childOrganization2.setId(3L);

        grandChildOrganization = Organization.builder()
                .orgName("Grand Child Organization")
                .description("Grand Child Description")
                .tenant(testTenant)
                .parentOrganization(childOrganization1)
                .status(Status.ACTIVE)
                .build();
        grandChildOrganization.setId(4L);
    }

    @Test
    @DisplayName("전체 조직 트리 조회 성공")
    void 전체_조직_트리_조회_성공() {
        // Given
        List<Organization> organizations = Arrays.asList(
                rootOrganization, childOrganization1, childOrganization2, grandChildOrganization
        );
        when(organizationRepository.findByTenantId(1L)).thenReturn(organizations);

        // When
        List<OrganizationHierarchyResponse> result = organizationService.getOrganizationTree(1L);

        // Then
        assertThat(result).hasSize(1); // 루트 조직 1개
        assertThat(result.get(0).getOrgName()).isEqualTo("Root Organization");
        assertThat(result.get(0).getChildren()).hasSize(2); // 하위 조직 2개
        assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1); // 손자 조직 1개

        verify(organizationRepository).findByTenantId(1L);
    }

    @Test
    @DisplayName("조직 경로 조회 성공")
    void 조직_경로_조회_성공() {
        // Given
        when(organizationRepository.findByIdAndTenantId(4L, 1L)).thenReturn(Optional.of(grandChildOrganization));

        // When
        OrganizationPathResponse result = organizationService.getOrganizationPath(4L, 1L);

        // Then
        assertThat(result.getPath()).hasSize(3); // 루트 -> 자식 -> 손자
        assertThat(result.getFullPath()).isEqualTo("Root Organization > Child Organization 1 > Grand Child Organization");
        assertThat(result.getLevel()).isEqualTo(2);

        verify(organizationRepository).findByIdAndTenantId(4L, 1L);
    }

    @Test
    @DisplayName("조직 경로 조회 실패 - 존재하지 않는 조직")
    void 조직_경로_조회_실패_존재하지_않는_조직() {
        // Given
        when(organizationRepository.findByIdAndTenantId(999L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> organizationService.getOrganizationPath(999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 조직입니다: 999");
    }

    @Test
    @DisplayName("상위 조직 목록 조회 성공")
    void 상위_조직_목록_조회_성공() {
        // Given
        when(organizationRepository.findByIdAndTenantId(4L, 1L)).thenReturn(Optional.of(grandChildOrganization));

        // When
        List<OrganizationResponse> result = organizationService.getAncestors(4L, 1L);

        // Then
        assertThat(result).hasSize(2); // 루트, 자식
        assertThat(result.get(0).getOrgName()).isEqualTo("Root Organization");
        assertThat(result.get(1).getOrgName()).isEqualTo("Child Organization 1");

        verify(organizationRepository).findByIdAndTenantId(4L, 1L);
    }

    @Test
    @DisplayName("하위 조직 목록 조회 성공")
    void 하위_조직_목록_조회_성공() {
        // Given
        when(organizationRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(rootOrganization));
        when(organizationRepository.findByParentOrganizationId(1L)).thenReturn(Arrays.asList(childOrganization1, childOrganization2));
        when(organizationRepository.findByParentOrganizationId(2L)).thenReturn(Arrays.asList(grandChildOrganization));
        when(organizationRepository.findByParentOrganizationId(3L)).thenReturn(Arrays.asList());
        when(organizationRepository.findByParentOrganizationId(4L)).thenReturn(Arrays.asList());

        // When
        List<OrganizationResponse> result = organizationService.getDescendants(1L, 1L);

        // Then
        assertThat(result).hasSize(3); // 자식 2개 + 손자 1개
        assertThat(result).extracting("orgName")
                .containsExactlyInAnyOrder("Child Organization 1", "Child Organization 2", "Grand Child Organization");

        verify(organizationRepository).findByIdAndTenantId(1L, 1L);
        verify(organizationRepository).findByParentOrganizationId(1L);
        verify(organizationRepository).findByParentOrganizationId(2L);
    }

    @Test
    @DisplayName("조직 이동 성공")
    void 조직_이동_성공() {
        // Given
        MoveOrganizationRequest request = MoveOrganizationRequest.builder()
                .newParentId(3L)
                .build();

        when(organizationRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(childOrganization1));
        when(organizationRepository.findByIdAndTenantId(3L, 1L)).thenReturn(Optional.of(childOrganization2));
        when(organizationRepository.save(any(Organization.class))).thenReturn(childOrganization1);

        // When
        OrganizationResponse result = organizationService.moveOrganization(2L, request, 1L);

        // Then
        assertThat(result.getOrgName()).isEqualTo("Child Organization 1");
        verify(organizationRepository).findByIdAndTenantId(2L, 1L);
        verify(organizationRepository).findByIdAndTenantId(3L, 1L);
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("조직 이동 실패 - 순환 참조")
    void 조직_이동_실패_순환_참조() {
        // Given
        MoveOrganizationRequest request = MoveOrganizationRequest.builder()
                .newParentId(2L) // 자신의 하위 조직으로 이동 시도
                .build();

        when(organizationRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(rootOrganization));
        when(organizationRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(childOrganization1));

        // When & Then
        assertThatThrownBy(() -> organizationService.moveOrganization(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("순환 참조가 발생합니다: Root Organization");
    }

    @Test
    @DisplayName("조직 이동 실패 - 존재하지 않는 조직")
    void 조직_이동_실패_존재하지_않는_조직() {
        // Given
        MoveOrganizationRequest request = MoveOrganizationRequest.builder()
                .newParentId(3L)
                .build();

        when(organizationRepository.findByIdAndTenantId(999L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> organizationService.moveOrganization(999L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 조직입니다: 999");
    }

    @Test
    @DisplayName("조직 이동 실패 - 존재하지 않는 상위 조직")
    void 조직_이동_실패_존재하지_않는_상위_조직() {
        // Given
        MoveOrganizationRequest request = MoveOrganizationRequest.builder()
                .newParentId(999L)
                .build();

        when(organizationRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(rootOrganization));
        when(organizationRepository.findByIdAndTenantId(999L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> organizationService.moveOrganization(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 상위 조직입니다: 999");
    }

    @Test
    @DisplayName("조직 통계 조회 성공")
    void 조직_통계_조회_성공() {
        // Given
        List<Organization> organizations = Arrays.asList(
                rootOrganization, childOrganization1, childOrganization2, grandChildOrganization
        );
        when(organizationRepository.findByTenantId(1L)).thenReturn(organizations);

        // When
        OrganizationStatsResponse result = organizationService.getOrganizationStats(1L);

        // Then
        assertThat(result.getTotalOrganizations()).isEqualTo(4);
        assertThat(result.getActiveOrganizations()).isEqualTo(4);
        assertThat(result.getInactiveOrganizations()).isEqualTo(0);
        assertThat(result.getMaxDepth()).isEqualTo(2);
        assertThat(result.getLevelStats()).hasSize(3); // 레벨 0, 1, 2

        verify(organizationRepository).findByTenantId(1L);
    }

    @Test
    @DisplayName("조직 통계 조회 - 비활성 조직 포함")
    void 조직_통계_조회_비활성_조직_포함() {
        // Given
        Organization inactiveOrg = Organization.builder()
                .orgName("Inactive Organization")
                .tenant(testTenant)
                .status(Status.INACTIVE)
                .build();
        inactiveOrg.setId(5L);

        List<Organization> organizations = Arrays.asList(
                rootOrganization, childOrganization1, inactiveOrg
        );
        when(organizationRepository.findByTenantId(1L)).thenReturn(organizations);

        // When
        OrganizationStatsResponse result = organizationService.getOrganizationStats(1L);

        // Then
        assertThat(result.getTotalOrganizations()).isEqualTo(3);
        assertThat(result.getActiveOrganizations()).isEqualTo(2);
        assertThat(result.getInactiveOrganizations()).isEqualTo(1);

        verify(organizationRepository).findByTenantId(1L);
    }

    @Test
    @DisplayName("루트 조직의 상위 조직 조회")
    void 루트_조직의_상위_조직_조회() {
        // Given
        when(organizationRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(rootOrganization));

        // When
        List<OrganizationResponse> result = organizationService.getAncestors(1L, 1L);

        // Then
        assertThat(result).isEmpty(); // 루트 조직은 상위 조직이 없음

        verify(organizationRepository).findByIdAndTenantId(1L, 1L);
    }

    @Test
    @DisplayName("리프 조직의 하위 조직 조회")
    void 리프_조직의_하위_조직_조회() {
        // Given
        when(organizationRepository.findByIdAndTenantId(4L, 1L)).thenReturn(Optional.of(grandChildOrganization));
        when(organizationRepository.findByParentOrganizationId(4L)).thenReturn(Arrays.asList());

        // When
        List<OrganizationResponse> result = organizationService.getDescendants(4L, 1L);

        // Then
        assertThat(result).isEmpty(); // 리프 조직은 하위 조직이 없음

        verify(organizationRepository).findByIdAndTenantId(4L, 1L);
        verify(organizationRepository).findByParentOrganizationId(4L);
    }
}
