package com.agenticcp.core.domain.cloud.service.cdn;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.CDNDistributionQueryRequest;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.model.cdn.*;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNInvalidationPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CDNUseCaseService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CDNUseCaseService 단위 테스트")
class CDNUseCaseServiceTest {

    @Mock
    private CDNPortRouter cdnPortRouter;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    @Mock
    private CDNManagementPort managementPort;

    @Mock
    private CDNDiscoveryPort discoveryPort;

    @Mock
    private CDNInvalidationPort invalidationPort;

    @Mock
    private CloudResourceRepository cloudResourceRepository;

    @InjectMocks
    private CDNUseCaseService cdnUseCaseService;

    private static final String TENANT_KEY = "test-tenant";
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String DISTRIBUTION_ID = "E2QWRUHAPOMQZL";
    private static final String DISTRIBUTION_NAME = "test-distribution";
    private static final String ETAG = "ETAG123456789";
    private static final String INVALIDATION_ID = "I2J3K4L5M6N7O";

    private AwsSessionCredential mockSession;
    private CloudResource mockDistribution;
    private OriginConfig originConfig;
    private CacheBehaviorConfig cacheBehaviorConfig;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey(TENANT_KEY);

        mockSession = AwsSessionCredential.builder()
                .accessKeyId("AKIA_TEST")
                .secretAccessKey("secret")
                .sessionToken("token")
                .region("us-east-1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        mockDistribution = CloudResource.builder()
                .resourceId(DISTRIBUTION_ID)
                .resourceName(DISTRIBUTION_NAME)
                .displayName(DISTRIBUTION_NAME)
                .resourceType(CloudResource.ResourceType.CDN_DISTRIBUTION)
                .lifecycleState(CloudResource.LifecycleState.RUNNING)
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

        // 기본 Mock 설정
        lenient().doNothing().when(capabilityGuard).ensureSupported(
                any(ProviderType.class),
                anyString(),
                anyString(),
                any(CapabilityGuard.Operation.class)
        );

        lenient().when(cdnPortRouter.management(ProviderType.AWS)).thenReturn(managementPort);
        lenient().when(cdnPortRouter.discovery(ProviderType.AWS)).thenReturn(discoveryPort);
        lenient().when(cdnPortRouter.invalidation(ProviderType.AWS)).thenReturn(invalidationPort);
        
        // 공통 Mock 설정: 모든 테스트에서 사용되는 기본 설정
        lenient().when(accountCredentialManagementPort.getSession(
                eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(ProviderType.AWS)))
                .thenReturn(mockSession);
        lenient().when(cloudResourceRepository.save(any(CloudResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("Distribution 생성 테스트")
    class CreateDistributionTest {

        @Test
        @DisplayName("정상적인 Distribution 생성")
        void createDistribution_Success() {
            // Given
            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .serviceKey("CloudFront")
                    .resourceType("CDN_DISTRIBUTION")
                    .distributionName(DISTRIBUTION_NAME)
                    .comment("Test distribution")
                    .enabled(true)
                    .origin(originConfig)
                    .cacheBehaviors(List.of(cacheBehaviorConfig))
                    .tags(Map.of("Environment", "Test"))
                    .build();

            // accountCredentialManagementPort는 @BeforeEach에서 설정됨
            CloudResource distributionWithMetadata = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_NAME)
                    .displayName(DISTRIBUTION_NAME)
                    .resourceType(CloudResource.ResourceType.CDN_DISTRIBUTION)
                    .lifecycleState(CloudResource.LifecycleState.RUNNING)
                    .metadata("{\"origin\":{\"id\":\"origin-1\"}}")
                    .build();
            
            when(managementPort.createDistribution(any(CreateDistributionCommand.class)))
                    .thenReturn(distributionWithMetadata);
            when(resourceHelper.registerResource(any(ProviderType.class), anyString(), any(ResourceRegistrationRequest.class)))
                    .thenReturn(mockDistribution);
            when(cloudResourceRepository.save(any(CloudResource.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CloudResource result = cdnUseCaseService.createDistribution(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(DISTRIBUTION_ID);
            assertThat(result.getResourceName()).isEqualTo(DISTRIBUTION_NAME);

            verify(capabilityGuard).ensureSupported(
                    eq(ProviderType.AWS), eq("CloudFront"), eq("CDN_DISTRIBUTION"),
                    eq(CapabilityGuard.Operation.TAGGING));
            verify(accountCredentialManagementPort).getSession(
                    eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(ProviderType.AWS));
            verify(managementPort).createDistribution(any(CreateDistributionCommand.class));
            verify(resourceHelper).registerResource(
                    eq(ProviderType.AWS), eq("CloudFront"), any(ResourceRegistrationRequest.class));
            // metadata 저장 검증
            verify(cloudResourceRepository).save(argThat(resource -> 
                    resource.getMetadata() != null && resource.getMetadata().equals("{\"origin\":{\"id\":\"origin-1\"}}")
            ));
        }

        @Test
        @DisplayName("DB 저장 실패 시 보상 트랜잭션 실행")
        void createDistribution_DbSaveFailure_ExecutesCompensation() {
            // Given
            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .serviceKey("CloudFront")
                    .resourceType("CDN_DISTRIBUTION")
                    .distributionName(DISTRIBUTION_NAME)
                    .origin(originConfig)
                    .build();

            // accountCredentialManagementPort는 @BeforeEach에서 설정됨
            when(managementPort.createDistribution(any(CreateDistributionCommand.class)))
                    .thenReturn(mockDistribution);
            when(resourceHelper.registerResource(any(), any(), any()))
                    .thenThrow(new RuntimeException("DB 저장 실패"));

            // Distribution 조회를 위한 Mock (보상 트랜잭션에서 사용)
            when(discoveryPort.getDistribution(eq(ACCOUNT_SCOPE), eq(DISTRIBUTION_ID)))
                    .thenReturn(Optional.of(mockDistribution));
            doNothing().when(managementPort).deleteDistribution(any(DeleteDistributionCommand.class));

            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.createDistribution(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.RESOURCE_CREATION_FAILED);

            verify(managementPort).createDistribution(any(CreateDistributionCommand.class));
            verify(resourceHelper).registerResource(any(), any(), any());
            verify(discoveryPort).getDistribution(eq(ACCOUNT_SCOPE), eq(DISTRIBUTION_ID));
            verify(managementPort).deleteDistribution(any(DeleteDistributionCommand.class));
        }

        @Test
        @DisplayName("accountScope가 null일 때 예외 발생")
        void createDistribution_NullAccountScope_ThrowsException() {
            // Given
            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(null)
                    .serviceKey("CloudFront")
                    .resourceType("CDN_DISTRIBUTION")
                    .distributionName(DISTRIBUTION_NAME)
                    .origin(originConfig)
                    .build();

            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.createDistribution(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);

            verify(accountCredentialManagementPort, never()).getSession(any(), any(), any());
        }

        @Test
        @DisplayName("자격증명을 찾을 수 없을 때 ACCOUNT_NOT_CONFIGURED 예외 발생")
        void createDistribution_CredentialNotFound_ThrowsException() {
            // Given
            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .serviceKey("CloudFront")
                    .resourceType("CDN_DISTRIBUTION")
                    .distributionName(DISTRIBUTION_NAME)
                    .origin(originConfig)
                    .build();

            when(accountCredentialManagementPort.getSession(
                    eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(ProviderType.AWS)))
                    .thenThrow(new BusinessException(
                            CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                            "자격증명을 찾을 수 없습니다"
                    ));

            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.createDistribution(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.ACCOUNT_NOT_CONFIGURED);
        }
    }

    @Nested
    @DisplayName("Distribution 조회 테스트")
    class GetDistributionTest {

        @Test
        @DisplayName("정상적인 Distribution 조회")
        void getDistribution_Success() {
            // Given
            when(discoveryPort.getDistribution(ACCOUNT_SCOPE, DISTRIBUTION_ID))
                    .thenReturn(Optional.of(mockDistribution));

            // When
            Optional<CloudResource> result = cdnUseCaseService.getDistribution(
                    ACCOUNT_SCOPE, DISTRIBUTION_ID, ProviderType.AWS);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getResourceId()).isEqualTo(DISTRIBUTION_ID);

            verify(discoveryPort).getDistribution(ACCOUNT_SCOPE, DISTRIBUTION_ID);
        }

        @Test
        @DisplayName("Distribution이 존재하지 않을 때 Optional.empty 반환")
        void getDistribution_NotFound_ReturnsEmpty() {
            // Given
            when(discoveryPort.getDistribution(ACCOUNT_SCOPE, DISTRIBUTION_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<CloudResource> result = cdnUseCaseService.getDistribution(
                    ACCOUNT_SCOPE, DISTRIBUTION_ID, ProviderType.AWS);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("accountScope가 null일 때 예외 발생")
        void getDistribution_NullAccountScope_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.getDistribution(
                    null, DISTRIBUTION_ID, ProviderType.AWS))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);
        }
    }

    @Nested
    @DisplayName("Distribution 목록 조회 테스트")
    class ListDistributionsTest {

        @Test
        @DisplayName("정상적인 Distribution 목록 조회")
        void listDistributions_Success() {
            // Given
            CDNDistributionQueryRequest query = CDNDistributionQueryRequest.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .tenantKey(TENANT_KEY)
                    .page(0)
                    .size(20)
                    .build();

            Page<CloudResource> expectedPage = new PageImpl<>(
                    List.of(mockDistribution),
                    PageRequest.of(0, 20),
                    1
            );

            // accountCredentialManagementPort는 @BeforeEach에서 설정됨
            when(discoveryPort.listDistributions(any(CDNDistributionQueryRequest.class)))
                    .thenReturn(expectedPage);

            // When
            Page<CloudResource> result = cdnUseCaseService.listDistributions(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);

            verify(accountCredentialManagementPort).getSession(
                    eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(ProviderType.AWS));
            verify(discoveryPort).listDistributions(any(CDNDistributionQueryRequest.class));
        }

        @Test
        @DisplayName("accountScope가 null일 때 예외 발생")
        void listDistributions_NullAccountScope_ThrowsException() {
            // Given
            CDNDistributionQueryRequest query = CDNDistributionQueryRequest.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(null)
                    .build();

            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.listDistributions(query))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);
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
                    .tags(Map.of("Environment", "Production"))
                    .build();

            CloudResource updatedDistribution = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_NAME)
                    .lifecycleState(CloudResource.LifecycleState.PENDING)
                    .build();

            // accountCredentialManagementPort는 @BeforeEach에서 설정됨
            when(managementPort.updateDistribution(any(UpdateDistributionCommand.class)))
                    .thenReturn(updatedDistribution);

            // When
            CloudResource result = cdnUseCaseService.updateDistribution(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(DISTRIBUTION_ID);

            verify(capabilityGuard).ensureSupported(
                    eq(ProviderType.AWS), eq("CloudFront"), eq("CDN_DISTRIBUTION"),
                    eq(CapabilityGuard.Operation.TAGGING));
            verify(accountCredentialManagementPort).getSession(
                    eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(ProviderType.AWS));
            verify(managementPort).updateDistribution(any(UpdateDistributionCommand.class));
        }

        @Test
        @DisplayName("accountScope가 null일 때 예외 발생")
        void updateDistribution_NullAccountScope_ThrowsException() {
            // Given
            UpdateDistributionCommand command = UpdateDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(null)
                    .distributionId(DISTRIBUTION_ID)
                    .etag(ETAG)
                    .build();

            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.updateDistribution(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);
        }
    }

    @Nested
    @DisplayName("Distribution 삭제 테스트")
    class DeleteDistributionTest {

        @Test
        @DisplayName("정상적인 Distribution 삭제")
        void deleteDistribution_Success() {
            // Given
            DeleteDistributionCommand command = DeleteDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag(ETAG)
                    .build();

            // accountCredentialManagementPort는 @BeforeEach에서 설정됨
            doNothing().when(managementPort).deleteDistribution(any(DeleteDistributionCommand.class));
            doNothing().when(resourceHelper).softDeleteResource(eq(DISTRIBUTION_ID));

            // When
            cdnUseCaseService.deleteDistribution(command);

            // Then
            verify(capabilityGuard).ensureSupported(
                    eq(ProviderType.AWS), eq("CloudFront"), eq("CDN_DISTRIBUTION"),
                    eq(CapabilityGuard.Operation.TERMINATE));
            verify(accountCredentialManagementPort).getSession(
                    eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(ProviderType.AWS));
            verify(managementPort).deleteDistribution(any(DeleteDistributionCommand.class));
            verify(resourceHelper).softDeleteResource(DISTRIBUTION_ID);
        }

        @Test
        @DisplayName("accountScope가 null일 때 예외 발생")
        void deleteDistribution_NullAccountScope_ThrowsException() {
            // Given
            DeleteDistributionCommand command = DeleteDistributionCommand.builder()
                    .providerType(ProviderType.AWS)
                    .accountScope(null)
                    .distributionId(DISTRIBUTION_ID)
                    .etag(ETAG)
                    .build();

            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.deleteDistribution(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);
        }
    }

    @Nested
    @DisplayName("캐시 무효화 테스트")
    class InvalidationTest {

        @Test
        @DisplayName("정상적인 캐시 무효화 생성")
        void createInvalidation_Success() {
            // Given
            CreateInvalidationCommand command = CreateInvalidationCommand.builder()
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .paths(List.of("/*", "/images/*"))
                    .callerReference("test-ref")
                    .build();

            InvalidationResult expectedResult = InvalidationResult.builder()
                    .invalidationId(INVALIDATION_ID)
                    .distributionId(DISTRIBUTION_ID)
                    .status("InProgress")
                    .createTime(LocalDateTime.now())
                    .paths(List.of("/*", "/images/*"))
                    .build();

            // accountCredentialManagementPort는 @BeforeEach에서 설정됨
            when(invalidationPort.createInvalidation(any(CreateInvalidationCommand.class)))
                    .thenReturn(expectedResult);

            // When
            InvalidationResult result = cdnUseCaseService.createInvalidation(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.invalidationId()).isEqualTo(INVALIDATION_ID);
            assertThat(result.distributionId()).isEqualTo(DISTRIBUTION_ID);
            assertThat(result.status()).isEqualTo("InProgress");

            verify(accountCredentialManagementPort).getSession(
                    eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(ProviderType.AWS));
            verify(invalidationPort).createInvalidation(any(CreateInvalidationCommand.class));
        }

        @Test
        @DisplayName("정상적인 캐시 무효화 상태 조회")
        void getInvalidation_Success() {
            // Given
            InvalidationResult expectedResult = InvalidationResult.builder()
                    .invalidationId(INVALIDATION_ID)
                    .distributionId(DISTRIBUTION_ID)
                    .status("Completed")
                    .createTime(LocalDateTime.now())
                    .paths(List.of("/*"))
                    .build();

            when(invalidationPort.getInvalidation(ACCOUNT_SCOPE, DISTRIBUTION_ID, INVALIDATION_ID))
                    .thenReturn(Optional.of(expectedResult));

            // When
            Optional<InvalidationResult> result = cdnUseCaseService.getInvalidation(
                    ACCOUNT_SCOPE, DISTRIBUTION_ID, INVALIDATION_ID, ProviderType.AWS);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().invalidationId()).isEqualTo(INVALIDATION_ID);
            assertThat(result.get().status()).isEqualTo("Completed");

            verify(invalidationPort).getInvalidation(ACCOUNT_SCOPE, DISTRIBUTION_ID, INVALIDATION_ID);
        }

        @Test
        @DisplayName("무효화가 존재하지 않을 때 Optional.empty 반환")
        void getInvalidation_NotFound_ReturnsEmpty() {
            // Given
            when(invalidationPort.getInvalidation(ACCOUNT_SCOPE, DISTRIBUTION_ID, INVALIDATION_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<InvalidationResult> result = cdnUseCaseService.getInvalidation(
                    ACCOUNT_SCOPE, DISTRIBUTION_ID, INVALIDATION_ID, ProviderType.AWS);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("accountScope가 null일 때 예외 발생")
        void createInvalidation_NullAccountScope_ThrowsException() {
            // Given
            CreateInvalidationCommand command = CreateInvalidationCommand.builder()
                    .accountScope(null)
                    .distributionId(DISTRIBUTION_ID)
                    .paths(List.of("/*"))
                    .build();

            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.createInvalidation(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);
        }
    }
}

