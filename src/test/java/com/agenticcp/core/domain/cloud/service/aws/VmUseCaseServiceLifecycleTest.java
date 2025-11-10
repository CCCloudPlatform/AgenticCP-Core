package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VmManagementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private VmManagementPort vmManagementPort;

    private VmUseCaseService vmUseCaseService;

    @BeforeEach
    void setUp() {
        vmUseCaseService = new VmUseCaseService(vmPortRouter, auditEventPort);
        
        // VmPortRouter가 AWS 포트를 반환하도록 설정
        when(vmPortRouter.vm(ProviderType.AWS)).thenReturn(vmManagementPort);
    }

    @Test
    void startInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        vmUseCaseService.startInstance(instanceId);

        // Then
        verify(vmManagementPort).startInstance(instanceId);

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
        verify(vmManagementPort).stopInstance(instanceId);

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
        verify(vmManagementPort).rebootInstance(instanceId);

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
        verify(vmManagementPort).terminateInstance(instanceId);

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
        verify(vmManagementPort).deleteInstance(request);

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
        doThrow(exception).when(vmManagementPort).startInstance(instanceId);

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
        doThrow(exception).when(vmManagementPort).stopInstance(instanceId);

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
        doThrow(exception).when(vmManagementPort).rebootInstance(instanceId);

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
        doThrow(exception).when(vmManagementPort).terminateInstance(instanceId);

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
        doThrow(exception).when(vmManagementPort).deleteInstance(request);

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
        verify(vmManagementPort).startInstance(instanceId);
        verify(vmManagementPort).stopInstance(instanceId);
        verify(vmManagementPort).rebootInstance(instanceId);
        verify(vmManagementPort).terminateInstance(instanceId);

        // 모든 작업에 대한 감사 로그 기록 확인
        verify(auditEventPort, times(4)).record(
            any(String.class), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }
}
