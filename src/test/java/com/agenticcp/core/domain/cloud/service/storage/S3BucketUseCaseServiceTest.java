package com.agenticcp.core.domain.cloud.service.storage;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.dto.CreateObjectStorageContainerRequest;
import com.agenticcp.core.domain.cloud.dto.ObjectStorageContainerQueryRequest;
import com.agenticcp.core.domain.cloud.dto.UpdateObjectStorageContainerRequest;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.storage.*;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageManagementPort;
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
 * ObjectStorageUseCaseService 단위 테스트
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectStorageUseCaseService 테스트")
class S3BucketUseCaseServiceTest {

    @Mock
    private ObjectStoragePortRouter router;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private ObjectStorageManagementPort managementPort;

    @Mock
    private ObjectStorageDiscoveryPort discoveryPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    @InjectMocks
    private ObjectStorageUseCaseService objectStorageUseCaseService;

    private static final String TENANT_KEY = "test-tenant";
    private static final CloudProvider.ProviderType AWS = CloudProvider.ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String CONTAINER_NAME = "test-container";
    private static final String REGION = "us-east-1";

    private CloudSessionCredential mockSession;

    @BeforeEach
    void setUp() {
        lenient().when(router.management(AWS)).thenReturn(managementPort);
        lenient().when(router.discovery(AWS)).thenReturn(discoveryPort);

        mockSession = AwsSessionCredential.builder()
                .accessKeyId("AKIA_TEST")
                .secretAccessKey("secret")
                .sessionToken("token")
                .region(REGION)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Nested
    @DisplayName("Object Storage Container 생성 테스트")
    class CreateContainerTest {

        private CreateObjectStorageContainerRequest request;
        private CloudResource expectedContainer;

        @BeforeEach
        void setUp() {
            Map<String, String> tags = new HashMap<>();
            tags.put("Environment", "test");
            tags.put("Project", "agenticcp");

            request = CreateObjectStorageContainerRequest.builder()
                    .providerType(AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .containerName(CONTAINER_NAME)
                    .region(REGION)
                    .objectOwnership("BucketOwnerEnforced")
                    .objectLockEnabled(false)
                    .tags(tags)
                    .build();

            expectedContainer = CloudResource.builder()
                    .resourceId("container-" + CONTAINER_NAME)
                    .name(CONTAINER_NAME)
                    .provider("AWS")
                    .region("us-east-1")
                    .type("BUCKET")
                    .build();
        }

        @Test
        @DisplayName("정상적인 Object Storage Container 생성")
        void createContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(accountCredentialManagementPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(managementPort.createContainer(any(CreateObjectStorageContainerCommand.class))).thenReturn(expectedContainer);
                when(resourceHelper.registerResource(any(), any(), any())).thenReturn(expectedContainer);

                // When
                CloudResource result = objectStorageUseCaseService.createContainer(request);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getName()).isEqualTo(CONTAINER_NAME);
                assertThat(result.getResourceId()).isEqualTo("container-" + CONTAINER_NAME);

                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
                verify(accountCredentialManagementPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(managementPort).createContainer(any(CreateObjectStorageContainerCommand.class));
                verify(resourceHelper).registerResource(eq(AWS), eq("S3"), any());
            }
        }

        @Test
        @DisplayName("Capability 검증 실패 시 예외 발생")
        void createContainer_CapabilityCheckFailed_ThrowsException() {
            // Given
            doThrow(new RuntimeException("Capability not supported"))
                    .when(capabilityGuard).ensureSupported(any(), any(), any(), any());

            // When & Then
            assertThatThrownBy(() -> objectStorageUseCaseService.createContainer(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Capability not supported");

            verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
            verify(accountCredentialManagementPort, never()).getSession(any(), any(), any());
            verify(managementPort, never()).createContainer(any());
        }

        @Test
        @DisplayName("Credential 검증 실패 시 예외 발생")
        void createContainer_CredentialCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(accountCredentialManagementPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS))
                        .thenThrow(new RuntimeException("Invalid credentials"));

                // When & Then
                assertThatThrownBy(() -> objectStorageUseCaseService.createContainer(request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Invalid credentials");

                verify(accountCredentialManagementPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
                verify(managementPort, never()).createContainer(any());
            }
        }
    }

    @Nested
    @DisplayName("Object Storage Container 업데이트 테스트")
    class UpdateContainerTest {

        private UpdateObjectStorageContainerRequest request;
        private CloudResource expectedContainer;

        @BeforeEach
        void setUp() {
            Map<String, String> tags = new HashMap<>();
            tags.put("Environment", "production");
            tags.put("Updated", "true");

            request = UpdateObjectStorageContainerRequest.builder()
                    .providerType(AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .containerName(CONTAINER_NAME)
                    .versioningEnabled(true)
                    .tags(tags)
                    .build();

            expectedContainer = CloudResource.builder()
                    .resourceId("container-" + CONTAINER_NAME)
                    .name(CONTAINER_NAME)
                    .provider("AWS")
                    .region("us-east-1")
                    .type("BUCKET")
                    .build();
        }

        @Test
        @DisplayName("정상적인 Object Storage Container 업데이트")
        void updateContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(accountCredentialManagementPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                when(managementPort.updateContainer(any(UpdateObjectStorageContainerCommand.class))).thenReturn(expectedContainer);

                // When
                CloudResource result = objectStorageUseCaseService.updateContainer(request);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getName()).isEqualTo(CONTAINER_NAME);

                verify(accountCredentialManagementPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TAGGING);
                verify(managementPort).updateContainer(any(UpdateObjectStorageContainerCommand.class));
            }
        }
    }

    @Nested
    @DisplayName("Object Storage Container 목록 조회 테스트")
    class ListContainersTest {

        private ObjectStorageContainerQueryRequest query;
        private Page<CloudResource> expectedPage;

        @BeforeEach
        void setUp() {
            query = ObjectStorageContainerQueryRequest.builder()
                    .providerType(AWS)
                    .accountScope(ACCOUNT_SCOPE)
                    .page(0)
                    .size(10)
                    .nameContains("test")
                    .sortBy("name")
                    .sortDirection("asc")
                    .build();

            CloudResource container1 = CloudResource.builder()
                    .resourceId("container-1")
                    .name("test-container-1")
                    .provider("AWS")
                    .region("us-east-1")
                    .type("BUCKET")
                    .build();

            CloudResource container2 = CloudResource.builder()
                    .resourceId("container-2")
                    .name("test-container-2")
                    .provider("AWS")
                    .region("us-east-1")
                    .type("BUCKET")
                    .build();

            expectedPage = new PageImpl<>(List.of(container1, container2), PageRequest.of(0, 10), 2);
        }

        @Test
        @DisplayName("정상적인 Object Storage Container 목록 조회")
        void listContainers_Success() {
            // Given
            when(discoveryPort.listContainers(query)).thenReturn(expectedPage);

            // When
            Page<CloudResource> result = objectStorageUseCaseService.listContainers(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getName()).isEqualTo("test-container-1");

            verify(discoveryPort).listContainers(query);
        }

        @Test
        @DisplayName("빈 목록 조회")
        void listContainers_EmptyResult() {
            // Given
            Page<CloudResource> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(discoveryPort.listContainers(query)).thenReturn(emptyPage);

            // When
            Page<CloudResource> result = objectStorageUseCaseService.listContainers(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();

            verify(discoveryPort).listContainers(query);
        }
    }

    @Nested
    @DisplayName("Object Storage Container 조회 테스트")
    class GetContainerTest {

        private CloudResource expectedContainer;

        @BeforeEach
        void setUp() {
            expectedContainer = CloudResource.builder()
                    .resourceId("container-" + CONTAINER_NAME)
                    .name(CONTAINER_NAME)
                    .provider("AWS")
                    .region("us-east-1")
                    .type("BUCKET")
                    .build();
        }

        @Test
        @DisplayName("정상적인 Object Storage Container 조회")
        void getContainer_Success() {
            // Given
            when(discoveryPort.getContainer(ACCOUNT_SCOPE, CONTAINER_NAME)).thenReturn(Optional.of(expectedContainer));

            // When
            CloudResource result = objectStorageUseCaseService.getContainer(AWS, ACCOUNT_SCOPE, CONTAINER_NAME);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(CONTAINER_NAME);
            assertThat(result.getResourceId()).isEqualTo("container-" + CONTAINER_NAME);

            verify(discoveryPort).getContainer(ACCOUNT_SCOPE, CONTAINER_NAME);
        }

        @Test
        @DisplayName("존재하지 않는 Container 조회 시 예외 발생")
        void getContainer_NotFound_ThrowsException() {
            // Given
            when(discoveryPort.getContainer(ACCOUNT_SCOPE, CONTAINER_NAME)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> objectStorageUseCaseService.getContainer(AWS, ACCOUNT_SCOPE, CONTAINER_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Object Storage Container를 찾을 수 없습니다: " + CONTAINER_NAME);

            verify(discoveryPort).getContainer(ACCOUNT_SCOPE, CONTAINER_NAME);
        }
    }

    @Nested
    @DisplayName("Object Storage Container 존재 확인 테스트")
    class ContainerExistsTest {

        @Test
        @DisplayName("Container가 존재하는 경우 true 반환")
        void containerExists_Exists_ReturnsTrue() {
            // Given
            when(discoveryPort.containerExists(ACCOUNT_SCOPE, CONTAINER_NAME)).thenReturn(true);

            // When
            boolean result = objectStorageUseCaseService.containerExists(AWS, ACCOUNT_SCOPE, CONTAINER_NAME);

            // Then
            assertThat(result).isTrue();

            verify(discoveryPort).containerExists(ACCOUNT_SCOPE, CONTAINER_NAME);
        }

        @Test
        @DisplayName("Container가 존재하지 않는 경우 false 반환")
        void containerExists_NotExists_ReturnsFalse() {
            // Given
            when(discoveryPort.containerExists(ACCOUNT_SCOPE, CONTAINER_NAME)).thenReturn(false);

            // When
            boolean result = objectStorageUseCaseService.containerExists(AWS, ACCOUNT_SCOPE, CONTAINER_NAME);

            // Then
            assertThat(result).isFalse();

            verify(discoveryPort).containerExists(ACCOUNT_SCOPE, CONTAINER_NAME);
        }
    }

    @Nested
    @DisplayName("Object Storage Container 삭제 테스트")
    class DeleteContainerTest {

        @Test
        @DisplayName("정상적인 Object Storage Container 삭제")
        void deleteContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(accountCredentialManagementPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                doNothing().when(managementPort).deleteContainer(any(CloudSessionCredential.class), eq(CONTAINER_NAME));
                doNothing().when(resourceHelper).softDeleteResource(eq(CONTAINER_NAME));

                // When
                objectStorageUseCaseService.deleteContainer(AWS, ACCOUNT_SCOPE, CONTAINER_NAME);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
                verify(accountCredentialManagementPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(managementPort).deleteContainer(mockSession, CONTAINER_NAME);
                verify(resourceHelper).softDeleteResource(CONTAINER_NAME);
            }
        }

        @Test
        @DisplayName("Capability 검증 실패 시 예외 발생")
        void deleteContainer_CapabilityCheckFailed_ThrowsException() {
            // Given
            doThrow(new RuntimeException("Delete capability not supported"))
                    .when(capabilityGuard).ensureSupported(any(), any(), any(), any());

            // When & Then
            assertThatThrownBy(() -> objectStorageUseCaseService.deleteContainer(AWS, ACCOUNT_SCOPE, CONTAINER_NAME))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delete capability not supported");

            verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
            verify(accountCredentialManagementPort, never()).getSession(any(), any(), any());
            verify(managementPort, never()).deleteContainer(any(), any());
        }
    }

    @Nested
    @DisplayName("Object Storage Container 강제 삭제 테스트")
    class ForceDeleteContainerTest {

        @Test
        @DisplayName("정상적인 Object Storage Container 강제 삭제")
        void forceDeleteContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(accountCredentialManagementPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS)).thenReturn(mockSession);
                doNothing().when(managementPort).forceDeleteContainer(eq(CONTAINER_NAME), any(CloudSessionCredential.class));
                doNothing().when(resourceHelper).softDeleteResource(eq(CONTAINER_NAME));

                // When
                objectStorageUseCaseService.forceDeleteContainer(AWS, ACCOUNT_SCOPE, CONTAINER_NAME);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
                verify(accountCredentialManagementPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(managementPort).forceDeleteContainer(CONTAINER_NAME, mockSession);
                verify(resourceHelper).softDeleteResource(CONTAINER_NAME);
            }
        }

        @Test
        @DisplayName("Credential 검증 실패 시 예외 발생")
        void forceDeleteContainer_CredentialCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(TENANT_KEY);
                when(accountCredentialManagementPort.getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS))
                        .thenThrow(new RuntimeException("Invalid credentials for force delete"));

                // When & Then
                assertThatThrownBy(() -> objectStorageUseCaseService.forceDeleteContainer(AWS, ACCOUNT_SCOPE, CONTAINER_NAME))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Invalid credentials for force delete");

                verify(capabilityGuard).ensureSupported(AWS, "S3", "BUCKET", CapabilityGuard.Operation.TERMINATE);
                verify(accountCredentialManagementPort).getSession(TENANT_KEY, ACCOUNT_SCOPE, AWS);
                verify(managementPort, never()).forceDeleteContainer(any(), any());
            }
        }
    }
}
