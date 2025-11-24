package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import com.agenticcp.core.domain.tenant.service.TenantIsolationService;
import com.agenticcp.core.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResourceAccessControlService 단위 테스트
 * 
 * <p>리소스 접근 제어 서비스의 핵심 기능을 검증합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceAccessControlService 단위 테스트")
class ResourceAccessControlServiceTest {

    @Mock
    private TenantIsolationService tenantIsolationService;

    @Mock
    private ResourceOwnerService resourceOwnerService;

    @InjectMocks
    private ResourceAccessControlService resourceAccessControlService;

    private Tenant testTenant1;
    private Tenant testTenant2;
    private User testUser1;
    private User testUser2;
    private CloudResource testResource1;
    private CloudResource testResource2;
    private CloudResource testResource3;

    @BeforeEach
    void setUp() {
        testTenant1 = Tenant.builder()
                .tenantKey("test-tenant-001")
                .tenantName("Test Tenant 1")
                .build();
        testTenant1.setId(1L);

        testTenant2 = Tenant.builder()
                .tenantKey("test-tenant-002")
                .tenantName("Test Tenant 2")
                .build();
        testTenant2.setId(2L);

        testUser1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .name("User 1")
                .tenant(testTenant1)
                .build();
        testUser1.setId(1L);

        testUser2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .name("User 2")
                .tenant(testTenant1)
                .build();
        testUser2.setId(2L);

        testResource1 = CloudResource.builder()
                .resourceId("resource-001")
                .resourceName("Resource 1")
                .tenant(testTenant1)
                .build();
        testResource1.setId(1L);

        testResource2 = CloudResource.builder()
                .resourceId("resource-002")
                .resourceName("Resource 2")
                .tenant(testTenant1)
                .build();
        testResource2.setId(2L);

        testResource3 = CloudResource.builder()
                .resourceId("resource-003")
                .resourceName("Resource 3")
                .tenant(testTenant2)
                .build();
        testResource3.setId(3L);
    }

    @Nested
    @DisplayName("canAccessResource 테스트 - SHARED 모드")
    class CanAccessResourceSharedModeTest {

        @Test
        @DisplayName("SHARED 모드에서 동일 테넌트 리소스 접근 → 접근 허용")
        void canAccessResource_SHARED모드_동일테넌트리소스접근_접근허용() {
            // Given
            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(TenantIsolation.IsolationLevel.SHARED);

            // When
            boolean result = resourceAccessControlService.canAccessResource(testUser1, testResource1);

            // Then
            assertThat(result).isTrue();
            verify(tenantIsolationService).getIsolationLevel(testTenant1);
            verify(resourceOwnerService, never()).isResourceOwner(any(), any());
        }

        @Test
        @DisplayName("SHARED 모드에서 다른 사용자의 리소스 접근 → 접근 허용")
        void canAccessResource_SHARED모드_다른사용자리소스접근_접근허용() {
            // Given
            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(TenantIsolation.IsolationLevel.SHARED);

            // When
            boolean result = resourceAccessControlService.canAccessResource(testUser2, testResource1);

            // Then
            assertThat(result).isTrue();
            verify(tenantIsolationService).getIsolationLevel(testTenant1);
            verify(resourceOwnerService, never()).isResourceOwner(any(), any());
        }

        @Test
        @DisplayName("SHARED 모드에서 다른 테넌트 리소스 접근 → 접근 거부")
        void canAccessResource_SHARED모드_다른테넌트리소스접근_접근거부() {
            // Given
            // 다른 테넌트의 리소스이므로 격리 수준 조회 전에 거부됨

            // When
            boolean result = resourceAccessControlService.canAccessResource(testUser1, testResource3);

            // Then
            assertThat(result).isFalse();
            verify(tenantIsolationService, never()).getIsolationLevel(any());
            verify(resourceOwnerService, never()).isResourceOwner(any(), any());
        }
    }

    @Nested
    @DisplayName("canAccessResource 테스트 - DEDICATED 모드")
    class CanAccessResourceDedicatedModeTest {

        @Test
        @DisplayName("DEDICATED 모드에서 소유한 리소스 접근 → 접근 허용")
        void canAccessResource_DEDICATED모드_소유한리소스접근_접근허용() {
            // Given
            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(TenantIsolation.IsolationLevel.DEDICATED);
            when(resourceOwnerService.isResourceOwner(testUser1, testResource1))
                    .thenReturn(true);

            // When
            boolean result = resourceAccessControlService.canAccessResource(testUser1, testResource1);

            // Then
            assertThat(result).isTrue();
            verify(tenantIsolationService).getIsolationLevel(testTenant1);
            verify(resourceOwnerService).isResourceOwner(testUser1, testResource1);
        }

        @Test
        @DisplayName("DEDICATED 모드에서 소유하지 않은 리소스 접근 → 접근 거부")
        void canAccessResource_DEDICATED모드_소유하지않은리소스접근_접근거부() {
            // Given
            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(TenantIsolation.IsolationLevel.DEDICATED);
            when(resourceOwnerService.isResourceOwner(testUser1, testResource2))
                    .thenReturn(false);

            // When
            boolean result = resourceAccessControlService.canAccessResource(testUser1, testResource2);

            // Then
            assertThat(result).isFalse();
            verify(tenantIsolationService).getIsolationLevel(testTenant1);
            verify(resourceOwnerService).isResourceOwner(testUser1, testResource2);
        }

        @Test
        @DisplayName("DEDICATED 모드에서 다른 테넌트 리소스 접근 → 접근 거부")
        void canAccessResource_DEDICATED모드_다른테넌트리소스접근_접근거부() {
            // Given
            // 다른 테넌트의 리소스이므로 격리 수준 조회 전에 거부됨

            // When
            boolean result = resourceAccessControlService.canAccessResource(testUser1, testResource3);

            // Then
            assertThat(result).isFalse();
            verify(tenantIsolationService, never()).getIsolationLevel(any());
            verify(resourceOwnerService, never()).isResourceOwner(any(), any());
        }
    }

    @Nested
    @DisplayName("canAccessResource 테스트 - 격리 수준 없음")
    class CanAccessResourceNoIsolationLevelTest {

        @Test
        @DisplayName("격리 수준이 설정되지 않은 경우 → 접근 거부")
        void canAccessResource_격리수준설정안됨_접근거부() {
            // Given
            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(null);

            // When
            boolean result = resourceAccessControlService.canAccessResource(testUser1, testResource1);

            // Then
            assertThat(result).isFalse();
            verify(tenantIsolationService).getIsolationLevel(testTenant1);
        }
    }

    @Nested
    @DisplayName("filterAccessibleResources 테스트 - SHARED 모드")
    class FilterAccessibleResourcesSharedModeTest {

        @Test
        @DisplayName("SHARED 모드에서 동일 테넌트 리소스 필터링 → 모든 리소스 반환")
        void filterAccessibleResources_SHARED모드_동일테넌트리소스필터링_모든리소스반환() {
            // Given
            List<CloudResource> resources = Arrays.asList(testResource1, testResource2);
            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(TenantIsolation.IsolationLevel.SHARED);

            // When
            List<CloudResource> result = resourceAccessControlService.filterAccessibleResources(testUser1, resources);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(testResource1, testResource2);
            verify(tenantIsolationService).getIsolationLevel(testTenant1);
            verify(resourceOwnerService, never()).getOwnedResourceIdsBatch(any(), any(), any());
        }

        @Test
        @DisplayName("SHARED 모드에서 다른 테넌트 리소스 포함 → 동일 테넌트 리소스만 반환")
        void filterAccessibleResources_SHARED모드_다른테넌트리소스포함_동일테넌트리소스만반환() {
            // Given
            List<CloudResource> resources = Arrays.asList(testResource1, testResource2, testResource3);
            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(TenantIsolation.IsolationLevel.SHARED);

            // When
            List<CloudResource> result = resourceAccessControlService.filterAccessibleResources(testUser1, resources);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(testResource1, testResource2);
            assertThat(result).doesNotContain(testResource3);
        }

        @Test
        @DisplayName("SHARED 모드에서 빈 리소스 목록 → 빈 목록 반환")
        void filterAccessibleResources_SHARED모드_빈리소스목록_빈목록반환() {
            // Given
            List<CloudResource> resources = List.of();

            // When
            List<CloudResource> result = resourceAccessControlService.filterAccessibleResources(testUser1, resources);

            // Then
            assertThat(result).isEmpty();
            verify(tenantIsolationService, never()).getIsolationLevel(any());
        }
    }

    @Nested
    @DisplayName("filterAccessibleResources 테스트 - DEDICATED 모드")
    class FilterAccessibleResourcesDedicatedModeTest {

        @Test
        @DisplayName("DEDICATED 모드에서 소유한 리소스만 필터링 → 소유한 리소스만 반환")
        void filterAccessibleResources_DEDICATED모드_소유한리소스만필터링_소유한리소스만반환() {
            // Given
            List<CloudResource> resources = Arrays.asList(testResource1, testResource2);
            List<Long> resourceIds = Arrays.asList(1L, 2L);
            List<Long> ownedResourceIds = Arrays.asList(1L); // testResource1만 소유

            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(TenantIsolation.IsolationLevel.DEDICATED);
            when(resourceOwnerService.getOwnedResourceIdsBatch(testUser1, resourceIds, testTenant1))
                    .thenReturn(ownedResourceIds);

            // When
            List<CloudResource> result = resourceAccessControlService.filterAccessibleResources(testUser1, resources);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testResource1);
            verify(tenantIsolationService).getIsolationLevel(testTenant1);
            verify(resourceOwnerService).getOwnedResourceIdsBatch(testUser1, resourceIds, testTenant1);
        }

        @Test
        @DisplayName("DEDICATED 모드에서 소유한 리소스가 없는 경우 → 빈 목록 반환")
        void filterAccessibleResources_DEDICATED모드_소유한리소스없음_빈목록반환() {
            // Given
            List<CloudResource> resources = Arrays.asList(testResource1, testResource2);
            List<Long> resourceIds = Arrays.asList(1L, 2L);

            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(TenantIsolation.IsolationLevel.DEDICATED);
            when(resourceOwnerService.getOwnedResourceIdsBatch(testUser1, resourceIds, testTenant1))
                    .thenReturn(List.of());

            // When
            List<CloudResource> result = resourceAccessControlService.filterAccessibleResources(testUser1, resources);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("DEDICATED 모드에서 다른 테넌트 리소스 포함 → 동일 테넌트 소유 리소스만 반환")
        void filterAccessibleResources_DEDICATED모드_다른테넌트리소스포함_동일테넌트소유리소스만반환() {
            // Given
            List<CloudResource> resources = Arrays.asList(testResource1, testResource2, testResource3);
            List<Long> resourceIds = Arrays.asList(1L, 2L); // testResource3는 다른 테넌트이므로 제외
            List<Long> ownedResourceIds = Arrays.asList(1L);

            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(TenantIsolation.IsolationLevel.DEDICATED);
            when(resourceOwnerService.getOwnedResourceIdsBatch(testUser1, resourceIds, testTenant1))
                    .thenReturn(ownedResourceIds);

            // When
            List<CloudResource> result = resourceAccessControlService.filterAccessibleResources(testUser1, resources);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testResource1);
            assertThat(result).doesNotContain(testResource2, testResource3);
        }
    }

    @Nested
    @DisplayName("filterAccessibleResources 테스트 - 격리 수준 없음")
    class FilterAccessibleResourcesNoIsolationLevelTest {

        @Test
        @DisplayName("격리 수준이 설정되지 않은 경우 → 빈 목록 반환")
        void filterAccessibleResources_격리수준설정안됨_빈목록반환() {
            // Given
            List<CloudResource> resources = Arrays.asList(testResource1, testResource2);
            when(tenantIsolationService.getIsolationLevel(testTenant1))
                    .thenReturn(null);

            // When
            List<CloudResource> result = resourceAccessControlService.filterAccessibleResources(testUser1, resources);

            // Then
            assertThat(result).isEmpty();
        }
    }
}

