package com.agenticcp.core.domain.ui.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.ui.entity.Menu;
import com.agenticcp.core.domain.ui.entity.MenuPermission;
import com.agenticcp.core.domain.ui.exception.MenuErrorCode;
import com.agenticcp.core.domain.ui.repository.MenuPermissionRepository;
import com.agenticcp.core.domain.ui.repository.MenuRepository;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MenuService 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuPermissionRepository menuPermissionRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private MenuService menuService;

    private Tenant testTenant;
    private Menu testMenu;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .tenantKey("test-tenant")
                .tenantName("Test Tenant")
                .build();

        testMenu = Menu.builder()
                .menuKey("TEST_MENU")
                .menuName("테스트 메뉴")
                .description("테스트 메뉴 설명")
                .url("/test")
                .icon("test-icon")
                .parentId(null)
                .sortOrder(1)
                .isActive(true)
                .isSystem(false)
                .build();
        testMenu.setTenant(testTenant);
    }

    @Test
    @DisplayName("메뉴 생성 - 성공")
    void createMenu_Success() {
        // Given
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(menuRepository.existsByMenuKeyAndTenant(anyString(), any(Tenant.class), anyLong()))
                    .thenReturn(false);
            when(menuRepository.save(any(Menu.class))).thenReturn(testMenu);

            // When
            Menu result = menuService.createMenu(
                    "TEST_MENU",
                    "테스트 메뉴",
                    "테스트 메뉴 설명",
                    "/test",
                    "test-icon",
                    null,
                    1,
                    false
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMenuKey()).isEqualTo("TEST_MENU");
            assertThat(result.getMenuName()).isEqualTo("테스트 메뉴");
            assertThat(result.getTenant()).isEqualTo(testTenant);

            verify(menuRepository).existsByMenuKeyAndTenant("TEST_MENU", testTenant, 0L);
            verify(menuRepository).save(any(Menu.class));
        }
    }

    @Test
    @DisplayName("메뉴 생성 - 중복 메뉴 키 예외")
    void createMenu_DuplicateMenuKey_ThrowsException() {
        // Given
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);
            
            when(menuRepository.existsByMenuKeyAndTenant(anyString(), any(Tenant.class), anyLong()))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> menuService.createMenu(
                    "TEST_MENU",
                    "테스트 메뉴",
                    "테스트 메뉴 설명",
                    "/test",
                    "test-icon",
                    null,
                    1,
                    false
            )).isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MenuErrorCode.DUPLICATE_MENU_KEY);

            verify(menuRepository).existsByMenuKeyAndTenant("TEST_MENU", testTenant, 0L);
            verify(menuRepository, never()).save(any(Menu.class));
        }
    }

    @Test
    @DisplayName("메뉴 조회 - 성공")
    void getMenuById_Success() {
        // Given
        when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));

        // When
        Menu result = menuService.getMenuById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMenuKey()).isEqualTo("TEST_MENU");
        assertThat(result.getMenuName()).isEqualTo("테스트 메뉴");

        verify(menuRepository).findById(1L);
    }

    @Test
    @DisplayName("메뉴 조회 - 메뉴 없음 예외")
    void getMenuById_NotFound_ThrowsException() {
        // Given
        when(menuRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> menuService.getMenuById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", MenuErrorCode.MENU_NOT_FOUND);

        verify(menuRepository).findById(1L);
    }

    @Test
    @DisplayName("메뉴 수정 - 성공")
    void updateMenu_Success() {
        // Given
        when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));
        when(menuRepository.existsByMenuKeyAndTenant(anyString(), any(Tenant.class), anyLong()))
                .thenReturn(false);
        when(menuRepository.save(any(Menu.class))).thenReturn(testMenu);

        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);

            // When
            Menu result = menuService.updateMenu(
                    1L,
                    "UPDATED_MENU",
                    "수정된 메뉴",
                    "수정된 설명",
                    "/updated",
                    "updated-icon",
                    null,
                    2,
                    true
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMenuKey()).isEqualTo("UPDATED_MENU");
            assertThat(result.getMenuName()).isEqualTo("수정된 메뉴");

            verify(menuRepository).findById(1L);
            verify(menuRepository).existsByMenuKeyAndTenant("UPDATED_MENU", testTenant, 1L);
            verify(menuRepository).save(any(Menu.class));
        }
    }

    @Test
    @DisplayName("메뉴 삭제 - 성공")
    void deleteMenu_Success() {
        // Given
        when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));
        when(menuRepository.findByParentIdAndTenant(anyLong(), any(Tenant.class)))
                .thenReturn(List.of());

        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);

            // When
            menuService.deleteMenu(1L);

            // Then
            verify(menuRepository).findById(1L);
            verify(menuRepository).findByParentIdAndTenant(1L, testTenant);
            verify(menuRepository).save(any(Menu.class));
            verify(menuPermissionRepository).deleteByMenuId(1L);
        }
    }

    @Test
    @DisplayName("메뉴 삭제 - 시스템 메뉴 예외")
    void deleteMenu_SystemMenu_ThrowsException() {
        // Given
        Menu systemMenu = Menu.builder()
                .menuKey("SYSTEM_MENU")
                .menuName("시스템 메뉴")
                .isSystem(true)
                .build();
        systemMenu.setTenant(testTenant);

        when(menuRepository.findById(1L)).thenReturn(Optional.of(systemMenu));

        // When & Then
        assertThatThrownBy(() -> menuService.deleteMenu(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MenuErrorCode.SYSTEM_MENU_CANNOT_DELETE);

        verify(menuRepository).findById(1L);
        verify(menuRepository, never()).save(any(Menu.class));
    }

    @Test
    @DisplayName("메뉴 삭제 - 하위 메뉴 존재 예외")
    void deleteMenu_WithChildren_ThrowsException() {
        // Given
        Menu childMenu = Menu.builder()
                .menuKey("CHILD_MENU")
                .menuName("하위 메뉴")
                .parentId(1L)
                .build();
        childMenu.setTenant(testTenant);

        when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));
        when(menuRepository.findByParentIdAndTenant(1L, testTenant))
                .thenReturn(List.of(childMenu));

        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            mockedStatic.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(testTenant);

            // When & Then
            assertThatThrownBy(() -> menuService.deleteMenu(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MenuErrorCode.CANNOT_DELETE_MENU_WITH_CHILDREN);

            verify(menuRepository).findById(1L);
            verify(menuRepository).findByParentIdAndTenant(1L, testTenant);
            verify(menuRepository, never()).save(any(Menu.class));
        }
    }

    @Test
    @DisplayName("메뉴 권한 할당 - 성공")
    void assignMenuPermission_Success() {
        // Given
        Permission permission = Permission.builder()
                .permissionKey("TEST_PERMISSION")
                .permissionName("테스트 권한")
                .build();

        MenuPermission menuPermission = MenuPermission.builder()
                .menu(testMenu)
                .permission(permission)
                .accessType(MenuPermission.AccessType.READ)
                .build();

        when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(menuPermissionRepository.existsByMenuIdAndPermissionId(1L, 1L)).thenReturn(false);
        when(menuPermissionRepository.save(any(MenuPermission.class))).thenReturn(menuPermission);

        // When
        MenuPermission result = menuService.assignMenuPermission(1L, 1L, MenuPermission.AccessType.READ);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMenu()).isEqualTo(testMenu);
        assertThat(result.getPermission()).isEqualTo(permission);
        assertThat(result.getAccessType()).isEqualTo(MenuPermission.AccessType.READ);

        verify(menuRepository).findById(1L);
        verify(permissionRepository).findById(1L);
        verify(menuPermissionRepository).existsByMenuIdAndPermissionId(1L, 1L);
        verify(menuPermissionRepository).save(any(MenuPermission.class));
    }

    @Test
    @DisplayName("메뉴 권한 할당 - 중복 권한 예외")
    void assignMenuPermission_DuplicatePermission_ThrowsException() {
        // Given
        when(menuRepository.findById(1L)).thenReturn(Optional.of(testMenu));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(Permission.builder().build()));
        when(menuPermissionRepository.existsByMenuIdAndPermissionId(1L, 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> menuService.assignMenuPermission(1L, 1L, MenuPermission.AccessType.READ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MenuErrorCode.DUPLICATE_MENU_PERMISSION);

        verify(menuRepository).findById(1L);
        verify(permissionRepository).findById(1L);
        verify(menuPermissionRepository).existsByMenuIdAndPermissionId(1L, 1L);
        verify(menuPermissionRepository, never()).save(any(MenuPermission.class));
    }
}
