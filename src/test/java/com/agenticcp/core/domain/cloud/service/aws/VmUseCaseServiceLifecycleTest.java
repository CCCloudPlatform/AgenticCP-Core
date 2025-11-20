package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.vm.VmDeleteCommand;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmTaggingPort;
import com.agenticcp.core.domain.cloud.service.vm.VmPortRouter;
import com.agenticcp.core.domain.cloud.service.vm.VmUseCaseService;
import java.util.Map;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
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
    private AuditEventPort auditEventPort;

    @Mock
    private VmLifecyclePort vmLifecyclePort;

    @Mock
    private VmDiscoveryPort vmDiscoveryPort;

    @Mock
    private VmTaggingPort vmTaggingPort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private CredentialProviderPort credentialProviderPort;

    private VmUseCaseService vmUseCaseService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey("tenant-test");
        vmUseCaseService = new VmUseCaseService(vmPortRouter, auditEventPort, capabilityGuard, credentialProviderPort);

        when(vmPortRouter.lifecycle(ProviderType.AWS)).thenReturn(vmLifecyclePort);
        when(vmPortRouter.discovery(ProviderType.AWS)).thenReturn(vmDiscoveryPort);
        when(vmPortRouter.tagging(ProviderType.AWS)).thenReturn(vmTaggingPort);
        when(credentialProviderPort.resolveCredentials(anyString(), any(), anyString())).thenReturn(new Object());
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
        vmUseCaseService.startInstance(instanceId);

        // Then
        verify(vmLifecyclePort).startInstance(instanceId);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("START_INSTANCE"), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void stopInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        vmUseCaseService.stopInstance(instanceId);

        // Then
        verify(vmLifecyclePort).stopInstance(instanceId);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("STOP_INSTANCE"), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void rebootInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        vmUseCaseService.rebootInstance(instanceId);

        // Then
        verify(vmLifecyclePort).rebootInstance(instanceId);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("REBOOT_INSTANCE"), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void terminateInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        vmUseCaseService.terminateInstance(instanceId);

        // Then
        verify(vmLifecyclePort).terminateInstance(instanceId);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("TERMINATE_INSTANCE"), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void deleteInstance_성공() {
        // Given
        VmDeleteRequest request = VmDeleteRequest.builder()
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

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("DELETE_INSTANCE"), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void startInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(vmLifecyclePort).startInstance(instanceId);

        // When & Then
        try {
            vmUseCaseService.startInstance(instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("START_INSTANCE"), 
            eq("VM"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void stopInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(vmLifecyclePort).stopInstance(instanceId);

        // When & Then
        try {
            vmUseCaseService.stopInstance(instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("STOP_INSTANCE"), 
            eq("VM"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void rebootInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(vmLifecyclePort).rebootInstance(instanceId);

        // When & Then
        try {
            vmUseCaseService.rebootInstance(instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("REBOOT_INSTANCE"), 
            eq("VM"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void terminateInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(vmLifecyclePort).terminateInstance(instanceId);

        // When & Then
        try {
            vmUseCaseService.terminateInstance(instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("TERMINATE_INSTANCE"), 
            eq("VM"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void deleteInstance_예외발생시_감사로그기록() {
        // Given
        VmDeleteRequest request = VmDeleteRequest.builder()
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

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("DELETE_INSTANCE"), 
            eq("VM"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void 생명주기관리_연속작업() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When - 인스턴스 시작 → 중지 → 재부팅 → 종료
        vmUseCaseService.startInstance(instanceId);
        vmUseCaseService.stopInstance(instanceId);
        vmUseCaseService.rebootInstance(instanceId);
        vmUseCaseService.terminateInstance(instanceId);

        // Then
        verify(vmLifecyclePort).startInstance(instanceId);
        verify(vmLifecyclePort).stopInstance(instanceId);
        verify(vmLifecyclePort).rebootInstance(instanceId);
        verify(vmLifecyclePort).terminateInstance(instanceId);

        // 모든 작업에 대한 감사 로그 기록 확인
        verify(auditEventPort, times(4)).record(
            any(String.class), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }
}
