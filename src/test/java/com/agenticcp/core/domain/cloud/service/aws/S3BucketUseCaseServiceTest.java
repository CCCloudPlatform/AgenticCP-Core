package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.aws.*;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.port.outbound.aws.S3BucketDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.aws.S3BucketManagementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * S3BucketUseCaseService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3BucketUseCaseService 테스트")
class S3BucketUseCaseServiceTest {

    @Mock
    private S3BucketPortRouter router;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private CredentialProviderPort credentialProviderPort;

    @Mock
    private S3BucketManagementPort managementPort;

    @Mock
    private S3BucketDiscoveryPort discoveryPort;

    @InjectMocks
    private S3BucketUseCaseService s3BucketUseCaseService;

    private static final String TENANT_KEY = "test-tenant";
    private static final CloudProvider.ProviderType AWS = CloudProvider.ProviderType.AWS;
    private static final String BUCKET_NAME = "test-bucket";
    private static final String REGION = "us-east-1";

    @BeforeEach
    void setUp() {
        lenient().when(router.management(AWS)).thenReturn(managementPort);
        lenient().when(router.discovery(AWS)).thenReturn(discoveryPort);
    }

    @Nested
    @DisplayName("S3 버킷 생성 테스트")
    class CreateBucketTest {

        private CreateS3BucketRequest request;
        private CloudResource expectedBucket;

        @BeforeEach
        void setUp() {
            Map<String, String> tags = new HashMap<>();
            tags.put("Environment", "test");
            tags.put("Project", "agenticcp");

            request = CreateS3BucketRequest.builder()
                    .bucketName(BUCKET_NAME)
                    .region(REGION)
                    .objectOwnership("BucketOwnerEnforced")
                    .objectLockEnabled(false)
                    .tags(tags)
                    .build();

            expectedBucket = CloudResource.builder()
                    .resourceId("bucket-" + BUCKET_NAME)
                    .resourceName(BUCKET_NAME)
                    .displayName("Test Bucket")
                    .build();
        }

        @Test
        @DisplayName("정상적인 S3 버킷 생성")
        void createBucket_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(managementPort.createBucket(any(CreateS3BucketCommand.class))).thenReturn(expectedBucket);

                // When
                CloudResource result = s3BucketUseCaseService.createBucket(AWS, request);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceName()).isEqualTo(BUCKET_NAME);
                assertThat(result.getResourceId()).isEqualTo("bucket-" + BUCKET_NAME);

                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, REGION);
                verify(managementPort).createBucket(any(CreateS3BucketCommand.class));
            }
        }

        @Test
        @DisplayName("Capability 검증 실패 시 예외 발생")
        void createBucket_CapabilityCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                doThrow(new RuntimeException("Capability not supported"))
                        .when(capabilityGuard).ensureSupported(any(), any(), any(), any());

                // When & Then
                assertThatThrownBy(() -> s3BucketUseCaseService.createBucket(AWS, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Capability not supported");

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, REGION);
                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
                verify(managementPort, never()).createBucket(any());
            }
        }

        @Test
        @DisplayName("Credential 검증 실패 시 예외 발생")
        void createBucket_CredentialCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                doThrow(new RuntimeException("Invalid credentials"))
                        .when(credentialProviderPort).resolveCredentials(any(), any(), any());

                // When & Then
                assertThatThrownBy(() -> s3BucketUseCaseService.createBucket(AWS, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Invalid credentials");

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, REGION);
                verify(capabilityGuard, never()).ensureSupported(any(), any(), any(), any());
                verify(managementPort, never()).createBucket(any());
            }
        }
    }

    @Nested
    @DisplayName("S3 버킷 업데이트 테스트")
    class UpdateBucketTest {

        private UpdateS3BucketRequest request;
        private CloudResource expectedBucket;

        @BeforeEach
        void setUp() {
            Map<String, String> tags = new HashMap<>();
            tags.put("Environment", "production");
            tags.put("Updated", "true");

            request = UpdateS3BucketRequest.builder()
                    .versioningEnabled(true)
                    .tags(tags)
                    .build();

            expectedBucket = CloudResource.builder()
                    .resourceId("bucket-" + BUCKET_NAME)
                    .resourceName(BUCKET_NAME)
                    .displayName("Updated Test Bucket")
                    .build();
        }

        @Test
        @DisplayName("정상적인 S3 버킷 업데이트")
        void updateBucket_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(managementPort.updateBucket(any(UpdateS3BucketCommand.class))).thenReturn(expectedBucket);

                // When
                CloudResource result = s3BucketUseCaseService.updateBucket(AWS, BUCKET_NAME, request);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceName()).isEqualTo(BUCKET_NAME);
                assertThat(result.getDisplayName()).isEqualTo("Updated Test Bucket");

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
                verify(managementPort).updateBucket(any(UpdateS3BucketCommand.class));
            }
        }
    }

    @Nested
    @DisplayName("S3 버킷 목록 조회 테스트")
    class ListBucketsTest {

        private S3BucketQuery query;
        private Page<CloudResource> expectedPage;

        @BeforeEach
        void setUp() {
            query = S3BucketQuery.builder()
                    .page(0)
                    .size(10)
                    .nameContains("test")
                    .sortBy("name")
                    .sortDirection("asc")
                    .build();

            CloudResource bucket1 = CloudResource.builder()
                    .resourceId("bucket-1")
                    .resourceName("test-bucket-1")
                    .build();

            CloudResource bucket2 = CloudResource.builder()
                    .resourceId("bucket-2")
                    .resourceName("test-bucket-2")
                    .build();

            expectedPage = new PageImpl<>(List.of(bucket1, bucket2), PageRequest.of(0, 10), 2);
        }

        @Test
        @DisplayName("정상적인 S3 버킷 목록 조회")
        void listBuckets_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.listBuckets(query)).thenReturn(expectedPage);

                // When
                Page<CloudResource> result = s3BucketUseCaseService.listBuckets(AWS, query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getTotalElements()).isEqualTo(2);
                assertThat(result.getContent()).hasSize(2);
                assertThat(result.getContent().get(0).getResourceName()).isEqualTo("test-bucket-1");

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).listBuckets(query);
            }
        }

        @Test
        @DisplayName("빈 목록 조회")
        void listBuckets_EmptyResult() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                Page<CloudResource> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
                when(discoveryPort.listBuckets(query)).thenReturn(emptyPage);

                // When
                Page<CloudResource> result = s3BucketUseCaseService.listBuckets(AWS, query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getTotalElements()).isEqualTo(0);
                assertThat(result.getContent()).isEmpty();

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).listBuckets(query);
            }
        }
    }

    @Nested
    @DisplayName("S3 버킷 조회 테스트")
    class GetBucketTest {

        private CloudResource expectedBucket;

        @BeforeEach
        void setUp() {
            expectedBucket = CloudResource.builder()
                    .resourceId("bucket-" + BUCKET_NAME)
                    .resourceName(BUCKET_NAME)
                    .displayName("Test Bucket")
                    .build();
        }

        @Test
        @DisplayName("정상적인 S3 버킷 조회")
        void getBucket_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.getBucket(BUCKET_NAME)).thenReturn(Optional.of(expectedBucket));

                // When
                CloudResource result = s3BucketUseCaseService.getBucket(AWS, BUCKET_NAME);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceName()).isEqualTo(BUCKET_NAME);
                assertThat(result.getResourceId()).isEqualTo("bucket-" + BUCKET_NAME);

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).getBucket(BUCKET_NAME);
            }
        }

        @Test
        @DisplayName("존재하지 않는 버킷 조회 시 예외 발생")
        void getBucket_NotFound_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.getBucket(BUCKET_NAME)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> s3BucketUseCaseService.getBucket(AWS, BUCKET_NAME))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("S3 버킷을 찾을 수 없습니다: " + BUCKET_NAME);

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).getBucket(BUCKET_NAME);
            }
        }
    }

    @Nested
    @DisplayName("S3 버킷 존재 확인 테스트")
    class BucketExistsTest {

        @Test
        @DisplayName("버킷이 존재하는 경우 true 반환")
        void bucketExists_Exists_ReturnsTrue() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.bucketExists(BUCKET_NAME)).thenReturn(true);

                // When
                boolean result = s3BucketUseCaseService.bucketExists(AWS, BUCKET_NAME);

                // Then
                assertThat(result).isTrue();

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).bucketExists(BUCKET_NAME);
            }
        }

        @Test
        @DisplayName("버킷이 존재하지 않는 경우 false 반환")
        void bucketExists_NotExists_ReturnsFalse() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.bucketExists(BUCKET_NAME)).thenReturn(false);

                // When
                boolean result = s3BucketUseCaseService.bucketExists(AWS, BUCKET_NAME);

                // Then
                assertThat(result).isFalse();

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).bucketExists(BUCKET_NAME);
            }
        }
    }

    @Nested
    @DisplayName("S3 버킷 삭제 테스트")
    class DeleteBucketTest {

        @Test
        @DisplayName("정상적인 S3 버킷 삭제")
        void deleteBucket_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);

                // When
                s3BucketUseCaseService.deleteBucket(AWS, BUCKET_NAME);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(managementPort).deleteBucket(BUCKET_NAME);
            }
        }

        @Test
        @DisplayName("Capability 검증 실패 시 예외 발생")
        void deleteBucket_CapabilityCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                doThrow(new RuntimeException("Delete capability not supported"))
                        .when(capabilityGuard).ensureSupported(any(), any(), any(), any());

                // When & Then
                assertThatThrownBy(() -> s3BucketUseCaseService.deleteBucket(AWS, BUCKET_NAME))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Delete capability not supported");

                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
                verify(credentialProviderPort, never()).resolveCredentials(any(), any(), any());
                verify(managementPort, never()).deleteBucket(any());
            }
        }
    }

    @Nested
    @DisplayName("S3 버킷 강제 삭제 테스트")
    class ForceDeleteBucketTest {

        @Test
        @DisplayName("정상적인 S3 버킷 강제 삭제")
        void forceDeleteBucket_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);

                // When
                s3BucketUseCaseService.forceDeleteBucket(AWS, BUCKET_NAME);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(managementPort).forceDeleteBucket(BUCKET_NAME);
            }
        }

        @Test
        @DisplayName("Credential 검증 실패 시 예외 발생")
        void forceDeleteBucket_CredentialCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                doThrow(new RuntimeException("Invalid credentials for force delete"))
                        .when(credentialProviderPort).resolveCredentials(any(), any(), any());

                // When & Then
                assertThatThrownBy(() -> s3BucketUseCaseService.forceDeleteBucket(AWS, BUCKET_NAME))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Invalid credentials for force delete");

                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(managementPort, never()).forceDeleteBucket(any());
            }
        }
    }
}
