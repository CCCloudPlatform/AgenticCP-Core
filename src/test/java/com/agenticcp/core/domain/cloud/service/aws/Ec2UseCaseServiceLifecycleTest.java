package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.Ec2DeleteRequest;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.aws.Ec2ManagementPort;
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
 * EC2 유스케이스 서비스 생명주기 관리 테스트
 */
@ExtendWith(MockitoExtension.class)
class Ec2UseCaseServiceLifecycleTest {

    @Mock
    private Ec2PortRouter ec2PortRouter;

    @Mock
    private AuditEventPort auditEventPort;

    @Mock
    private Ec2ManagementPort ec2ManagementPort;

    private Ec2UseCaseService ec2UseCaseService;

    @BeforeEach
    void setUp() {
        ec2UseCaseService = new Ec2UseCaseService(ec2PortRouter, auditEventPort);
        
        // Ec2PortRouter가 AWS 포트를 반환하도록 설정
        when(ec2PortRouter.ec2(ProviderType.AWS)).thenReturn(ec2ManagementPort);
    }

    @Test
    void startInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        ec2UseCaseService.startInstance(instanceId);

        // Then
        verify(ec2ManagementPort).startInstance(instanceId);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("START_INSTANCE"), 
            eq("EC2"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void stopInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        ec2UseCaseService.stopInstance(instanceId);

        // Then
        verify(ec2ManagementPort).stopInstance(instanceId);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("STOP_INSTANCE"), 
            eq("EC2"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void rebootInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        ec2UseCaseService.rebootInstance(instanceId);

        // Then
        verify(ec2ManagementPort).rebootInstance(instanceId);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("REBOOT_INSTANCE"), 
            eq("EC2"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void terminateInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When
        ec2UseCaseService.terminateInstance(instanceId);

        // Then
        verify(ec2ManagementPort).terminateInstance(instanceId);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("TERMINATE_INSTANCE"), 
            eq("EC2"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void deleteInstance_성공() {
        // Given
        Ec2DeleteRequest request = Ec2DeleteRequest.builder()
            .instanceId("i-1234567890abcdef0")
            .force(true)
            .build();

        // When
        ec2UseCaseService.deleteInstance(request);

        // Then
        verify(ec2ManagementPort).deleteInstance(request);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("DELETE_INSTANCE"), 
            eq("EC2"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void startInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(ec2ManagementPort).startInstance(instanceId);

        // When & Then
        try {
            ec2UseCaseService.startInstance(instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("START_INSTANCE"), 
            eq("EC2"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void stopInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(ec2ManagementPort).stopInstance(instanceId);

        // When & Then
        try {
            ec2UseCaseService.stopInstance(instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("STOP_INSTANCE"), 
            eq("EC2"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void rebootInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(ec2ManagementPort).rebootInstance(instanceId);

        // When & Then
        try {
            ec2UseCaseService.rebootInstance(instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("REBOOT_INSTANCE"), 
            eq("EC2"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void terminateInstance_예외발생시_감사로그기록() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(ec2ManagementPort).terminateInstance(instanceId);

        // When & Then
        try {
            ec2UseCaseService.terminateInstance(instanceId);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("TERMINATE_INSTANCE"), 
            eq("EC2"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void deleteInstance_예외발생시_감사로그기록() {
        // Given
        Ec2DeleteRequest request = Ec2DeleteRequest.builder()
            .instanceId("i-1234567890abcdef0")
            .force(true)
            .build();
        
        RuntimeException exception = new RuntimeException("AWS API Error");
        doThrow(exception).when(ec2ManagementPort).deleteInstance(request);

        // When & Then
        try {
            ec2UseCaseService.deleteInstance(request);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("DELETE_INSTANCE"), 
            eq("EC2"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void 생명주기관리_연속작업() {
        // Given
        String instanceId = "i-1234567890abcdef0";

        // When - 인스턴스 시작 → 중지 → 재부팅 → 종료
        ec2UseCaseService.startInstance(instanceId);
        ec2UseCaseService.stopInstance(instanceId);
        ec2UseCaseService.rebootInstance(instanceId);
        ec2UseCaseService.terminateInstance(instanceId);

        // Then
        verify(ec2ManagementPort).startInstance(instanceId);
        verify(ec2ManagementPort).stopInstance(instanceId);
        verify(ec2ManagementPort).rebootInstance(instanceId);
        verify(ec2ManagementPort).terminateInstance(instanceId);

        // 모든 작업에 대한 감사 로그 기록 확인
        verify(auditEventPort, times(4)).record(
            any(String.class), 
            eq("EC2"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }
}
