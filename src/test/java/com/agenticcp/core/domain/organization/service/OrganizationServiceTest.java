package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.organization.dto.CreateOrganizationRequest;
import com.agenticcp.core.domain.organization.dto.OrganizationResponse;
import com.agenticcp.core.domain.organization.dto.UpdateOrganizationRequest;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationService 테스트")
class OrganizationServiceTest {
    
    @Mock
    private OrganizationRepository organizationRepository;
    
    @InjectMocks
    private OrganizationService organizationService;
    
    private Organization testOrganization;
    
    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .orgKey("TEST_ORG")
                .orgName("Test Organization")
                .description("Test Description")
                .status(Status.ACTIVE)
                .build();
        testOrganization.setId(1L);
    }
    
    @Test
    @DisplayName("조직 생성 성공")
    void 조직_생성_성공() {
        // Given
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .orgName("New Organization")
                .description("New Description")
                .build();
        
        when(organizationRepository.existsByOrgName("New Organization")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
        
        // When
        OrganizationResponse response = organizationService.createOrganization(request);
        
        // Then
        assertThat(response.getOrgName()).isEqualTo("Test Organization");
        assertThat(response.getDescription()).isEqualTo("Test Description");
        
        verify(organizationRepository).existsByOrgName("New Organization");
        verify(organizationRepository).save(any(Organization.class));
    }
    
    @Test
    @DisplayName("조직 생성 실패 - 중복된 조직명")
    void 조직_생성_실패_중복된_조직명() {
        // Given
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .orgName("Existing Organization")
                .description("New Description")
                .build();
        
        when(organizationRepository.existsByOrgName("Existing Organization")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> organizationService.createOrganization(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 조직명입니다: Existing Organization");
    }
    
    @Test
    @DisplayName("조직 조회 성공")
    void 조직_조회_성공() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        
        // When
        OrganizationResponse response = organizationService.getOrganization(1L);
        
        // Then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOrgName()).isEqualTo("Test Organization");
        
        verify(organizationRepository).findById(1L);
    }
    
    @Test
    @DisplayName("조직 조회 실패 - 존재하지 않는 조직")
    void 조직_조회_실패_존재하지_않는_조직() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> organizationService.getOrganization(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 조직입니다: 1");
    }
    
    @Test
    @DisplayName("조직 목록 조회 성공")
    void 조직_목록_조회_성공() {
        // Given
        List<Organization> organizations = Arrays.asList(testOrganization);
        when(organizationRepository.findAll()).thenReturn(organizations);
        
        // When
        List<OrganizationResponse> responses = organizationService.getOrganizations();
        
        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOrgName()).isEqualTo("Test Organization");
        
        verify(organizationRepository).findAll();
    }
    
    @Test
    @DisplayName("조직 수정 성공")
    void 조직_수정_성공() {
        // Given
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .orgName("Updated Organization")
                .description("Updated Description")
                .build();
        
        Organization updatedOrganization = Organization.builder()
                .orgKey("UPDATED_ORG")
                .orgName("Updated Organization")
                .description("Updated Description")
                .status(Status.ACTIVE)
                .build();
        updatedOrganization.setId(1L);
        
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.existsByOrgName("Updated Organization")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(updatedOrganization);
        
        // When
        OrganizationResponse response = organizationService.updateOrganization(1L, request);
        
        // Then
        assertThat(response.getOrgName()).isEqualTo("Updated Organization");
        
        verify(organizationRepository).findById(1L);
        verify(organizationRepository).existsByOrgName("Updated Organization");
        verify(organizationRepository).save(any(Organization.class));
    }
    
    @Test
    @DisplayName("조직 삭제 성공")
    void 조직_삭제_성공() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.existsByParentOrganizationId(1L)).thenReturn(false);
        
        // When
        organizationService.deleteOrganization(1L);
        
        // Then
        verify(organizationRepository).findById(1L);
        verify(organizationRepository).existsByParentOrganizationId(1L);
        verify(organizationRepository).delete(testOrganization);
    }
    
    @Test
    @DisplayName("조직 삭제 실패 - 하위 조직 존재")
    void 조직_삭제_실패_하위_조직_존재() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.existsByParentOrganizationId(1L)).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> organizationService.deleteOrganization(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("하위 조직이 존재하는 조직은 삭제할 수 없습니다: 1");
    }
    
    @Test
    @DisplayName("조직 수 조회 성공")
    void 조직_수_조회_성공() {
        // Given
        when(organizationRepository.count()).thenReturn(5L);
        
        // When
        long count = organizationService.getOrganizationCount();
        
        // Then
        assertThat(count).isEqualTo(5L);
        
        verify(organizationRepository).count();
    }
}