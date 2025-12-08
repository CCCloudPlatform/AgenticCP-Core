package com.agenticcp.core.domain.ui.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.AuthorizationException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.ui.entity.Menu;
import com.agenticcp.core.domain.ui.entity.MenuPermission;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import com.agenticcp.core.domain.ui.repository.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MenuAuthorizationService 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
class MenuAuthorizationServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private MenuAuthorizationService menuAuthorizationService;

    private Tenant testTenant;
    private User testUser;
    private Role testRole;
    private Permission testPermission;
    private Menu testMenu;
    private MenuPermission testMenuPermission;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .tenantKey("test-tenant")
                .tenantName("Test Tenant")
                .build();

        testPermission = Permission.builder()
                .permissionKey("MENU_READ")
                .permissionName("메뉴 조회")
                .build();
        testPermission.setTenant(testTenant);

        testRole = Role.builder()
                .roleKey("ADMIN")
                .roleName("관리자")
                .permissions(List.of(testPermission))
                .build();
        testRole.setTenant(testTenant);

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .roles(List.of(testRole))
                .permissions(List.of())
                .build();
        testUser.setTenant(testTenant);

        testMenu = Menu.builder()
                .menuKey("TEST_MENU")
                .menuName("테스트 메뉴")
                .url("/test")
                .tenant(testTenant)
                .build();

        testMenuPermission = MenuPermission.builder()
                .menu(testMenu)
                .permission(testPermission)
                .accessType(MenuPermission.AccessType.READ)
                .build();

        testMenu.setPermissions(List.of(testMenuPermission));
    }

    @Test
    @DisplayName("사용자별 접근 가능한 메뉴 조회 - 캐시 히트")
    void getAuthorizedMenus_CacheHit() {
        // Given
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(List.of(testMenu));

            // When
            List<Menu> result = menuAuthorizationService.getAuthorizedMenus("testuser");

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testMenu);

            verify(redisTemplate).opsForValue();
            verify(valueOperations).get(anyString());
            verify(menuRepository, never()).findByTenantAndActive(any(Tenant.class));
        }
    }

    @Test
    @DisplayName("사용자별 접근 가능한 메뉴 조회 - 캐시 미스")
    void getAuthorizedMenus_CacheMiss() {
        // Given
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(menuRepository.findByTenantAndActive(testTenant)).thenReturn(List.of(testMenu));
            doNothing().when(valueOperations).set(anyString(), any(), any());

            // When
            List<Menu> result = menuAuthorizationService.getAuthorizedMenus("testuser");

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testMenu);

            verify(redisTemplate, times(2)).opsForValue();
            verify(valueOperations).get(anyString());
            verify(userRepository, times(2)).findByUsername("testuser");
            verify(menuRepository).findByTenantAndActive(testTenant);
            verify(valueOperations).set(anyString(), any(), any());
        }
    }

    @Test
    @DisplayName("메뉴 접근 권한 확인 - 권한 있음")
    void hasMenuAccess_WithPermission_ReturnsTrue() {
        // Given
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));

            // When
            boolean result = menuAuthorizationService.hasMenuAccess("testuser", 1L, MenuPermission.AccessType.READ);

            // Then
            assertThat(result).isTrue();

            verify(userRepository).findByUsername("testuser");
            verify(menuRepository).findById(1L);
        }
    }

    @Test
    @DisplayName("메뉴 접근 권한 확인 - 권한 없음")
    void hasMenuAccess_WithoutPermission_ReturnsFalse() {
        // Given
        User userWithoutPermission = User.builder()
                .username("nopermission")
                .roles(List.of())
                .permissions(List.of())
                .build();

        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(userRepository.findByUsername("nopermission")).thenReturn(Optional.of(userWithoutPermission));
            when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));

            // When
            boolean result = menuAuthorizationService.hasMenuAccess("nopermission", 1L, MenuPermission.AccessType.READ);

            // Then
            assertThat(result).isFalse();

            verify(userRepository).findByUsername("nopermission");
            verify(menuRepository).findById(1L);
        }
    }

    @Test
    @DisplayName("메뉴 접근 권한 확인 - 권한 설정 없음 (접근 허용)")
    void hasMenuAccess_NoPermissions_ReturnsTrue() {
        // Given
        Menu menuWithoutPermissions = Menu.builder()
                .menuKey("NO_PERM_MENU")
                .menuName("권한 없는 메뉴")
                .permissions(List.of())
                .build();

        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(menuRepository.findById(2L)).thenReturn(Optional.of(menuWithoutPermissions));

            // When
            boolean result = menuAuthorizationService.hasMenuAccess("testuser", 2L, MenuPermission.AccessType.READ);

            // Then
            assertThat(result).isTrue();

            verify(userRepository).findByUsername("testuser");
            verify(menuRepository).findById(2L);
        }
    }

    @Test
    @DisplayName("메뉴 접근 권한 확인 - 존재하지 않는 사용자 예외")
    void hasMenuAccess_UserNotFound_ThrowsException() {
        // Given
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> menuAuthorizationService.hasMenuAccess("nonexistent", 1L, MenuPermission.AccessType.READ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found: nonexistent");

            verify(userRepository).findByUsername("nonexistent");
            verify(menuRepository, never()).findById(anyLong());
        }
    }

    @Test
    @DisplayName("사용자 메뉴 캐시 무효화")
    void evictUserMenuCache_Success() {
        // Given
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // When
            menuAuthorizationService.evictUserMenuCache("testuser");

            // Then
            verify(redisTemplate).delete(anyString());
        }
    }

    @Test
    @DisplayName("전체 메뉴 캐시 무효화")
    void evictAllMenuCache_Success() {
        // Given
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(redisTemplate.keys(anyString())).thenReturn(Set.of("key1", "key2"));
            when(redisTemplate.delete(anySet())).thenReturn(2L);

            // When
            menuAuthorizationService.evictAllMenuCache();

            // Then
            verify(redisTemplate).keys(anyString());
            verify(redisTemplate).delete(anySet());
        }
    }

    @Test
    @DisplayName("사용자 권한 조회 - 역할 기반 권한")
    void getUserPermissions_RoleBasedPermissions() {
        // Given
        User userWithRolePermissions = User.builder()
                .username("roleuser")
                .roles(List.of(testRole))
                .permissions(List.of())
                .build();

        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(userRepository.findByUsername("roleuser")).thenReturn(Optional.of(userWithRolePermissions));
            when(menuRepository.findByTenantAndActive(testTenant)).thenReturn(List.of(testMenu));
            doNothing().when(valueOperations).set(anyString(), any(), any());

            // When
            List<Menu> result = menuAuthorizationService.getAuthorizedMenus("roleuser");

            // Then
            assertThat(result).isNotNull();
            verify(userRepository, times(2)).findByUsername("roleuser");
        }
    }

    @Test
    @DisplayName("사용자 권한 조회 - 직접 할당된 권한")
    void getUserPermissions_DirectPermissions() {
        // Given
        User userWithDirectPermissions = User.builder()
                .username("directuser")
                .roles(List.of())
                .permissions(List.of(testPermission))
                .build();

        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(userRepository.findByUsername("directuser")).thenReturn(Optional.of(userWithDirectPermissions));
            when(menuRepository.findByTenantAndActive(testTenant)).thenReturn(List.of(testMenu));
            doNothing().when(valueOperations).set(anyString(), any(), any());

            // When
            List<Menu> result = menuAuthorizationService.getAuthorizedMenus("directuser");

            // Then
            assertThat(result).isNotNull();
            verify(userRepository, times(2)).findByUsername("directuser");
        }
    }
}
