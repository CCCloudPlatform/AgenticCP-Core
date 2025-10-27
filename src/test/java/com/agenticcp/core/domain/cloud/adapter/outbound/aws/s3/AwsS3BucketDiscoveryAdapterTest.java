package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudRegion;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.aws.S3BucketQuery;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudRegionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesResponse;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AwsS3BucketDiscoveryAdapter 단위 테스트 (간단 버전)
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AwsS3BucketDiscoveryAdapter 테스트")
class AwsS3BucketDiscoveryAdapterTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private ResourceGroupsTaggingApiClient taggingClient;

    @Mock
    private AwsS3BucketMapper mapper;

    @Mock
    private CloudProviderRepository cloudProviderRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CredentialProviderPort credentialProviderPort;

    @Mock
    private CloudRegionRepository cloudRegionRepository;

    @InjectMocks
    private AwsS3BucketDiscoveryAdapter adapter;

    private static final String TENANT_KEY = "test-tenant";
    private static final String BUCKET_NAME = "test-bucket";
    private CloudProvider awsProvider;

    @BeforeEach
    void setUp() {
        awsProvider = CloudProvider.builder()
                .providerKey("aws-provider")
                .providerType(CloudProvider.ProviderType.AWS)
                .providerName("Amazon Web Services")
                .status(Status.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("버킷 목록 조회 테스트")
    class ListBucketsTest {

        @Test
        @DisplayName("정상적인 버킷 목록 조회")
        void listBuckets_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));
                when(cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS)).thenReturn(Optional.of(awsProvider));
                
                S3BucketQuery query = S3BucketQuery.builder()
                        .page(0)
                        .size(10)
                        .build();

                CloudResource mockResource = CloudResource.builder()
                        .resourceId("bucket-" + BUCKET_NAME)
                        .resourceName(BUCKET_NAME)
                        .build();
                when(mapper.toCloudResource(any(), any())).thenReturn(mockResource);

                // Mock Resource Groups Tagging API response
                ResourceTagMapping mapping = ResourceTagMapping.builder()
                        .resourceARN("arn:aws:s3:::" + BUCKET_NAME)
                        .tags(Collections.emptyList())
                        .build();

                GetResourcesResponse response = GetResourcesResponse.builder()
                        .resourceTagMappingList(Collections.singletonList(mapping))
                        .paginationToken(null)
                        .build();

                when(taggingClient.getResources(any(GetResourcesRequest.class))).thenReturn(response);
                when(objectMapper.writeValueAsString(any())).thenReturn("{}");

                // When
                Page<CloudResource> result = adapter.listBuckets(query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).getResourceName()).isEqualTo(BUCKET_NAME);

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, CloudProvider.ProviderType.AWS, "default");
                verify(cloudProviderRepository).findFirstByProviderType(CloudProvider.ProviderType.AWS);
                verify(taggingClient).getResources(any(GetResourcesRequest.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("자격증명 해결 실패 시 예외 발생")
        void listBuckets_CredentialResolutionFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any()))
                        .thenThrow(new RuntimeException("Credential resolution failed"));

                S3BucketQuery query = S3BucketQuery.builder()
                        .page(0)
                        .size(10)
                        .build();

                // When & Then
                assertThatThrownBy(() -> adapter.listBuckets(query))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("클라우드 서비스 연결에 실패했습니다");

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, CloudProvider.ProviderType.AWS, "default");
            }
        }
    }

    @Nested
    @DisplayName("버킷 조회 테스트")
    class GetBucketTest {

        @Test
        @DisplayName("정상적인 버킷 조회")
        void getBucket_Success() throws JsonProcessingException {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));
                when(cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS))
                        .thenReturn(Optional.of(awsProvider));

                // Mock CloudRegion
                CloudRegion mockRegion = CloudRegion.builder()
                        .regionKey("us-east-1")
                        .regionName("US East (N. Virginia)")
                        .build();

                CloudResource mockResource = CloudResource.builder()
                        .resourceId("bucket-" + BUCKET_NAME)
                        .resourceName(BUCKET_NAME)
                        .build();
                when(mapper.toCloudResource(any(), any())).thenReturn(mockResource);

                // Mock S3 API responses
                when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());
                when(s3Client.getBucketLocation(any(Consumer.class)))
                        .thenReturn(GetBucketLocationResponse.builder()
                                .locationConstraint("us-east-1")
                                .build());
                when(s3Client.getBucketTagging(any(Consumer.class)))
                        .thenReturn(GetBucketTaggingResponse.builder()
                                .tagSet(Collections.emptyList())
                                .build());

                when(objectMapper.writeValueAsString(any())).thenReturn("{}");

                // When
                Optional<CloudResource> result = adapter.getBucket(BUCKET_NAME);

                // Then
                assertThat(result).isPresent();
                assertThat(result.get().getResourceName()).isEqualTo(BUCKET_NAME);

                verify(cloudProviderRepository).findFirstByProviderType(CloudProvider.ProviderType.AWS);
                verify(s3Client).headBucket(any(HeadBucketRequest.class));
                verify(s3Client).getBucketLocation(any(Consumer.class));
                verify(s3Client).getBucketTagging(any(Consumer.class));
                verify(mapper).toCloudRegion(anyString());
            }
        }

        @Test
        @DisplayName("버킷이 존재하지 않는 경우")
        void getBucket_NotFound_ReturnsEmpty() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));
                when(s3Client.headBucket(any(HeadBucketRequest.class)))
                        .thenThrow(NoSuchBucketException.builder().build());

                // When
                Optional<CloudResource> result = adapter.getBucket(BUCKET_NAME);

                // Then
                assertThat(result).isEmpty();
                verify(s3Client).headBucket(any(HeadBucketRequest.class));
            }
        }
    }

    @Nested
    @DisplayName("버킷 존재 확인 테스트")
    class BucketExistsTest {

        @Test
        @DisplayName("버킷이 존재하는 경우 true 반환")
        void bucketExists_Exists_ReturnsTrue() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));
                when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

                // When
                boolean result = adapter.bucketExists(BUCKET_NAME);

                // Then
                assertThat(result).isTrue();
                verify(s3Client).headBucket(any(HeadBucketRequest.class));
            }
        }

        @Test
        @DisplayName("버킷이 존재하지 않는 경우 false 반환")
        void bucketExists_NotExists_ReturnsFalse() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));
                when(s3Client.headBucket(any(HeadBucketRequest.class)))
                        .thenThrow(NoSuchBucketException.builder().build());

                // When
                boolean result = adapter.bucketExists(BUCKET_NAME);

                // Then
                assertThat(result).isFalse();
                verify(s3Client).headBucket(any(HeadBucketRequest.class));
            }
        }
    }

    @Test
    @DisplayName("ProviderType 반환 테스트")
    void getProviderType_ReturnsAws() {
        // When
        CloudProvider.ProviderType result = adapter.getProviderType();

        // Then
        assertThat(result).isEqualTo(CloudProvider.ProviderType.AWS);
    }
}