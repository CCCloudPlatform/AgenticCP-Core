package com.agenticcp.core.domain.cloud.adapter.outbound.aws.cloudfront;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsCloudFrontConfig;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.cdn.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * AwsCloudFrontManagementAdapter 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AwsCloudFrontManagementAdapter 테스트")
class AwsCloudFrontManagementAdapterTest {

    @Mock
    private AwsCloudFrontConfig awsCloudFrontConfig;

    @Mock
    private AwsCloudFrontMapper awsCloudFrontMapper;

    @Mock
    private CloudFrontClient cloudFrontClient;

    @InjectMocks
    private AwsCloudFrontManagementAdapter adapter;

    private static final String DISTRIBUTION_ID = "E2QWRUHAPOMQZL";
    private static final String DISTRIBUTION_ARN = "arn:aws:cloudfront::123456789012:distribution/E2QWRUHAPOMQZL";
    private static final String ETAG = "ETAG123456789";
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String TENANT_KEY = "test-tenant";

    private AwsSessionCredential mockSession;
    private OriginConfig originConfig;
    private CacheBehaviorConfig cacheBehaviorConfig;

    @BeforeEach
    void setUp() {
        mockSession = AwsSessionCredential.builder()
                .accessKeyId("AKIA_TEST")
                .secretAccessKey("secret")
                .sessionToken("token")
                .region("us-east-1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        originConfig = OriginConfig.builder()
                .id("origin-1")
                .domainName("example.com")
                .type(OriginConfig.OriginType.CUSTOM)
                .httpPort(80)
                .httpsPort(443)
                .originProtocolPolicy("https-only")
                .build();

        cacheBehaviorConfig = CacheBehaviorConfig.builder()
                .pathPattern("/*")
                .ttl(86400L)
                .allowedMethods(List.of("GET", "HEAD", "OPTIONS"))
                .compress(true)
                .viewerProtocolPolicy("redirect-to-https")
                .build();
    }

    @Nested
    @DisplayName("Distribution 생성 테스트")
    class CreateDistributionTest {

        @Test
        @DisplayName("정상적인 Distribution 생성 (태그 없음)")
        void createDistribution_Success_WithoutTags() {
            // Given
            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionName("test-distribution")
                    .comment("Test distribution")
                    .enabled(true)
                    .origin(originConfig)
                    .cacheBehaviors(List.of(cacheBehaviorConfig))
                    .tenantKey(TENANT_KEY)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);

            Distribution distribution = createMockDistribution(DISTRIBUTION_ID, true);
            CreateDistributionResponse createResponse = CreateDistributionResponse.builder()
                    .distribution(distribution)
                    .eTag(ETAG)
                    .location("https://cloudfront.amazonaws.com/distribution/" + DISTRIBUTION_ID)
                    .build();

            when(cloudFrontClient.createDistribution(any(CreateDistributionRequest.class)))
                    .thenReturn(createResponse);

            CloudResource mockResource = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName("test-distribution")
                    .build();

            when(awsCloudFrontMapper.toCloudResource(eq(distribution), eq(ETAG), eq(command)))
                    .thenReturn(mockResource);

            // When
            CloudResource result = adapter.createDistribution(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(DISTRIBUTION_ID);
            assertThat(result.getResourceName()).isEqualTo("test-distribution");

            verify(awsCloudFrontConfig).createCloudFrontClient(mockSession);
            verify(cloudFrontClient).createDistribution(any(CreateDistributionRequest.class));
            verify(cloudFrontClient, never()).tagResource(any(TagResourceRequest.class));
            verify(awsCloudFrontMapper).toCloudResource(eq(distribution), eq(ETAG), eq(command));
        }

        @Test
        @DisplayName("정상적인 Distribution 생성 (태그 포함)")
        void createDistribution_Success_WithTags() {
            // Given
            Map<String, String> tags = Map.of(
                    "Environment", "Test",
                    "Project", "CDN"
            );

            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionName("test-distribution")
                    .comment("Test distribution")
                    .enabled(true)
                    .origin(originConfig)
                    .cacheBehaviors(List.of(cacheBehaviorConfig))
                    .tags(tags)
                    .tenantKey(TENANT_KEY)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);

            Distribution distribution = createMockDistribution(DISTRIBUTION_ID, true);
            CreateDistributionResponse createResponse = CreateDistributionResponse.builder()
                    .distribution(distribution)
                    .eTag(ETAG)
                    .location("https://cloudfront.amazonaws.com/distribution/" + DISTRIBUTION_ID)
                    .build();

            when(cloudFrontClient.createDistribution(any(CreateDistributionRequest.class)))
                    .thenReturn(createResponse);
            when(cloudFrontClient.tagResource(any(TagResourceRequest.class)))
                    .thenReturn(TagResourceResponse.builder().build());

            CloudResource mockResource = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName("test-distribution")
                    .build();

            when(awsCloudFrontMapper.toCloudResource(eq(distribution), eq(ETAG), eq(command)))
                    .thenReturn(mockResource);

            // When
            CloudResource result = adapter.createDistribution(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(DISTRIBUTION_ID);

            verify(awsCloudFrontConfig).createCloudFrontClient(mockSession);
            verify(cloudFrontClient).createDistribution(any(CreateDistributionRequest.class));
            verify(cloudFrontClient).tagResource(any(TagResourceRequest.class));
            verify(awsCloudFrontMapper).toCloudResource(eq(distribution), eq(ETAG), eq(command));
        }

        @Test
        @DisplayName("Distribution 생성 실패 - CloudFrontException 발생")
        void createDistribution_Failure_CloudFrontException() {
            // Given
            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionName("test-distribution")
                    .origin(originConfig)
                    .tenantKey(TENANT_KEY)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);
            doThrow(CloudFrontException.builder()
                    .message("Too many distributions")
                    .statusCode(400)
                    .build())
                    .when(cloudFrontClient).createDistribution(any(CreateDistributionRequest.class));

            // When & Then
            assertThatThrownBy(() -> adapter.createDistribution(command))
                    .isInstanceOf(BusinessException.class);

            verify(awsCloudFrontConfig).createCloudFrontClient(mockSession);
            verify(cloudFrontClient).createDistribution(any(CreateDistributionRequest.class));
            verify(awsCloudFrontMapper, never()).toCloudResource(
                    any(Distribution.class), anyString(), any(CreateDistributionCommand.class));
        }

        @Test
        @DisplayName("Distribution 생성 후 태그 추가 실패 - 경고만 발생하고 계속 진행")
        void createDistribution_TagFailure_Continues() {
            // Given
            Map<String, String> tags = Map.of("Environment", "Test");

            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionName("test-distribution")
                    .origin(originConfig)
                    .cacheBehaviors(List.of(cacheBehaviorConfig))
                    .tags(tags)
                    .tenantKey(TENANT_KEY)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);

            Distribution distribution = createMockDistribution(DISTRIBUTION_ID, true);
            CreateDistributionResponse createResponse = CreateDistributionResponse.builder()
                    .distribution(distribution)
                    .eTag(ETAG)
                    .build();

            when(cloudFrontClient.createDistribution(any(CreateDistributionRequest.class)))
                    .thenReturn(createResponse);
            when(cloudFrontClient.tagResource(any(TagResourceRequest.class)))
                    .thenThrow(CloudFrontException.builder()
                            .message("Access denied")
                            .statusCode(403)
                            .build());

            CloudResource mockResource = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .build();

            when(awsCloudFrontMapper.toCloudResource(eq(distribution), eq(ETAG), eq(command)))
                    .thenReturn(mockResource);

            // When
            CloudResource result = adapter.createDistribution(command);

            // Then
            assertThat(result).isNotNull();
            verify(cloudFrontClient).tagResource(any(TagResourceRequest.class));
            verify(awsCloudFrontMapper).toCloudResource(eq(distribution), eq(ETAG), eq(command));
        }
    }

    @Nested
    @DisplayName("Distribution 수정 테스트")
    class UpdateDistributionTest {

        @Test
        @DisplayName("정상적인 Distribution 수정")
        void updateDistribution_Success() {
            // Given
            UpdateDistributionCommand command = UpdateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag(ETAG)
                    .comment("Updated comment")
                    .enabled(false)
                    .cacheBehaviors(List.of(cacheBehaviorConfig))
                    .tenantKey(TENANT_KEY)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);

            Distribution currentDistribution = createMockDistribution(DISTRIBUTION_ID, true);
            GetDistributionResponse getResponse = GetDistributionResponse.builder()
                    .distribution(currentDistribution)
                    .eTag(ETAG)
                    .build();

            when(cloudFrontClient.getDistribution(any(GetDistributionRequest.class)))
                    .thenReturn(getResponse);

            Distribution updatedDistribution = createMockDistribution(DISTRIBUTION_ID, false);
            UpdateDistributionResponse updateResponse = UpdateDistributionResponse.builder()
                    .distribution(updatedDistribution)
                    .eTag("ETAG_UPDATED")
                    .build();

            when(cloudFrontClient.updateDistribution(any(UpdateDistributionRequest.class)))
                    .thenReturn(updateResponse);

            CloudResource mockResource = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .build();

            when(awsCloudFrontMapper.toCloudResource(eq(updatedDistribution), eq("ETAG_UPDATED"), eq(command)))
                    .thenReturn(mockResource);

            // When
            CloudResource result = adapter.updateDistribution(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(DISTRIBUTION_ID);

            verify(awsCloudFrontConfig).createCloudFrontClient(mockSession);
            verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
            verify(cloudFrontClient).updateDistribution(any(UpdateDistributionRequest.class));
            verify(awsCloudFrontMapper).toCloudResource(eq(updatedDistribution), eq("ETAG_UPDATED"), eq(command));
        }

        @Test
        @DisplayName("Distribution 수정 실패 - ETag 불일치 (PreconditionFailed)")
        void updateDistribution_Failure_ETagMismatch() {
            // Given
            UpdateDistributionCommand command = UpdateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag("OLD_ETAG")
                    .comment("Updated comment")
                    .tenantKey(TENANT_KEY)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);

            Distribution currentDistribution = createMockDistribution(DISTRIBUTION_ID, true);
            GetDistributionResponse getResponse = GetDistributionResponse.builder()
                    .distribution(currentDistribution)
                    .eTag("NEW_ETAG")
                    .build();

            when(cloudFrontClient.getDistribution(any(GetDistributionRequest.class)))
                    .thenReturn(getResponse);

            doThrow(CloudFrontException.builder()
                    .message("Precondition failed")
                    .statusCode(412)
                    .build())
                    .when(cloudFrontClient).updateDistribution(any(UpdateDistributionRequest.class));

            // When & Then
            assertThatThrownBy(() -> adapter.updateDistribution(command))
                    .isInstanceOf(BusinessException.class);

            verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
            verify(cloudFrontClient).updateDistribution(any(UpdateDistributionRequest.class));
        }

        @Test
        @DisplayName("Distribution 수정 실패 - Distribution 없음")
        void updateDistribution_Failure_DistributionNotFound() {
            // Given
            UpdateDistributionCommand command = UpdateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag(ETAG)
                    .comment("Updated comment")
                    .tenantKey(TENANT_KEY)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);
            doThrow(NoSuchDistributionException.builder()
                    .message("Distribution not found")
                    .build())
                    .when(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));

            // When & Then
            assertThatThrownBy(() -> adapter.updateDistribution(command))
                    .isInstanceOf(BusinessException.class);

            verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
            verify(cloudFrontClient, never()).updateDistribution(any(UpdateDistributionRequest.class));
        }
    }

    @Nested
    @DisplayName("Distribution 삭제 테스트")
    class DeleteDistributionTest {

        @Test
        @DisplayName("정상적인 Distribution 삭제 (활성화된 경우 - 비활성화 후 삭제)")
        void deleteDistribution_Success_Enabled() {
            // Given
            DeleteDistributionCommand command = DeleteDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag(ETAG)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);

            Distribution enabledDistribution = createMockDistribution(DISTRIBUTION_ID, true);
            GetDistributionResponse getResponse = GetDistributionResponse.builder()
                    .distribution(enabledDistribution)
                    .eTag(ETAG)
                    .build();

            // waitForDeployment에서도 getDistribution을 호출하므로 Deployed 상태 반환
            DistributionConfig deployedConfig = DistributionConfig.builder()
                    .enabled(false)
                    .comment("Test distribution")
                    .callerReference("test-caller-ref")
                    .origins(Origins.builder()
                            .quantity(1)
                            .items(Origin.builder()
                                    .id("origin-1")
                                    .domainName("example.com")
                                    .build())
                            .build())
                    .defaultCacheBehavior(DefaultCacheBehavior.builder()
                            .targetOriginId("origin-1")
                            .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                            .build())
                    .build();
            
            Distribution deployedDistribution = Distribution.builder()
                    .id(DISTRIBUTION_ID)
                    .arn(DISTRIBUTION_ARN)
                    .status("Deployed")  // 배포 완료 상태
                    .domainName("d1234567890.cloudfront.net")
                    .distributionConfig(deployedConfig)
                    .build();
            
            GetDistributionResponse deployedResponse = GetDistributionResponse.builder()
                    .distribution(deployedDistribution)
                    .eTag("ETAG_DEPLOYED")
                    .build();

            // 첫 번째 호출: deleteDistribution에서 상태 확인 (enabled=true, status=Deployed)
            // 두 번째 호출: waitForDeployment에서 배포 상태 확인 (status=Deployed)
            when(cloudFrontClient.getDistribution(any(GetDistributionRequest.class)))
                    .thenReturn(getResponse)  // 첫 번째: 활성화된 상태
                    .thenReturn(deployedResponse);  // 두 번째: 배포 완료 상태

            Distribution disabledDistribution = createMockDistribution(DISTRIBUTION_ID, false);
            UpdateDistributionResponse updateResponse = UpdateDistributionResponse.builder()
                    .distribution(disabledDistribution)
                    .eTag("ETAG_DISABLED")
                    .build();

            when(cloudFrontClient.updateDistribution(any(UpdateDistributionRequest.class)))
                    .thenReturn(updateResponse);
            when(cloudFrontClient.deleteDistribution(any(DeleteDistributionRequest.class)))
                    .thenReturn(DeleteDistributionResponse.builder().build());

            // When
            adapter.deleteDistribution(command);

            // Then
            verify(awsCloudFrontConfig).createCloudFrontClient(mockSession);
            // getDistribution은 최소 2번 호출됨 (상태 확인 + 배포 완료 확인)
            verify(cloudFrontClient, atLeast(2)).getDistribution(any(GetDistributionRequest.class));
            verify(cloudFrontClient).updateDistribution(any(UpdateDistributionRequest.class));
            verify(cloudFrontClient).deleteDistribution(any(DeleteDistributionRequest.class));
        }

        @Test
        @DisplayName("정상적인 Distribution 삭제 (비활성화된 경우 - 바로 삭제)")
        void deleteDistribution_Success_Disabled() {
            // Given
            DeleteDistributionCommand command = DeleteDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag(ETAG)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);

            Distribution disabledDistribution = createMockDistribution(DISTRIBUTION_ID, false);
            GetDistributionResponse getResponse = GetDistributionResponse.builder()
                    .distribution(disabledDistribution)
                    .eTag(ETAG)
                    .build();

            when(cloudFrontClient.getDistribution(any(GetDistributionRequest.class)))
                    .thenReturn(getResponse);
            when(cloudFrontClient.deleteDistribution(any(DeleteDistributionRequest.class)))
                    .thenReturn(DeleteDistributionResponse.builder().build());

            // When
            adapter.deleteDistribution(command);

            // Then
            verify(awsCloudFrontConfig).createCloudFrontClient(mockSession);
            verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
            verify(cloudFrontClient, never()).updateDistribution(any(UpdateDistributionRequest.class));
            verify(cloudFrontClient).deleteDistribution(any(DeleteDistributionRequest.class));
        }

        @Test
        @DisplayName("Distribution 삭제 실패 - Distribution 없음")
        void deleteDistribution_Failure_DistributionNotFound() {
            // Given
            DeleteDistributionCommand command = DeleteDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag(ETAG)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);
            doThrow(NoSuchDistributionException.builder()
                    .message("Distribution not found")
                    .build())
                    .when(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));

            // When & Then
            assertThatThrownBy(() -> adapter.deleteDistribution(command))
                    .isInstanceOf(BusinessException.class);

            verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
            verify(cloudFrontClient, never()).deleteDistribution(any(DeleteDistributionRequest.class));
        }

        @Test
        @DisplayName("Distribution 삭제 실패 - 삭제 중 예외 발생")
        void deleteDistribution_Failure_DeleteException() {
            // Given
            DeleteDistributionCommand command = DeleteDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag(ETAG)
                    .session(mockSession)
                    .build();

            when(awsCloudFrontConfig.createCloudFrontClient(mockSession)).thenReturn(cloudFrontClient);

            Distribution disabledDistribution = createMockDistribution(DISTRIBUTION_ID, false);
            GetDistributionResponse getResponse = GetDistributionResponse.builder()
                    .distribution(disabledDistribution)
                    .eTag(ETAG)
                    .build();

            when(cloudFrontClient.getDistribution(any(GetDistributionRequest.class)))
                    .thenReturn(getResponse);
            doThrow(CloudFrontException.builder()
                    .message("Distribution is still deployed")
                    .statusCode(409)
                    .build())
                    .when(cloudFrontClient).deleteDistribution(any(DeleteDistributionRequest.class));

            // When & Then
            assertThatThrownBy(() -> adapter.deleteDistribution(command))
                    .isInstanceOf(BusinessException.class);

            verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
            verify(cloudFrontClient).deleteDistribution(any(DeleteDistributionRequest.class));
        }
    }

    @Nested
    @DisplayName("ProviderScoped 테스트")
    class ProviderScopedTest {

        @Test
        @DisplayName("getProviderType은 AWS를 반환")
        void getProviderType_ReturnsAWS() {
            // When
            ProviderType result = adapter.getProviderType();

            // Then
            assertThat(result).isEqualTo(ProviderType.AWS);
        }
    }

    /**
     * Mock Distribution 객체 생성 헬퍼 메서드
     */
    private Distribution createMockDistribution(String distributionId, boolean enabled) {
        DistributionConfig config = DistributionConfig.builder()
                .enabled(enabled)
                .comment("Test distribution")
                .callerReference("test-caller-ref")
                .origins(Origins.builder()
                        .quantity(1)
                        .items(Origin.builder()
                                .id("origin-1")
                                .domainName("example.com")
                                .build())
                        .build())
                .defaultCacheBehavior(DefaultCacheBehavior.builder()
                        .targetOriginId("origin-1")
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .build())
                .build();

        return Distribution.builder()
                .id(distributionId)
                .arn(DISTRIBUTION_ARN)
                .status(enabled ? "Deployed" : "InProgress")
                .domainName("d1234567890.cloudfront.net")
                .distributionConfig(config)
                .build();
    }
}

