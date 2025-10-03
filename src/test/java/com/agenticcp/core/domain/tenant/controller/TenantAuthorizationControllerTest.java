package com.agenticcp.core.domain.tenant.controller;

import com.agenticcp.core.domain.tenant.service.TenantAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAuthorizationControllerTest {

    @Mock
    private TenantAuthorizationService tenantAuthorizationService;

    private TenantAuthorizationController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new TenantAuthorizationController(tenantAuthorizationService);
    }

    @Test
    void GET_roles_정상동작() {
        // Given
        when(tenantAuthorizationService.getTenantRoles(anyString()))
                .thenReturn(Collections.emptyList());

        // When
        var result = controller.getTenantRoles("t-001");

        // Then
        verify(tenantAuthorizationService).getTenantRoles("t-001");
        assert result.getStatusCode().is2xxSuccessful();
    }

    @Test
    void GET_permissions_정상동작() {
        // Given
        when(tenantAuthorizationService.getTenantPermissions(anyString()))
                .thenReturn(Collections.emptyList());

        // When
        var result = controller.getTenantPermissions("t-001");

        // Then
        verify(tenantAuthorizationService).getTenantPermissions("t-001");
        assert result.getStatusCode().is2xxSuccessful();
    }

    @Test
    void POST_init_permissions_정상동작() {
        // Given
        Mockito.doNothing().when(tenantAuthorizationService).initializeTenantPermissions(anyString());

        // When
        var result = controller.initializeTenantPermissions("t-001");

        // Then
        verify(tenantAuthorizationService).initializeTenantPermissions("t-001");
        assert result.getStatusCode().is2xxSuccessful();
    }

    @Test
    void POST_cache_evict_정상동작() {
        // Given
        Mockito.doNothing().when(tenantAuthorizationService).evictTenantCache(anyString());

        // When
        var result = controller.evictTenantCache("t-001");

        // Then
        verify(tenantAuthorizationService).evictTenantCache("t-001");
        assert result.getStatusCode().is2xxSuccessful();
    }

    // 시나리오 1: 테넌트별 역할 격리
    @Test
    void 시나리오1_테넌트별_역할_격리() {
        var roleA = com.agenticcp.core.domain.user.dto.RoleResponse.builder().roleKey("ADMIN").tenantKey("tenantA").build();
        var roleB = com.agenticcp.core.domain.user.dto.RoleResponse.builder().roleKey("ADMIN").tenantKey("tenantB").build();

        when(tenantAuthorizationService.getTenantRoles("tenantA"))
                .thenReturn(List.of(roleA));
        when(tenantAuthorizationService.getTenantRoles("tenantB"))
                .thenReturn(List.of(roleB));

        var resultA = controller.getTenantRoles("tenantA");
        var resultB = controller.getTenantRoles("tenantB");

        assert resultA.getStatusCode().is2xxSuccessful();
        assert resultB.getStatusCode().is2xxSuccessful();
        verify(tenantAuthorizationService).getTenantRoles("tenantA");
        verify(tenantAuthorizationService).getTenantRoles("tenantB");
    }

    // 시나리오 2: 권한 데이터 격리
    @Test
    void 시나리오2_권한_데이터_격리() {
        var permA = com.agenticcp.core.domain.user.dto.PermissionResponse.builder().permissionKey("READ_ONLY").tenantKey("tenantA").build();
        var permB = com.agenticcp.core.domain.user.dto.PermissionResponse.builder().permissionKey("READ_ONLY").tenantKey("tenantB").build();

        when(tenantAuthorizationService.getTenantPermissions("tenantA"))
                .thenReturn(List.of(permA));
        when(tenantAuthorizationService.getTenantPermissions("tenantB"))
                .thenReturn(List.of(permB));

        var resultA = controller.getTenantPermissions("tenantA");
        var resultB = controller.getTenantPermissions("tenantB");

        assert resultA.getStatusCode().is2xxSuccessful();
        assert resultB.getStatusCode().is2xxSuccessful();
        verify(tenantAuthorizationService).getTenantPermissions("tenantA");
        verify(tenantAuthorizationService).getTenantPermissions("tenantB");
    }

    // 시나리오 3: 테넌트별 기본 권한 설정(초기화 후 조회 가능)
    @Test
    void 시나리오3_초기화후_기본_권한_역할_조회() {
        // Given
        Mockito.doNothing().when(tenantAuthorizationService).initializeTenantPermissions("tenantC");
        when(tenantAuthorizationService.getTenantRoles("tenantC"))
                .thenReturn(Collections.emptyList());
        when(tenantAuthorizationService.getTenantPermissions("tenantC"))
                .thenReturn(Collections.emptyList());

        // When - 초기화
        var initResult = controller.initializeTenantPermissions("tenantC");
        
        // When - 조회
        var rolesResult = controller.getTenantRoles("tenantC");
        var permissionsResult = controller.getTenantPermissions("tenantC");

        // Then
        assert initResult.getStatusCode().is2xxSuccessful();
        assert rolesResult.getStatusCode().is2xxSuccessful();
        assert permissionsResult.getStatusCode().is2xxSuccessful();
        
        verify(tenantAuthorizationService).initializeTenantPermissions("tenantC");
        verify(tenantAuthorizationService).getTenantRoles("tenantC");
        verify(tenantAuthorizationService).getTenantPermissions("tenantC");
    }
}