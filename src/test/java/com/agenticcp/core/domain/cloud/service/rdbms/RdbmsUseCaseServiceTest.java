package com.agenticcp.core.domain.cloud.service.rdbms;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.RdbmsCreateRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsDeleteRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsQueryRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsUpdateRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudResource.LifecycleState;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsManagementPort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RdbmsUseCaseService 단위 테스트
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RdbmsUseCaseService 테스트")
class RdbmsUseCaseServiceTest {

    @Mock
    private RdbmsPortRouter portRouter;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort credentialPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private RdbmsManagementPort managementPort;

    @Mock
    private RdbmsDiscoveryPort discoveryPort;

    @Mock
    private RdbmsLifecyclePort lifecyclePort;

    @InjectMocks
    private RdbmsUseCaseService rdbmsUseCaseService;

    private static final String TENANT_KEY = "test-tenant";
    private static final CloudProvider.ProviderType AWS = CloudProvider.ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String INSTANCE_ID = "test-instance-123";
    private static final String REGION = "us-east-1";

    private CloudSessionCredential mockSession;

    @BeforeEach
    void setUp() {
        lenient().when(portRouter.management(AWS)).thenReturn(managementPort);
        lenient().when(portRouter.discovery(AWS)).thenReturn(discoveryPort);
        lenient().when(portRouter.lifecycle(AWS)).thenReturn(lifecyclePort);

        mockSession = AwsSessionCredential.builder()
                .accessKeyId("AKIA_TEST")
                .secretAccessKey("secret")
                .sessionToken("token")
                .region(REGION)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Nested
    @DisplayName("RDBMS 인스턴스 목록 조회 테스트")
    class ListRdbmsInstancesTest {

        private RdbmsQueryRequest query;
        private Page<CloudResource> expectedPage;

        @BeforeEach
        void setUp() {
            query = RdbmsQueryRequest.builder()
                    .providerType(AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .page(0)
                    .size(10)
                    .instanceName("test")
                    .engine("mysql")
                    .build();

            CloudResource instance1 = CloudResource.builder()
                    .resourceId("instance-1")
                    .resourceName("test-instance-1")
                    .build();

            CloudResource instance2 = CloudResource.builder()
                    .resourceId("instance-2")
                    .resourceName("test-instance-2")
                    .build();

            expectedPage = new PageImpl<>(List.of(instance1, instance2), PageRequest.of(0, 10), 2);
        }

        @Test
        @DisplayName("정상적인 RDBMS 인스턴스 목록 조회")
        void listRdbmsInstances_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(discoveryPort.listRdbmsInstances(any(), eq(mockSession))).thenReturn(expectedPage);

                // When
                Page<CloudResource> result = rdbmsUseCaseService.listRdbmsInstances(AWS, ACCOUNT_SCOPE, query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getTotalElements()).isEqualTo(2);
                assertThat(result.getContent()).hasSize(2);
                assertThat(result.getContent().get(0).getResourceName()).isEqualTo("test-instance-1");

                verify(credentialPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(discoveryPort).listRdbmsInstances(any(), eq(mockSession));
            }
        }

        @Test
        @DisplayName("빈 목록 조회")
        void listRdbmsInstances_EmptyResult() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                Page<CloudResource> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
                when(discoveryPort.listRdbmsInstances(any(), eq(mockSession))).thenReturn(emptyPage);

                // When
                Page<CloudResource> result = rdbmsUseCaseService.listRdbmsInstances(AWS, ACCOUNT_SCOPE, query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getTotalElements()).isEqualTo(0);
                assertThat(result.getContent()).isEmpty();

                verify(discoveryPort).listRdbmsInstances(any(), eq(mockSession));
            }
        }
    }

    @Nested
    @DisplayName("RDBMS 인스턴스 조회 테스트")
    class GetRdbmsInstanceTest {

        private CloudResource expectedInstance;

        @BeforeEach
        void setUp() {
            expectedInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .displayName("Test RDBMS Instance")
                    .build();
        }

        @Test
        @DisplayName("정상적인 RDBMS 인스턴스 조회")
        void getRdbmsInstance_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(discoveryPort.getRdbmsInstance(INSTANCE_ID, mockSession))
                        .thenReturn(Optional.of(expectedInstance));

                // When
                Optional<CloudResource> result = rdbmsUseCaseService.getRdbmsInstance(AWS, ACCOUNT_SCOPE, INSTANCE_ID);

                // Then
                assertThat(result).isPresent();
                assertThat(result.get().getResourceId()).isEqualTo(INSTANCE_ID);
                assertThat(result.get().getResourceName()).isEqualTo("test-instance");

                verify(credentialPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(discoveryPort).getRdbmsInstance(INSTANCE_ID, mockSession);
            }
        }

        @Test
        @DisplayName("존재하지 않는 인스턴스 조회 시 Optional.empty() 반환")
        void getRdbmsInstance_NotFound_ReturnsEmpty() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(discoveryPort.getRdbmsInstance(INSTANCE_ID, mockSession)).thenReturn(Optional.empty());

                // When
                Optional<CloudResource> result = rdbmsUseCaseService.getRdbmsInstance(AWS, ACCOUNT_SCOPE, INSTANCE_ID);

                // Then
                assertThat(result).isEmpty();

                verify(discoveryPort).getRdbmsInstance(INSTANCE_ID, mockSession);
            }
        }
    }

    @Nested
    @DisplayName("RDBMS 인스턴스 생성 테스트")
    class CreateRdbmsTest {

        private RdbmsCreateRequest request;
        private CloudResource expectedInstance;

        @BeforeEach
        void setUp() {
            Map<String, String> tags = new HashMap<>();
            tags.put("Environment", "test");
            tags.put("Project", "agenticcp");

            request = RdbmsCreateRequest.builder()
                    .providerType(AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .region(REGION)
                    .instanceName("test-instance")
                    .engine("mysql")
                    .engineVersion("8.0")
                    .instanceSize("db.t3.micro")
                    .allocatedStorage(20)
                    .masterUsername("admin")
                    .masterPassword("password123")
                    .dbName("testdb")
                    .tags(tags)
                    .build();

            expectedInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .displayName("Test RDBMS Instance")
                    .build();
        }

        @Test
        @DisplayName("정상적인 RDBMS 인스턴스 생성")
        void createRdbms_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(encryptionService.encrypt("password123")).thenReturn("encrypted-password");
                when(managementPort.createRdbms(any())).thenReturn(expectedInstance);
                when(resourceHelper.registerResource(any(), any(), any())).thenReturn(expectedInstance);

                // When
                CloudResource result = rdbmsUseCaseService.createRdbms(request);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceId()).isEqualTo(INSTANCE_ID);
                assertThat(result.getResourceName()).isEqualTo("test-instance");

                verify(capabilityGuard).ensureSupported(AWS, "RDS", "DATABASE", CapabilityGuard.Operation.CREATE);
                verify(credentialPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(encryptionService).encrypt("password123");
                verify(managementPort).createRdbms(any());
                verify(resourceHelper).registerResource(eq(AWS), eq("RDS"), any());
            }
        }

        @Test
        @DisplayName("Capability 검증 실패 시 예외 발생")
        void createRdbms_CapabilityCheckFailed_ThrowsException() {
            // Given
            doThrow(new RuntimeException("Capability not supported"))
                    .when(capabilityGuard).ensureSupported(any(), any(), any(), any());

            // When & Then
            assertThatThrownBy(() -> rdbmsUseCaseService.createRdbms(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Capability not supported");

            verify(capabilityGuard).ensureSupported(AWS, "RDS", "DATABASE", CapabilityGuard.Operation.CREATE);
            verify(credentialPort, never()).getSession(any(), any(), any());
            verify(managementPort, never()).createRdbms(any());
        }

        @Test
        @DisplayName("암호화 실패 시 예외 발생")
        void createRdbms_EncryptionFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(encryptionService.encrypt("password123"))
                        .thenThrow(new RuntimeException("Encryption failed"));

                // When & Then
                assertThatThrownBy(() -> rdbmsUseCaseService.createRdbms(request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Encryption failed");

                verify(encryptionService).encrypt("password123");
                verify(managementPort, never()).createRdbms(any());
            }
        }

        @Test
        @DisplayName("DB 저장 실패 시 보상 트랜잭션 실행")
        void createRdbms_DbSaveFailed_ExecutesCompensatingTransaction() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(encryptionService.encrypt("password123")).thenReturn("encrypted-password");
                when(managementPort.createRdbms(any())).thenReturn(expectedInstance);
                when(resourceHelper.registerResource(any(), any(), any()))
                        .thenThrow(new RuntimeException("DB save failed"));
                doNothing().when(managementPort).deleteRdbms(any());

                // When & Then
                assertThatThrownBy(() -> rdbmsUseCaseService.createRdbms(request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("RDBMS 인스턴스 생성 후 DB 저장 실패");

                verify(managementPort).createRdbms(any());
                verify(resourceHelper).registerResource(any(), any(), any());
                verify(managementPort).deleteRdbms(any()); // 보상 트랜잭션 확인
            }
        }
    }

    @Nested
    @DisplayName("RDBMS 인스턴스 수정 테스트")
    class UpdateRdbmsTest {

        private RdbmsUpdateRequest request;
        private CloudResource expectedInstance;

        @BeforeEach
        void setUp() {
            Map<String, String> tags = new HashMap<>();
            tags.put("Environment", "production");
            tags.put("Updated", "true");

            request = RdbmsUpdateRequest.builder()
                    .providerType(AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .instanceId(INSTANCE_ID)
                    .instanceSize("db.t3.small")
                    .allocatedStorage(50)
                    .tagsToAdd(tags)
                    .build();

            expectedInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .displayName("Updated Test RDBMS Instance")
                    .build();
        }

        @Test
        @DisplayName("정상적인 RDBMS 인스턴스 수정")
        void updateRdbms_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(managementPort.updateRdbms(any())).thenReturn(expectedInstance);

                // When
                CloudResource result = rdbmsUseCaseService.updateRdbms(request);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceId()).isEqualTo(INSTANCE_ID);
                assertThat(result.getDisplayName()).isEqualTo("Updated Test RDBMS Instance");

                verify(capabilityGuard).ensureSupported(AWS, "RDS", "DATABASE", CapabilityGuard.Operation.UPDATE);
                verify(credentialPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(managementPort).updateRdbms(any());
            }
        }

        @Test
        @DisplayName("패스워드 변경 포함 수정")
        void updateRdbms_WithPasswordChange_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                request.setMasterPassword("newpassword123");
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(encryptionService.encrypt("newpassword123")).thenReturn("encrypted-new-password");
                when(managementPort.updateRdbms(any())).thenReturn(expectedInstance);

                // When
                CloudResource result = rdbmsUseCaseService.updateRdbms(request);

                // Then
                assertThat(result).isNotNull();
                verify(encryptionService).encrypt("newpassword123");
                verify(managementPort).updateRdbms(any());
            }
        }
    }

    @Nested
    @DisplayName("RDBMS 인스턴스 삭제 테스트")
    class DeleteRdbmsTest {

        private RdbmsDeleteRequest request;

        @BeforeEach
        void setUp() {
            request = RdbmsDeleteRequest.builder()
                    .providerType(AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .instanceId(INSTANCE_ID)
                    .skipSnapshot(false)
                    .snapshotName("final-snapshot")
                    .deleteAutomatedBackups(false)
                    .build();
        }

        @Test
        @DisplayName("정상적인 RDBMS 인스턴스 삭제")
        void deleteRdbms_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                doNothing().when(managementPort).deleteRdbms(any());
                doNothing().when(resourceHelper).softDeleteResource(INSTANCE_ID);

                // When
                rdbmsUseCaseService.deleteRdbms(request);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "RDS", "DATABASE", CapabilityGuard.Operation.TERMINATE);
                verify(credentialPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(managementPort).deleteRdbms(any());
                verify(resourceHelper).softDeleteResource(INSTANCE_ID);
            }
        }

        @Test
        @DisplayName("Capability 검증 실패 시 예외 발생")
        void deleteRdbms_CapabilityCheckFailed_ThrowsException() {
            // Given
            doThrow(new RuntimeException("Delete capability not supported"))
                    .when(capabilityGuard).ensureSupported(any(), any(), any(), any());

            // When & Then
            assertThatThrownBy(() -> rdbmsUseCaseService.deleteRdbms(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delete capability not supported");

            verify(capabilityGuard).ensureSupported(AWS, "RDS", "DATABASE", CapabilityGuard.Operation.TERMINATE);
            verify(credentialPort, never()).getSession(any(), any(), any());
            verify(managementPort, never()).deleteRdbms(any());
        }
    }

    @Nested
    @DisplayName("RDBMS 인스턴스 시작 테스트")
    class StartInstanceTest {

        private CloudResource mockInstance;

        @BeforeEach
        void setUp() {
            mockInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .build();
        }

        @Test
        @DisplayName("정상적인 RDBMS 인스턴스 시작")
        void startInstance_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(discoveryPort.getRdbmsInstance(INSTANCE_ID, mockSession))
                        .thenReturn(Optional.of(mockInstance));
                doNothing().when(lifecyclePort).start(any(ResourceIdentity.class), eq(mockSession));
                doNothing().when(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.RUNNING);

                // When
                rdbmsUseCaseService.startInstance(AWS, ACCOUNT_SCOPE, INSTANCE_ID);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "RDS", "DATABASE", CapabilityGuard.Operation.START);
                verify(credentialPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(discoveryPort).getRdbmsInstance(INSTANCE_ID, mockSession);
                verify(lifecyclePort).start(any(ResourceIdentity.class), eq(mockSession));
                verify(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.RUNNING);
            }
        }
    }

    @Nested
    @DisplayName("RDBMS 인스턴스 중지 테스트")
    class StopInstanceTest {

        private CloudResource mockInstance;

        @BeforeEach
        void setUp() {
            mockInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .build();
        }

        @Test
        @DisplayName("정상적인 RDBMS 인스턴스 중지")
        void stopInstance_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(discoveryPort.getRdbmsInstance(INSTANCE_ID, mockSession))
                        .thenReturn(Optional.of(mockInstance));
                doNothing().when(lifecyclePort).stop(any(ResourceIdentity.class), eq(mockSession));
                doNothing().when(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.STOPPED);

                // When
                rdbmsUseCaseService.stopInstance(AWS, ACCOUNT_SCOPE, INSTANCE_ID);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "RDS", "DATABASE", CapabilityGuard.Operation.STOP);
                verify(credentialPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(discoveryPort).getRdbmsInstance(INSTANCE_ID, mockSession);
                verify(lifecyclePort).stop(any(ResourceIdentity.class), eq(mockSession));
                verify(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.STOPPED);
            }
        }
    }

    @Nested
    @DisplayName("RDBMS 인스턴스 재시작 테스트")
    class RebootInstanceTest {

        @Test
        @DisplayName("정상적인 RDBMS 인스턴스 재시작")
        void rebootInstance_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                doNothing().when(lifecyclePort).rebootInstance(INSTANCE_ID, mockSession);
                doNothing().when(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.RUNNING);

                // When
                rdbmsUseCaseService.rebootInstance(AWS, ACCOUNT_SCOPE, INSTANCE_ID);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "RDS", "DATABASE", CapabilityGuard.Operation.STOP);
                verify(capabilityGuard).ensureSupported(AWS, "RDS", "DATABASE", CapabilityGuard.Operation.START);
                verify(credentialPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(lifecyclePort).rebootInstance(INSTANCE_ID, mockSession);
                verify(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.RUNNING);
            }
        }
    }

    @Nested
    @DisplayName("RDBMS 인스턴스 상태 확인 테스트")
    class GetInstanceStatusTest {

        @Test
        @DisplayName("정상적인 RDBMS 인스턴스 상태 확인")
        void getInstanceStatus_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(credentialPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(discoveryPort.getInstanceStatus(INSTANCE_ID, mockSession)).thenReturn("available");

                // When
                String result = rdbmsUseCaseService.getInstanceStatus(AWS, ACCOUNT_SCOPE, INSTANCE_ID);

                // Then
                assertThat(result).isEqualTo("available");

                verify(credentialPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(discoveryPort).getInstanceStatus(INSTANCE_ID, mockSession);
            }
        }
    }
}
