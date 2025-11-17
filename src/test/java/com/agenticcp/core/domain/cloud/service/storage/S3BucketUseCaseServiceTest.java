package com.agenticcp.core.domain.cloud.service.storage;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.storage.*;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageManagementPort;
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
    private CredentialProviderPort credentialProviderPort;

    @Mock
    private ObjectStorageManagementPort managementPort;

    @Mock
    private ObjectStorageDiscoveryPort discoveryPort;

    @InjectMocks
    private ObjectStorageUseCaseService objectStorageUseCaseService;

    private static final String TENANT_KEY = "test-tenant";
    private static final CloudProvider.ProviderType AWS = CloudProvider.ProviderType.AWS;
    private static final String CONTAINER_NAME = "test-container";
    private static final String REGION = "us-east-1";

    @BeforeEach
    void setUp() {
        lenient().when(router.management(AWS)).thenReturn(managementPort);
        lenient().when(router.discovery(AWS)).thenReturn(discoveryPort);
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
                    .containerName(CONTAINER_NAME)
                    .region(REGION)
                    .objectOwnership("BucketOwnerEnforced")
                    .objectLockEnabled(false)
                    .tags(tags)
                    .build();

            expectedContainer = CloudResource.builder()
                    .resourceId("container-" + CONTAINER_NAME)
                    .resourceName(CONTAINER_NAME)
                    .displayName("Test Container")
                    .build();
        }

        @Test
        @DisplayName("정상적인 Object Storage Container 생성")
        void createContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(managementPort.createContainer(any(CreateObjectStorageContainerCommand.class))).thenReturn(expectedContainer);

                // When
                CloudResource result = objectStorageUseCaseService.createContainer(AWS, request);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceName()).isEqualTo(CONTAINER_NAME);
                assertThat(result.getResourceId()).isEqualTo("container-" + CONTAINER_NAME);

                verify(capabilityGuard).ensureSupported(AWS, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TAGGING);
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, REGION);
                verify(managementPort).createContainer(any(CreateObjectStorageContainerCommand.class));
            }
        }

        @Test
        @DisplayName("Capability 검증 실패 시 예외 발생")
        void createContainer_CapabilityCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                doThrow(new RuntimeException("Capability not supported"))
                        .when(capabilityGuard).ensureSupported(any(), any(), any(), any());

                // When & Then
                assertThatThrownBy(() -> objectStorageUseCaseService.createContainer(AWS, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Capability not supported");

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, REGION);
                verify(capabilityGuard).ensureSupported(AWS, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TAGGING);
                verify(managementPort, never()).createContainer(any());
            }
        }

        @Test
        @DisplayName("Credential 검증 실패 시 예외 발생")
        void createContainer_CredentialCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                doThrow(new RuntimeException("Invalid credentials"))
                        .when(credentialProviderPort).resolveCredentials(any(), any(), any());

                // When & Then
                assertThatThrownBy(() -> objectStorageUseCaseService.createContainer(AWS, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Invalid credentials");

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, REGION);
                verify(capabilityGuard, never()).ensureSupported(any(), any(), any(), any());
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
                    .versioningEnabled(true)
                    .tags(tags)
                    .build();

            expectedContainer = CloudResource.builder()
                    .resourceId("container-" + CONTAINER_NAME)
                    .resourceName(CONTAINER_NAME)
                    .displayName("Updated Test Container")
                    .build();
        }

        @Test
        @DisplayName("정상적인 Object Storage Container 업데이트")
        void updateContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(managementPort.updateContainer(any(UpdateObjectStorageContainerCommand.class))).thenReturn(expectedContainer);

                // When
                CloudResource result = objectStorageUseCaseService.updateContainer(AWS, CONTAINER_NAME, request);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceName()).isEqualTo(CONTAINER_NAME);
                assertThat(result.getDisplayName()).isEqualTo("Updated Test Container");

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(capabilityGuard).ensureSupported(AWS, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TAGGING);
                verify(managementPort).updateContainer(any(UpdateObjectStorageContainerCommand.class));
            }
        }
    }

    @Nested
    @DisplayName("Object Storage Container 목록 조회 테스트")
    class ListContainersTest {

        private ObjectStorageContainerQuery query;
        private Page<CloudResource> expectedPage;

        @BeforeEach
        void setUp() {
            query = ObjectStorageContainerQuery.builder()
                    .page(0)
                    .size(10)
                    .nameContains("test")
                    .sortBy("name")
                    .sortDirection("asc")
                    .build();

            CloudResource container1 = CloudResource.builder()
                    .resourceId("container-1")
                    .resourceName("test-container-1")
                    .build();

            CloudResource container2 = CloudResource.builder()
                    .resourceId("container-2")
                    .resourceName("test-container-2")
                    .build();

            expectedPage = new PageImpl<>(List.of(container1, container2), PageRequest.of(0, 10), 2);
        }

        @Test
        @DisplayName("정상적인 Object Storage Container 목록 조회")
        void listContainers_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.listContainers(query)).thenReturn(expectedPage);

                // When
                Page<CloudResource> result = objectStorageUseCaseService.listContainers(AWS, query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getTotalElements()).isEqualTo(2);
                assertThat(result.getContent()).hasSize(2);
                assertThat(result.getContent().get(0).getResourceName()).isEqualTo("test-container-1");

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).listContainers(query);
            }
        }

        @Test
        @DisplayName("빈 목록 조회")
        void listContainers_EmptyResult() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                Page<CloudResource> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
                when(discoveryPort.listContainers(query)).thenReturn(emptyPage);

                // When
                Page<CloudResource> result = objectStorageUseCaseService.listContainers(AWS, query);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getTotalElements()).isEqualTo(0);
                assertThat(result.getContent()).isEmpty();

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).listContainers(query);
            }
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
                    .resourceName(CONTAINER_NAME)
                    .displayName("Test Container")
                    .build();
        }

        @Test
        @DisplayName("정상적인 Object Storage Container 조회")
        void getContainer_Success() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.getContainer(CONTAINER_NAME)).thenReturn(Optional.of(expectedContainer));

                // When
                CloudResource result = objectStorageUseCaseService.getContainer(AWS, CONTAINER_NAME);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getResourceName()).isEqualTo(CONTAINER_NAME);
                assertThat(result.getResourceId()).isEqualTo("container-" + CONTAINER_NAME);

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).getContainer(CONTAINER_NAME);
            }
        }

        @Test
        @DisplayName("존재하지 않는 Container 조회 시 예외 발생")
        void getContainer_NotFound_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.getContainer(CONTAINER_NAME)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> objectStorageUseCaseService.getContainer(AWS, CONTAINER_NAME))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Object Storage Container를 찾을 수 없습니다: " + CONTAINER_NAME);

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).getContainer(CONTAINER_NAME);
            }
        }
    }

    @Nested
    @DisplayName("Object Storage Container 존재 확인 테스트")
    class ContainerExistsTest {

        @Test
        @DisplayName("Container가 존재하는 경우 true 반환")
        void containerExists_Exists_ReturnsTrue() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.containerExists(CONTAINER_NAME)).thenReturn(true);

                // When
                boolean result = objectStorageUseCaseService.containerExists(AWS, CONTAINER_NAME);

                // Then
                assertThat(result).isTrue();

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).containerExists(CONTAINER_NAME);
            }
        }

        @Test
        @DisplayName("Container가 존재하지 않는 경우 false 반환")
        void containerExists_NotExists_ReturnsFalse() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                when(discoveryPort.containerExists(CONTAINER_NAME)).thenReturn(false);

                // When
                boolean result = objectStorageUseCaseService.containerExists(AWS, CONTAINER_NAME);

                // Then
                assertThat(result).isFalse();

                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(discoveryPort).containerExists(CONTAINER_NAME);
            }
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
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);

                // When
                objectStorageUseCaseService.deleteContainer(AWS, CONTAINER_NAME);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TERMINATE);
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(managementPort).deleteContainer(CONTAINER_NAME);
            }
        }

        @Test
        @DisplayName("Capability 검증 실패 시 예외 발생")
        void deleteContainer_CapabilityCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                doThrow(new RuntimeException("Delete capability not supported"))
                        .when(capabilityGuard).ensureSupported(any(), any(), any(), any());

                // When & Then
                assertThatThrownBy(() -> objectStorageUseCaseService.deleteContainer(AWS, CONTAINER_NAME))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Delete capability not supported");

                verify(capabilityGuard).ensureSupported(AWS, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TERMINATE);
                verify(credentialProviderPort, never()).resolveCredentials(any(), any(), any());
                verify(managementPort, never()).deleteContainer(any());
            }
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
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);

                // When
                objectStorageUseCaseService.forceDeleteContainer(AWS, CONTAINER_NAME);

                // Then
                verify(capabilityGuard).ensureSupported(AWS, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TERMINATE);
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(managementPort).forceDeleteContainer(CONTAINER_NAME);
            }
        }

        @Test
        @DisplayName("Credential 검증 실패 시 예외 발생")
        void forceDeleteContainer_CredentialCheckFailed_ThrowsException() {
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                // Given
                mockedStatic.when(TenantContextHolder::getCurrentTenantKey).thenReturn(TENANT_KEY);
                doThrow(new RuntimeException("Invalid credentials for force delete"))
                        .when(credentialProviderPort).resolveCredentials(any(), any(), any());

                // When & Then
                assertThatThrownBy(() -> objectStorageUseCaseService.forceDeleteContainer(AWS, CONTAINER_NAME))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Invalid credentials for force delete");

                verify(capabilityGuard).ensureSupported(AWS, "OBJECT_STORAGE", "CONTAINER", CapabilityGuard.Operation.TERMINATE);
                verify(credentialProviderPort).resolveCredentials(TENANT_KEY, AWS, null);
                verify(managementPort, never()).forceDeleteContainer(any());
            }
        }
    }
}
