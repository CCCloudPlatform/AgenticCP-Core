package com.agenticcp.core.domain.cloud.service.vm;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.domain.cloud.dto.VmCreateRequest;
import com.agenticcp.core.domain.cloud.dto.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
// LifecycleState removed - using JSON status field instead
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmTaggingPort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private CloudResourceManagementHelper resourceHelper;

    private VmUseCaseService vmUseCaseService;
    private CloudSessionCredential mockSession;

    private static final ProviderType PROVIDER_TYPE = ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String TENANT_KEY = "tenant-test";
    private static final String INSTANCE_ID = "i-1234567890abcdef0";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey(TENANT_KEY);
        
        vmUseCaseService = new VmUseCaseService(
                vmPortRouter,
                capabilityGuard,
                credentialProviderPort,
                resourceHelper
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
            Map<String, String> tags = Map.of("Name", "test-instance", "Environment", "test");
            VmCreateRequest request = VmCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .image("ami-12345678")
                    .instanceSize("t3.micro")
                    .tags(tags)
                    .build();

            CloudResource mockCloudResource = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .name("test-instance")
                    .provider("AWS")
                    .region("us-east-1")
                    .type("INSTANCE")
                    .build();

            when(vmLifecyclePort.createInstance(any())).thenReturn(INSTANCE_ID);
            when(resourceHelper.extractResourceName(tags, INSTANCE_ID)).thenReturn("test-instance");
            when(resourceHelper.registerResource(
                    eq(PROVIDER_TYPE),
                    eq("EC2"),
                    any(ResourceRegistrationRequest.class)
            )).thenReturn(mockCloudResource);

            // When
            CloudResource result = vmUseCaseService.createInstance(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(INSTANCE_ID);
            assertThat(result.getName()).isEqualTo("test-instance");
            
            verify(resourceHelper).registerResource(
                    eq(PROVIDER_TYPE),
                    eq("EC2"),
                    any(ResourceRegistrationRequest.class)
            );
        }

        @Test
        @DisplayName("DB 저장 실패 시 보상 트랜잭션이 실행되고 예외가 발생한다")
        void createInstance_DbSaveFails_CompensatingTransactionExecuted() {
            // Given
            VmCreateRequest request = VmCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .image("ami-12345678")
                    .instanceSize("t3.micro")
                    .build();

            when(vmLifecyclePort.createInstance(any())).thenReturn(INSTANCE_ID);
            when(resourceHelper.extractResourceName(any(), eq(INSTANCE_ID))).thenReturn(INSTANCE_ID);
            // DB 저장 실패
            doThrow(new RuntimeException("DB 저장 실패")).when(resourceHelper)
                    .registerResource(any(), any(), any());

            // When & Then
            assertThatThrownBy(() -> vmUseCaseService.createInstance(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.RESOURCE_CREATION_FAILED);
                    });

            // 보상 트랜잭션 실행 검증: CSP 인스턴스 종료 호출됨
            verify(vmLifecyclePort).terminateInstance(eq(INSTANCE_ID), any());
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

            // When
            vmUseCaseService.startInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).startInstance(eq(INSTANCE_ID), any());
            verify(resourceHelper).updateLifecycleState(eq(INSTANCE_ID), eq("running"));
        }

        @Test
        @DisplayName("인스턴스 중지 시 lifecycleState가 STOPPED로 업데이트된다")
        void stopInstance_Success_UpdatesLifecycleStateToStopped() {
            // Given
            doNothing().when(vmLifecyclePort).stopInstance(eq(INSTANCE_ID), any());

            // When
            vmUseCaseService.stopInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).stopInstance(eq(INSTANCE_ID), any());
            verify(resourceHelper).updateLifecycleState(eq(INSTANCE_ID), eq("stopped"));
        }

        @Test
        @DisplayName("인스턴스 재부팅 시 lifecycleState가 RUNNING으로 유지된다")
        void rebootInstance_Success_KeepsLifecycleStateRunning() {
            // Given
            doNothing().when(vmLifecyclePort).rebootInstance(eq(INSTANCE_ID), any());

            // When
            vmUseCaseService.rebootInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).rebootInstance(eq(INSTANCE_ID), any());
            verify(resourceHelper).updateLifecycleState(eq(INSTANCE_ID), eq("running"));
        }

        @Test
        @DisplayName("인스턴스 종료 시 lifecycleState가 TERMINATED로 업데이트된다")
        void terminateInstance_Success_UpdatesLifecycleStateToTerminated() {
            // Given
            doNothing().when(vmLifecyclePort).terminateInstance(eq(INSTANCE_ID), any());

            // When
            vmUseCaseService.terminateInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).terminateInstance(eq(INSTANCE_ID), any());
            verify(resourceHelper).updateLifecycleState(eq(INSTANCE_ID), eq("terminated"));
        }

        @Test
        @DisplayName("DB에 리소스가 없어도 CSP 작업은 성공한다")
        void startInstance_ResourceNotInDb_CspOperationSucceeds() {
            // Given
            doNothing().when(vmLifecyclePort).startInstance(eq(INSTANCE_ID), any());
            // Helper 내부에서 리소스가 없으면 로그만 출력하고 예외 발생 안함

            // When
            vmUseCaseService.startInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(vmLifecyclePort).startInstance(eq(INSTANCE_ID), any()); // CSP 작업 성공
            verify(resourceHelper).updateLifecycleState(eq(INSTANCE_ID), eq("running"));
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

            // When
            vmUseCaseService.deleteInstance(request);

            // Then
            verify(vmLifecyclePort).deleteInstance(any());
            verify(resourceHelper).softDeleteResource(INSTANCE_ID);
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
            // Helper 내부에서 리소스가 없으면 로그만 출력하고 예외 발생 안함

            // When
            vmUseCaseService.deleteInstance(request);

            // Then
            verify(vmLifecyclePort).deleteInstance(any()); // CSP 작업 성공
            verify(resourceHelper).softDeleteResource(INSTANCE_ID);
        }
    }
}
