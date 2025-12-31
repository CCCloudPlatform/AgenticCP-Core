package com.agenticcp.core.domain.cloud.service.rdbms;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.RdbmsCreateRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsDeleteRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsUpdateRequest;
import com.agenticcp.core.domain.cloud.dto.ResourceRegistrationRequest;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudResource.LifecycleState;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsManagementPort;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RdbmsUseCaseService DB 동기화 로직 단위 테스트
 * CSP 작업 후 CloudResource 엔티티가 올바르게 DB에 저장/삭제되는지 검증합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RdbmsUseCaseService DB 동기화 테스트")
class RdbmsUseCaseServiceDbSyncTest {

    @Mock
    private RdbmsPortRouter portRouter;

    @Mock
    private RdbmsManagementPort managementPort;

    @Mock
    private RdbmsDiscoveryPort discoveryPort;

    @Mock
    private RdbmsLifecyclePort lifecyclePort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort credentialPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    @Mock
    private EncryptionService encryptionService;

    private RdbmsUseCaseService rdbmsUseCaseService;
    private CloudSessionCredential mockSession;

    private static final ProviderType PROVIDER_TYPE = ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String TENANT_KEY = "tenant-test";
    private static final String INSTANCE_ID = "test-instance-123";
    private static final String REGION = "us-east-1";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey(TENANT_KEY);
        
        rdbmsUseCaseService = new RdbmsUseCaseService(
                portRouter,
                capabilityGuard,
                credentialPort,
                resourceHelper,
                encryptionService
        );
        
        mockSession = mock(CloudSessionCredential.class);

        // 공통 Mock 설정
        lenient().when(portRouter.management(PROVIDER_TYPE)).thenReturn(managementPort);
        lenient().when(portRouter.discovery(PROVIDER_TYPE)).thenReturn(discoveryPort);
        lenient().when(portRouter.lifecycle(PROVIDER_TYPE)).thenReturn(lifecyclePort);
        lenient().when(credentialPort.getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(PROVIDER_TYPE)))
                .thenReturn(mockSession);
        lenient().doNothing().when(capabilityGuard).ensureSupported(any(), anyString(), anyString(), any());
        lenient().when(encryptionService.encrypt(anyString())).thenReturn("encrypted-password");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("인스턴스 생성 테스트")
    class CreateRdbmsTest {

        @Test
        @DisplayName("인스턴스 생성 성공 시 CloudResource가 DB에 저장된다")
        void createRdbms_Success_SavesCloudResource() {
            // Given
            Map<String, String> tags = Map.of("Environment", "test");
            RdbmsCreateRequest request = RdbmsCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .region(REGION)
                    .instanceName("test-instance")
                    .engine("mysql")
                    .engineVersion("8.0")
                    .instanceSize("db.t3.micro")
                    .allocatedStorage(20)
                    .masterUsername("admin")
                    .masterPassword("password123")
                    .tags(tags)
                    .build();

            CloudResource mockCreatedInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .build();

            when(managementPort.createRdbms(any())).thenReturn(mockCreatedInstance);
            when(resourceHelper.registerResource(any(), any(), any())).thenReturn(mockCreatedInstance);

            // When
            CloudResource result = rdbmsUseCaseService.createRdbms(request);

            // Then
            assertThat(result).isNotNull();
            
            verify(resourceHelper).registerResource(
                    eq(PROVIDER_TYPE),
                    eq("RDS"),
                    any(ResourceRegistrationRequest.class)
            );
        }

        @Test
        @DisplayName("DB 저장 실패 시 보상 트랜잭션이 실행되고 예외가 발생한다")
        void createRdbms_DbSaveFails_CompensatingTransactionExecuted() {
            // Given
            RdbmsCreateRequest request = RdbmsCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .region(REGION)
                    .instanceName("test-instance")
                    .engine("mysql")
                    .instanceSize("db.t3.micro")
                    .allocatedStorage(20)
                    .masterUsername("admin")
                    .masterPassword("password123")
                    .build();

            CloudResource mockCreatedInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .build();

            when(managementPort.createRdbms(any())).thenReturn(mockCreatedInstance);
            // DB 저장 실패
            doThrow(new RuntimeException("DB 저장 실패")).when(resourceHelper)
                    .registerResource(any(), any(), any());
            doNothing().when(managementPort).deleteRdbms(any());

            // When & Then
            assertThatThrownBy(() -> rdbmsUseCaseService.createRdbms(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.RESOURCE_CREATION_FAILED);
                    });

            // 보상 트랜잭션 실행 검증: CSP 인스턴스 삭제 호출됨
            verify(managementPort).deleteRdbms(any());
        }

        @Test
        @DisplayName("보상 트랜잭션도 실패하면 Ghost Resource 경고 로그가 출력된다")
        void createRdbms_CompensationFails_GhostResourceWarningLogged() {
            // Given
            RdbmsCreateRequest request = RdbmsCreateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .region(REGION)
                    .instanceName("test-instance")
                    .engine("mysql")
                    .instanceSize("db.t3.micro")
                    .allocatedStorage(20)
                    .masterUsername("admin")
                    .masterPassword("password123")
                    .build();

            CloudResource mockCreatedInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .build();

            when(managementPort.createRdbms(any())).thenReturn(mockCreatedInstance);
            // DB 저장 실패
            doThrow(new RuntimeException("DB 저장 실패")).when(resourceHelper)
                    .registerResource(any(), any(), any());
            // 보상 트랜잭션(CSP 삭제)도 실패
            doThrow(new RuntimeException("CSP 삭제 실패")).when(managementPort)
                    .deleteRdbms(any());

            // When & Then
            assertThatThrownBy(() -> rdbmsUseCaseService.createRdbms(request))
                    .isInstanceOf(BusinessException.class);

            // 보상 트랜잭션 시도 검증
            verify(managementPort).deleteRdbms(any());
            // Ghost Resource 발생 - 실제로는 모니터링/배치로 처리 필요
        }
    }

    @Nested
    @DisplayName("인스턴스 삭제 테스트")
    class DeleteRdbmsTest {

        @Test
        @DisplayName("인스턴스 삭제 시 소프트 삭제가 수행된다")
        void deleteRdbms_Success_SoftDeletesResource() {
            // Given
            RdbmsDeleteRequest request = RdbmsDeleteRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .instanceId(INSTANCE_ID)
                    .skipSnapshot(false)
                    .build();

            doNothing().when(managementPort).deleteRdbms(any());

            // When
            rdbmsUseCaseService.deleteRdbms(request);

            // Then
            verify(managementPort).deleteRdbms(any());
            verify(resourceHelper).softDeleteResource(INSTANCE_ID);
        }

        @Test
        @DisplayName("DB에 리소스가 없어도 CSP 삭제는 성공한다")
        void deleteRdbms_ResourceNotInDb_CspDeletionSucceeds() {
            // Given
            RdbmsDeleteRequest request = RdbmsDeleteRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .instanceId(INSTANCE_ID)
                    .skipSnapshot(true)
                    .build();

            doNothing().when(managementPort).deleteRdbms(any());
            // Helper 내부에서 리소스가 없으면 로그만 출력하고 예외 발생 안함

            // When
            rdbmsUseCaseService.deleteRdbms(request);

            // Then
            verify(managementPort).deleteRdbms(any()); // CSP 작업 성공
            verify(resourceHelper).softDeleteResource(INSTANCE_ID);
        }
    }

    @Nested
    @DisplayName("인스턴스 수정 테스트")
    class UpdateRdbmsTest {

        @Test
        @DisplayName("인스턴스 수정 시 생명주기 상태가 DB에 업데이트된다")
        void updateRdbms_Success_UpdatesLifecycleState() {
            // Given
            RdbmsUpdateRequest request = RdbmsUpdateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .instanceId(INSTANCE_ID)
                    .instanceSize("db.t3.small")
                    .allocatedStorage(50)
                    .build();

            CloudResource mockUpdatedInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .lifecycleState(LifecycleState.RUNNING)
                    .build();

            when(managementPort.updateRdbms(any())).thenReturn(mockUpdatedInstance);

            // When
            CloudResource result = rdbmsUseCaseService.updateRdbms(request);

            // Then
            assertThat(result).isNotNull();
            verify(managementPort).updateRdbms(any());
            verify(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.RUNNING);
        }

        @Test
        @DisplayName("DB 업데이트 실패해도 CSP 수정은 완료된다")
        void updateRdbms_DbUpdateFails_CspUpdateSucceeds() {
            // Given
            RdbmsUpdateRequest request = RdbmsUpdateRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .instanceId(INSTANCE_ID)
                    .instanceSize("db.t3.small")
                    .build();

            CloudResource mockUpdatedInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .lifecycleState(LifecycleState.RUNNING)
                    .build();

            when(managementPort.updateRdbms(any())).thenReturn(mockUpdatedInstance);
            // DB 업데이트 실패
            doThrow(new RuntimeException("DB 업데이트 실패")).when(resourceHelper)
                    .updateLifecycleState(anyString(), any());

            // When
            CloudResource result = rdbmsUseCaseService.updateRdbms(request);

            // Then
            assertThat(result).isNotNull();
            verify(managementPort).updateRdbms(any()); // CSP 수정은 성공
            verify(resourceHelper).updateLifecycleState(anyString(), any()); // DB 업데이트 시도
        }
    }

    @Nested
    @DisplayName("생명주기 관리 테스트")
    class LifecycleManagementTest {

        @Test
        @DisplayName("인스턴스 시작 시 생명주기 상태가 RUNNING으로 업데이트된다")
        void startInstance_Success_UpdatesLifecycleStateToRunning() {
            // Given
            CloudResource mockInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .build();

            when(discoveryPort.getRdbmsInstance(INSTANCE_ID, mockSession))
                    .thenReturn(java.util.Optional.of(mockInstance));
            doNothing().when(lifecyclePort).start(any(), eq(mockSession));

            // When
            rdbmsUseCaseService.startInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(lifecyclePort).start(any(), eq(mockSession));
            verify(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.RUNNING);
        }

        @Test
        @DisplayName("인스턴스 중지 시 생명주기 상태가 STOPPED로 업데이트된다")
        void stopInstance_Success_UpdatesLifecycleStateToStopped() {
            // Given
            CloudResource mockInstance = CloudResource.builder()
                    .resourceId(INSTANCE_ID)
                    .resourceName("test-instance")
                    .build();

            when(discoveryPort.getRdbmsInstance(INSTANCE_ID, mockSession))
                    .thenReturn(java.util.Optional.of(mockInstance));
            doNothing().when(lifecyclePort).stop(any(), eq(mockSession));

            // When
            rdbmsUseCaseService.stopInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(lifecyclePort).stop(any(), eq(mockSession));
            verify(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.STOPPED);
        }

        @Test
        @DisplayName("인스턴스 재시작 시 생명주기 상태가 RUNNING으로 유지된다")
        void rebootInstance_Success_MaintainsRunningState() {
            // Given
            doNothing().when(lifecyclePort).rebootInstance(INSTANCE_ID, mockSession);

            // When
            rdbmsUseCaseService.rebootInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, INSTANCE_ID);

            // Then
            verify(lifecyclePort).rebootInstance(INSTANCE_ID, mockSession);
            verify(resourceHelper).updateLifecycleState(INSTANCE_ID, LifecycleState.RUNNING);
        }
    }
}
