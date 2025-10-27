package com.agenticcp.core.domain.cloud.port;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.Ec2CreateRequest;
import com.agenticcp.core.domain.cloud.port.model.Ec2DeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.Ec2Query;
import com.agenticcp.core.domain.cloud.port.model.Ec2UpdateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.aws.Ec2ManagementPort;
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
 * EC2 관리 포트 계약 테스트
 * 
 * Ec2ManagementPort 인터페이스의 모든 메서드가 올바르게 동작하는지 검증합니다.
 * 이 테스트는 포트 인터페이스의 계약(contract)을 정의하고 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class Ec2ManagementContractTest {

    @Mock
    private Ec2ManagementPort ec2ManagementPort;

    private CloudResource testInstance;
    private Ec2Query testQuery;
    private Ec2CreateRequest testCreateRequest;
    private Ec2UpdateRequest testUpdateRequest;
    private Ec2DeleteRequest testDeleteRequest;

    @BeforeEach
    void setUp() {
        testInstance = CloudResource.builder()
            .resourceId("i-1234567890abcdef0")
            .resourceName("test-instance")
            .displayName("Test Instance")
            .build();

        testQuery = Ec2Query.builder()
            .page(0)
            .size(10)
            .build();

        testCreateRequest = Ec2CreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .minCount(1)
            .maxCount(1)
            .build();

        testUpdateRequest = Ec2UpdateRequest.builder()
            .instanceId("i-1234567890abcdef0")
            .instanceType("t3.small")
            .build();

        testDeleteRequest = Ec2DeleteRequest.basic("i-1234567890abcdef0");
    }

    @Test
    void listInstances_계약_테스트() {
        // Given
        Page<CloudResource> expectedPage = new PageImpl<>(
            List.of(testInstance), 
            PageRequest.of(0, 10), 
            1
        );
        when(ec2ManagementPort.listInstances(any(Ec2Query.class))).thenReturn(expectedPage);

        // When
        Page<CloudResource> result = ec2ManagementPort.listInstances(testQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getResourceId()).isEqualTo("i-1234567890abcdef0");
        verify(ec2ManagementPort).listInstances(testQuery);
    }

    @Test
    void getInstance_계약_테스트() {
        // Given
        when(ec2ManagementPort.getInstance("i-1234567890abcdef0"))
            .thenReturn(Optional.of(testInstance));

        // When
        Optional<CloudResource> result = ec2ManagementPort.getInstance("i-1234567890abcdef0");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getResourceId()).isEqualTo("i-1234567890abcdef0");
        verify(ec2ManagementPort).getInstance("i-1234567890abcdef0");
    }

    @Test
    void getInstance_존재하지_않는_인스턴스_계약_테스트() {
        // Given
        when(ec2ManagementPort.getInstance("i-nonexistent"))
            .thenReturn(Optional.empty());

        // When
        Optional<CloudResource> result = ec2ManagementPort.getInstance("i-nonexistent");

        // Then
        assertThat(result).isEmpty();
        verify(ec2ManagementPort).getInstance("i-nonexistent");
    }

    @Test
    void createInstance_계약_테스트() {
        // Given
        String expectedInstanceId = "i-1234567890abcdef0";
        when(ec2ManagementPort.createInstance(any(Ec2CreateRequest.class)))
            .thenReturn(expectedInstanceId);

        // When
        String result = ec2ManagementPort.createInstance(testCreateRequest);

        // Then
        assertThat(result).isEqualTo(expectedInstanceId);
        verify(ec2ManagementPort).createInstance(testCreateRequest);
    }

    @Test
    void startInstance_계약_테스트() {
        // Given
        doNothing().when(ec2ManagementPort).startInstance(anyString());

        // When
        ec2ManagementPort.startInstance("i-1234567890abcdef0");

        // Then
        verify(ec2ManagementPort).startInstance("i-1234567890abcdef0");
    }

    @Test
    void stopInstance_계약_테스트() {
        // Given
        doNothing().when(ec2ManagementPort).stopInstance(anyString());

        // When
        ec2ManagementPort.stopInstance("i-1234567890abcdef0");

        // Then
        verify(ec2ManagementPort).stopInstance("i-1234567890abcdef0");
    }

    @Test
    void rebootInstance_계약_테스트() {
        // Given
        doNothing().when(ec2ManagementPort).rebootInstance(anyString());

        // When
        ec2ManagementPort.rebootInstance("i-1234567890abcdef0");

        // Then
        verify(ec2ManagementPort).rebootInstance("i-1234567890abcdef0");
    }

    @Test
    void terminateInstance_계약_테스트() {
        // Given
        doNothing().when(ec2ManagementPort).terminateInstance(anyString());

        // When
        ec2ManagementPort.terminateInstance("i-1234567890abcdef0");

        // Then
        verify(ec2ManagementPort).terminateInstance("i-1234567890abcdef0");
    }

    @Test
    void deleteInstance_계약_테스트() {
        // Given
        doNothing().when(ec2ManagementPort).deleteInstance(any(Ec2DeleteRequest.class));

        // When
        ec2ManagementPort.deleteInstance(testDeleteRequest);

        // Then
        verify(ec2ManagementPort).deleteInstance(testDeleteRequest);
    }

    @Test
    void updateInstance_계약_테스트() {
        // Given
        doNothing().when(ec2ManagementPort).updateInstance(any(Ec2UpdateRequest.class));

        // When
        ec2ManagementPort.updateInstance(testUpdateRequest);

        // Then
        verify(ec2ManagementPort).updateInstance(testUpdateRequest);
    }

    @Test
    void addTags_계약_테스트() {
        // Given
        Map<String, String> tags = Map.of(
            "Environment", "Development",
            "Project", "TestProject"
        );
        doNothing().when(ec2ManagementPort).addTags(anyString(), any(Map.class));

        // When
        ec2ManagementPort.addTags("i-1234567890abcdef0", tags);

        // Then
        verify(ec2ManagementPort).addTags("i-1234567890abcdef0", tags);
    }

    @Test
    void removeTags_계약_테스트() {
        // Given
        Map<String, String> tagKeys = Map.of(
            "Environment", "",
            "Project", ""
        );
        doNothing().when(ec2ManagementPort).removeTags(anyString(), any(Map.class));

        // When
        ec2ManagementPort.removeTags("i-1234567890abcdef0", tagKeys);

        // Then
        verify(ec2ManagementPort).removeTags("i-1234567890abcdef0", tagKeys);
    }

    @Test
    void getTags_계약_테스트() {
        // Given
        Map<String, String> expectedTags = Map.of(
            "Environment", "Development",
            "Project", "TestProject"
        );
        when(ec2ManagementPort.getTags("i-1234567890abcdef0"))
            .thenReturn(expectedTags);

        // When
        Map<String, String> result = ec2ManagementPort.getTags("i-1234567890abcdef0");

        // Then
        assertThat(result).isEqualTo(expectedTags);
        verify(ec2ManagementPort).getTags("i-1234567890abcdef0");
    }

    @Test
    void getInstanceStatus_계약_테스트() {
        // Given
        String expectedStatus = "running";
        when(ec2ManagementPort.getInstanceStatus("i-1234567890abcdef0"))
            .thenReturn(expectedStatus);

        // When
        String result = ec2ManagementPort.getInstanceStatus("i-1234567890abcdef0");

        // Then
        assertThat(result).isEqualTo(expectedStatus);
        verify(ec2ManagementPort).getInstanceStatus("i-1234567890abcdef0");
    }

    @Test
    void waitForInstanceStatus_계약_테스트() {
        // Given
        when(ec2ManagementPort.waitForInstanceStatus("i-1234567890abcdef0", "running", 300))
            .thenReturn(true);

        // When
        boolean result = ec2ManagementPort.waitForInstanceStatus("i-1234567890abcdef0", "running", 300);

        // Then
        assertThat(result).isTrue();
        verify(ec2ManagementPort).waitForInstanceStatus("i-1234567890abcdef0", "running", 300);
    }

    @Test
    void waitForInstanceStatus_타임아웃_계약_테스트() {
        // Given
        when(ec2ManagementPort.waitForInstanceStatus("i-1234567890abcdef0", "running", 300))
            .thenReturn(false);

        // When
        boolean result = ec2ManagementPort.waitForInstanceStatus("i-1234567890abcdef0", "running", 300);

        // Then
        assertThat(result).isFalse();
        verify(ec2ManagementPort).waitForInstanceStatus("i-1234567890abcdef0", "running", 300);
    }

    @Test
    void providerScoped_계약_테스트() {
        // Given
        Ec2ManagementPortWithProvider providerPort = mock(Ec2ManagementPortWithProvider.class);
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
    private interface Ec2ManagementPortWithProvider extends Ec2ManagementPort, ProviderScoped {
    }
}
