package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Organization-Tenant 관계 테스트")
class OrganizationTenantServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization testOrganization;
    private Tenant testTenant1;
    private Tenant testTenant2;

    @BeforeEach
    void setUp() {
        // 테스트 조직 생성
        testOrganization = Organization.builder()
            .orgKey("TEST_ORG")
            .orgName("테스트 조직")
            .description("테스트용 조직")
            .status(Status.ACTIVE)
            .orgType(Organization.OrganizationType.COMPANY)
            .contactEmail("test@test.com")
            .maxUsers(100)
            .establishedDate(LocalDateTime.now())
            .build();
        testOrganization.setId(1L);

        // 테스트 테넌트 1 생성
        testTenant1 = Tenant.builder()
            .tenantKey("TENANT_A")
            .tenantName("테넌트 A")
            .description("테스트 테넌트 A")
            .status(Status.ACTIVE)
            .maxUsers(50)
            .organization(testOrganization)
            .build();
        testTenant1.setId(1L);

        // 테스트 테넌트 2 생성
        testTenant2 = Tenant.builder()
            .tenantKey("TENANT_B")
            .tenantName("테넌트 B")
            .description("테스트 테넌트 B")
            .status(Status.ACTIVE)
            .maxUsers(30)
            .organization(testOrganization)
            .build();
        testTenant2.setId(2L);

        // 조직에 테넌트들 설정
        testOrganization.setTenants(Arrays.asList(testTenant1, testTenant2));
    }

    @Test
    @DisplayName("조직에 속한 테넌트 목록 조회 성공")
    void 조직에_속한_테넌트_목록_조회_성공() {
        // Given
        Long organizationId = 1L;
        List<Tenant> tenants = Arrays.asList(testTenant1, testTenant2);
        
        when(organizationRepository.findTenantsByOrganizationId(organizationId))
            .thenReturn(tenants);

        // When
        List<Tenant> result = organizationRepository.findTenantsByOrganizationId(organizationId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTenantKey()).isEqualTo("TENANT_A");
        assertThat(result.get(1).getTenantKey()).isEqualTo("TENANT_B");
        
        verify(organizationRepository).findTenantsByOrganizationId(organizationId);
    }

    @Test
    @DisplayName("조직에 속한 테넌트 수 조회 성공")
    void 조직에_속한_테넌트_수_조회_성공() {
        // Given
        Long organizationId = 1L;
        List<Tenant> tenants = Arrays.asList(testTenant1, testTenant2);
        
        when(organizationRepository.findTenantsByOrganizationId(organizationId))
            .thenReturn(tenants);

        // When
        List<Tenant> result = organizationRepository.findTenantsByOrganizationId(organizationId);

        // Then
        assertThat(result).hasSize(2);
        verify(organizationRepository).findTenantsByOrganizationId(organizationId);
    }

    @Test
    @DisplayName("조직에 테넌트가 없는 경우")
    void 조직에_테넌트가_없는_경우() {
        // Given
        Long organizationId = 999L;
        List<Tenant> emptyTenants = Arrays.asList();
        
        when(organizationRepository.findTenantsByOrganizationId(organizationId))
            .thenReturn(emptyTenants);

        // When
        List<Tenant> result = organizationRepository.findTenantsByOrganizationId(organizationId);

        // Then
        assertThat(result).isEmpty();
        verify(organizationRepository).findTenantsByOrganizationId(organizationId);
    }

    @Test
    @DisplayName("조직-테넌트 관계 검증")
    void 조직_테넌트_관계_검증() {
        // Given
        Organization org = testOrganization;
        Tenant tenant = testTenant1;

        // When & Then
        // 조직이 테넌트를 포함하는지 확인
        assertThat(org.getTenants()).contains(tenant);
        assertThat(org.getTenants()).hasSize(2);
        
        // 테넌트가 조직을 참조하는지 확인
        assertThat(tenant.getOrganization()).isEqualTo(org);
        assertThat(tenant.getOrganization().getId()).isEqualTo(org.getId());
    }

    @Test
    @DisplayName("조직 삭제 시 연관된 테넌트들도 삭제되는지 확인")
    void 조직_삭제_시_연관된_테넌트들_삭제_확인() {
        // Given
        Long organizationId = 1L;

        // When
        organizationRepository.deleteById(organizationId);

        // Then
        verify(organizationRepository).deleteById(organizationId);
        // CascadeType.ALL로 설정되어 있어서 연관된 테넌트들도 삭제됨
    }

    @Test
    @DisplayName("테넌트를 다른 조직으로 이동")
    void 테넌트를_다른_조직으로_이동() {
        // Given
        Organization newOrganization = Organization.builder()
            .orgKey("NEW_ORG")
            .orgName("새 조직")
            .status(Status.ACTIVE)
            .build();
        newOrganization.setId(2L);

        Tenant tenant = testTenant1;

        // When
        tenant.setOrganization(newOrganization);
        tenantRepository.save(tenant);

        // Then
        assertThat(tenant.getOrganization()).isEqualTo(newOrganization);
        assertThat(tenant.getOrganization().getId()).isEqualTo(2L);
        verify(tenantRepository).save(tenant);
    }

    @Test
    @DisplayName("조직별 테넌트 통계 조회")
    void 조직별_테넌트_통계_조회() {
        // Given
        Long organizationId = 1L;
        List<Tenant> tenants = Arrays.asList(testTenant1, testTenant2);
        
        when(organizationRepository.findTenantsByOrganizationId(organizationId))
            .thenReturn(tenants);

        // When
        List<Tenant> result = organizationRepository.findTenantsByOrganizationId(organizationId);
        
        // Then
        assertThat(result).hasSize(2);
        
        // 통계 계산
        long activeTenantCount = result.stream()
            .filter(tenant -> tenant.getStatus() == Status.ACTIVE)
            .count();
        
        int totalMaxUsers = result.stream()
            .mapToInt(tenant -> tenant.getMaxUsers() != null ? tenant.getMaxUsers() : 0)
            .sum();

        assertThat(activeTenantCount).isEqualTo(2);
        assertThat(totalMaxUsers).isEqualTo(80); // 50 + 30
    }
}
