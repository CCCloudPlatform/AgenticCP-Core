package com.agenticcp.core.domain.cloud.port;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.vm.VmCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmUpdateCommand;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmTaggingPort;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VM 관리 포트 계약 테스트
 *
 * VmDiscoveryPort / VmLifecyclePort / VmTaggingPort 조합이 기대하는 계약을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class VmManagementContractTest {

    @Mock
    private VmCompositePort vmManagementPort;

    private CloudResource testInstance;
    private VmQuery testQuery;
    private VmCreateCommand testCreateCommand;
    private VmUpdateCommand testUpdateCommand;
    private VmDeleteCommand testDeleteCommand;

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

        testCreateCommand = VmCreateCommand.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .minCount(1)
            .maxCount(1)
            .build();

        testUpdateCommand = VmUpdateCommand.builder()
            .instanceId("i-1234567890abcdef0")
            .instanceType("t3.small")
            .build();

        testDeleteCommand = VmDeleteCommand.builder()
            .instanceId("i-1234567890abcdef0")
            .force(false)
            .build();
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
        when(vmManagementPort.createInstance(any(VmCreateCommand.class)))
            .thenReturn(expectedInstanceId);

        // When
        String result = vmManagementPort.createInstance(testCreateCommand);

        // Then
        assertThat(result).isEqualTo(expectedInstanceId);
        verify(vmManagementPort).createInstance(testCreateCommand);
    }

    @Test
    void startInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).startInstance(anyString(), any(CloudSessionCredential.class));
        CloudSessionCredential mockSession = mock(CloudSessionCredential.class);

        // When
        vmManagementPort.startInstance("i-1234567890abcdef0", mockSession);

        // Then
        verify(vmManagementPort).startInstance(eq("i-1234567890abcdef0"), any(CloudSessionCredential.class));
    }

    @Test
    void stopInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).stopInstance(anyString(), any(CloudSessionCredential.class));
        CloudSessionCredential mockSession = mock(CloudSessionCredential.class);

        // When
        vmManagementPort.stopInstance("i-1234567890abcdef0", mockSession);

        // Then
        verify(vmManagementPort).stopInstance(eq("i-1234567890abcdef0"), any(CloudSessionCredential.class));
    }

    @Test
    void rebootInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).rebootInstance(anyString(), any(CloudSessionCredential.class));
        CloudSessionCredential mockSession = mock(CloudSessionCredential.class);

        // When
        vmManagementPort.rebootInstance("i-1234567890abcdef0", mockSession);

        // Then
        verify(vmManagementPort).rebootInstance(eq("i-1234567890abcdef0"), any(CloudSessionCredential.class));
    }

    @Test
    void terminateInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).terminateInstance(anyString(), any(CloudSessionCredential.class));
        CloudSessionCredential mockSession = mock(CloudSessionCredential.class);

        // When
        vmManagementPort.terminateInstance("i-1234567890abcdef0", mockSession);

        // Then
        verify(vmManagementPort).terminateInstance(eq("i-1234567890abcdef0"), any(CloudSessionCredential.class));
    }

    @Test
    void deleteInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).deleteInstance(any(VmDeleteCommand.class));

        // When
        vmManagementPort.deleteInstance(testDeleteCommand);

        // Then
        verify(vmManagementPort).deleteInstance(testDeleteCommand);
    }

    @Test
    void updateInstance_계약_테스트() {
        // Given
        doNothing().when(vmManagementPort).updateInstance(any(VmUpdateCommand.class));

        // When
        vmManagementPort.updateInstance(testUpdateCommand);

        // Then
        verify(vmManagementPort).updateInstance(testUpdateCommand);
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
    private interface VmManagementPortWithProvider extends VmCompositePort, ProviderScoped {
    }

    private interface VmCompositePort extends VmDiscoveryPort, VmLifecyclePort, VmTaggingPort {
    }
}
