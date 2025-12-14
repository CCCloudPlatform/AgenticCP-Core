package com.agenticcp.core.domain.cloud.service.vpc;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.vpc.VpcManagementPort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * VpcUseCaseService DB 동기화 로직 단위 테스트
 * CSP 작업 후 CloudResource 엔티티가 올바르게 DB에 저장/삭제되는지 검증합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VpcUseCaseService DB 동기화 테스트")
class VpcUseCaseServiceDbSyncTest {

    @Mock
    private VpcPortRouter vpcPortRouter;

    @Mock
    private VpcManagementPort vpcManagementPort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    private VpcUseCaseService vpcUseCaseService;
    private CloudSessionCredential mockSession;

    private static final ProviderType PROVIDER_TYPE = ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String TENANT_KEY = "tenant-test";
    private static final String VPC_ID = "vpc-12345678";
    private static final String VPC_NAME = "my-test-vpc";
    private static final String CIDR_BLOCK = "10.0.0.0/16";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey(TENANT_KEY);
        
        vpcUseCaseService = new VpcUseCaseService(
                vpcPortRouter,
                capabilityGuard,
                accountCredentialManagementPort,
                resourceHelper
        );
        
        mockSession = mock(CloudSessionCredential.class);
        when(mockSession.getExpiresAt()).thenReturn(LocalDateTime.now().plusHours(1));

        // 공통 Mock 설정
        lenient().when(vpcPortRouter.getPort(PROVIDER_TYPE)).thenReturn(vpcManagementPort);
        lenient().when(accountCredentialManagementPort.getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(PROVIDER_TYPE)))
                .thenReturn(mockSession);
        lenient().doNothing().when(capabilityGuard).ensureSupported(any(), anyString(), anyString(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("VPC 생성 테스트")
    class CreateVpcTest {

        @Test
        @DisplayName("VPC 생성 성공 시 CloudResource가 DB에 저장된다")
        void createVpc_Success_SavesCloudResource() {
            // Given
            Map<String, String> tags = Map.of("Environment", "test");
            VpcCreateRequest request = VpcCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .vpcName(VPC_NAME)
                    .cidrBlock(CIDR_BLOCK)
                    .region("us-east-1")
                    .tags(tags)
                    .build();

            CloudResource mockCreatedVpc = CloudResource.builder()
                    .resourceId(VPC_ID)
                    .resourceName(VPC_NAME)
                    .build();

            when(vpcManagementPort.createVpc(any())).thenReturn(mockCreatedVpc);

            // When
            CloudResource result = vpcUseCaseService.createVpc(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo(VPC_ID);
            
            verify(resourceHelper).registerVpc(
                    eq(PROVIDER_TYPE),
                    eq("EC2"),
                    eq(VPC_ID),
                    eq(VPC_NAME),
                    eq(CIDR_BLOCK),
                    eq(tags)
            );
        }

        @Test
        @DisplayName("DB 저장 실패 시에도 CSP 생성은 성공한다")
        void createVpc_DbSaveFails_CspCreationSucceeds() {
            // Given
            VpcCreateRequest request = VpcCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .vpcName(VPC_NAME)
                    .cidrBlock(CIDR_BLOCK)
                    .region("us-east-1")
                    .build();

            CloudResource mockCreatedVpc = CloudResource.builder()
                    .resourceId(VPC_ID)
                    .resourceName(VPC_NAME)
                    .build();

            when(vpcManagementPort.createVpc(any())).thenReturn(mockCreatedVpc);
            // Helper 내부에서 예외 발생해도 경고 로그만 출력되고 계속 진행됨
            doThrow(new RuntimeException("DB 저장 실패")).when(resourceHelper)
                    .registerVpc(any(), any(), any(), any(), any(), any());

            // When
            CloudResource result = vpcUseCaseService.createVpc(request);

            // Then
            assertThat(result).isNotNull(); // CSP 생성은 성공
            assertThat(result.getResourceId()).isEqualTo(VPC_ID);
            verify(resourceHelper).registerVpc(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("VPC 이름이 없으면 VPC ID가 리소스 이름으로 사용된다")
        void createVpc_NoVpcName_UsesVpcIdAsResourceName() {
            // Given
            VpcCreateRequest request = VpcCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .vpcName(null) // VPC 이름 없음
                    .cidrBlock(CIDR_BLOCK)
                    .region("us-east-1")
                    .build();

            CloudResource mockCreatedVpc = CloudResource.builder()
                    .resourceId(VPC_ID)
                    .resourceName(VPC_ID)
                    .build();

            when(vpcManagementPort.createVpc(any())).thenReturn(mockCreatedVpc);

            // When
            vpcUseCaseService.createVpc(request);

            // Then
            verify(resourceHelper).registerVpc(
                    eq(PROVIDER_TYPE),
                    eq("EC2"),
                    eq(VPC_ID),
                    eq(VPC_ID),  // VPC ID가 이름으로 사용됨
                    eq(CIDR_BLOCK),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("VPC 삭제 테스트")
    class DeleteVpcTest {

        @Test
        @DisplayName("VPC 삭제 시 소프트 삭제가 수행된다")
        void deleteVpc_Success_SoftDeletesResource() {
            // Given
            ResourceIdentity vpcId = ResourceIdentity.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .providerResourceId(VPC_ID)
                    .region("us-east-1")
                    .build();

            doNothing().when(vpcManagementPort).deleteVpc(any());

            // When
            vpcUseCaseService.deleteVpc(vpcId);

            // Then
            verify(vpcManagementPort).deleteVpc(any());
            verify(resourceHelper).softDeleteResource(VPC_ID);
        }

        @Test
        @DisplayName("DB에 리소스가 없어도 CSP 삭제는 성공한다")
        void deleteVpc_ResourceNotInDb_CspDeletionSucceeds() {
            // Given
            ResourceIdentity vpcId = ResourceIdentity.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .providerResourceId(VPC_ID)
                    .region("us-east-1")
                    .build();

            doNothing().when(vpcManagementPort).deleteVpc(any());
            // Helper 내부에서 리소스가 없으면 로그만 출력하고 예외 발생 안함

            // When
            vpcUseCaseService.deleteVpc(vpcId);

            // Then
            verify(vpcManagementPort).deleteVpc(any()); // CSP 작업 성공
            verify(resourceHelper).softDeleteResource(VPC_ID);
        }
    }
}
