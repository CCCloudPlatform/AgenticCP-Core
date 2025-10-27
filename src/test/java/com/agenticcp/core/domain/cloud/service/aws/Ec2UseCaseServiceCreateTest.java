package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.Ec2CreateRequest;
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
 * EC2 유스케이스 서비스 생성 기능 테스트
 */
@ExtendWith(MockitoExtension.class)
class Ec2UseCaseServiceCreateTest {

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
    void createInstance_성공() {
        // Given
        Ec2CreateRequest request = Ec2CreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .keyName("my-key")
            .securityGroupId("sg-12345678")
            .subnetId("subnet-12345678")
            .minCount(1)
            .maxCount(1)
            .build();

        String expectedInstanceId = "i-1234567890abcdef0";
        when(ec2ManagementPort.createInstance(request)).thenReturn(expectedInstanceId);

        // When
        String result = ec2UseCaseService.createInstance(request);

        // Then
        assertThat(result).isEqualTo(expectedInstanceId);

        // 포트 호출 확인
        verify(ec2ManagementPort).createInstance(request);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("CREATE_INSTANCE"), 
            eq("EC2"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void createInstance_최소요청() {
        // Given - 최소 필수 정보만 포함
        Ec2CreateRequest request = Ec2CreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .minCount(1)
            .maxCount(1)
            .build();

        String expectedInstanceId = "i-abcdef1234567890";
        when(ec2ManagementPort.createInstance(request)).thenReturn(expectedInstanceId);

        // When
        String result = ec2UseCaseService.createInstance(request);

        // Then
        assertThat(result).isEqualTo(expectedInstanceId);

        // 포트 호출 확인
        verify(ec2ManagementPort).createInstance(request);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("CREATE_INSTANCE"), 
            eq("EC2"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void createInstance_예외발생시_감사로그기록() {
        // Given
        Ec2CreateRequest request = Ec2CreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .minCount(1)
            .maxCount(1)
            .build();

        RuntimeException exception = new RuntimeException("AWS API Error");
        when(ec2ManagementPort.createInstance(request)).thenThrow(exception);

        // When & Then
        try {
            ec2UseCaseService.createInstance(request);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("CREATE_INSTANCE"), 
            eq("EC2"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }

    @Test
    void createInstance_태그포함요청() {
        // Given - 태그가 포함된 요청
        Ec2CreateRequest request = Ec2CreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .keyName("my-key")
            .securityGroupId("sg-12345678")
            .subnetId("subnet-12345678")
            .userData("#!/bin/bash\necho 'Hello World'")
            .tags(Map.of(
                "Environment", "Development",
                "Project", "TestProject",
                "Owner", "TestUser"
            ))
            .minCount(1)
            .maxCount(1)
            .build();

        String expectedInstanceId = "i-tagged1234567890";
        when(ec2ManagementPort.createInstance(request)).thenReturn(expectedInstanceId);

        // When
        String result = ec2UseCaseService.createInstance(request);

        // Then
        assertThat(result).isEqualTo(expectedInstanceId);

        // 포트 호출 확인
        verify(ec2ManagementPort).createInstance(request);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("CREATE_INSTANCE"), 
            eq("EC2"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }
}
