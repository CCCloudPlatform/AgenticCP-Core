package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.port.model.S3BucketQuery;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import com.agenticcp.core.domain.cloud.port.outbound.aws.S3BucketDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.aws.S3BucketManagementPort;
import com.agenticcp.core.domain.cloud.service.aws.S3BucketPortRouter;
import com.agenticcp.core.domain.cloud.service.aws.S3BucketUseCaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;

/**
 * S3BucketUseCaseService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3 버킷 유스케이스 서비스 테스트")
class S3BucketUseCaseServiceTest {

    @Mock
    private S3BucketPortRouter router;
    
    @Mock
    private CapabilityGuard capabilityGuard;
    
    @Mock
    private CredentialProviderPort credentialProviderPort;
    
    @Mock
    private TracingPort tracingPort;
    
    @Mock
    private S3BucketDiscoveryPort discoveryPort;
    
    @Mock
    private S3BucketManagementPort managementPort;
    
    @Mock
    private AutoCloseable span;
    
    private S3BucketUseCaseService s3BucketUseCaseService;
    
    private CloudProvider.ProviderType providerType;
    private String bucketName;
    private String region;
    private Map<String, String> tags;
    private CloudResource mockBucket;
    
    @BeforeEach
    void setUp() {
        s3BucketUseCaseService = new S3BucketUseCaseService(
            router, capabilityGuard, credentialProviderPort, tracingPort
        );
        
        providerType = CloudProvider.ProviderType.AWS;
        bucketName = "test-bucket";
        region = "ap-northeast-2";
        tags = Map.of("Environment", "test", "Project", "agenticcp");
        
        // Mock CloudResource 생성
        mockBucket = CloudResource.builder()
            .resourceId(bucketName)
            .resourceName(bucketName)
            .resourceType(CloudResource.ResourceType.BUCKET)
            .lifecycleState(CloudResource.LifecycleState.RUNNING)
            .createdInCloud(LocalDateTime.now())
            .build();
    }
    
    @Test
    @DisplayName("S3 버킷 생성 성공")
    void createBucket_Success() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.management(providerType)).thenReturn(managementPort);
            when(managementPort.createBucket(bucketName, region, tags)).thenReturn(mockBucket);
            
            // When
            CloudResource result = s3BucketUseCaseService.createBucket(providerType, bucketName, region, tags);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(bucketName);
            assertThat(result.getResourceType()).isEqualTo(CloudResource.ResourceType.BUCKET);
            
            verify(capabilityGuard).ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(managementPort).createBucket(bucketName, region, tags);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 생성 실패 - 예외 발생")
    void createBucket_Failure() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            RuntimeException exception = new RuntimeException("버킷 생성 실패");
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.management(providerType)).thenReturn(managementPort);
            when(managementPort.createBucket(bucketName, region, tags)).thenThrow(exception);
            
            // When & Then
            assertThatThrownBy(() -> s3BucketUseCaseService.createBucket(providerType, bucketName, region, tags))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("버킷 생성 실패");
            
            verify(capabilityGuard).ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 수정 성공")
    void updateBucket_Success() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            Boolean versioningEnabled = true;
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.management(providerType)).thenReturn(managementPort);
            when(managementPort.updateBucket(bucketName, versioningEnabled, tags)).thenReturn(mockBucket);
            
            // When
            CloudResource result = s3BucketUseCaseService.updateBucket(providerType, bucketName, versioningEnabled, tags);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(bucketName);
            
            verify(capabilityGuard).ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(managementPort).updateBucket(bucketName, versioningEnabled, tags);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 수정 실패 - 예외 발생")
    void updateBucket_Failure() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            Boolean versioningEnabled = true;
            RuntimeException exception = new RuntimeException("버킷 수정 실패");
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.management(providerType)).thenReturn(managementPort);
            when(managementPort.updateBucket(bucketName, versioningEnabled, tags)).thenThrow(exception);
            
            // When & Then
            assertThatThrownBy(() -> s3BucketUseCaseService.updateBucket(providerType, bucketName, versioningEnabled, tags))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("버킷 수정 실패");
            
            verify(capabilityGuard).ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 삭제 성공")
    void deleteBucket_Success() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.management(providerType)).thenReturn(managementPort);
            
            // When
            s3BucketUseCaseService.deleteBucket(providerType, bucketName);
            
            // Then
            verify(capabilityGuard).ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(managementPort).deleteBucket(bucketName);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 삭제 실패 - 예외 발생")
    void deleteBucket_Failure() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            RuntimeException exception = new RuntimeException("버킷 삭제 실패");
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.management(providerType)).thenReturn(managementPort);
            doThrow(exception).when(managementPort).deleteBucket(bucketName);
            
            // When & Then
            assertThatThrownBy(() -> s3BucketUseCaseService.deleteBucket(providerType, bucketName))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("버킷 삭제 실패");
            
            verify(capabilityGuard).ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 강제 삭제 성공")
    void forceDeleteBucket_Success() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.management(providerType)).thenReturn(managementPort);
            
            // When
            s3BucketUseCaseService.forceDeleteBucket(providerType, bucketName);
            
            // Then
            verify(capabilityGuard).ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(managementPort).forceDeleteBucket(bucketName);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 강제 삭제 실패 - 예외 발생")
    void forceDeleteBucket_Failure() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            RuntimeException exception = new RuntimeException("버킷 강제 삭제 실패");
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.management(providerType)).thenReturn(managementPort);
            doThrow(exception).when(managementPort).forceDeleteBucket(bucketName);
            
            // When & Then
            assertThatThrownBy(() -> s3BucketUseCaseService.forceDeleteBucket(providerType, bucketName))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("버킷 강제 삭제 실패");
            
            verify(capabilityGuard).ensureSupported(providerType, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 목록 조회 성공")
    void listBuckets_Success() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            S3BucketQuery query = S3BucketQuery.builder()
                .page(0)
                .size(10)
                .build();
            
            Page<CloudResource> mockPage = new PageImpl<>(List.of(mockBucket), PageRequest.of(0, 10), 1);
            
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.discovery(providerType)).thenReturn(discoveryPort);
            when(discoveryPort.listBuckets(query)).thenReturn(mockPage);
            
            // When
            Page<CloudResource> result = s3BucketUseCaseService.listBuckets(providerType, query);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getResourceId()).isEqualTo(bucketName);
            
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(discoveryPort).listBuckets(query);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 조회 성공")
    void getBucket_Success() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.discovery(providerType)).thenReturn(discoveryPort);
            when(discoveryPort.getBucket(bucketName)).thenReturn(Optional.of(mockBucket));
            
            // When
            CloudResource result = s3BucketUseCaseService.getBucket(providerType, bucketName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(bucketName);
            
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(discoveryPort).getBucket(bucketName);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 조회 실패 - 버킷 없음")
    void getBucket_NotFound() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.discovery(providerType)).thenReturn(discoveryPort);
            when(discoveryPort.getBucket(bucketName)).thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> s3BucketUseCaseService.getBucket(providerType, bucketName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("S3 버킷을 찾을 수 없습니다: " + bucketName);
            
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(discoveryPort).getBucket(bucketName);
            verify(span).close();
        }
    }
    
    @Test
    @DisplayName("S3 버킷 존재 확인 성공")
    void bucketExists_Success() throws Exception {
        // Given
        try (MockedStatic<TenantContextHolder> mockedTenantContext = mockStatic(TenantContextHolder.class)) {
            mockedTenantContext.when(TenantContextHolder::getCurrentTenantKey).thenReturn("test-tenant");
            
            when(tracingPort.startSpan(anyString(), anyMap())).thenReturn(span);
            when(router.discovery(providerType)).thenReturn(discoveryPort);
            when(discoveryPort.bucketExists(bucketName)).thenReturn(true);
            
            // When
            boolean exists = s3BucketUseCaseService.bucketExists(providerType, bucketName);
            
            // Then
            assertThat(exists).isTrue();
            
            verify(credentialProviderPort).resolveCredentials("test-tenant", providerType, null);
            verify(discoveryPort).bucketExists(bucketName);
            verify(span).close();
        }
    }
}
