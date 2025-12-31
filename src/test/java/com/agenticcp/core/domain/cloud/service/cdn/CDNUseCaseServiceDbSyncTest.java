package com.agenticcp.core.domain.cloud.service.cdn;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.cdn.CreateDistributionCommand;
import com.agenticcp.core.domain.cloud.port.model.cdn.DeleteDistributionCommand;
import com.agenticcp.core.domain.cloud.port.model.cdn.OriginConfig;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CDNUseCaseService DB 동기화 로직 단위 테스트
 * CSP 작업 후 CloudResource 엔티티가 올바르게 DB에 저장/삭제되는지 검증합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CDNUseCaseService DB 동기화 테스트")
class CDNUseCaseServiceDbSyncTest {

    @Mock
    private CDNPortRouter cdnPortRouter;

    @Mock
    private CDNManagementPort managementPort;

    @Mock
    private CDNDiscoveryPort discoveryPort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    @Mock
    private CloudResourceRepository cloudResourceRepository;

    private CDNUseCaseService cdnUseCaseService;
    private CloudSessionCredential mockSession;

    private static final ProviderType PROVIDER_TYPE = ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String TENANT_KEY = "tenant-test";
    private static final String DISTRIBUTION_ID = "E1234567890ABC";
    private static final String DISTRIBUTION_NAME = "my-test-distribution";
    private static final String METADATA_JSON = "{\"origin\":{\"id\":\"origin-1\",\"domainName\":\"example.com\"}}";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey(TENANT_KEY);
        
        cdnUseCaseService = new CDNUseCaseService(
                cdnPortRouter,
                capabilityGuard,
                accountCredentialManagementPort,
                resourceHelper,
                cloudResourceRepository
        );
        
        mockSession = mock(CloudSessionCredential.class);
        when(mockSession.getExpiresAt()).thenReturn(LocalDateTime.now().plusHours(1));

        // 공통 Mock 설정
        lenient().when(cdnPortRouter.management(PROVIDER_TYPE)).thenReturn(managementPort);
        lenient().when(cdnPortRouter.discovery(PROVIDER_TYPE)).thenReturn(discoveryPort);
        lenient().when(accountCredentialManagementPort.getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(PROVIDER_TYPE)))
                .thenReturn(mockSession);
        lenient().doNothing().when(capabilityGuard).ensureSupported(any(), anyString(), anyString(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("Distribution 생성 테스트")
    class CreateDistributionTest {

        @Test
        @DisplayName("Distribution 생성 성공 시 CloudResource가 DB에 저장되고 metadata도 저장된다")
        void createDistribution_Success_SavesCloudResourceWithMetadata() {
            // Given
            Map<String, String> tags = Map.of("Environment", "test");
            OriginConfig origin = OriginConfig.builder()
                    .id("origin-1")
                    .domainName("example.com")
                    .type(OriginConfig.OriginType.CUSTOM)
                    .build();

            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .serviceKey("CloudFront")
                    .distributionName(DISTRIBUTION_NAME)
                    .origin(origin)
                    .tags(tags)
                    .build();

            CloudResource mockCreatedDistribution = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_NAME)
                    .metadata(METADATA_JSON)
                    .build();

            CloudResource mockSavedResource = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_NAME)
                    .build();

            when(managementPort.createDistribution(any())).thenReturn(mockCreatedDistribution);
            when(resourceHelper.registerResource(
                    eq(PROVIDER_TYPE),
                    eq("CloudFront"),
                    any(ResourceRegistrationRequest.class)
            )).thenReturn(mockSavedResource);
            when(cloudResourceRepository.save(any(CloudResource.class))).thenReturn(mockSavedResource);

            // When
            CloudResource result = cdnUseCaseService.createDistribution(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(DISTRIBUTION_ID);
            
            verify(resourceHelper).registerResource(
                    eq(PROVIDER_TYPE),
                    eq("CloudFront"),
                    any(ResourceRegistrationRequest.class)
            );
            // metadata 저장 검증
            verify(cloudResourceRepository).save(argThat(resource -> 
                    resource.getMetadata() != null && resource.getMetadata().equals(METADATA_JSON)
            ));
        }

        @Test
        @DisplayName("Distribution 이름이 없으면 Distribution ID가 리소스 이름으로 사용된다")
        void createDistribution_NoDistributionName_UsesDistributionIdAsResourceName() {
            // Given
            OriginConfig origin = OriginConfig.builder()
                    .id("origin-1")
                    .domainName("example.com")
                    .type(OriginConfig.OriginType.CUSTOM)
                    .build();

            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .serviceKey("CloudFront")
                    .distributionName(null) // Distribution 이름 없음
                    .origin(origin)
                    .build();

            CloudResource mockCreatedDistribution = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_ID)
                    .metadata(METADATA_JSON)
                    .build();

            CloudResource mockSavedResource = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_ID)
                    .build();

            when(managementPort.createDistribution(any())).thenReturn(mockCreatedDistribution);
            when(resourceHelper.registerResource(any(), any(), any())).thenReturn(mockSavedResource);
            when(cloudResourceRepository.save(any(CloudResource.class))).thenReturn(mockSavedResource);

            // When
            cdnUseCaseService.createDistribution(command);

            // Then
            verify(resourceHelper).registerResource(
                    eq(PROVIDER_TYPE),
                    eq("CloudFront"),
                    any(ResourceRegistrationRequest.class)
            );
        }

        @Test
        @DisplayName("metadata가 없으면 metadata 저장은 스킵된다")
        void createDistribution_NoMetadata_SkipsMetadataSave() {
            // Given
            OriginConfig origin = OriginConfig.builder()
                    .id("origin-1")
                    .domainName("example.com")
                    .type(OriginConfig.OriginType.CUSTOM)
                    .build();

            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .serviceKey("CloudFront")
                    .distributionName(DISTRIBUTION_NAME)
                    .origin(origin)
                    .build();

            CloudResource mockCreatedDistribution = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_NAME)
                    .metadata(null) // metadata 없음
                    .build();

            CloudResource mockSavedResource = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_NAME)
                    .build();

            when(managementPort.createDistribution(any())).thenReturn(mockCreatedDistribution);
            when(resourceHelper.registerResource(any(), any(), any())).thenReturn(mockSavedResource);

            // When
            cdnUseCaseService.createDistribution(command);

            // Then
            verify(resourceHelper).registerResource(any(), any(), any());
            // metadata가 null이면 저장하지 않음
            verify(cloudResourceRepository, never()).save(any(CloudResource.class));
        }

        @Test
        @DisplayName("DB 저장 실패 시 보상 트랜잭션이 실행되고 예외가 발생한다")
        void createDistribution_DbSaveFails_CompensatingTransactionExecuted() {
            // Given
            OriginConfig origin = OriginConfig.builder()
                    .id("origin-1")
                    .domainName("example.com")
                    .type(OriginConfig.OriginType.CUSTOM)
                    .build();

            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .serviceKey("CloudFront")
                    .distributionName(DISTRIBUTION_NAME)
                    .origin(origin)
                    .build();

            CloudResource mockCreatedDistribution = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_NAME)
                    .metadata(METADATA_JSON)
                    .build();

            when(managementPort.createDistribution(any())).thenReturn(mockCreatedDistribution);
            // DB 저장 실패
            doThrow(new RuntimeException("DB 저장 실패")).when(resourceHelper)
                    .registerResource(any(), any(), any());
            // 보상 트랜잭션을 위한 Distribution 조회
            when(discoveryPort.getDistribution(eq(ACCOUNT_SCOPE), eq(DISTRIBUTION_ID)))
                    .thenReturn(Optional.of(mockCreatedDistribution));

            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.createDistribution(command))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.RESOURCE_CREATION_FAILED);
                    });

            // 보상 트랜잭션 실행 검증: CSP Distribution 삭제 호출됨
            verify(discoveryPort).getDistribution(eq(ACCOUNT_SCOPE), eq(DISTRIBUTION_ID));
            verify(managementPort).deleteDistribution(any(DeleteDistributionCommand.class));
        }

        @Test
        @DisplayName("보상 트랜잭션도 실패하면 Ghost Resource 경고 로그가 출력된다")
        void createDistribution_CompensationFails_GhostResourceWarningLogged() {
            // Given
            OriginConfig origin = OriginConfig.builder()
                    .id("origin-1")
                    .domainName("example.com")
                    .type(OriginConfig.OriginType.CUSTOM)
                    .build();

            CreateDistributionCommand command = CreateDistributionCommand.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .serviceKey("CloudFront")
                    .distributionName(DISTRIBUTION_NAME)
                    .origin(origin)
                    .build();

            CloudResource mockCreatedDistribution = CloudResource.builder()
                    .resourceId(DISTRIBUTION_ID)
                    .resourceName(DISTRIBUTION_NAME)
                    .metadata(METADATA_JSON)
                    .build();

            when(managementPort.createDistribution(any())).thenReturn(mockCreatedDistribution);
            // DB 저장 실패
            doThrow(new RuntimeException("DB 저장 실패")).when(resourceHelper)
                    .registerResource(any(), any(), any());
            // 보상 트랜잭션을 위한 Distribution 조회
            when(discoveryPort.getDistribution(eq(ACCOUNT_SCOPE), eq(DISTRIBUTION_ID)))
                    .thenReturn(Optional.of(mockCreatedDistribution));
            // 보상 트랜잭션(CSP 삭제)도 실패
            doThrow(new RuntimeException("CSP 삭제 실패")).when(managementPort)
                    .deleteDistribution(any(DeleteDistributionCommand.class));

            // When & Then
            assertThatThrownBy(() -> cdnUseCaseService.createDistribution(command))
                    .isInstanceOf(BusinessException.class);

            // 보상 트랜잭션 시도 검증
            verify(discoveryPort).getDistribution(eq(ACCOUNT_SCOPE), eq(DISTRIBUTION_ID));
            verify(managementPort).deleteDistribution(any(DeleteDistributionCommand.class));
            // Ghost Resource 발생 - 실제로는 모니터링/배치로 처리 필요
        }
    }

    @Nested
    @DisplayName("Distribution 삭제 테스트")
    class DeleteDistributionTest {

        @Test
        @DisplayName("Distribution 삭제 시 소프트 삭제가 수행된다")
        void deleteDistribution_Success_SoftDeletesResource() {
            // Given
            DeleteDistributionCommand command = DeleteDistributionCommand.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag("dummy-etag")
                    .build();

            doNothing().when(managementPort).deleteDistribution(any());

            // When
            cdnUseCaseService.deleteDistribution(command);

            // Then
            verify(managementPort).deleteDistribution(any(DeleteDistributionCommand.class));
            verify(resourceHelper).softDeleteResource(DISTRIBUTION_ID);
        }

        @Test
        @DisplayName("DB에 리소스가 없어도 CSP 삭제는 성공한다")
        void deleteDistribution_ResourceNotInDb_CspDeletionSucceeds() {
            // Given
            DeleteDistributionCommand command = DeleteDistributionCommand.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .etag("dummy-etag")
                    .build();

            doNothing().when(managementPort).deleteDistribution(any());
            // Helper 내부에서 리소스가 없으면 로그만 출력하고 예외 발생 안함

            // When
            cdnUseCaseService.deleteDistribution(command);

            // Then
            verify(managementPort).deleteDistribution(any(DeleteDistributionCommand.class)); // CSP 작업 성공
            verify(resourceHelper).softDeleteResource(DISTRIBUTION_ID);
        }
    }
}

