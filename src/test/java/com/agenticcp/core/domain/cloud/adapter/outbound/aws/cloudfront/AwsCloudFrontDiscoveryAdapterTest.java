package com.agenticcp.core.domain.cloud.adapter.outbound.aws.cloudfront;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsCloudFrontConfig;
import com.agenticcp.core.domain.cloud.dto.CDNDistributionQueryRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AwsCloudFrontDiscoveryAdapter 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AwsCloudFrontDiscoveryAdapter 테스트")
class AwsCloudFrontDiscoveryAdapterTest {

    @Mock
    private AwsCloudFrontMapper mapper;

    @Mock
    private CloudProviderRepository cloudProviderRepository;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private AwsCloudFrontConfig awsCloudFrontConfig;

    @Mock
    private AwsCloudFrontErrorTranslator errorTranslator;

    @Mock
    private CloudFrontClient cloudFrontClient;

    @InjectMocks
    private AwsCloudFrontDiscoveryAdapter adapter;

    private static final String TENANT_KEY = "test-tenant";
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String DISTRIBUTION_ID = "E2QWRUHAPOMQZL";
    private static final String DISTRIBUTION_ARN = "arn:aws:cloudfront::123456789012:distribution/E2QWRUHAPOMQZL";
    private static final String ETAG = "ETAG123456789";

    private AwsSessionCredential session;

    @BeforeEach
    void setUp() {
        session = AwsSessionCredential.builder()
                .accessKeyId("AKIA_TEST")
                .secretAccessKey("secret")
                .sessionToken("token")
                .region("us-east-1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        // CloudFrontClient close()가 예외를 던지지 않도록 설정
        doNothing().when(cloudFrontClient).close();
        
        // errorTranslator 기본 모킹: 예외가 발생할 경우를 대비
        lenient().when(errorTranslator.translate(any(Exception.class)))
                .thenAnswer(invocation -> {
                    Exception e = invocation.getArgument(0);
                    return new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE, e.getMessage());
                });
        
        // 공통 Mock 설정: 모든 테스트에서 사용되는 기본 설정
        lenient().when(accountCredentialManagementPort.getSession(
                eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(CloudProvider.ProviderType.AWS)))
                .thenReturn(session);
        lenient().when(awsCloudFrontConfig.createCloudFrontClient(session))
                .thenReturn(cloudFrontClient);
    }

    @Nested
    @DisplayName("Distribution 목록 조회 테스트")
    class ListDistributionsTest {

        @Test
        @DisplayName("정상적인 Distribution 목록 조회")
        void listDistributions_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                // accountCredentialManagementPort와 awsCloudFrontConfig는 @BeforeEach에서 설정됨

                DistributionSummary summary1 = createMockDistributionSummary("E2QWRUHAPOMQZL", true);
                DistributionSummary summary2 = createMockDistributionSummary("E2QWRUHAPOMQZL2", false);

                DistributionList distributionList = DistributionList.builder()
                        .items(summary1, summary2)
                        .isTruncated(false)
                        .nextMarker(null)
                        .build();

                ListDistributionsResponse listResponse = ListDistributionsResponse.builder()
                        .distributionList(distributionList)
                        .build();

                when(cloudFrontClient.listDistributions(any(ListDistributionsRequest.class)))
                        .thenReturn(listResponse);

                CloudResource resource1 = createMockCloudResource("E2QWRUHAPOMQZL", "dist-1");
                CloudResource resource2 = createMockCloudResource("E2QWRUHAPOMQZL2", "dist-2");

                when(mapper.toCloudResource(eq(summary1), any(CDNDistributionQueryRequest.class)))
                        .thenReturn(resource1);
                when(mapper.toCloudResource(eq(summary2), any(CDNDistributionQueryRequest.class)))
                        .thenReturn(resource2);

                when(cloudFrontClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                        .thenReturn(ListTagsForResourceResponse.builder()
                                .tags(Tags.builder()
                                        .items(Collections.emptyList())
                                        .build())
                                .build());

                CDNDistributionQueryRequest query = CDNDistributionQueryRequest.builder()
                        .providerType(CloudProvider.ProviderType.AWS)
                        .accountScope(ACCOUNT_SCOPE)
                        .tenantKey(TENANT_KEY)
                        .page(0)
                        .size(20)
                        .sortBy("name")
                        .sortDirection("asc")
                        .build();

                // When
                Page<CloudResource> result = adapter.listDistributions(query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(2);
                assertThat(result.getTotalElements()).isEqualTo(2);

                verify(accountCredentialManagementPort).getSession(
                        eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(CloudProvider.ProviderType.AWS));
                verify(cloudFrontClient).listDistributions(any(ListDistributionsRequest.class));
            }
        }

        @Test
        @DisplayName("Distribution 이름 필터링")
        void listDistributions_WithNameFilter_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                // accountCredentialManagementPort와 awsCloudFrontConfig는 @BeforeEach에서 설정됨

                DistributionSummary summary1 = createMockDistributionSummary("E2QWRUHAPOMQZL", true);
                DistributionSummary summary2 = createMockDistributionSummary("E2QWRUHAPOMQZL2", false);

                DistributionList distributionList = DistributionList.builder()
                        .items(summary1, summary2)
                        .isTruncated(false)
                        .build();

                when(cloudFrontClient.listDistributions(any(ListDistributionsRequest.class)))
                        .thenReturn(ListDistributionsResponse.builder()
                                .distributionList(distributionList)
                                .build());

                CloudResource resource1 = createMockCloudResource("E2QWRUHAPOMQZL", "test-distribution");
                CloudResource resource2 = createMockCloudResource("E2QWRUHAPOMQZL2", "other-distribution");

                when(mapper.toCloudResource(any(DistributionSummary.class), any(CDNDistributionQueryRequest.class)))
                        .thenReturn(resource1, resource2);

                when(cloudFrontClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                        .thenReturn(ListTagsForResourceResponse.builder()
                                .tags(Tags.builder().items(Collections.emptyList()).build())
                                .build());

                CDNDistributionQueryRequest query = CDNDistributionQueryRequest.builder()
                        .providerType(CloudProvider.ProviderType.AWS)
                        .accountScope(ACCOUNT_SCOPE)
                        .tenantKey(TENANT_KEY)
                        .distributionName("test")
                        .page(0)
                        .size(20)
                        .build();

                // When
                Page<CloudResource> result = adapter.listDistributions(query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).getResourceName()).contains("test");
            }
        }

        @Test
        @DisplayName("태그 필터링")
        void listDistributions_WithTagFilter_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                // accountCredentialManagementPort와 awsCloudFrontConfig는 @BeforeEach에서 설정됨

                DistributionSummary summary = createMockDistributionSummary(DISTRIBUTION_ID, true);
                DistributionList distributionList = DistributionList.builder()
                        .items(summary)
                        .isTruncated(false)
                        .build();

                when(cloudFrontClient.listDistributions(any(ListDistributionsRequest.class)))
                        .thenReturn(ListDistributionsResponse.builder()
                                .distributionList(distributionList)
                                .build());

                CloudResource resource = createMockCloudResource(DISTRIBUTION_ID, "test-distribution");
                resource.setTags(Map.of("Environment", "Test", "Project", "CDN"));

                when(mapper.toCloudResource(any(DistributionSummary.class), any(CDNDistributionQueryRequest.class)))
                        .thenReturn(resource);

                when(cloudFrontClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                        .thenReturn(ListTagsForResourceResponse.builder()
                                .tags(Tags.builder()
                                        .items(
                                                Tag.builder().key("Environment").value("Test").build(),
                                                Tag.builder().key("Project").value("CDN").build()
                                        )
                                        .build())
                                .build());

                CDNDistributionQueryRequest query = CDNDistributionQueryRequest.builder()
                        .providerType(CloudProvider.ProviderType.AWS)
                        .accountScope(ACCOUNT_SCOPE)
                        .tenantKey(TENANT_KEY)
                        .tags(Map.of("Environment", "Test"))
                        .page(0)
                        .size(20)
                        .build();

                // When
                Page<CloudResource> result = adapter.listDistributions(query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(1);
            }
        }

        @Test
        @DisplayName("페이징 처리")
        void listDistributions_WithPagination_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                // accountCredentialManagementPort와 awsCloudFrontConfig는 @BeforeEach에서 설정됨

                List<DistributionSummary> summaries = new ArrayList<>();
                for (int i = 0; i < 25; i++) {
                    summaries.add(createMockDistributionSummary("E" + i, true));
                }

                DistributionList distributionList = DistributionList.builder()
                        .items(summaries)
                        .isTruncated(false)
                        .build();

                when(cloudFrontClient.listDistributions(any(ListDistributionsRequest.class)))
                        .thenReturn(ListDistributionsResponse.builder()
                                .distributionList(distributionList)
                                .build());

                CloudResource mockResource = createMockCloudResource("E1", "dist-1");
                when(mapper.toCloudResource(any(DistributionSummary.class), any(CDNDistributionQueryRequest.class)))
                        .thenReturn(mockResource);

                when(cloudFrontClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                        .thenReturn(ListTagsForResourceResponse.builder()
                                .tags(Tags.builder().items(Collections.emptyList()).build())
                                .build());

                CDNDistributionQueryRequest query = CDNDistributionQueryRequest.builder()
                        .providerType(CloudProvider.ProviderType.AWS)
                        .accountScope(ACCOUNT_SCOPE)
                        .tenantKey(TENANT_KEY)
                        .page(0)
                        .size(10)
                        .build();

                // When
                Page<CloudResource> result = adapter.listDistributions(query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getSize()).isEqualTo(10);
                assertThat(result.getTotalElements()).isEqualTo(25);
            }
        }

        @Test
        @DisplayName("AccountScope 누락 시 예외 발생")
        void listDistributions_WithoutAccountScope_ThrowsException() {
            // Given
            CDNDistributionQueryRequest query = CDNDistributionQueryRequest.builder()
                    .providerType(CloudProvider.ProviderType.AWS)
                    .accountScope(null)
                    .tenantKey(TENANT_KEY)
                    .build();

            // When & Then
            assertThatThrownBy(() -> adapter.listDistributions(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("AccountScope");
        }
    }

    @Nested
    @DisplayName("Distribution 단건 조회 테스트")
    class GetDistributionTest {

        @Test
        @DisplayName("정상적인 Distribution 조회")
        void getDistribution_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                // accountCredentialManagementPort와 awsCloudFrontConfig는 @BeforeEach에서 설정됨

                Distribution distribution = createMockDistribution(DISTRIBUTION_ID, true);
                GetDistributionResponse getResponse = GetDistributionResponse.builder()
                        .distribution(distribution)
                        .eTag(ETAG)
                        .build();

                when(cloudFrontClient.getDistribution(any(GetDistributionRequest.class)))
                        .thenReturn(getResponse);

                CloudResource mockResource = createMockCloudResource(DISTRIBUTION_ID, "test-distribution");
                when(mapper.toCloudResource(
                        eq(distribution), eq(ETAG), eq(CloudProvider.ProviderType.AWS), anyMap()))
                        .thenReturn(mockResource);

                when(cloudFrontClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                        .thenReturn(ListTagsForResourceResponse.builder()
                                .tags(Tags.builder()
                                        .items(
                                                Tag.builder().key("Environment").value("Test").build()
                                        )
                                        .build())
                                .build());

                // When
                Optional<CloudResource> result = adapter.getDistribution(ACCOUNT_SCOPE, DISTRIBUTION_ID);

                // Then
                assertThat(result).isPresent();
                assertThat(result.get().getResourceId()).isEqualTo(DISTRIBUTION_ID);

                verify(accountCredentialManagementPort).getSession(
                        eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(CloudProvider.ProviderType.AWS));
                verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
                verify(cloudFrontClient).listTagsForResource(any(ListTagsForResourceRequest.class));
            }
        }

        @Test
        @DisplayName("Distribution이 존재하지 않을 때 Optional.empty 반환")
        void getDistribution_NotFound_ReturnsEmpty() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                // accountCredentialManagementPort와 awsCloudFrontConfig는 @BeforeEach에서 설정됨

                when(cloudFrontClient.getDistribution(any(GetDistributionRequest.class)))
                        .thenThrow(NoSuchDistributionException.builder()
                                .message("Distribution not found")
                                .build());

                // When
                Optional<CloudResource> result = adapter.getDistribution(ACCOUNT_SCOPE, DISTRIBUTION_ID);

                // Then
                assertThat(result).isEmpty();

                verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
                verify(cloudFrontClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
            }
        }
    }

    @Nested
    @DisplayName("Distribution 존재 여부 확인 테스트")
    class DistributionExistsTest {

        @Test
        @DisplayName("Distribution이 존재할 때 true 반환")
        void distributionExists_Exists_ReturnsTrue() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                // accountCredentialManagementPort와 awsCloudFrontConfig는 @BeforeEach에서 설정됨

                Distribution distribution = createMockDistribution(DISTRIBUTION_ID, true);
                GetDistributionResponse getResponse = GetDistributionResponse.builder()
                        .distribution(distribution)
                        .eTag(ETAG)
                        .build();

                when(cloudFrontClient.getDistribution(any(GetDistributionRequest.class)))
                        .thenReturn(getResponse);

                // When
                boolean exists = adapter.distributionExists(ACCOUNT_SCOPE, DISTRIBUTION_ID);

                // Then
                assertThat(exists).isTrue();

                verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
            }
        }

        @Test
        @DisplayName("Distribution이 존재하지 않을 때 false 반환")
        void distributionExists_NotExists_ReturnsFalse() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                // accountCredentialManagementPort와 awsCloudFrontConfig는 @BeforeEach에서 설정됨

                when(cloudFrontClient.getDistribution(any(GetDistributionRequest.class)))
                        .thenThrow(NoSuchDistributionException.builder()
                                .message("Distribution not found")
                                .build());

                // When
                boolean exists = adapter.distributionExists(ACCOUNT_SCOPE, DISTRIBUTION_ID);

                // Then
                assertThat(exists).isFalse();

                verify(cloudFrontClient).getDistribution(any(GetDistributionRequest.class));
            }
        }
    }

    @Nested
    @DisplayName("ProviderScoped 테스트")
    class ProviderScopedTest {

        @Test
        @DisplayName("getProviderType은 AWS를 반환")
        void getProviderType_ReturnsAWS() {
            // When
            CloudProvider.ProviderType result = adapter.getProviderType();

            // Then
            assertThat(result).isEqualTo(CloudProvider.ProviderType.AWS);
        }
    }

    /**
     * Mock DistributionSummary 객체 생성 헬퍼 메서드
     */
    private DistributionSummary createMockDistributionSummary(String distributionId, boolean enabled) {
        return DistributionSummary.builder()
                .id(distributionId)
                .arn("arn:aws:cloudfront::123456789012:distribution/" + distributionId)
                .status(enabled ? "Deployed" : "InProgress")
                .domainName("d1234567890.cloudfront.net")
                .enabled(enabled)
                .comment("Test distribution")
                .lastModifiedTime(Instant.now())
                .build();
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
                .lastModifiedTime(Instant.now())
                .build();
    }

    /**
     * Mock CloudResource 객체 생성 헬퍼 메서드
     */
    private CloudResource createMockCloudResource(String resourceId, String resourceName) {
        return CloudResource.builder()
                .resourceId(resourceId)
                .resourceName(resourceName)
                .displayName(resourceName)
                .resourceType(CloudResource.ResourceType.CDN_DISTRIBUTION)
                .lifecycleState(CloudResource.LifecycleState.RUNNING)
                .createdInCloud(LocalDateTime.now())
                .build();
    }
}

