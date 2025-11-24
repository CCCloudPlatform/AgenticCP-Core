package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.dto.*;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Organization 계층 구조 관리 테스트")
class OrganizationHierarchyServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization rootOrganization;
    private Organization childOrganization1;
    private Organization childOrganization2;
    private Organization grandChildOrganization;

    @BeforeEach
    void setUp() {
        rootOrganization = Organization.builder()
                .orgKey("ROOT_ORG")
                .orgName("Root Organization")
                .status(Status.ACTIVE)
                .build();
        rootOrganization.setId(1L);

        childOrganization1 = Organization.builder()
                .orgKey("CHILD_ORG_1")
                .orgName("Child Organization 1")
                .parentOrganization(rootOrganization)
                .status(Status.ACTIVE)
                .build();
        childOrganization1.setId(2L);

        childOrganization2 = Organization.builder()
                .orgKey("CHILD_ORG_2")
                .orgName("Child Organization 2")
                .parentOrganization(rootOrganization)
                .status(Status.ACTIVE)
                .build();
        childOrganization2.setId(3L);

        grandChildOrganization = Organization.builder()
                .orgKey("GRAND_CHILD_ORG")
                .orgName("Grand Child Organization")
                .parentOrganization(childOrganization1)
                .status(Status.ACTIVE)
                .build();
        grandChildOrganization.setId(4L);
    }

    @Nested
    @DisplayName("조직 트리 조회")
    class GetOrganizationTreeTest {
        @Test
        @DisplayName("전체 조직 트리 조회 성공")
        void getOrganizationTree_Success() {
            // Given
            List<Organization> allOrganizations = Arrays.asList(
                    rootOrganization, childOrganization1, childOrganization2, grandChildOrganization
            );
            List<Organization> rootOrganizations = Arrays.asList(rootOrganization);
            
            when(organizationRepository.findAll()).thenReturn(allOrganizations);
            when(organizationRepository.findRootOrganizations()).thenReturn(rootOrganizations);

            // When
            List<OrganizationHierarchyResponse> result = organizationService.getOrganizationTree();

            // Then
            assertThat(result).hasSize(1);
            OrganizationHierarchyResponse rootResponse = result.get(0);
            assertThat(rootResponse.getId()).isEqualTo(rootOrganization.getId());
            assertThat(rootResponse.getOrgName()).isEqualTo(rootOrganization.getOrgName());
            assertThat(rootResponse.getLevel()).isEqualTo(0);
            assertThat(rootResponse.getPath()).isEqualTo(rootOrganization.getOrgName());
            assertThat(rootResponse.getChildren()).hasSize(2);
            assertThat(rootResponse.getChildrenCount()).isEqualTo(2);

            verify(organizationRepository).findAll();
            verify(organizationRepository).findRootOrganizations();
        }
    }

    @Nested
    @DisplayName("조직 경로 조회")
    class GetOrganizationPathTest {
        @Test
        @DisplayName("조직 경로 조회 성공")
        void getOrganizationPath_Success() {
            // Given
            when(organizationRepository.findById(grandChildOrganization.getId()))
                    .thenReturn(Optional.of(grandChildOrganization));

            // When
            OrganizationPathResponse result = organizationService.getOrganizationPath(grandChildOrganization.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPath()).hasSize(3);
            assertThat(result.getPath().get(0).getOrgName()).isEqualTo(rootOrganization.getOrgName());
            assertThat(result.getPath().get(1).getOrgName()).isEqualTo(childOrganization1.getOrgName());
            assertThat(result.getPath().get(2).getOrgName()).isEqualTo(grandChildOrganization.getOrgName());
            assertThat(result.getFullPath()).isEqualTo("Root Organization > Child Organization 1 > Grand Child Organization");

            verify(organizationRepository).findById(grandChildOrganization.getId());
        }

        @Test
        @DisplayName("조직 경로 조회 실패 - 존재하지 않는 조직")
        void getOrganizationPath_NotFound_ThrowsException() {
            // Given
            when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationService.getOrganizationPath(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("존재하지 않는 조직입니다: 999");
        }
    }

    @Nested
    @DisplayName("상위/하위 조직 조회")
    class GetAncestorsAndDescendantsTest {
        @Test
        @DisplayName("상위 조직 목록 조회 성공")
        void getAncestors_Success() {
            // Given
            when(organizationRepository.findById(grandChildOrganization.getId()))
                    .thenReturn(Optional.of(grandChildOrganization));

            // When
            List<OrganizationResponse> ancestors = organizationService.getAncestors(grandChildOrganization.getId());

            // Then
            assertThat(ancestors).hasSize(2);
            assertThat(ancestors.get(0).getOrgName()).isEqualTo(rootOrganization.getOrgName());
            assertThat(ancestors.get(1).getOrgName()).isEqualTo(childOrganization1.getOrgName());
        }

        @Test
        @DisplayName("하위 조직 목록 조회 성공 (모든 레벨)")
        void getDescendants_Success() {
            // Given
            when(organizationRepository.findById(rootOrganization.getId()))
                    .thenReturn(Optional.of(rootOrganization));
            when(organizationRepository.findByParentOrganizationId(rootOrganization.getId()))
                    .thenReturn(Arrays.asList(childOrganization1, childOrganization2));
            when(organizationRepository.findByParentOrganizationId(childOrganization1.getId()))
                    .thenReturn(List.of(grandChildOrganization));
            when(organizationRepository.findByParentOrganizationId(childOrganization2.getId()))
                    .thenReturn(List.of());
            when(organizationRepository.findByParentOrganizationId(grandChildOrganization.getId()))
                    .thenReturn(List.of());

            // When
            List<OrganizationResponse> descendants = organizationService.getDescendants(rootOrganization.getId());

            // Then
            assertThat(descendants).hasSize(3);
            List<String> descendantNames = descendants.stream()
                    .map(OrganizationResponse::getOrgName)
                    .toList();
            assertThat(descendantNames).containsExactlyInAnyOrder(
                    childOrganization1.getOrgName(),
                    childOrganization2.getOrgName(),
                    grandChildOrganization.getOrgName()
            );
        }
    }

    @Nested
    @DisplayName("조직 이동")
    class MoveOrganizationTest {
        @Test
        @DisplayName("조직 이동 성공")
        void moveOrganization_Success() {
            // Given
            MoveOrganizationRequest request = MoveOrganizationRequest.builder()
                    .newParentId(childOrganization2.getId())
                    .build();

            when(organizationRepository.findById(childOrganization1.getId()))
                    .thenReturn(Optional.of(childOrganization1));
            when(organizationRepository.findById(childOrganization2.getId()))
                    .thenReturn(Optional.of(childOrganization2));
            when(organizationRepository.save(any(Organization.class)))
                    .thenReturn(childOrganization1);

            // When
            OrganizationResponse result = organizationService.moveOrganization(childOrganization1.getId(), request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(childOrganization1.getId());

            verify(organizationRepository).findById(childOrganization1.getId());
            verify(organizationRepository).findById(childOrganization2.getId());
            verify(organizationRepository).save(childOrganization1);
        }

        @Test
        @DisplayName("조직 이동 실패 - 순환 참조")
        void moveOrganization_CircularReference_ThrowsException() {
            // Given
            MoveOrganizationRequest request = MoveOrganizationRequest.builder()
                    .newParentId(grandChildOrganization.getId())
                    .build();

            when(organizationRepository.findById(childOrganization1.getId()))
                    .thenReturn(Optional.of(childOrganization1));
            when(organizationRepository.findById(grandChildOrganization.getId()))
                    .thenReturn(Optional.of(grandChildOrganization));

            // When & Then
            assertThatThrownBy(() -> organizationService.moveOrganization(childOrganization1.getId(), request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("순환 참조가 발생합니다: " + childOrganization1.getOrgName());
        }
    }

    @Nested
    @DisplayName("조직 통계")
    class GetOrganizationStatsTest {
        @Test
        @DisplayName("조직 통계 조회 성공")
        void getOrganizationStats_Success() {
            // Given
            List<Organization> organizations = Arrays.asList(
                    rootOrganization, childOrganization1, childOrganization2, grandChildOrganization
            );
            when(organizationRepository.findAll()).thenReturn(organizations);

            // When
            OrganizationStatsResponse result = organizationService.getOrganizationStats();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalOrganizations()).isEqualTo(4);
            assertThat(result.getActiveOrganizations()).isEqualTo(4);
            assertThat(result.getInactiveOrganizations()).isEqualTo(0);
            assertThat(result.getMaxDepth()).isEqualTo(2);
            assertThat(result.getLevelStats()).hasSize(3); // Level 0, 1, 2

            verify(organizationRepository).findAll();
        }
    }
}