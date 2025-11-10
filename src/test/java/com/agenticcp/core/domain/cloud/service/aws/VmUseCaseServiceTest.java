package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VmManagementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * VM 유스케이스 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class VmUseCaseServiceTest {

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
    void listInstances_성공() {
        // Given
        VmQuery query = VmQuery.builder()
            .page(0)
            .size(10)
            .build();

        CloudResource resource = CloudResource.builder()
            .resourceId("i-1234567890abcdef0")
            .resourceName("test-instance")
            .build();

        Page<CloudResource> expectedPage = new PageImpl<>(
            List.of(resource), 
            PageRequest.of(0, 10), 
            1
        );

        when(vmManagementPort.listInstances(query)).thenReturn(expectedPage);

        // When
        Page<CloudResource> result = vmUseCaseService.listInstances(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getResourceId()).isEqualTo("i-1234567890abcdef0");

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("LIST_INSTANCES"), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void getInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        
        CloudResource resource = CloudResource.builder()
            .resourceId(instanceId)
            .resourceName("test-instance")
            .build();

        when(vmManagementPort.getInstance(instanceId)).thenReturn(Optional.of(resource));

        // When
        Optional<CloudResource> result = vmUseCaseService.getInstance(instanceId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getResourceId()).isEqualTo(instanceId);

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("GET_INSTANCE"), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void getInstance_인스턴스없음() {
        // Given
        String instanceId = "i-nonexistent";
        
        when(vmManagementPort.getInstance(instanceId)).thenReturn(Optional.empty());

        // When
        Optional<CloudResource> result = vmUseCaseService.getInstance(instanceId);

        // Then
        assertThat(result).isEmpty();

        // 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("GET_INSTANCE"), 
            eq("VM"), 
            eq("SUCCESS"), 
            any(Map.class)
        );
    }

    @Test
    void listInstances_예외발생시_감사로그기록() {
        // Given
        VmQuery query = VmQuery.builder()
            .page(0)
            .size(10)
            .build();

        RuntimeException exception = new RuntimeException("AWS API Error");
        when(vmManagementPort.listInstances(query)).thenThrow(exception);

        // When & Then
        try {
            vmUseCaseService.listInstances(query);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }

        // 실패 감사 로그 기록 확인
        verify(auditEventPort).record(
            eq("LIST_INSTANCES"), 
            eq("VM"), 
            eq("FAILED"), 
            any(Map.class)
        );
    }
}
