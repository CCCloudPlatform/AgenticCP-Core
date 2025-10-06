package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.service.CloudResourceService;
import com.agenticcp.core.domain.platform.enums.MultiCloudEnvironment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * MultiCloudEnvironmentService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MultiCloudEnvironmentServiceTest {
    
    @Mock
    private CloudResourceService cloudResourceService;
    
    @InjectMocks
    private MultiCloudEnvironmentService multiCloudEnvironmentService;
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - SINGLE_CLOUD (AWS만 사용)")
    void detectEnvironment_SingleCloud_AWS() {
        // Given
        String tenantId = "tenant-single-aws";
        List<CloudResource> resources = createMockResources(
                CloudProvider.ProviderType.AWS
        );
        when(cloudResourceService.getResourcesByTenant(tenantId)).thenReturn(resources);
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.SINGLE_CLOUD);
    }
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - SINGLE_CLOUD (Azure만 사용)")
    void detectEnvironment_SingleCloud_Azure() {
        // Given
        String tenantId = "tenant-single-azure";
        List<CloudResource> resources = createMockResources(
                CloudProvider.ProviderType.AZURE
        );
        when(cloudResourceService.getResourcesByTenant(tenantId)).thenReturn(resources);
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.SINGLE_CLOUD);
    }
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - MULTI_CLOUD (AWS + Azure)")
    void detectEnvironment_MultiCloud_AWS_Azure() {
        // Given
        String tenantId = "tenant-multi-cloud";
        List<CloudResource> resources = createMockResources(
                CloudProvider.ProviderType.AWS,
                CloudProvider.ProviderType.AZURE
        );
        when(cloudResourceService.getResourcesByTenant(tenantId)).thenReturn(resources);
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.MULTI_CLOUD);
    }
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - MULTI_CLOUD (AWS + GCP)")
    void detectEnvironment_MultiCloud_AWS_GCP() {
        // Given
        String tenantId = "tenant-multi-cloud-gcp";
        List<CloudResource> resources = createMockResources(
                CloudProvider.ProviderType.AWS,
                CloudProvider.ProviderType.GCP
        );
        when(cloudResourceService.getResourcesByTenant(tenantId)).thenReturn(resources);
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.MULTI_CLOUD);
    }
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - MULTI_CLOUD (3개 프로바이더)")
    void detectEnvironment_MultiCloud_Three_Providers() {
        // Given
        String tenantId = "tenant-three-clouds";
        List<CloudResource> resources = createMockResources(
                CloudProvider.ProviderType.AWS,
                CloudProvider.ProviderType.AZURE,
                CloudProvider.ProviderType.GCP
        );
        when(cloudResourceService.getResourcesByTenant(tenantId)).thenReturn(resources);
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.MULTI_CLOUD);
    }
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - HYBRID (AWS + ON_PREMISE)")
    void detectEnvironment_Hybrid_AWS_OnPremise() {
        // Given
        String tenantId = "tenant-hybrid";
        List<CloudResource> resources = createMockResources(
                CloudProvider.ProviderType.AWS,
                CloudProvider.ProviderType.ON_PREMISE
        );
        when(cloudResourceService.getResourcesByTenant(tenantId)).thenReturn(resources);
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.HYBRID);
    }
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - HYBRID (Azure + GCP + ON_PREMISE)")
    void detectEnvironment_Hybrid_MultiCloud_OnPremise() {
        // Given
        String tenantId = "tenant-hybrid-multi";
        List<CloudResource> resources = createMockResources(
                CloudProvider.ProviderType.AZURE,
                CloudProvider.ProviderType.GCP,
                CloudProvider.ProviderType.ON_PREMISE
        );
        when(cloudResourceService.getResourcesByTenant(tenantId)).thenReturn(resources);
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.HYBRID);
    }
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - ON_PREMISE (온프레미스만)")
    void detectEnvironment_OnPremise_Only() {
        // Given
        String tenantId = "tenant-onpremise";
        List<CloudResource> resources = createMockResources(
                CloudProvider.ProviderType.ON_PREMISE
        );
        when(cloudResourceService.getResourcesByTenant(tenantId)).thenReturn(resources);
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.ON_PREMISE);
    }
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - ON_PREMISE (리소스 없음)")
    void detectEnvironment_OnPremise_NoResources() {
        // Given
        String tenantId = "tenant-no-resources";
        when(cloudResourceService.getResourcesByTenant(tenantId))
                .thenReturn(Collections.emptyList());
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.ON_PREMISE);
    }
    
    @Test
    @DisplayName("멀티클라우드 환경 감지 - ON_PREMISE (null 리소스)")
    void detectEnvironment_OnPremise_NullResources() {
        // Given
        String tenantId = "tenant-null-resources";
        when(cloudResourceService.getResourcesByTenant(tenantId)).thenReturn(null);
        
        // When
        MultiCloudEnvironment result = multiCloudEnvironmentService.detectEnvironment(tenantId);
        
        // Then
        assertThat(result).isEqualTo(MultiCloudEnvironment.ON_PREMISE);
    }
    
    /**
     * 목 CloudResource 리스트 생성 헬퍼 메서드
     */
    private List<CloudResource> createMockResources(CloudProvider.ProviderType... providerTypes) {
        return Arrays.stream(providerTypes)
                .map(providerType -> {
                    CloudProvider provider = CloudProvider.builder()
                            .providerType(providerType)
                            .providerKey(providerType.name().toLowerCase())
                            .providerName(providerType.name())
                            .build();
                    
                    return CloudResource.builder()
                            .resourceId("resource-" + providerType.name())
                            .resourceName(providerType.name() + " Resource")
                            .provider(provider)
                            .build();
                })
                .toList();
    }
}

