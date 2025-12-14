package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.dto.VmCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.vm.VmCreateCommand;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmLifecyclePort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import com.agenticcp.core.domain.cloud.service.vm.VmPortRouter;
import com.agenticcp.core.domain.cloud.service.vm.VmUseCaseService;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VM 유스케이스 서비스 생성 기능 테스트
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class VmUseCaseServiceCreateTest {

    @Mock
    private VmPortRouter vmPortRouter;

    @Mock
    private VmLifecyclePort vmLifecyclePort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort credentialProviderPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    private VmUseCaseService vmUseCaseService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey("tenant-test");
        vmUseCaseService = new VmUseCaseService(
                vmPortRouter,
                capabilityGuard,
                credentialProviderPort,
                resourceHelper
        );

        when(vmPortRouter.lifecycle(ProviderType.AWS)).thenReturn(vmLifecyclePort);
        when(credentialProviderPort.getSession(anyString(), anyString(), any())).thenReturn(null);
        doNothing().when(capabilityGuard).ensureSupported(any(), anyString(), anyString(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createInstance_성공() {
        // Given
        VmCreateRequest request = VmCreateRequest.builder()
            .providerType(ProviderType.AWS)
            .accountScope("123456789012")
            .image("ami-12345678")
            .instanceSize("t3.micro")
            .sshKey("my-key")
            .networkSecurityId("sg-12345678")
            .subnetId("subnet-12345678")
            .minCount(1)
            .maxCount(1)
            .build();

        String expectedInstanceId = "i-1234567890abcdef0";
        when(vmLifecyclePort.createInstance(any(VmCreateCommand.class))).thenReturn(expectedInstanceId);

        // When
        String result = vmUseCaseService.createInstance(request);

        // Then
        assertThat(result).isEqualTo(expectedInstanceId);

        // 포트 호출 확인
        verify(vmLifecyclePort).createInstance(any(VmCreateCommand.class));
    }

    @Test
    void createInstance_최소요청() {
        // Given - 최소 필수 정보만 포함
        VmCreateRequest request = VmCreateRequest.builder()
            .providerType(ProviderType.AWS)
            .accountScope("123456789012")
            .image("ami-12345678")
            .instanceSize("t3.micro")
            .minCount(1)
            .maxCount(1)
            .build();

        String expectedInstanceId = "i-abcdef1234567890";
        when(vmLifecyclePort.createInstance(any(VmCreateCommand.class))).thenReturn(expectedInstanceId);

        // When
        String result = vmUseCaseService.createInstance(request);

        // Then
        assertThat(result).isEqualTo(expectedInstanceId);

        // 포트 호출 확인
        verify(vmLifecyclePort).createInstance(any(VmCreateCommand.class));
    }

    @Test
    void createInstance_예외발생시() {
        // Given
        VmCreateRequest request = VmCreateRequest.builder()
            .providerType(ProviderType.AWS)
            .accountScope("123456789012")
            .image("ami-12345678")
            .instanceSize("t3.micro")
            .minCount(1)
            .maxCount(1)
            .build();

        RuntimeException exception = new RuntimeException("AWS API Error");
        when(vmLifecyclePort.createInstance(any(VmCreateCommand.class))).thenThrow(exception);

        // When & Then
        try {
            vmUseCaseService.createInstance(request);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }
    }

    @Test
    void createInstance_태그포함요청() {
        // Given - 태그가 포함된 요청
        VmCreateRequest request = VmCreateRequest.builder()
            .providerType(ProviderType.AWS)
            .accountScope("123456789012")
            .image("ami-12345678")
            .instanceSize("t3.micro")
            .sshKey("my-key")
            .networkSecurityId("sg-12345678")
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
        when(vmLifecyclePort.createInstance(any(VmCreateCommand.class))).thenReturn(expectedInstanceId);

        // When
        String result = vmUseCaseService.createInstance(request);

        // Then
        assertThat(result).isEqualTo(expectedInstanceId);

        // 포트 호출 확인
        verify(vmLifecyclePort).createInstance(any(VmCreateCommand.class));
    }
}
