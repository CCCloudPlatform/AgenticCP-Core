package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VpcManagementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VpcUseCaseService 단위 테스트
 * 
 * 외부 의존성을 Mock으로 대체하여 서비스 로직만 순수하게 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VpcUseCaseService 단위 테스트")
class VpcUseCaseServiceTest {

    @Mock
    private VpcPortRouter vpcPortRouter;

    @Mock
    private VpcManagementPort vpcManagementPort;

    @InjectMocks
    private VpcUseCaseService vpcUseCaseService;

    private VpcCreateRequest testRequest;
    private CloudResource testCloudResource;

    @BeforeEach
    void setUp() {
        testRequest = createTestVpcRequest();
        testCloudResource = createTestCloudResource();
    }

    @Test
    @DisplayName("정상적인 VPC 생성 요청 시 CloudResource를 반환한다")
    void createVpc_ValidRequest_ReturnsCloudResource() {
        // Given
        when(vpcPortRouter.getPort(ProviderType.AWS))
                .thenReturn(vpcManagementPort);
        when(vpcManagementPort.createVpc(testRequest))
                .thenReturn(testCloudResource);

        // When
        CloudResource result = vpcUseCaseService.createVpc(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getResourceId()).isEqualTo("vpc-12345678");
        assertThat(result.getResourceName()).isEqualTo("test-vpc");
        assertThat(result.getProvider()).isNotNull();
    }

    @Test
    @DisplayName("VPC 생성 시 포트 라우터가 올바른 포트를 반환하는지 확인한다")
    void createVpc_CallsPortRouterWithCorrectProvider() {
        // Given
        when(vpcPortRouter.getPort(ProviderType.AWS))
                .thenReturn(vpcManagementPort);
        when(vpcManagementPort.createVpc(testRequest))
                .thenReturn(testCloudResource);

        // When
        vpcUseCaseService.createVpc(testRequest);

        // Then
        verify(vpcPortRouter).getPort(ProviderType.AWS);
    }

    @Test
    @DisplayName("VPC 생성 시 포트의 createVpc 메서드가 호출되는지 확인한다")
    void createVpc_CallsPortCreateVpcMethod() {
        // Given
        when(vpcPortRouter.getPort(ProviderType.AWS))
                .thenReturn(vpcManagementPort);
        when(vpcManagementPort.createVpc(testRequest))
                .thenReturn(testCloudResource);

        // When
        vpcUseCaseService.createVpc(testRequest);

        // Then
        verify(vpcManagementPort).createVpc(testRequest);
    }

    @Test
    @DisplayName("포트 라우터에서 예외가 발생하면 예외가 전파된다")
    void createVpc_PortRouterThrowsException_PropagatesException() {
        // Given
        when(vpcPortRouter.getPort(ProviderType.AWS))
                .thenThrow(new IllegalArgumentException("Unsupported provider"));

        // When & Then
        assertThatThrownBy(() -> vpcUseCaseService.createVpc(testRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported provider");
    }

    @Test
    @DisplayName("포트에서 예외가 발생하면 예외가 전파된다")
    void createVpc_PortThrowsException_PropagatesException() {
        // Given
        when(vpcPortRouter.getPort(ProviderType.AWS))
                .thenReturn(vpcManagementPort);
        when(vpcManagementPort.createVpc(testRequest))
                .thenThrow(new RuntimeException("VPC creation failed"));

        // When & Then
        assertThatThrownBy(() -> vpcUseCaseService.createVpc(testRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("VPC creation failed");
    }

    @Test
    @DisplayName("다른 프로바이더 요청 시에도 올바른 포트가 호출된다")
    void createVpc_GcpProvider_CallsCorrectPort() {
        // Given
        VpcCreateRequest gcpRequest = VpcCreateRequest.builder()
                .providerType(ProviderType.GCP)
                .accountScope("my-project")
                .region("asia-northeast1")
                .vpcName("gcp-vpc")
                .cidrBlock("10.0.0.0/16")
                .build();

        when(vpcPortRouter.getPort(ProviderType.GCP))
                .thenReturn(vpcManagementPort);
        when(vpcManagementPort.createVpc(gcpRequest))
                .thenReturn(testCloudResource);

        // When
        vpcUseCaseService.createVpc(gcpRequest);

        // Then
        verify(vpcPortRouter).getPort(ProviderType.GCP);
        verify(vpcManagementPort).createVpc(gcpRequest);
    }

    @Test
    @DisplayName("포트 라우터가 null을 반환하면 NullPointerException이 발생한다")
    void createVpc_PortRouterReturnsNull_ThrowsNullPointerException() {
        // Given
        when(vpcPortRouter.getPort(ProviderType.AWS))
                .thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> vpcUseCaseService.createVpc(testRequest))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("포트가 null을 반환하면 null이 그대로 반환된다")
    void createVpc_PortReturnsNull_ReturnsNull() {
        // Given
        when(vpcPortRouter.getPort(ProviderType.AWS))
                .thenReturn(vpcManagementPort);
        when(vpcManagementPort.createVpc(testRequest))
                .thenReturn(null);

        // When
        CloudResource result = vpcUseCaseService.createVpc(testRequest);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("메서드 호출 순서가 올바른지 확인한다")
    void createVpc_CallsMethodsInCorrectOrder() {
        // Given
        when(vpcPortRouter.getPort(ProviderType.AWS))
                .thenReturn(vpcManagementPort);
        when(vpcManagementPort.createVpc(testRequest))
                .thenReturn(testCloudResource);

        // When
        vpcUseCaseService.createVpc(testRequest);

        // Then
        var inOrder = inOrder(vpcPortRouter, vpcManagementPort);
        inOrder.verify(vpcPortRouter).getPort(ProviderType.AWS);
        inOrder.verify(vpcManagementPort).createVpc(testRequest);
    }

    // ==================== 테스트 헬퍼 메서드 ====================

    private VpcCreateRequest createTestVpcRequest() {
        return VpcCreateRequest.builder()
                .providerType(ProviderType.AWS)
                .accountScope("123456789012")
                .region("us-east-1")
                .vpcName("test-vpc")
                .cidrBlock("10.0.0.0/16")
                .description("Test VPC")
                .tenantKey("tenant-001")
                .build();
    }

    private CloudResource createTestCloudResource() {
        CloudProvider awsProvider = CloudProvider.builder()
                .providerKey("aws")
                .providerName("Amazon Web Services")
                .providerType(ProviderType.AWS)
                .status(Status.ACTIVE)
                .build();
                
        return CloudResource.builder()
                .resourceId("vpc-12345678")
                .resourceName("test-vpc")
                .provider(awsProvider)
                .status(Status.ACTIVE)
                .build();
    }
}
