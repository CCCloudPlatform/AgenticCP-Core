package com.agenticcp.core.domain.cloud.adapter.outbound.aws.s3;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.storage.CreateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.model.storage.UpdateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AwsS3BucketManagementAdapter 단위 테스트
 * @author AgenticCP Team
 * @version 1.0.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AwsS3BucketManagementAdapter 테스트")
class AwsS3BucketManagementAdapterTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private AwsS3BucketMapper mapper;

    @Mock
    private CloudProviderRepository cloudProviderRepository;

    @Mock
    private CredentialProviderPort credentialProviderPort;

    @InjectMocks
    private AwsS3BucketManagementAdapter adapter;

    private static final String CONTAINER_NAME = "test-container";
    private static final String TENANT_KEY = "test-tenant";
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
    @DisplayName("Container 생성 테스트")
    class CreateContainerTest {

        @Test
        @DisplayName("정상적인 Container 생성")
        void createContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));
                when(cloudProviderRepository.findFirstByProviderType(any())).thenReturn(Optional.of(awsProvider));

                CreateObjectStorageContainerCommand command = CreateObjectStorageContainerCommand.builder()
                        .containerName(CONTAINER_NAME)
                        .region("us-east-1")
                        .build();

                CloudResource mockResource = CloudResource.builder()
                        .resourceId(CONTAINER_NAME)
                        .resourceName(CONTAINER_NAME)
                        .build();
                when(mapper.toCloudResource(any(Bucket.class), any(CloudProvider.class))).thenReturn(mockResource);

                // Mock S3 API responses
                when(s3Client.createBucket(any(CreateBucketRequest.class)))
                        .thenReturn(CreateBucketResponse.builder().build());

                // When
                CloudResource result = adapter.createContainer(command);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceName()).isEqualTo(CONTAINER_NAME);

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, CloudProvider.ProviderType.AWS, "default");
                verify(s3Client).createBucket(any(CreateBucketRequest.class));
                verify(mapper).toCloudResource(any(Bucket.class), any(CloudProvider.class));
            }
        }

        @Test
        @DisplayName("이미 존재하는 Container명으로 생성 시 예외 발생 (다른 계정 소유)")
        void createContainer_AlreadyExists_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));

                when(s3Client.createBucket(any(CreateBucketRequest.class)))
                        .thenThrow(BucketAlreadyExistsException.builder().build());

                CreateObjectStorageContainerCommand command = CreateObjectStorageContainerCommand.builder()
                        .containerName(CONTAINER_NAME)
                        .region("us-east-1")
                        .build();

                // When & Then
                assertThatThrownBy(() -> adapter.createContainer(command))
                        .isInstanceOf(BusinessException.class);

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, CloudProvider.ProviderType.AWS, "default");
                verify(s3Client).createBucket(any(CreateBucketRequest.class));
            }
        }
    }

    @Nested
    @DisplayName("Container 삭제 테스트")
    class DeleteContainerTest {

        @Test
        @DisplayName("정상적인 Container 삭제")
        void deleteContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));

                when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                        .thenReturn(ListObjectsV2Response.builder()
                                .contents(Collections.emptyList())
                                .build());
                when(s3Client.deleteBucket(any(DeleteBucketRequest.class)))
                        .thenReturn(DeleteBucketResponse.builder().build());

                // When
                adapter.deleteContainer(CONTAINER_NAME);

                // Then
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, CloudProvider.ProviderType.AWS, "default");
                verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
                verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
            }
        }

        @Test
        @DisplayName("존재하지 않는 Container 삭제 시 정상 처리")
        void deleteContainer_NotFound_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));

                when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                        .thenThrow(NoSuchBucketException.builder().build());
                when(s3Client.deleteBucket(any(DeleteBucketRequest.class)))
                        .thenReturn(DeleteBucketResponse.builder().build());

                // When
                assertThatCode(() -> adapter.deleteContainer(CONTAINER_NAME))
                        .doesNotThrowAnyException();

                // Then
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, CloudProvider.ProviderType.AWS, "default");
                verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
                verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
            }
        }
    }

    @Nested
    @DisplayName("Container 강제 삭제 테스트")
    class ForceDeleteContainerTest {

        @Test
        @DisplayName("정상적인 Container 강제 삭제")
        void forceDeleteContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));

                // deleteAllObjects()를 위한 Mocking
                when(s3Client.listObjectVersions(any(ListObjectVersionsRequest.class)))
                        .thenReturn(ListObjectVersionsResponse.builder()
                                .versions(Collections.emptyList())
                                .deleteMarkers(Collections.emptyList())
                                .isTruncated(false)
                                .build());
                when(s3Client.deleteBucket(any(DeleteBucketRequest.class)))
                        .thenReturn(DeleteBucketResponse.builder().build());

                // When
                adapter.forceDeleteContainer(CONTAINER_NAME);

                // Then
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, CloudProvider.ProviderType.AWS, "default");
                verify(s3Client).listObjectVersions(any(ListObjectVersionsRequest.class));
                verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
            }
        }
    }

    @Nested
    @DisplayName("Container 수정 테스트")
    class UpdateContainerTest {

        @Test
        @DisplayName("정상적인 Container 수정")
        void updateContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));
                when(cloudProviderRepository.findFirstByProviderType(any())).thenReturn(Optional.of(awsProvider));

                UpdateObjectStorageContainerCommand command = UpdateObjectStorageContainerCommand.builder()
                        .containerName(CONTAINER_NAME)
                        .tags(Map.of("Environment", "Test"))
                        .build();

                CloudResource mockResource = CloudResource.builder()
                        .resourceId(CONTAINER_NAME)
                        .resourceName(CONTAINER_NAME)
                        .build();
                when(mapper.toCloudResource(any(Bucket.class), any(CloudProvider.class))).thenReturn(mockResource);

                // Mock S3 API responses
                when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());
                when(s3Client.putBucketTagging(any(PutBucketTaggingRequest.class)))
                        .thenReturn(PutBucketTaggingResponse.builder().build());

                // When
                CloudResource result = adapter.updateContainer(command);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceName()).isEqualTo(CONTAINER_NAME);

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, CloudProvider.ProviderType.AWS, "default");
                verify(s3Client).headBucket(any(HeadBucketRequest.class));
                verify(s3Client).putBucketTagging(any(PutBucketTaggingRequest.class));
                verify(mapper).toCloudResource(any(Bucket.class), any(CloudProvider.class));
            }
        }

        @Test
        @DisplayName("존재하지 않는 Container 수정 시 예외 발생")
        void updateContainer_NotFound_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(credentialProviderPort.resolveCredentials(any(), any(), any())).thenReturn(mock(AwsCredentials.class));

                when(s3Client.headBucket(any(HeadBucketRequest.class)))
                        .thenThrow(NoSuchBucketException.builder().build());

                UpdateObjectStorageContainerCommand command = UpdateObjectStorageContainerCommand.builder()
                        .containerName(CONTAINER_NAME)
                        .tags(Map.of("Environment", "Test"))
                        .build();

                // When & Then
                assertThatThrownBy(() -> adapter.updateContainer(command))
                        .isInstanceOf(BusinessException.class);

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, CloudProvider.ProviderType.AWS, "default");
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
