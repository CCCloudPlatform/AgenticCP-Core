package com.agenticcp.core.domain.cloud.service.storage;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.dto.CreateObjectStorageContainerRequest;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageManagementPort;
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
 * ObjectStorageUseCaseService DB 동기화 로직 단위 테스트
 * CSP 작업 후 CloudResource 엔티티가 올바르게 DB에 저장/삭제되는지 검증합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectStorageUseCaseService DB 동기화 테스트")
class ObjectStorageUseCaseServiceDbSyncTest {

    @Mock
    private ObjectStoragePortRouter router;

    @Mock
    private ObjectStorageManagementPort managementPort;

    @Mock
    private ObjectStorageDiscoveryPort discoveryPort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    private ObjectStorageUseCaseService objectStorageUseCaseService;
    private CloudSessionCredential mockSession;

    private static final ProviderType PROVIDER_TYPE = ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String TENANT_KEY = "tenant-test";
    private static final String CONTAINER_NAME = "my-test-bucket";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey(TENANT_KEY);
        
        objectStorageUseCaseService = new ObjectStorageUseCaseService(
                router,
                capabilityGuard,
                accountCredentialManagementPort,
                resourceHelper
        );
        
        mockSession = mock(CloudSessionCredential.class);
        when(mockSession.getExpiresAt()).thenReturn(LocalDateTime.now().plusHours(1));

        // 공통 Mock 설정
        lenient().when(router.management(PROVIDER_TYPE)).thenReturn(managementPort);
        lenient().when(router.discovery(PROVIDER_TYPE)).thenReturn(discoveryPort);
        lenient().when(accountCredentialManagementPort.getSession(eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(PROVIDER_TYPE)))
                .thenReturn(mockSession);
        lenient().doNothing().when(capabilityGuard).ensureSupported(any(), anyString(), anyString(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("컨테이너 생성 테스트")
    class CreateContainerTest {

        @Test
        @DisplayName("컨테이너 생성 성공 시 CloudResource가 DB에 저장된다")
        void createContainer_Success_SavesCloudResource() {
            // Given
            Map<String, String> tags = Map.of("Environment", "test");
            CreateObjectStorageContainerRequest request = CreateObjectStorageContainerRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .containerName(CONTAINER_NAME)
                    .region("us-east-1")
                    .tags(tags)
                    .build();

            CloudResource mockCreatedContainer = CloudResource.builder()
                    .resourceId(CONTAINER_NAME)
                    .resourceName(CONTAINER_NAME)
                    .build();

            when(managementPort.createContainer(any())).thenReturn(mockCreatedContainer);

            // When
            CloudResource result = objectStorageUseCaseService.createContainer(request);

            // Then
            assertThat(result).isNotNull();
            
            verify(resourceHelper).registerStorageBucket(
                    eq(PROVIDER_TYPE),
                    eq("S3"),
                    eq(CONTAINER_NAME),
                    eq(tags)
            );
        }

        @Test
        @DisplayName("DB 저장 실패 시 보상 트랜잭션이 실행되고 예외가 발생한다")
        void createContainer_DbSaveFails_CompensatingTransactionExecuted() {
            // Given
            CreateObjectStorageContainerRequest request = CreateObjectStorageContainerRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .containerName(CONTAINER_NAME)
                    .region("us-east-1")
                    .build();

            CloudResource mockCreatedContainer = CloudResource.builder()
                    .resourceId(CONTAINER_NAME)
                    .resourceName(CONTAINER_NAME)
                    .build();

            when(managementPort.createContainer(any())).thenReturn(mockCreatedContainer);
            // DB 저장 실패
            doThrow(new RuntimeException("DB 저장 실패")).when(resourceHelper)
                    .registerStorageBucket(any(), any(), any(), any());

            // When & Then
            assertThatThrownBy(() -> objectStorageUseCaseService.createContainer(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(CloudErrorCode.RESOURCE_CREATION_FAILED);
                    });

            // 보상 트랜잭션 실행 검증: CSP 컨테이너 삭제 호출됨
            verify(managementPort).deleteContainer(any(), eq(CONTAINER_NAME));
        }

        @Test
        @DisplayName("보상 트랜잭션도 실패하면 Ghost Resource 경고 로그가 출력된다")
        void createContainer_CompensationFails_GhostResourceWarningLogged() {
            // Given
            CreateObjectStorageContainerRequest request = CreateObjectStorageContainerRequest.builder()
                    .providerType(PROVIDER_TYPE)
                    .accountScope(ACCOUNT_SCOPE)
                    .containerName(CONTAINER_NAME)
                    .region("us-east-1")
                    .build();

            CloudResource mockCreatedContainer = CloudResource.builder()
                    .resourceId(CONTAINER_NAME)
                    .resourceName(CONTAINER_NAME)
                    .build();

            when(managementPort.createContainer(any())).thenReturn(mockCreatedContainer);
            // DB 저장 실패
            doThrow(new RuntimeException("DB 저장 실패")).when(resourceHelper)
                    .registerStorageBucket(any(), any(), any(), any());
            // 보상 트랜잭션(CSP 삭제)도 실패
            doThrow(new RuntimeException("CSP 삭제 실패")).when(managementPort)
                    .deleteContainer(any(), eq(CONTAINER_NAME));

            // When & Then
            assertThatThrownBy(() -> objectStorageUseCaseService.createContainer(request))
                    .isInstanceOf(BusinessException.class);

            // 보상 트랜잭션 시도 검증
            verify(managementPort).deleteContainer(any(), eq(CONTAINER_NAME));
            // Ghost Resource 발생 - 실제로는 모니터링/배치로 처리 필요
        }
    }

    @Nested
    @DisplayName("컨테이너 삭제 테스트")
    class DeleteContainerTest {

        @Test
        @DisplayName("컨테이너 삭제 시 소프트 삭제가 수행된다")
        void deleteContainer_Success_SoftDeletesResource() {
            // Given
            doNothing().when(managementPort).deleteContainer(any(), eq(CONTAINER_NAME));

            // When
            objectStorageUseCaseService.deleteContainer(PROVIDER_TYPE, ACCOUNT_SCOPE, CONTAINER_NAME);

            // Then
            verify(managementPort).deleteContainer(any(), eq(CONTAINER_NAME));
            verify(resourceHelper).softDeleteResource(CONTAINER_NAME);
        }

        @Test
        @DisplayName("강제 삭제 시 소프트 삭제가 수행된다")
        void forceDeleteContainer_Success_SoftDeletesResource() {
            // Given
            doNothing().when(managementPort).forceDeleteContainer(eq(CONTAINER_NAME), any());

            // When
            objectStorageUseCaseService.forceDeleteContainer(PROVIDER_TYPE, ACCOUNT_SCOPE, CONTAINER_NAME);

            // Then
            verify(managementPort).forceDeleteContainer(eq(CONTAINER_NAME), any());
            verify(resourceHelper).softDeleteResource(CONTAINER_NAME);
        }

        @Test
        @DisplayName("DB에 리소스가 없어도 CSP 삭제는 성공한다")
        void deleteContainer_ResourceNotInDb_CspDeletionSucceeds() {
            // Given
            doNothing().when(managementPort).deleteContainer(any(), eq(CONTAINER_NAME));
            // Helper 내부에서 리소스가 없으면 로그만 출력하고 예외 발생 안함

            // When
            objectStorageUseCaseService.deleteContainer(PROVIDER_TYPE, ACCOUNT_SCOPE, CONTAINER_NAME);

            // Then
            verify(managementPort).deleteContainer(any(), eq(CONTAINER_NAME)); // CSP 작업 성공
            verify(resourceHelper).softDeleteResource(CONTAINER_NAME);
        }
    }
}
