package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.AuthorizationException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.util.SecurityContextUtils;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import com.agenticcp.core.domain.tenant.service.TenantIsolationService;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CloudResourceService 단위 테스트
 * 
 * <p>클라우드 리소스 관리 서비스의 핵심 기능과 접근 제어를 검증합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CloudResourceService 단위 테스트")
class CloudResourceServiceTest {

    @Mock
    private CloudResourceRepository cloudResourceRepository;

    @Mock
    private ResourceOwnerService resourceOwnerService;

    @Mock
    private ResourceAccessControlService accessControlService;

    @Mock
    private TenantIsolationService tenantIsolationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CloudResourceService cloudResourceService;

    private Tenant testTenant;
    private User testUser;
    private CloudResource testResource1;
    private CloudResource testResource2;

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

        testResource1 = CloudResource.builder()
                .resourceId("resource-001")
                .resourceName("Resource 1")
                .tenant(testTenant)
                .build();
        testResource1.setId(1L);

        testResource2 = CloudResource.builder()
                .resourceId("resource-002")
                .resourceName("Resource 2")
                .tenant(testTenant)
                .build();
        testResource2.setId(2L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("createResource 테스트")
    class CreateResourceTest {

        @Test
        @DisplayName("리소스 생성 → 테넌트 설정 및 소유권 등록")
        void createResource_리소스생성_테넌트설정및소유권등록() {
            // Given
            CloudResource newResource = CloudResource.builder()
                    .resourceId("new-resource-001")
                    .resourceName("New Resource")
                    .build();

            try (MockedStatic<TenantContextHolder> tenantContextHolder = mockStatic(TenantContextHolder.class);
                 MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {

                tenantContextHolder.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
                securityContextUtils.when(SecurityContextUtils::getCurrentUsernameOrThrow).thenReturn("testuser");
                when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(testUser);
                when(cloudResourceRepository.save(any(CloudResource.class))).thenReturn(testResource1);

                // When
                CloudResource result = cloudResourceService.createResource(newResource);

                // Then
                assertThat(result).isEqualTo(testResource1);
                assertThat(newResource.getTenant()).isEqualTo(testTenant);
                verify(cloudResourceRepository).save(newResource);
                verify(resourceOwnerService).createResourceOwnership(testResource1, testUser);
            }
        }
    }

    @Nested
    @DisplayName("getAccessibleResources 테스트")
    class GetAccessibleResourcesTest {

        @Test
        @DisplayName("SHARED 모드에서 접근 가능한 리소스 조회 → 테넌트의 모든 리소스 반환")
        void getAccessibleResources_SHARED모드_접근가능한리소스조회_테넌트의모든리소스반환() {
            // Given
            List<CloudResource> allResources = Arrays.asList(testResource1, testResource2);

            try (MockedStatic<TenantContextHolder> tenantContextHolder = mockStatic(TenantContextHolder.class);
                 MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {

                tenantContextHolder.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
                securityContextUtils.when(SecurityContextUtils::getCurrentUsernameOrThrow).thenReturn("testuser");
                when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(testUser);
                when(tenantIsolationService.getIsolationLevel(testTenant))
                        .thenReturn(TenantIsolation.IsolationLevel.SHARED);
                when(cloudResourceRepository.findByTenantId(testTenant.getTenantKey()))
                        .thenReturn(allResources);

                // When
                List<CloudResource> result = cloudResourceService.getAccessibleResources();

                // Then
                assertThat(result).hasSize(2);
                assertThat(result).containsExactly(testResource1, testResource2);
                verify(tenantIsolationService).getIsolationLevel(testTenant);
                verify(cloudResourceRepository).findByTenantId(testTenant.getTenantKey());
                verify(resourceOwnerService, never()).getOwnedResources(any(), any());
            }
        }

        @Test
        @DisplayName("DEDICATED 모드에서 접근 가능한 리소스 조회 → 소유한 리소스만 반환")
        void getAccessibleResources_DEDICATED모드_접근가능한리소스조회_소유한리소스만반환() {
            // Given
            List<CloudResource> ownedResources = Arrays.asList(testResource1);

            try (MockedStatic<TenantContextHolder> tenantContextHolder = mockStatic(TenantContextHolder.class);
                 MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {

                tenantContextHolder.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
                securityContextUtils.when(SecurityContextUtils::getCurrentUsernameOrThrow).thenReturn("testuser");
                when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(testUser);
                when(tenantIsolationService.getIsolationLevel(testTenant))
                        .thenReturn(TenantIsolation.IsolationLevel.DEDICATED);
                when(resourceOwnerService.getOwnedResources(testUser, testTenant))
                        .thenReturn(ownedResources);

                // When
                List<CloudResource> result = cloudResourceService.getAccessibleResources();

                // Then
                assertThat(result).hasSize(1);
                assertThat(result).containsExactly(testResource1);
                verify(tenantIsolationService).getIsolationLevel(testTenant);
                verify(resourceOwnerService).getOwnedResources(testUser, testTenant);
                verify(cloudResourceRepository, never()).findByTenantId(any());
            }
        }
    }

    @Nested
    @DisplayName("getResource 테스트")
    class GetResourceTest {

        @Test
        @DisplayName("접근 권한이 있는 리소스 조회 → 리소스 반환")
        void getResource_접근권한있는리소스조회_리소스반환() {
            // Given
            try (MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {
                securityContextUtils.when(SecurityContextUtils::getCurrentUsernameOrThrow).thenReturn("testuser");
                when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(testUser);
                when(cloudResourceRepository.findById(1L)).thenReturn(Optional.of(testResource1));
                when(accessControlService.canAccessResource(testUser, testResource1)).thenReturn(true);

                // When
                CloudResource result = cloudResourceService.getResource(1L);

                // Then
                assertThat(result).isEqualTo(testResource1);
                verify(cloudResourceRepository).findById(1L);
                verify(accessControlService).canAccessResource(testUser, testResource1);
            }
        }

        @Test
        @DisplayName("리소스를 찾을 수 없는 경우 → ResourceNotFoundException 발생")
        void getResource_리소스찾을수없음_ResourceNotFoundException발생() {
            // Given
            try (MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {
                when(cloudResourceRepository.findById(999L)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> cloudResourceService.getResource(999L))
                        .isInstanceOf(ResourceNotFoundException.class);
                verify(cloudResourceRepository).findById(999L);
                verify(accessControlService, never()).canAccessResource(any(), any());
            }
        }

        @Test
        @DisplayName("접근 권한이 없는 경우 → AuthorizationException 발생")
        void getResource_접근권한없음_AuthorizationException발생() {
            // Given
            try (MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {
                securityContextUtils.when(SecurityContextUtils::getCurrentUsernameOrThrow).thenReturn("testuser");
                when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(testUser);
                when(cloudResourceRepository.findById(1L)).thenReturn(Optional.of(testResource1));
                when(accessControlService.canAccessResource(testUser, testResource1)).thenReturn(false);

                // When & Then
                assertThatThrownBy(() -> cloudResourceService.getResource(1L))
                        .isInstanceOf(AuthorizationException.class);
                verify(cloudResourceRepository).findById(1L);
                verify(accessControlService).canAccessResource(testUser, testResource1);
            }
        }
    }

    @Nested
    @DisplayName("updateResource 테스트")
    class UpdateResourceTest {

        @Test
        @DisplayName("접근 권한이 있는 리소스 수정 → 리소스 수정 성공")
        void updateResource_접근권한있는리소스수정_리소스수정성공() {
            // Given
            CloudResource updateData = CloudResource.builder()
                    .resourceName("Updated Resource")
                    .displayName("Updated Display Name")
                    .build();

            try (MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {
                securityContextUtils.when(SecurityContextUtils::getCurrentUsernameOrThrow).thenReturn("testuser");
                when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(testUser);
                when(cloudResourceRepository.findById(1L)).thenReturn(Optional.of(testResource1));
                when(accessControlService.canAccessResource(testUser, testResource1)).thenReturn(true);
                when(cloudResourceRepository.save(testResource1)).thenReturn(testResource1);

                // When
                CloudResource result = cloudResourceService.updateResource(1L, updateData);

                // Then
                assertThat(result).isEqualTo(testResource1);
                assertThat(testResource1.getResourceName()).isEqualTo("Updated Resource");
                assertThat(testResource1.getDisplayName()).isEqualTo("Updated Display Name");
                verify(cloudResourceRepository).save(testResource1);
            }
        }

        @Test
        @DisplayName("접근 권한이 없는 리소스 수정 → AuthorizationException 발생")
        void updateResource_접근권한없는리소스수정_AuthorizationException발생() {
            // Given
            CloudResource updateData = CloudResource.builder()
                    .resourceName("Updated Resource")
                    .build();

            try (MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {
                securityContextUtils.when(SecurityContextUtils::getCurrentUsernameOrThrow).thenReturn("testuser");
                when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(testUser);
                when(cloudResourceRepository.findById(1L)).thenReturn(Optional.of(testResource1));
                when(accessControlService.canAccessResource(testUser, testResource1)).thenReturn(false);

                // When & Then
                assertThatThrownBy(() -> cloudResourceService.updateResource(1L, updateData))
                        .isInstanceOf(AuthorizationException.class);
                verify(cloudResourceRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("deleteResource 테스트")
    class DeleteResourceTest {

        @Test
        @DisplayName("접근 권한이 있는 리소스 삭제 → Soft Delete 처리 및 소유권 삭제")
        void deleteResource_접근권한있는리소스삭제_SoftDelete처리및소유권삭제() {
            // Given
            try (MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {
                securityContextUtils.when(SecurityContextUtils::getCurrentUsernameOrThrow).thenReturn("testuser");
                when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(testUser);
                when(cloudResourceRepository.findById(1L)).thenReturn(Optional.of(testResource1));
                when(accessControlService.canAccessResource(testUser, testResource1)).thenReturn(true);
                when(cloudResourceRepository.save(testResource1)).thenReturn(testResource1);

                // When
                cloudResourceService.deleteResource(1L);

                // Then
                assertThat(testResource1.getIsDeleted()).isTrue();
                verify(cloudResourceRepository).save(testResource1);
                verify(resourceOwnerService).deleteResourceOwnership(testResource1, testUser);
            }
        }

        @Test
        @DisplayName("접근 권한이 없는 리소스 삭제 → AuthorizationException 발생")
        void deleteResource_접근권한없는리소스삭제_AuthorizationException발생() {
            // Given
            try (MockedStatic<SecurityContextUtils> securityContextUtils = mockStatic(SecurityContextUtils.class)) {
                securityContextUtils.when(SecurityContextUtils::getCurrentUsernameOrThrow).thenReturn("testuser");
                when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(testUser);
                when(cloudResourceRepository.findById(1L)).thenReturn(Optional.of(testResource1));
                when(accessControlService.canAccessResource(testUser, testResource1)).thenReturn(false);

                // When & Then
                assertThatThrownBy(() -> cloudResourceService.deleteResource(1L))
                        .isInstanceOf(AuthorizationException.class);
                verify(cloudResourceRepository, never()).save(any());
                verify(resourceOwnerService, never()).deleteResourceOwnership(any(), any());
            }
        }
    }
}

