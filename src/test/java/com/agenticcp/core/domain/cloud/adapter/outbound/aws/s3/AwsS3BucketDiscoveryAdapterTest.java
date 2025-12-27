package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsS3Config;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.dto.ObjectStorageContainerQueryRequest;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesResponse;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsS3BucketDiscoveryAdapter 테스트")
class AwsS3BucketDiscoveryAdapterTest {

    @Mock
    private AwsS3BucketMapper mapper;

    @Mock
    private CloudProviderRepository cloudProviderRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private AwsS3Config awsS3Config;

    @Mock
    private ResourceGroupsTaggingApiClient taggingClient;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private AwsS3BucketDiscoveryAdapter adapter;

    private static final String TENANT_KEY = "test-tenant";
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String BUCKET_NAME = "test-bucket";

    private CloudProvider awsProvider;
    private AwsSessionCredential session;

    @BeforeEach
    void setUp() {
        awsProvider = CloudProvider.builder()
                .providerKey("aws-provider")
                .providerType(CloudProvider.ProviderType.AWS)
                .providerName("Amazon Web Services")
                .status(Status.ACTIVE)
                .build();

        session = AwsSessionCredential.builder()
                .accessKeyId("AKIA_TEST")
                .secretAccessKey("secret")
                .sessionToken("token")
                .region("us-east-1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Test
    @DisplayName("버킷 목록 조회 성공")
    void listBuckets_Success() throws Exception {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
            when(accountCredentialManagementPort.getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(CloudProvider.ProviderType.AWS)))
                    .thenReturn(session);
            when(awsS3Config.createResourceGroupsTaggingApiClient(eq(session), isNull()))
                    .thenReturn(taggingClient);
            when(cloudProviderRepository.findFirstByProviderType(CloudProvider.ProviderType.AWS))
                    .thenReturn(Optional.of(awsProvider));

            CloudResource mockResource = CloudResource.builder()
                    .resourceId("bucket-" + BUCKET_NAME)
                    .name(BUCKET_NAME)
                    .build();
            when(mapper.toCloudResource(any(), any())).thenReturn(mockResource);

            ResourceTagMapping mapping = ResourceTagMapping.builder()
                    .resourceARN("arn:aws:s3:::" + BUCKET_NAME)
                    .tags(Collections.emptyList())
                    .build();
            GetResourcesResponse response = GetResourcesResponse.builder()
                    .resourceTagMappingList(Collections.singletonList(mapping))
                    .paginationToken(null)
                    .build();
            when(taggingClient.getResources(any(GetResourcesRequest.class))).thenReturn(response);

            ObjectStorageContainerQueryRequest query = ObjectStorageContainerQueryRequest.builder()
                    .accountScope(ACCOUNT_SCOPE)
                    .page(0)
                    .size(10)
                    .build();

            Page<CloudResource> result = adapter.listContainers(query);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo(BUCKET_NAME);
            verify(accountCredentialManagementPort).getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(CloudProvider.ProviderType.AWS));
            verify(taggingClient).getResources(any(GetResourcesRequest.class));
        }
    }

    @Test
    @DisplayName("버킷 조회 시 존재하지 않으면 Optional.empty 반환")
    void getBucket_NotFound_ReturnsEmpty() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
            when(accountCredentialManagementPort.getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(CloudProvider.ProviderType.AWS)))
                    .thenReturn(session);
            when(awsS3Config.createS3Client(eq(session), isNull())).thenReturn(s3Client);
            when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().build());

            Optional<CloudResource> result = adapter.getContainer(ACCOUNT_SCOPE, BUCKET_NAME);

            assertThat(result).isEmpty();
            verify(accountCredentialManagementPort).getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(CloudProvider.ProviderType.AWS));
        }
    }

    @Test
    @DisplayName("버킷 존재 여부 확인 - 존재하지 않으면 false")
    void containerExists_NotExists_ReturnsFalse() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
            when(accountCredentialManagementPort.getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(CloudProvider.ProviderType.AWS)))
                    .thenReturn(session);
            when(awsS3Config.createS3Client(eq(session), isNull())).thenReturn(s3Client);
            when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().build());

            boolean exists = adapter.containerExists(ACCOUNT_SCOPE, BUCKET_NAME);

            assertThat(exists).isFalse();
            verify(s3Client).headBucket(any(HeadBucketRequest.class));
        }
    }

    @Test
    @DisplayName("AccountScope 누락 시 예외 발생")
    void listBuckets_WithoutAccountScope_ThrowsException() {
        ObjectStorageContainerQueryRequest query = ObjectStorageContainerQueryRequest.builder()
                .page(0)
                .size(10)
                .build();

        assertThatThrownBy(() -> adapter.listContainers(query))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("ProviderType 반환 테스트")
    void getProviderType_ReturnsAws() {
        assertThat(adapter.getProviderType()).isEqualTo(CloudProvider.ProviderType.AWS);
    }
}