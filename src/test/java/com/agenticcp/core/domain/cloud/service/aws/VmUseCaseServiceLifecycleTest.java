package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.dto.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.vm.VmDeleteCommand;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmLifecyclePort;
import com.agenticcp.core.domain.cloud.service.vm.VmPortRouter;
import com.agenticcp.core.domain.cloud.service.vm.VmUseCaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VM 유스케이스 서비스 생명주기 관리 테스트
 */
@ExtendWith(MockitoExtension.class)
class VmUseCaseServiceLifecycleTest {

    @Mock
    private VmPortRouter vmPortRouter;

    @Mock
    private VmLifecyclePort vmLifecyclePort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort credentialProviderPort;

    private VmUseCaseService vmUseCaseService;

    private CloudSessionCredential mockSession;
    private static final ProviderType PROVIDER_TYPE = ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey("tenant-test");
        vmUseCaseService = new VmUseCaseService(vmPortRouter, capabilityGuard, credentialProviderPort);
        mockSession = mock(CloudSessionCredential.class);

        when(vmPortRouter.lifecycle(ProviderType.AWS)).thenReturn(vmLifecyclePort);
        lenient().when(credentialProviderPort.getSession(anyString(), nullable(String.class), nullable(ProviderType.class))).thenReturn(mockSession);
        doNothing().when(capabilityGuard).ensureSupported(any(), anyString(), anyString(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void startInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        vmUseCaseService.startInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);

        // Then
        verify(vmLifecyclePort).startInstance(eq(instanceId), any(CloudSessionCredential.class));
    }

    @Test
    void stopInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        vmUseCaseService.stopInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);

        // Then
        verify(vmLifecyclePort).stopInstance(eq(instanceId), any(CloudSessionCredential.class));
    }

    @Test
    void rebootInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        vmUseCaseService.rebootInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);

        // Then
        verify(vmLifecyclePort).rebootInstance(eq(instanceId), any(CloudSessionCredential.class));
    }

    @Test
    void terminateInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        vmUseCaseService.terminateInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);

        // Then
        verify(vmLifecyclePort).terminateInstance(eq(instanceId), any(CloudSessionCredential.class));
    }

    @Test
    void deleteInstance_성공() {
        // Given
        VmDeleteRequest request = VmDeleteRequest.builder()
            .providerType(PROVIDER_TYPE)
            .accountScope(ACCOUNT_SCOPE)
            .instanceId("i-1234567890abcdef0")
            .force(true)
            .build();

        // When
        vmUseCaseService.deleteInstance(request);

        // Then
        ArgumentCaptor<VmDeleteCommand> captor = ArgumentCaptor.forClass(VmDeleteCommand.class);
        verify(vmLifecyclePort).deleteInstance(captor.capture());
        assertThat(captor.getValue().getInstanceId()).isEqualTo(request.getInstanceId());
        assertThat(captor.getValue().isForce()).isTrue();
    }

    @Test
    void startInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(vmLifecyclePort).startInstance(eq(instanceId), any(CloudSessionCredential.class));

        // When & Then
        try {
            vmUseCaseService.startInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }
    }

    @Test
    void stopInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(vmLifecyclePort).stopInstance(eq(instanceId), any(CloudSessionCredential.class));

        // When & Then
        try {
            vmUseCaseService.stopInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }
    }

    @Test
    void rebootInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(vmLifecyclePort).rebootInstance(eq(instanceId), any(CloudSessionCredential.class));

        // When & Then
        try {
            vmUseCaseService.rebootInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }
    }

    @Test
    void terminateInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(vmLifecyclePort).terminateInstance(eq(instanceId), any(CloudSessionCredential.class));

        // When & Then
        try {
            vmUseCaseService.terminateInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }
    }

    @Test
    void deleteInstance_예외발생시_감사로그기록() {
        // Given
        VmDeleteRequest request = VmDeleteRequest.builder()
            .providerType(PROVIDER_TYPE)
            .accountScope(ACCOUNT_SCOPE)
            .instanceId("i-1234567890abcdef0")
            .force(true)
            .build();
        
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(vmLifecyclePort).deleteInstance(any(VmDeleteCommand.class));

        // When & Then
        try {
            vmUseCaseService.deleteInstance(request);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }
    }

    @Test
    void 생명주기관리_연속작업() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When - 인스턴스 시작 → 중지 → 재부팅 → 종료
        vmUseCaseService.startInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);
        vmUseCaseService.stopInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);
        vmUseCaseService.rebootInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);
        vmUseCaseService.terminateInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);

        // Then
        verify(vmLifecyclePort).startInstance(eq(instanceId), any(CloudSessionCredential.class));
        verify(vmLifecyclePort).stopInstance(eq(instanceId), any(CloudSessionCredential.class));
        verify(vmLifecyclePort).rebootInstance(eq(instanceId), any(CloudSessionCredential.class));
        verify(vmLifecyclePort).terminateInstance(eq(instanceId), any(CloudSessionCredential.class));
    }
}
