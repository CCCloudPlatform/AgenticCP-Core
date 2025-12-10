package com.agenticcp.core.domain.cloud.service.vpc;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudResource.LifecycleState;
import com.agenticcp.core.domain.cloud.entity.CloudResource.ResourceType;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.vpc.VpcManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.cloud.repository.CloudServiceRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * VpcUseCaseService DB 동기화 로직 단위 테스트
 * CSP 작업 후 CloudResource 엔티티가 올바르게 DB에 저장/삭제되는지 검증합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VpcUseCaseService DB 동기화 테스트")
class VpcUseCaseServiceDbSyncTest {

    @Mock
    private VpcPortRouter vpcPortRouter;

    @Mock
    private VpcManagementPort vpcManagementPort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private CloudResourceRepository cloudResourceRepository;

    @Mock
    private CloudProviderRepository cloudProviderRepository;

    @Mock
    private CloudServiceRepository cloudServiceRepository;

    @Mock
    private TenantRepository tenantRepository;

    private ObjectMapper objectMapper;

    private VpcUseCaseService vpcUseCaseService;
    private CloudSessionCredential mockSession;

    private static final ProviderType PROVIDER_TYPE = ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String TENANT_KEY = "tenant-test";
    private static final String VPC_ID = "vpc-12345678";
    private static final String VPC_NAME = "my-test-vpc";
    private static final String CIDR_BLOCK = "10.0.0.0/16";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey(TENANT_KEY);
        objectMapper = new ObjectMapper();
        
        vpcUseCaseService = new VpcUseCaseService(
                vpcPortRouter,
                capabilityGuard,
                accountCredentialManagementPort,
                cloudResourceRepository,
                cloudProviderRepository,
                cloudServiceRepository,
                tenantRepository,
                objectMapper
        );
        
        mockSession = mock(CloudSessionCredential.class);
        when(mockSession.getExpiresAt()).thenReturn(LocalDateTime.now().plusHours(1));

        // 공통 Mock 설정
        lenient().when(vpcPortRouter.getPort(PROVIDER_TYPE)).thenReturn(vpcManagementPort);
        lenient().when(accountCredentialManagementPort.getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(PROVIDER_TYPE)))
                .thenReturn(mockSession);
        lenient().doNothing().when(capabilityGuard).ensureSupported(any(), anyString(), anyString(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("VPC 생성 테스트")
    class CreateVpcTest {

        @Test
        @DisplayName("VPC 생성 성공 시 CloudResource가 DB에 저장된다")
        void createVpc_Success_SavesCloudResource() {
            // Given
            VpcCreateRequest request = VpcCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .vpcName(VPC_NAME)
                    .cidrBlock(CIDR_BLOCK)
                    .region("us-east-1")
                    .tags(Map.of("Environment", "test"))
                    .build();

            CloudResource mockCreatedVpc = CloudResource.builder()
                    .resourceId(VPC_ID)
                    .resourceName(VPC_NAME)
                    .build();

            CloudProvider mockProvider = CloudProvider.builder()
                    .providerType(PROVIDER_TYPE)
                    .providerName("AWS")
                    .build();

            CloudService mockService = CloudService.builder()
                    .serviceKey("VPC")
                    .build();

            Tenant mockTenant = Tenant.builder()
                    .tenantKey(TENANT_KEY)
                    .build();

            when(vpcManagementPort.createVpc(any())).thenReturn(mockCreatedVpc);
            when(cloudProviderRepository.findFirstByProviderType(PROVIDER_TYPE))
                    .thenReturn(Optional.of(mockProvider));
            when(cloudServiceRepository.findByProviderTypeAndServiceKey(eq(PROVIDER_TYPE), eq("VPC")))
                    .thenReturn(Optional.of(mockService));
            when(tenantRepository.findByTenantKey(TENANT_KEY))
                    .thenReturn(Optional.of(mockTenant));
            when(cloudResourceRepository.save(any(CloudResource.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CloudResource result = vpcUseCaseService.createVpc(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(VPC_ID);
            
            ArgumentCaptor<CloudResource> captor = ArgumentCaptor.forClass(CloudResource.class);
            verify(cloudResourceRepository).save(captor.capture());
            
            CloudResource savedResource = captor.getValue();
            assertThat(savedResource.getResourceId()).isEqualTo(VPC_ID);
            assertThat(savedResource.getResourceName()).isEqualTo(VPC_NAME);
            assertThat(savedResource.getResourceType()).isEqualTo(ResourceType.NETWORK);
            assertThat(savedResource.getLifecycleState()).isEqualTo(LifecycleState.RUNNING);
            assertThat(savedResource.getConfiguration()).isEqualTo(CIDR_BLOCK);
            assertThat(savedResource.getProvider()).isEqualTo(mockProvider);
            assertThat(savedResource.getTenant()).isEqualTo(mockTenant);
        }

        @Test
        @DisplayName("DB 저장 실패 시에도 CSP 생성은 성공한다")
        void createVpc_DbSaveFails_CspCreationSucceeds() {
            // Given
            VpcCreateRequest request = VpcCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .vpcName(VPC_NAME)
                    .cidrBlock(CIDR_BLOCK)
                    .region("us-east-1")
                    .build();

            CloudResource mockCreatedVpc = CloudResource.builder()
                    .resourceId(VPC_ID)
                    .resourceName(VPC_NAME)
                    .build();

            when(vpcManagementPort.createVpc(any())).thenReturn(mockCreatedVpc);
            when(cloudProviderRepository.findFirstByProviderType(PROVIDER_TYPE))
                    .thenReturn(Optional.empty()); // Provider 없음 -> DB 저장 실패

            // When
            CloudResource result = vpcUseCaseService.createVpc(request);

            // Then
            assertThat(result).isNotNull(); // CSP 생성은 성공
            assertThat(result.getResourceId()).isEqualTo(VPC_ID);
            verify(cloudResourceRepository, never()).save(any()); // DB 저장은 스킵됨
        }

        @Test
        @DisplayName("VPC 이름이 없으면 VPC ID가 리소스 이름으로 사용된다")
        void createVpc_NoVpcName_UsesVpcIdAsResourceName() {
            // Given
            VpcCreateRequest request = VpcCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .vpcName(null) // VPC 이름 없음
                    .cidrBlock(CIDR_BLOCK)
                    .region("us-east-1")
                    .build();

            CloudResource mockCreatedVpc = CloudResource.builder()
                    .resourceId(VPC_ID)
                    .resourceName(VPC_ID)
                    .build();

            CloudProvider mockProvider = CloudProvider.builder()
                    .providerType(PROVIDER_TYPE)
                    .build();

            Tenant mockTenant = Tenant.builder()
                    .tenantKey(TENANT_KEY)
                    .build();

            when(vpcManagementPort.createVpc(any())).thenReturn(mockCreatedVpc);
            when(cloudProviderRepository.findFirstByProviderType(PROVIDER_TYPE))
                    .thenReturn(Optional.of(mockProvider));
            when(cloudServiceRepository.findByProviderTypeAndServiceKey(any(), any()))
                    .thenReturn(Optional.empty());
            when(tenantRepository.findByTenantKey(TENANT_KEY))
                    .thenReturn(Optional.of(mockTenant));
            when(cloudResourceRepository.save(any(CloudResource.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            vpcUseCaseService.createVpc(request);

            // Then
            ArgumentCaptor<CloudResource> captor = ArgumentCaptor.forClass(CloudResource.class);
            verify(cloudResourceRepository).save(captor.capture());
            
            CloudResource savedResource = captor.getValue();
            assertThat(savedResource.getResourceName()).isEqualTo(VPC_ID); // VPC ID가 이름으로 사용됨
        }
    }

    @Nested
    @DisplayName("VPC 삭제 테스트")
    class DeleteVpcTest {

        @Test
        @DisplayName("VPC 삭제 시 소프트 삭제가 수행된다")
        void deleteVpc_Success_SoftDeletesResource() {
            // Given
            ResourceIdentity vpcId = ResourceIdentity.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .providerResourceId(VPC_ID)
                    .region("us-east-1")
                    .build();

            doNothing().when(vpcManagementPort).deleteVpc(any());
            when(cloudResourceRepository.softDeleteByResourceId(VPC_ID)).thenReturn(1);

            // When
            vpcUseCaseService.deleteVpc(vpcId);

            // Then
            verify(vpcManagementPort).deleteVpc(any());
            verify(cloudResourceRepository).softDeleteByResourceId(VPC_ID);
        }

        @Test
        @DisplayName("DB에 리소스가 없어도 CSP 삭제는 성공한다")
        void deleteVpc_ResourceNotInDb_CspDeletionSucceeds() {
            // Given
            ResourceIdentity vpcId = ResourceIdentity.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .providerResourceId(VPC_ID)
                    .region("us-east-1")
                    .build();

            doNothing().when(vpcManagementPort).deleteVpc(any());
            when(cloudResourceRepository.softDeleteByResourceId(VPC_ID)).thenReturn(0); // DB에 없음

            // When
            vpcUseCaseService.deleteVpc(vpcId);

            // Then
            verify(vpcManagementPort).deleteVpc(any()); // CSP 작업 성공
        }
    }
}
