package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.ResourceOwner;
import com.agenticcp.core.domain.cloud.repository.ResourceOwnerRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResourceOwnerService 단위 테스트
 * 
 * <p>리소스 소유권 관리 서비스의 핵심 기능을 검증합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceOwnerService 단위 테스트")
class ResourceOwnerServiceTest {

    @Mock
    private ResourceOwnerRepository resourceOwnerRepository;

    @InjectMocks
    private ResourceOwnerService resourceOwnerService;

    private Tenant testTenant;
    private User testUser;
    private CloudResource testResource;
    private ResourceOwner testResourceOwner;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .tenantKey("test-tenant-001")
                .tenantName("Test Tenant")
                .build();
        testTenant.setId(1L);

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .tenant(testTenant)
                .build();
        testUser.setId(1L);

        testResource = CloudResource.builder()
                .resourceId("resource-001")
                .resourceName("Test Resource")
                .tenant(testTenant)
                .build();
        testResource.setId(1L);

        testResourceOwner = ResourceOwner.builder()
                .resource(testResource)
                .user(testUser)
                .tenant(testTenant)
                .accessType(ResourceOwner.AccessType.OWNER)
                .build();
        testResourceOwner.setId(1L);
    }

    @Nested
    @DisplayName("createResourceOwnership 테스트")
    class CreateResourceOwnershipTest {

        @Test
        @DisplayName("새로운 소유권 생성 → ResourceOwner 저장")
        void createResourceOwnership_새로운소유권생성_ResourceOwner저장() {
            // Given
            when(resourceOwnerRepository.existsByResourceAndUserAndIsDeletedFalse(testResource, testUser))
                    .thenReturn(false);
            when(resourceOwnerRepository.save(any(ResourceOwner.class)))
                    .thenReturn(testResourceOwner);

            // When
            resourceOwnerService.createResourceOwnership(testResource, testUser);

            // Then
            ArgumentCaptor<ResourceOwner> captor = ArgumentCaptor.forClass(ResourceOwner.class);
            verify(resourceOwnerRepository).save(captor.capture());
            
            ResourceOwner saved = captor.getValue();
            assertThat(saved.getResource()).isEqualTo(testResource);
            assertThat(saved.getUser()).isEqualTo(testUser);
            assertThat(saved.getTenant()).isEqualTo(testTenant);
            assertThat(saved.getAccessType()).isEqualTo(ResourceOwner.AccessType.OWNER);
        }

        @Test
        @DisplayName("이미 소유권이 존재하는 경우 → 저장하지 않고 반환")
        void createResourceOwnership_이미소유권존재_저장하지않고반환() {
            // Given
            when(resourceOwnerRepository.existsByResourceAndUserAndIsDeletedFalse(testResource, testUser))
                    .thenReturn(true);

            // When
            resourceOwnerService.createResourceOwnership(testResource, testUser);

            // Then
            verify(resourceOwnerRepository, never()).save(any(ResourceOwner.class));
        }
    }

    @Nested
    @DisplayName("getOwnedResources 테스트")
    class GetOwnedResourcesTest {

        @Test
        @DisplayName("사용자가 소유한 리소스 조회 → 소유한 리소스 목록 반환")
        void getOwnedResources_사용자가소유한리소스조회_소유한리소스목록반환() {
            // Given
            CloudResource resource1 = CloudResource.builder()
                    .resourceId("resource-001")
                    .resourceName("Resource 1")
                    .tenant(testTenant)
                    .build();
            resource1.setId(1L);

            CloudResource resource2 = CloudResource.builder()
                    .resourceId("resource-002")
                    .resourceName("Resource 2")
                    .tenant(testTenant)
                    .build();
            resource2.setId(2L);

            List<CloudResource> ownedResources = Arrays.asList(resource1, resource2);
            when(resourceOwnerRepository.findResourcesByOwner(testUser, testTenant))
                    .thenReturn(ownedResources);

            // When
            List<CloudResource> result = resourceOwnerService.getOwnedResources(testUser, testTenant);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(resource1, resource2);
            verify(resourceOwnerRepository).findResourcesByOwner(testUser, testTenant);
        }

        @Test
        @DisplayName("소유한 리소스가 없는 경우 → 빈 목록 반환")
        void getOwnedResources_소유한리소스없음_빈목록반환() {
            // Given
            when(resourceOwnerRepository.findResourcesByOwner(testUser, testTenant))
                    .thenReturn(List.of());

            // When
            List<CloudResource> result = resourceOwnerService.getOwnedResources(testUser, testTenant);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isResourceOwner 테스트")
    class IsResourceOwnerTest {

        @Test
        @DisplayName("사용자가 리소스 소유자인 경우 → true 반환")
        void isResourceOwner_사용자가리소스소유자_true반환() {
            // Given
            when(resourceOwnerRepository.existsByResourceAndUserAndIsDeletedFalse(testResource, testUser))
                    .thenReturn(true);

            // When
            boolean result = resourceOwnerService.isResourceOwner(testUser, testResource);

            // Then
            assertThat(result).isTrue();
            verify(resourceOwnerRepository).existsByResourceAndUserAndIsDeletedFalse(testResource, testUser);
        }

        @Test
        @DisplayName("사용자가 리소스 소유자가 아닌 경우 → false 반환")
        void isResourceOwner_사용자가리소스소유자아님_false반환() {
            // Given
            when(resourceOwnerRepository.existsByResourceAndUserAndIsDeletedFalse(testResource, testUser))
                    .thenReturn(false);

            // When
            boolean result = resourceOwnerService.isResourceOwner(testUser, testResource);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getResourceOwnership 테스트")
    class GetResourceOwnershipTest {

        @Test
        @DisplayName("소유권 정보 조회 → ResourceOwner 반환")
        void getResourceOwnership_소유권정보조회_ResourceOwner반환() {
            // Given
            when(resourceOwnerRepository.findByResourceAndUserAndIsDeletedFalse(testResource, testUser))
                    .thenReturn(Optional.of(testResourceOwner));

            // When
            Optional<ResourceOwner> result = resourceOwnerService.getResourceOwnership(testResource, testUser);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(testResourceOwner);
        }

        @Test
        @DisplayName("소유권 정보가 없는 경우 → Optional.empty() 반환")
        void getResourceOwnership_소유권정보없음_OptionalEmpty반환() {
            // Given
            when(resourceOwnerRepository.findByResourceAndUserAndIsDeletedFalse(testResource, testUser))
                    .thenReturn(Optional.empty());

            // When
            Optional<ResourceOwner> result = resourceOwnerService.getResourceOwnership(testResource, testUser);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteResourceOwnership 테스트")
    class DeleteResourceOwnershipTest {

        @Test
        @DisplayName("소유권 삭제 → Soft Delete 처리")
        void deleteResourceOwnership_소유권삭제_SoftDelete처리() {
            // Given
            when(resourceOwnerRepository.findByResourceAndUserAndIsDeletedFalse(testResource, testUser))
                    .thenReturn(Optional.of(testResourceOwner));
            when(resourceOwnerRepository.save(testResourceOwner))
                    .thenReturn(testResourceOwner);

            // When
            resourceOwnerService.deleteResourceOwnership(testResource, testUser);

            // Then
            assertThat(testResourceOwner.getIsDeleted()).isTrue();
            verify(resourceOwnerRepository).findByResourceAndUserAndIsDeletedFalse(testResource, testUser);
            verify(resourceOwnerRepository).save(testResourceOwner);
        }

        @Test
        @DisplayName("소유권이 없는 경우 → 삭제하지 않음")
        void deleteResourceOwnership_소유권없음_삭제하지않음() {
            // Given
            when(resourceOwnerRepository.findByResourceAndUserAndIsDeletedFalse(testResource, testUser))
                    .thenReturn(Optional.empty());

            // When
            resourceOwnerService.deleteResourceOwnership(testResource, testUser);

            // Then
            verify(resourceOwnerRepository, never()).save(any(ResourceOwner.class));
        }
    }

    @Nested
    @DisplayName("getOwnedResourceIdsBatch 테스트")
    class GetOwnedResourceIdsBatchTest {

        @Test
        @DisplayName("배치로 소유한 리소스 ID 조회 → 소유한 리소스 ID 목록 반환")
        void getOwnedResourceIdsBatch_배치로소유한리소스ID조회_소유한리소스ID목록반환() {
            // Given
            List<Long> resourceIds = Arrays.asList(1L, 2L, 3L, 4L);
            List<Long> ownedResourceIds = Arrays.asList(1L, 3L); // 1L, 3L만 소유

            when(resourceOwnerRepository.findOwnedResourceIds(testUser, resourceIds, testTenant))
                    .thenReturn(ownedResourceIds);

            // When
            List<Long> result = resourceOwnerService.getOwnedResourceIdsBatch(testUser, resourceIds, testTenant);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(1L, 3L);
            verify(resourceOwnerRepository).findOwnedResourceIds(testUser, resourceIds, testTenant);
        }

        @Test
        @DisplayName("빈 리소스 ID 목록 → 빈 목록 반환")
        void getOwnedResourceIdsBatch_빈리소스ID목록_빈목록반환() {
            // When
            List<Long> result = resourceOwnerService.getOwnedResourceIdsBatch(testUser, List.of(), testTenant);

            // Then
            assertThat(result).isEmpty();
            verify(resourceOwnerRepository, never()).findOwnedResourceIds(any(), any(), any());
        }

        @Test
        @DisplayName("소유한 리소스가 없는 경우 → 빈 목록 반환")
        void getOwnedResourceIdsBatch_소유한리소스없음_빈목록반환() {
            // Given
            List<Long> resourceIds = Arrays.asList(1L, 2L, 3L);
            when(resourceOwnerRepository.findOwnedResourceIds(testUser, resourceIds, testTenant))
                    .thenReturn(List.of());

            // When
            List<Long> result = resourceOwnerService.getOwnedResourceIdsBatch(testUser, resourceIds, testTenant);

            // Then
            assertThat(result).isEmpty();
        }
    }
}

