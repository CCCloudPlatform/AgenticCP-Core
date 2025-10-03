package com.agenticcp.core.domain.tenant.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.tenant.service.TenantAuthorizationService;
import com.agenticcp.core.domain.user.dto.PermissionResponse;
import com.agenticcp.core.domain.user.dto.RoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants/{tenantKey}")
@RequiredArgsConstructor
@Tag(name = "Tenant Authorization", description = "테넌트 권한/역할 관리 API")
public class TenantAuthorizationController {

    private final TenantAuthorizationService tenantAuthorizationService;

    @PostMapping("/init-permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "테넌트 기본 권한/역할 초기화", description = "신규 테넌트의 기본 권한/역할을 초기화합니다")
    public ResponseEntity<ApiResponse<Void>> initializeTenantPermissions(@PathVariable String tenantKey) {
        tenantAuthorizationService.initializeTenantPermissions(tenantKey);
        return ResponseEntity.ok(ApiResponse.success(null, "테넌트 권한/역할이 초기화되었습니다"));
    }

    @GetMapping("/roles")
    @Operation(summary = "테넌트 역할 조회", description = "테넌트별 역할 목록을 조회합니다(캐시 10분)")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getTenantRoles(@PathVariable String tenantKey) {
        List<RoleResponse> roles = tenantAuthorizationService.getTenantRoles(tenantKey);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/permissions")
    @Operation(summary = "테넌트 권한 조회", description = "테넌트별 권한 목록을 조회합니다(캐시 10분)")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getTenantPermissions(@PathVariable String tenantKey) {
        List<PermissionResponse> permissions = tenantAuthorizationService.getTenantPermissions(tenantKey);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @PostMapping("/cache/evict")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "테넌트 캐시 무효화", description = "테넌트의 역할/권한 캐시를 무효화합니다")
    public ResponseEntity<ApiResponse<Void>> evictTenantCache(@PathVariable String tenantKey) {
        tenantAuthorizationService.evictTenantCache(tenantKey);
        return ResponseEntity.ok(ApiResponse.success(null, "캐시가 무효화되었습니다"));
    }
}


