package com.agenticcp.core.domain.cloud.port;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.VmUpdateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VmManagementPort;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
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
import static org.mockito.Mockito.*;

/**
 * VM 관리 포트 계약 테스트
 * 
 * VmManagementPort 인터페이스의 모든 메서드가 올바르게 동작하는지 검증합니다.
 * 이 테스트는 포트 인터페이스의 계약(contract)을 정의하고 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class VmManagementContractTest {

    @Mock
    private VmManagementPort vmManagementPort;

    private CloudResource testInstance;
    private VmQuery testQuery;
    private VmCreateRequest testCreateRequest;
    private VmUpdateRequest testUpdateRequest;
    private VmDeleteRequest testDeleteRequest;

    @BeforeEach
    void setUp() {
        testInstance = CloudResource.builder()
            .resourceId("i-1234567890abcdef0")
            .resourceName("test-instance")
            .displayName("Test Instance")
            .build();

        testQuery = VmQuery.builder()
            .page(0)
            .size(10)
            .build();

        testCreateRequest = VmCreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .minCount(1)
            .maxCount(1)
            .build();

        testUpdateRequest = VmUpdateRequest.builder()
            .instanceId("i-1234567890abcdef0")
            .instanceType("t3.small")
            .build();

        testDeleteRequest = VmDeleteRequest.basic("i-1234567890abcdef0");
    }

    @Test
    void listInstances_계약_테스트() {
        // Given
        Page<CloudResource> expectedPage = new PageImpl<>(
            List.of(testInstance), 
            PageRequest.of(0, 10), 
            1
        );
        when(vmManagementPort.listInstances(any(VmQuery.class))).thenReturn(expectedPage);

        // When
        Page<CloudResource> result = vmManagementPort.listInstances(testQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getResourceId()).isEqualTo("i-1234567890abcdef0");
        verify(vmManagementPort).listInstances(testQuery);
    }

    @Test
    void getInstance_계약_테스트() {
        // Given
        when(vmManagementPort.getInstance("i-1234567890abcdef0"))
            .thenReturn(Optional.of(testInstance));

        // When
        Optional<CloudResource> result = vmManagementPort.getInstance("i-1234567890abcdef0");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getResourceId()).isEqualTo("i-1234567890abcdef0");
        verify(vmManagementPort).getInstance("i-1234567890abcdef0");
    }

    @Test
    void getInstance_존재하지_않는_인스턴스_계약_테스트() {
        // Given
        when(vmManagementPort.getInstance("i-nonexistent"))
            .thenReturn(Optional.empty());

        // When
        Optional<CloudResource> result = vmManagementPort.getInstance("i-nonexistent");

        // Then
        assertThat(result).isEmpty();
        verify(vmManagementPort).getInstance("i-nonexistent");
    }

    @Test
    void createInstance_계약_테스트() {
        // Given
        String expectedInstanceId = "i-1234567890abcdef0";
        when(vmManagementPort.createInstance(any(VmCreateRequest.class)))
            .thenReturn(expectedInstanceId);

        // When
        String result = vmManagementPort.createInstance(testCreateRequest);

        // Then
        assertThat(result).isEqualTo(expectedInstanceId);
        verify(vmManagementPort).createInstance(testCreateRequest);
    }

    @Test
    void startInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).startInstance(anyString());

        // When
        vmManagementPort.startInstance("i-1234567890abcdef0");

        // Then
        verify(vmManagementPort).startInstance("i-1234567890abcdef0");
    }

    @Test
    void stopInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).stopInstance(anyString());

        // When
        vmManagementPort.stopInstance("i-1234567890abcdef0");

        // Then
        verify(vmManagementPort).stopInstance("i-1234567890abcdef0");
    }

    @Test
    void rebootInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).rebootInstance(anyString());

        // When
        vmManagementPort.rebootInstance("i-1234567890abcdef0");

        // Then
        verify(vmManagementPort).rebootInstance("i-1234567890abcdef0");
    }

    @Test
    void terminateInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).terminateInstance(anyString());

        // When
        vmManagementPort.terminateInstance("i-1234567890abcdef0");

        // Then
        verify(vmManagementPort).terminateInstance("i-1234567890abcdef0");
    }

    @Test
    void deleteInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).deleteInstance(any(VmDeleteRequest.class));

        // When
        vmManagementPort.deleteInstance(testDeleteRequest);

        // Then
        verify(vmManagementPort).deleteInstance(testDeleteRequest);
    }

    @Test
    void updateInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).updateInstance(any(VmUpdateRequest.class));

        // When
        vmManagementPort.updateInstance(testUpdateRequest);

        // Then
        verify(vmManagementPort).updateInstance(testUpdateRequest);
    }

    @Test
    void addTags_계약_테스트() {
        // Given
        Map<String, String> tags = Map.of(
            "Environment", "Development",
            "Project", "TestProject"
        );
        doNothing().when(vmManagementPort).addTags(anyString(), any(Map.class));

        // When
        vmManagementPort.addTags("i-1234567890abcdef0", tags);

        // Then
        verify(vmManagementPort).addTags("i-1234567890abcdef0", tags);
    }

    @Test
    void removeTags_계약_테스트() {
        // Given
        Map<String, String> tagKeys = Map.of(
            "Environment", "",
            "Project", ""
        );
        doNothing().when(vmManagementPort).removeTags(anyString(), any(Map.class));

        // When
        vmManagementPort.removeTags("i-1234567890abcdef0", tagKeys);

        // Then
        verify(vmManagementPort).removeTags("i-1234567890abcdef0", tagKeys);
    }

    @Test
    void getTags_계약_테스트() {
        // Given
        Map<String, String> expectedTags = Map.of(
            "Environment", "Development",
            "Project", "TestProject"
        );
        when(vmManagementPort.getTags("i-1234567890abcdef0"))
            .thenReturn(expectedTags);

        // When
        Map<String, String> result = vmManagementPort.getTags("i-1234567890abcdef0");

        // Then
        assertThat(result).isEqualTo(expectedTags);
        verify(vmManagementPort).getTags("i-1234567890abcdef0");
    }

    @Test
    void getInstanceStatus_계약_테스트() {
        // Given
        String expectedStatus = "running";
        when(vmManagementPort.getInstanceStatus("i-1234567890abcdef0"))
            .thenReturn(expectedStatus);

        // When
        String result = vmManagementPort.getInstanceStatus("i-1234567890abcdef0");

        // Then
        assertThat(result).isEqualTo(expectedStatus);
        verify(vmManagementPort).getInstanceStatus("i-1234567890abcdef0");
    }

    @Test
    void waitForInstanceStatus_계약_테스트() {
        // Given
        when(vmManagementPort.waitForInstanceStatus("i-1234567890abcdef0", "running", 300))
            .thenReturn(true);

        // When
        boolean result = vmManagementPort.waitForInstanceStatus("i-1234567890abcdef0", "running", 300);

        // Then
        assertThat(result).isTrue();
        verify(vmManagementPort).waitForInstanceStatus("i-1234567890abcdef0", "running", 300);
    }

    @Test
    void waitForInstanceStatus_타임아웃_계약_테스트() {
        // Given
        when(vmManagementPort.waitForInstanceStatus("i-1234567890abcdef0", "running", 300))
            .thenReturn(false);

        // When
        boolean result = vmManagementPort.waitForInstanceStatus("i-1234567890abcdef0", "running", 300);

        // Then
        assertThat(result).isFalse();
        verify(vmManagementPort).waitForInstanceStatus("i-1234567890abcdef0", "running", 300);
    }

    @Test
    void providerScoped_계약_테스트() {
        // Given
        VmManagementPortWithProvider providerPort = mock(VmManagementPortWithProvider.class);
        when(providerPort.getProviderType()).thenReturn(ProviderType.AWS);

        // When
        ProviderType result = providerPort.getProviderType();

        // Then
        assertThat(result).isEqualTo(ProviderType.AWS);
        verify(providerPort).getProviderType();
    }

    /**
     * ProviderScoped를 구현하는 테스트용 인터페이스
     */
    private interface VmManagementPortWithProvider extends VmManagementPort, ProviderScoped {
    }
}
