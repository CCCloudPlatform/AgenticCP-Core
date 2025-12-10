package com.agenticcp.core.domain.cloud.service.vm;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.VmCreateRequest;
import com.agenticcp.core.domain.cloud.dto.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudResource.LifecycleState;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmTaggingPort;
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
 * VmUseCaseService DB 동기화 로직 단위 테스트
 * CSP 작업 후 CloudResource 엔티티가 올바르게 DB에 저장/업데이트되는지 검증합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VmUseCaseService DB 동기화 테스트")
class VmUseCaseServiceDbSyncTest {

    @Mock
    private VmPortRouter vmPortRouter;

    @Mock
    private VmDiscoveryPort vmDiscoveryPort;

    @Mock
    private VmLifecyclePort vmLifecyclePort;

    @Mock
    private VmTaggingPort vmTaggingPort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort credentialProviderPort;

    @Mock
    private CloudResourceRepository cloudResourceRepository;

    @Mock
    private CloudProviderRepository cloudProviderRepository;

    @Mock
    private CloudServiceRepository cloudServiceRepository;

    @Mock
    private TenantRepository tenantRepository;

    private ObjectMapper objectMapper;

    private VmUseCaseService vmUseCaseService;
    private CloudSessionCredential mockSession;

    private static final ProviderType PROVIDER_TYPE = ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String TENANT_KEY = "tenant-test";
    private static final String INSTANCE_ID = "i-1234567890abcdef0";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey(TENANT_KEY);
        objectMapper = new ObjectMapper();
        
        vmUseCaseService = new VmUseCaseService(
                vmPortRouter,
                capabilityGuard,
                credentialProviderPort,
                cloudResourceRepository,
                cloudProviderRepository,
                cloudServiceRepository,
                tenantRepository,
                objectMapper
        );
        
        mockSession = mock(CloudSessionCredential.class);
        lenient().when(mockSession.getExpiresAt()).thenReturn(LocalDateTime.now().plusHours(1));

        // 공통 Mock 설정
        lenient().when(vmPortRouter.discovery(PROVIDER_TYPE)).thenReturn(vmDiscoveryPort);
        lenient().when(vmPortRouter.lifecycle(PROVIDER_TYPE)).thenReturn(vmLifecyclePort);
        lenient().when(vmPortRouter.tagging(PROVIDER_TYPE)).thenReturn(vmTaggingPort);
        lenient().when(credentialProviderPort.getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(PROVIDER_TYPE)))
                .thenReturn(mockSession);
        lenient().doNothing().when(capabilityGuard).ensureSupported(any(), anyString(), anyString(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("인스턴스 생성 테스트")
    class CreateInstanceTest {

        @Test
        @DisplayName("인스턴스 생성 성공 시 CloudResource가 DB에 저장된다")
        void createInstance_Success_SavesCloudResource() {
            // Given
            VmCreateRequest request = VmCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .image("ami-12345678")
                    .instanceSize("t3.micro")
                    .tags(Map.of("Name", "test-instance", "Environment", "test"))
                    .build();

            CloudProvider mockProvider = CloudProvider.builder()
                    .providerType(PROVIDER_TYPE)
                    .providerName("AWS")
                    .build();

            CloudService mockService = CloudService.builder()
                    .serviceKey("EC2")
                    .build();

            Tenant mockTenant = Tenant.builder()
                    .tenantKey(TENANT_KEY)
                    .build();

            when(vmLifecyclePort.createInstance(any())).thenReturn(INSTANCE_ID);
            when(cloudProviderRepository.findFirstByProviderType(PROVIDER_TYPE))
                    .thenReturn(Optional.of(mockProvider));
            when(cloudServiceRepository.findByProviderTypeAndServiceKey(eq(PROVIDER_TYPE), eq("EC2")))
                    .thenReturn(Optional.of(mockService));
            when(tenantRepository.findByTenantKey(TENANT_KEY))
                    .thenReturn(Optional.of(mockTenant));
            when(cloudResourceRepository.save(any(CloudResource.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            String result = vmUseCaseService.createInstance(request);

            // Then
            assertThat(result).isEqualTo(INSTANCE_ID);
            
            ArgumentCaptor<CloudResource> captor = ArgumentCaptor.forClass(CloudResource.class);
            verify(cloudResourceRepository).save(captor.capture());
            
            CloudResource savedResource = captor.getValue();
            assertThat(savedResource.getResourceId()).isEqualTo(INSTANCE_ID);
            assertThat(savedResource.getResourceName()).isEqualTo("test-instance");
            assertThat(savedResource.getResourceType()).isEqualTo(CloudResource.ResourceType.INSTANCE);
            assertThat(savedResource.getLifecycleState()).isEqualTo(LifecycleState.PENDING);
            assertThat(savedResource.getProvider()).isEqualTo(mockProvider);
            assertThat(savedResource.getTenant()).isEqualTo(mockTenant);
        }

        @Test
        @DisplayName("DB 저장 실패 시에도 CSP 생성은 성공한다")
        void createInstance_DbSaveFails_CspCreationSucceeds() {
            // Given
            VmCreateRequest request = VmCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .image("ami-12345678")
                    .instanceSize("t3.micro")
                    .build();

            when(vmLifecyclePort.createInstance(any())).thenReturn(INSTANCE_ID);
            when(cloudProviderRepository.findFirstByProviderType(PROVIDER_TYPE))
                    .thenReturn(Optional.empty()); // Provider 없음 -> DB 저장 실패

            // When
            String result = vmUseCaseService.createInstance(request);

            // Then
            assertThat(result).isEqualTo(INSTANCE_ID); // CSP 생성은 성공
            verify(cloudResourceRepository, never()).save(any()); // DB 저장은 스킵됨
        }
    }

    @Nested
    @DisplayName("인스턴스 생명주기 상태 업데이트 테스트")
    class LifecycleStateUpdateTest {

        @Test
        @DisplayName("인스턴스 시작 시 lifecycleState가 RUNNING으로 업데이트된다")
        void startInstance_Success_UpdatesLifecycleStateToRunning() {
            // Given
            doNothing().when(vmLifecyclePort).startInstance(eq(INSTANCE_ID), any());
            when(cloudResourceRepository.updateLifecycleState(
                    eq(INSTANCE_ID), eq(LifecycleState.RUNNING), any(LocalDateTime.class)))
                    .thenReturn(1);

            // When
            vmUseCaseService.startInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).startInstance(eq(INSTANCE_ID), any());
            verify(cloudResourceRepository).updateLifecycleState(
                    eq(INSTANCE_ID), eq(LifecycleState.RUNNING), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("인스턴스 중지 시 lifecycleState가 STOPPED로 업데이트된다")
        void stopInstance_Success_UpdatesLifecycleStateToStopped() {
            // Given
            doNothing().when(vmLifecyclePort).stopInstance(eq(INSTANCE_ID), any());
            when(cloudResourceRepository.updateLifecycleState(
                    eq(INSTANCE_ID), eq(LifecycleState.STOPPED), any(LocalDateTime.class)))
                    .thenReturn(1);

            // When
            vmUseCaseService.stopInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).stopInstance(eq(INSTANCE_ID), any());
            verify(cloudResourceRepository).updateLifecycleState(
                    eq(INSTANCE_ID), eq(LifecycleState.STOPPED), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("인스턴스 재부팅 시 lifecycleState가 RUNNING으로 유지된다")
        void rebootInstance_Success_KeepsLifecycleStateRunning() {
            // Given
            doNothing().when(vmLifecyclePort).rebootInstance(eq(INSTANCE_ID), any());
            when(cloudResourceRepository.updateLifecycleState(
                    eq(INSTANCE_ID), eq(LifecycleState.RUNNING), any(LocalDateTime.class)))
                    .thenReturn(1);

            // When
            vmUseCaseService.rebootInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).rebootInstance(eq(INSTANCE_ID), any());
            verify(cloudResourceRepository).updateLifecycleState(
                    eq(INSTANCE_ID), eq(LifecycleState.RUNNING), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("인스턴스 종료 시 lifecycleState가 TERMINATED로 업데이트된다")
        void terminateInstance_Success_UpdatesLifecycleStateToTerminated() {
            // Given
            doNothing().when(vmLifecyclePort).terminateInstance(eq(INSTANCE_ID), any());
            when(cloudResourceRepository.updateLifecycleState(
                    eq(INSTANCE_ID), eq(LifecycleState.TERMINATED), any(LocalDateTime.class)))
                    .thenReturn(1);

            // When
            vmUseCaseService.terminateInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).terminateInstance(eq(INSTANCE_ID), any());
            verify(cloudResourceRepository).updateLifecycleState(
                    eq(INSTANCE_ID), eq(LifecycleState.TERMINATED), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("DB에 리소스가 없어도 CSP 작업은 성공한다")
        void startInstance_ResourceNotInDb_CspOperationSucceeds() {
            // Given
            doNothing().when(vmLifecyclePort).startInstance(eq(INSTANCE_ID), any());
            when(cloudResourceRepository.updateLifecycleState(
                    eq(INSTANCE_ID), eq(LifecycleState.RUNNING), any(LocalDateTime.class)))
                    .thenReturn(0); // DB에 리소스 없음

            // When
            vmUseCaseService.startInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).startInstance(eq(INSTANCE_ID), any()); // CSP 작업 성공
        }
    }

    @Nested
    @DisplayName("인스턴스 삭제 테스트")
    class DeleteInstanceTest {

        @Test
        @DisplayName("인스턴스 삭제 시 소프트 삭제가 수행된다")
        void deleteInstance_Success_SoftDeletesResource() {
            // Given
            VmDeleteRequest request = VmDeleteRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .instanceId(INSTANCE_ID)
                    .force(false)
                    .build();

            doNothing().when(vmLifecyclePort).deleteInstance(any());
            when(cloudResourceRepository.softDeleteByResourceId(INSTANCE_ID)).thenReturn(1);

            // When
            vmUseCaseService.deleteInstance(request);

            // Then
            verify(vmLifecyclePort).deleteInstance(any());
            verify(cloudResourceRepository).softDeleteByResourceId(INSTANCE_ID);
        }

        @Test
        @DisplayName("DB에 리소스가 없어도 CSP 삭제는 성공한다")
        void deleteInstance_ResourceNotInDb_CspDeletionSucceeds() {
            // Given
            VmDeleteRequest request = VmDeleteRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .instanceId(INSTANCE_ID)
                    .force(false)
                    .build();

            doNothing().when(vmLifecyclePort).deleteInstance(any());
            when(cloudResourceRepository.softDeleteByResourceId(INSTANCE_ID)).thenReturn(0); // DB에 없음

            // When
            vmUseCaseService.deleteInstance(request);

            // Then
            verify(vmLifecyclePort).deleteInstance(any()); // CSP 작업 성공
        }
    }
}
