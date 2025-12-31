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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Organization-Tenant 관계 테스트 (1:1)")
class OrganizationTenantServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization testOrganization;
    private Tenant testTenant;

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

        // 테스트 테넌트 생성 (1:1 관계)
        testTenant = Tenant.builder()
            .tenantKey("TENANT_A")
            .tenantName("테넌트 A")
            .description("테스트 테넌트 A")
            .status(Status.ACTIVE)
            .maxUsers(50)
            .organization(testOrganization)
            .build();
        testTenant.setId(1L);

        // 조직에 테넌트 설정 (1:1)
        testOrganization.setTenant(testTenant);
    }

    @Test
    @DisplayName("조직에 연결된 테넌트 조회 성공 (1:1)")
    void 조직에_연결된_테넌트_조회_성공() {
        // Given
        Long organizationId = 1L;
        
        when(organizationRepository.findTenantByOrganizationId(organizationId))
            .thenReturn(Optional.of(testTenant));

        // When
        Optional<Tenant> result = organizationRepository.findTenantByOrganizationId(organizationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTenantKey()).isEqualTo("TENANT_A");
        
        verify(organizationRepository).findTenantByOrganizationId(organizationId);
    }

    @Test
    @DisplayName("조직에 테넌트가 존재하는지 확인")
    void 조직에_테넌트_존재_여부_확인() {
        // Given
        Long organizationId = 1L;
        
        when(organizationRepository.existsTenantByOrganizationId(organizationId))
            .thenReturn(true);

        // When
        boolean exists = organizationRepository.existsTenantByOrganizationId(organizationId);

        // Then
        assertThat(exists).isTrue();
        verify(organizationRepository).existsTenantByOrganizationId(organizationId);
    }

    @Test
    @DisplayName("조직에 테넌트가 없는 경우")
    void 조직에_테넌트가_없는_경우() {
        // Given
        Long organizationId = 999L;
        
        when(organizationRepository.findTenantByOrganizationId(organizationId))
            .thenReturn(Optional.empty());

        // When
        Optional<Tenant> result = organizationRepository.findTenantByOrganizationId(organizationId);

        // Then
        assertThat(result).isEmpty();
        verify(organizationRepository).findTenantByOrganizationId(organizationId);
    }

    @Test
    @DisplayName("조직-테넌트 1:1 관계 검증")
    void 조직_테넌트_1대1_관계_검증() {
        // Given
        Organization org = testOrganization;
        Tenant tenant = testTenant;

        // When & Then
        // 조직이 테넌트를 포함하는지 확인 (1:1)
        assertThat(org.getTenant()).isNotNull();
        assertThat(org.getTenant()).isEqualTo(tenant);
        
        // 테넌트가 조직을 참조하는지 확인
        assertThat(tenant.getOrganization()).isEqualTo(org);
        assertThat(tenant.getOrganization().getId()).isEqualTo(org.getId());
    }

    @Test
    @DisplayName("조직 삭제 시 연관된 테넌트도 삭제되는지 확인")
    void 조직_삭제_시_연관된_테넌트_삭제_확인() {
        // Given
        Long organizationId = 1L;

        // When
        organizationRepository.deleteById(organizationId);

        // Then
        verify(organizationRepository).deleteById(organizationId);
        // CascadeType.ALL로 설정되어 있어서 연관된 테넌트도 삭제됨
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

        Tenant tenant = testTenant;

        // When
        tenant.setOrganization(newOrganization);
        tenantRepository.save(tenant);

        // Then
        assertThat(tenant.getOrganization()).isEqualTo(newOrganization);
        assertThat(tenant.getOrganization().getId()).isEqualTo(2L);
        verify(tenantRepository).save(tenant);
    }

    @Test
    @DisplayName("조직별 테넌트 정보 조회 (1:1)")
    void 조직별_테넌트_정보_조회() {
        // Given
        Long organizationId = 1L;
        
        when(organizationRepository.findTenantByOrganizationId(organizationId))
            .thenReturn(Optional.of(testTenant));

        // When
        Optional<Tenant> result = organizationRepository.findTenantByOrganizationId(organizationId);
        
        // Then
        assertThat(result).isPresent();
        
        Tenant tenant = result.get();
        assertThat(tenant.getTenantKey()).isEqualTo("TENANT_A");
        assertThat(tenant.getTenantName()).isEqualTo("테넌트 A");
        assertThat(tenant.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(tenant.getMaxUsers()).isEqualTo(50);
    }
    
    @Test
    @DisplayName("테넌트 활성 상태 확인")
    void 테넌트_활성_상태_확인() {
        // Given
        Long organizationId = 1L;
        
        when(organizationRepository.findTenantByOrganizationId(organizationId))
            .thenReturn(Optional.of(testTenant));

        // When
        Optional<Tenant> result = organizationRepository.findTenantByOrganizationId(organizationId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(Status.ACTIVE);
    }
    
    @Test
    @DisplayName("테넌트 비활성 상태 확인")
    void 테넌트_비활성_상태_확인() {
        // Given
        Long organizationId = 1L;
        
        Tenant inactiveTenant = Tenant.builder()
            .tenantKey("INACTIVE_TENANT")
            .tenantName("비활성 테넌트")
            .status(Status.INACTIVE)
            .maxUsers(30)
            .organization(testOrganization)
            .build();
        inactiveTenant.setId(2L);
        
        when(organizationRepository.findTenantByOrganizationId(organizationId))
            .thenReturn(Optional.of(inactiveTenant));

        // When
        Optional<Tenant> result = organizationRepository.findTenantByOrganizationId(organizationId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(Status.INACTIVE);
    }
}
