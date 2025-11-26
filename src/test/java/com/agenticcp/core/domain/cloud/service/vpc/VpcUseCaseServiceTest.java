package com.agenticcp.core.domain.cloud.service.vpc;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.vpc.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.vpc.VpcQueryRequest;
import com.agenticcp.core.domain.cloud.port.model.vpc.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.vpc.*;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;

import com.agenticcp.core.domain.cloud.port.outbound.vpc.VpcManagementPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * VpcUseCaseService 단위 테스트
 * 
 * 자격증명 관리 가이드라인을 준수하는 VPC UseCase 서비스의 테스트
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
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private VpcManagementPort vpcManagementPort;

    @InjectMocks
    private VpcUseCaseService vpcUseCaseService;

    private String tenantKey;
    private String accountScope;
    private ProviderType providerType;
    private CloudSessionCredential mockSession;
    private CloudResource mockVpcResource;

    @BeforeEach
    void setUp() {
        tenantKey = "test-tenant-key";
        accountScope = "123456789012";
        providerType = ProviderType.AWS;

        TenantContextHolder.setTenantKey(tenantKey);

        mockSession = createMockSession();
        mockVpcResource = createMockVpcResource();

        // 기본 Mock 설정
        lenient().doNothing().when(capabilityGuard).ensureSupported(
            any(ProviderType.class),
            anyString(),
            anyString(),
            any(CapabilityGuard.Operation.class)
        );

        lenient().when(vpcPortRouter.getPort(any(ProviderType.class)))
            .thenReturn(vpcManagementPort);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("VPC 생성 테스트")
    class CreateVpcTest {

        @Test
        @DisplayName("정상적인 VPC 생성")
        void createVpc_Success() {
            // Given
            VpcCreateRequest request = createVpcCreateRequest();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.createVpc(any(CreateVpcCommand.class)))
                .willReturn(mockVpcResource);

            // When
            CloudResource result = vpcUseCaseService.createVpc(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo("vpc-12345678");

            // 검증: 자격증명 관리 가이드라인 준수
            then(accountCredentialManagementPort).should(times(1)).getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            );
            then(capabilityGuard).should(times(1)).ensureSupported(
                eq(providerType),
                eq(VpcConstants.SERVICE_KEY),
                eq(VpcConstants.RESOURCE_TYPE),
                eq(CapabilityGuard.Operation.TAGGING)
            );
            then(vpcManagementPort).should(times(1)).createVpc(argThat(cmd ->
                cmd.accountScope().equals(accountScope) &&
                cmd.session() != null
            ));
        }

        @Test
        @DisplayName("accountScope가 null일 때 ACCOUNT_SCOPE_REQUIRED 예외 발생")
        void createVpc_NullAccountScope_ThrowsException() {
            // Given
            VpcCreateRequest request = VpcCreateRequest.builder()
                .providerType(providerType)
                .accountScope(null)
                .region("us-east-1")
                .vpcName("test-vpc")
                .cidrBlock("10.0.0.0/16")
                .build();

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.createVpc(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);

            // 검증: 세션 획득 호출되지 않음
            then(accountCredentialManagementPort).should(never()).getSession(
                anyString(),
                anyString(),
                any(ProviderType.class)
            );
        }

        @Test
        @DisplayName("accountScope가 빈 문자열일 때 ACCOUNT_SCOPE_REQUIRED 예외 발생")
        void createVpc_EmptyAccountScope_ThrowsException() {
            // Given
            VpcCreateRequest request = VpcCreateRequest.builder()
                .providerType(providerType)
                .accountScope("")
                .region("us-east-1")
                .vpcName("test-vpc")
                .cidrBlock("10.0.0.0/16")
                .build();

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.createVpc(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);

            then(accountCredentialManagementPort).should(never()).getSession(
                anyString(),
                anyString(),
                any(ProviderType.class)
            );
        }

        @Test
        @DisplayName("accountScope가 공백만 있을 때 ACCOUNT_SCOPE_REQUIRED 예외 발생")
        void createVpc_WhitespaceAccountScope_ThrowsException() {
            // Given
            VpcCreateRequest request = VpcCreateRequest.builder()
                .providerType(providerType)
                .accountScope("   ")
                .region("us-east-1")
                .vpcName("test-vpc")
                .cidrBlock("10.0.0.0/16")
                .build();

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.createVpc(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);

            then(accountCredentialManagementPort).should(never()).getSession(
                anyString(),
                anyString(),
                any(ProviderType.class)
            );
        }

        @Test
        @DisplayName("자격증명을 찾을 수 없을 때 ACCOUNT_NOT_CONFIGURED 예외 발생")
        void createVpc_CredentialNotFound_ThrowsException() {
            // Given
            VpcCreateRequest request = createVpcCreateRequest();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willThrow(new BusinessException(
                CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                "자격증명을 찾을 수 없습니다"
            ));

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.createVpc(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_NOT_CONFIGURED);

            // 검증: getSession 호출 확인
            then(accountCredentialManagementPort).should(times(1)).getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            );
            // 검증: VPC 생성 호출되지 않음
            then(vpcManagementPort).should(never()).createVpc(any(CreateVpcCommand.class));
        }

        @Test
        @DisplayName("CapabilityGuard 검증 실패 시 UNSUPPORTED_OPERATION 예외 발생")
        void createVpc_CapabilityNotSupported_ThrowsException() {
            // Given
            VpcCreateRequest request = createVpcCreateRequest();

            doThrow(new BusinessException(CloudErrorCode.UNSUPPORTED_OPERATION))
                .when(capabilityGuard).ensureSupported(
                    any(ProviderType.class),
                    anyString(),
                    anyString(),
                    any(CapabilityGuard.Operation.class)
                );

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.createVpc(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.UNSUPPORTED_OPERATION);

            // 검증: 세션 획득 호출되지 않음
            then(accountCredentialManagementPort).should(never()).getSession(
                anyString(),
                anyString(),
                any(ProviderType.class)
            );
        }

        @Test
        @DisplayName("VpcManagementPort에서 예외 발생 시 예외 전파")
        void createVpc_VpcPortException_PropagatesException() {
            // Given
            VpcCreateRequest request = createVpcCreateRequest();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.createVpc(any(CreateVpcCommand.class)))
                .willThrow(new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED, "VPC 생성 실패"));

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.createVpc(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.CLOUD_CONNECTION_FAILED);
        }
    }

    @Nested
    @DisplayName("VPC 조회 테스트")
    class GetVpcTest {

        @Test
        @DisplayName("정상적인 VPC 조회")
        void getVpc_Success() {
            // Given
            ResourceIdentity vpcId = createResourceIdentity();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.getVpc(any(GetVpcCommand.class)))
                .willReturn(Optional.of(mockVpcResource));

            // When
            Optional<CloudResource> result = vpcUseCaseService.getVpc(vpcId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getResourceId()).isEqualTo("vpc-12345678");

            // 검증
            then(accountCredentialManagementPort).should(times(1)).getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            );
            then(vpcManagementPort).should(times(1)).getVpc(any(GetVpcCommand.class));
        }

        @Test
        @DisplayName("accountScope가 null일 때 ACCOUNT_SCOPE_REQUIRED 예외 발생")
        void getVpc_NullAccountScope_ThrowsException() {
            // Given
            ResourceIdentity vpcId = ResourceIdentity.builder()
                .providerType(providerType)
                .accountScope(null)
                .region("us-east-1")
                .providerResourceId("vpc-12345678")
                .build();

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.getVpc(vpcId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);

            then(accountCredentialManagementPort).should(never()).getSession(
                anyString(),
                anyString(),
                any(ProviderType.class)
            );
        }

        @Test
        @DisplayName("자격증명을 찾을 수 없을 때 ACCOUNT_NOT_CONFIGURED 예외 발생")
        void getVpc_CredentialNotFound_ThrowsException() {
            // Given
            ResourceIdentity vpcId = createResourceIdentity();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willThrow(new BusinessException(
                CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                "자격증명을 찾을 수 없습니다"
            ));

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.getVpc(vpcId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_NOT_CONFIGURED);
        }

        @Test
        @DisplayName("VPC가 존재하지 않을 때 Optional.empty() 반환")
        void getVpc_NotFound_ReturnsEmpty() {
            // Given
            ResourceIdentity vpcId = createResourceIdentity();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.getVpc(any(GetVpcCommand.class)))
                .willReturn(Optional.empty());

            // When
            Optional<CloudResource> result = vpcUseCaseService.getVpc(vpcId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("VPC 목록 조회 테스트")
    class ListVpcsTest {

        @Test
        @DisplayName("정상적인 VPC 목록 조회")
        void listVpcs_Success() {
            // Given
            VpcQueryRequest query = createVpcQuery();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.listVpcs(any(ListVpcsQueryRequest.class)))
                .willReturn(List.of(mockVpcResource));

            // When
            List<CloudResource> result = vpcUseCaseService.listVpcs(query);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getResourceId()).isEqualTo("vpc-12345678");

            // 검증
            then(accountCredentialManagementPort).should(times(1)).getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            );
            then(vpcManagementPort).should(times(1)).listVpcs(any(ListVpcsQueryRequest.class));
        }

        @Test
        @DisplayName("accountScope가 null일 때 ACCOUNT_SCOPE_REQUIRED 예외 발생")
        void listVpcs_NullAccountScope_ThrowsException() {
            // Given
            VpcQueryRequest query = VpcQueryRequest.builder()
                .providerType(providerType)
                .accountScope(null)
                .region("us-east-1")
                .build();

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.listVpcs(query))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);

            then(accountCredentialManagementPort).should(never()).getSession(
                anyString(),
                anyString(),
                any(ProviderType.class)
            );
        }

        @Test
        @DisplayName("자격증명을 찾을 수 없을 때 ACCOUNT_NOT_CONFIGURED 예외 발생")
        void listVpcs_CredentialNotFound_ThrowsException() {
            // Given
            VpcQueryRequest query = createVpcQuery();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willThrow(new BusinessException(
                CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                "자격증명을 찾을 수 없습니다"
            ));

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.listVpcs(query))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_NOT_CONFIGURED);
        }

        @Test
        @DisplayName("빈 VPC 목록 반환")
        void listVpcs_EmptyList_ReturnsEmptyList() {
            // Given
            VpcQueryRequest query = createVpcQuery();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.listVpcs(any(ListVpcsQueryRequest.class)))
                .willReturn(List.of());

            // When
            List<CloudResource> result = vpcUseCaseService.listVpcs(query);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("VPC 수정 테스트")
    class UpdateVpcTest {

        @Test
        @DisplayName("정상적인 VPC 수정")
        void updateVpc_Success() {
            // Given
            ResourceIdentity vpcId = createResourceIdentity();
            VpcUpdateRequest request = createVpcUpdateRequest();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.updateVpc(any(UpdateVpcCommand.class)))
                .willReturn(mockVpcResource);

            // When
            CloudResource result = vpcUseCaseService.updateVpc(vpcId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceId()).isEqualTo("vpc-12345678");

            // 검증
            then(accountCredentialManagementPort).should(times(1)).getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            );
            then(capabilityGuard).should(times(1)).ensureSupported(
                eq(providerType),
                eq(VpcConstants.SERVICE_KEY),
                eq(VpcConstants.RESOURCE_TYPE),
                eq(CapabilityGuard.Operation.TAGGING)
            );
            then(vpcManagementPort).should(times(1)).updateVpc(any(UpdateVpcCommand.class));
        }

        @Test
        @DisplayName("accountScope가 null일 때 ACCOUNT_SCOPE_REQUIRED 예외 발생")
        void updateVpc_NullAccountScope_ThrowsException() {
            // Given
            ResourceIdentity vpcId = ResourceIdentity.builder()
                .providerType(providerType)
                .accountScope(null)
                .region("us-east-1")
                .providerResourceId("vpc-12345678")
                .build();
            VpcUpdateRequest request = createVpcUpdateRequest();

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.updateVpc(vpcId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);

            then(accountCredentialManagementPort).should(never()).getSession(
                anyString(),
                anyString(),
                any(ProviderType.class)
            );
        }

        @Test
        @DisplayName("자격증명을 찾을 수 없을 때 ACCOUNT_NOT_CONFIGURED 예외 발생")
        void updateVpc_CredentialNotFound_ThrowsException() {
            // Given
            ResourceIdentity vpcId = createResourceIdentity();
            VpcUpdateRequest request = createVpcUpdateRequest();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willThrow(new BusinessException(
                CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                "자격증명을 찾을 수 없습니다"
            ));

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.updateVpc(vpcId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_NOT_CONFIGURED);
        }

        @Test
        @DisplayName("CapabilityGuard 검증 실패 시 UNSUPPORTED_OPERATION 예외 발생")
        void updateVpc_CapabilityNotSupported_ThrowsException() {
            // Given
            ResourceIdentity vpcId = createResourceIdentity();
            VpcUpdateRequest request = createVpcUpdateRequest();

            doThrow(new BusinessException(CloudErrorCode.UNSUPPORTED_OPERATION))
                .when(capabilityGuard).ensureSupported(
                    any(ProviderType.class),
                    anyString(),
                    anyString(),
                    any(CapabilityGuard.Operation.class)
                );

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.updateVpc(vpcId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.UNSUPPORTED_OPERATION);
        }
    }

    @Nested
    @DisplayName("VPC 삭제 테스트")
    class DeleteVpcTest {

        @Test
        @DisplayName("정상적인 VPC 삭제")
        void deleteVpc_Success() {
            // Given
            ResourceIdentity vpcId = createResourceIdentity();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            doNothing().when(vpcManagementPort).deleteVpc(any(DeleteVpcCommand.class));

            // When
            vpcUseCaseService.deleteVpc(vpcId);

            // Then
            // 검증
            then(accountCredentialManagementPort).should(times(1)).getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            );
            then(capabilityGuard).should(times(1)).ensureSupported(
                eq(providerType),
                eq(VpcConstants.SERVICE_KEY),
                eq(VpcConstants.RESOURCE_TYPE),
                eq(CapabilityGuard.Operation.TAGGING)
            );
            then(vpcManagementPort).should(times(1)).deleteVpc(any(DeleteVpcCommand.class));
        }

        @Test
        @DisplayName("accountScope가 null일 때 ACCOUNT_SCOPE_REQUIRED 예외 발생")
        void deleteVpc_NullAccountScope_ThrowsException() {
            // Given
            ResourceIdentity vpcId = ResourceIdentity.builder()
                .providerType(providerType)
                .accountScope(null)
                .region("us-east-1")
                .providerResourceId("vpc-12345678")
                .build();

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.deleteVpc(vpcId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_SCOPE_REQUIRED);

            then(accountCredentialManagementPort).should(never()).getSession(
                anyString(),
                anyString(),
                any(ProviderType.class)
            );
        }

        @Test
        @DisplayName("자격증명을 찾을 수 없을 때 ACCOUNT_NOT_CONFIGURED 예외 발생")
        void deleteVpc_CredentialNotFound_ThrowsException() {
            // Given
            ResourceIdentity vpcId = createResourceIdentity();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willThrow(new BusinessException(
                CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                "자격증명을 찾을 수 없습니다"
            ));

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.deleteVpc(vpcId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.ACCOUNT_NOT_CONFIGURED);
        }

        @Test
        @DisplayName("CapabilityGuard 검증 실패 시 UNSUPPORTED_OPERATION 예외 발생")
        void deleteVpc_CapabilityNotSupported_ThrowsException() {
            // Given
            ResourceIdentity vpcId = createResourceIdentity();

            doThrow(new BusinessException(CloudErrorCode.UNSUPPORTED_OPERATION))
                .when(capabilityGuard).ensureSupported(
                    any(ProviderType.class),
                    anyString(),
                    anyString(),
                    any(CapabilityGuard.Operation.class)
                );

            // When & Then
            assertThatThrownBy(() -> vpcUseCaseService.deleteVpc(vpcId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CloudErrorCode.UNSUPPORTED_OPERATION);
        }
    }

    @Nested
    @DisplayName("자격증명 관리 가이드라인 준수 테스트")
    class CredentialManagementGuidelineTest {

        @Test
        @DisplayName("TenantContextHolder를 통해서만 tenantKey를 획득하는지 확인")
        void createVpc_UsesTenantContextHolder() {
            // Given
            VpcCreateRequest request = createVpcCreateRequest();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.createVpc(any(CreateVpcCommand.class)))
                .willReturn(mockVpcResource);

            // When
            vpcUseCaseService.createVpc(request);

            // Then
            // TenantContextHolder의 getCurrentTenantKeyOrThrow()가 호출되는지 확인
            // (간접적으로 검증: accountCredentialManagementPort.getSession이
            //  setUp()에서 설정한 tenantKey로 호출되었는지 확인)
            then(accountCredentialManagementPort).should(times(1)).getSession(
                eq(tenantKey),  // TenantContextHolder에서 획득한 tenantKey
                eq(accountScope),
                eq(providerType)
            );
        }

        @Test
        @DisplayName("accountScope를 String 타입으로 직접 사용하는지 확인")
        void createVpc_UsesAccountScopeDirectly() {
            // Given
            VpcCreateRequest request = createVpcCreateRequest();
            String expectedAccountScope = request.getAccountScope();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(expectedAccountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.createVpc(any(CreateVpcCommand.class)))
                .willReturn(mockVpcResource);

            // When
            vpcUseCaseService.createVpc(request);

            // Then
            // accountScope가 변환 없이 직접 사용되는지 확인
            then(accountCredentialManagementPort).should(times(1)).getSession(
                eq(tenantKey),
                eq(expectedAccountScope),  // String 타입 그대로 전달
                eq(providerType)
            );

            then(vpcManagementPort).should(times(1)).createVpc(argThat(cmd ->
                cmd.accountScope().equals(expectedAccountScope)
            ));
        }

        @Test
        @DisplayName("AccountCredentialManagementPort.getSession()을 올바르게 호출하는지 확인")
        void createVpc_CallsGetSessionCorrectly() {
            // Given
            VpcCreateRequest request = createVpcCreateRequest();

            given(accountCredentialManagementPort.getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            )).willReturn(mockSession);

            given(vpcManagementPort.createVpc(any(CreateVpcCommand.class)))
                .willReturn(mockVpcResource);

            // When
            vpcUseCaseService.createVpc(request);

            // Then
            then(accountCredentialManagementPort).should(times(1)).getSession(
                eq(tenantKey),
                eq(accountScope),
                eq(providerType)
            );
            verifyNoMoreInteractions(accountCredentialManagementPort);
        }
    }

    // ========== Helper 메서드 ==========

    private CloudSessionCredential createMockSession() {
        return AwsSessionCredential.builder()
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
            .sessionToken("session-token-example")
            .region("us-east-1")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .build();
    }

    private CloudResource createMockVpcResource() {
        CloudResource resource = CloudResource.builder()
            .resourceId("vpc-12345678")
            .resourceName("test-vpc")
            .resourceType(CloudResource.ResourceType.NETWORK)
            .lifecycleState(CloudResource.LifecycleState.RUNNING)
            .build();
        return resource;
    }

    private VpcCreateRequest createVpcCreateRequest() {
        return VpcCreateRequest.builder()
            .providerType(providerType)
            .accountScope(accountScope)
            .region("us-east-1")
            .vpcName("test-vpc")
            .cidrBlock("10.0.0.0/16")
            .description("Test VPC")
            .tags(Map.of("Environment", "Test"))
            .build();
    }

    private ResourceIdentity createResourceIdentity() {
        return ResourceIdentity.builder()
            .providerType(providerType)
            .accountScope(accountScope)
            .region("us-east-1")
            .providerResourceId("vpc-12345678")
            .serviceKey(VpcConstants.SERVICE_KEY)
            .resourceType(VpcConstants.RESOURCE_TYPE)
            .build();
    }

    private VpcQueryRequest createVpcQuery() {
        return VpcQueryRequest.builder()
            .providerType(providerType)
            .accountScope(accountScope)
            .region("us-east-1")
            .vpcName("test-vpc")
            .cidrBlock("10.0.0.0/16")
            .build();
    }

    private VpcUpdateRequest createVpcUpdateRequest() {
        return VpcUpdateRequest.builder()
            .vpcName("updated-vpc")
            .description("Updated VPC Description")
            .tags(Map.of("Environment", "Production"))
            .build();
    }
}

