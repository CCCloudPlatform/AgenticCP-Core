package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.security.annotation.RequirePermission;
import com.agenticcp.core.domain.security.annotation.RequireRole;
import com.agenticcp.core.domain.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
public class SecurityInspectorController {

    private final AuthorizationService authorizationService;

    @GetMapping("/check/permission")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkPermission(@RequestParam String permissionKey) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean result = authorizationService.hasPermission(username, permissionKey);
        return ResponseEntity.ok(ApiResponse.success(Map.of("hasPermission", result)));
    }

    @GetMapping("/check/role")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkRole(@RequestParam String roleKey) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean result = authorizationService.hasRole(username, roleKey);
        return ResponseEntity.ok(ApiResponse.success(Map.of("hasRole", result)));
    }

    @PostMapping("/tenant/validate")
    public ResponseEntity<Void> validateTenant(@RequestHeader(value = "X-Tenant-Key", required = false) String tenantHeader,
                                               @RequestBody(required = false) Map<String, String> body) {
        String tenantKey = tenantHeader != null ? tenantHeader : (body != null ? body.get("tenantKey") : null);
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        authorizationService.validateTenantAccess(username, tenantKey);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> me() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        // displayName/tenantKey는 실제 UserService를 통해 가져올 수 있으나 여기서는 username만 반환
        return ResponseEntity.ok(ApiResponse.success(Map.of("username", username)));
    }

    @GetMapping("/me/permissions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myPermissions() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        var permissions = authorizationService.getUserPermissions(username);
        return ResponseEntity.ok(ApiResponse.success(Map.of("permissions", permissions)));
    }

    @PostMapping("/cache/permissions/evict")
    public ResponseEntity<Void> evictCache(@RequestBody(required = false) Map<String, String> body) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        String target = body != null && body.get("username") != null ? body.get("username") : username;
        authorizationService.evictUserPermissionCache(target);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/permissions/warm")
    public ResponseEntity<Void> warmCache(@RequestBody(required = false) Map<String, String> body) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        String target = body != null && body.get("username") != null ? body.get("username") : username;
        authorizationService.warmUserPermissionCache(target);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/_test/protected/permission")
    @RequirePermission("sample.permission")
    public ResponseEntity<ApiResponse<String>> protectedByPermission() {
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }

    @GetMapping("/_test/protected/role")
    @RequireRole({"TESTER"})
    public ResponseEntity<ApiResponse<String>> protectedByRole() {
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }

    @GetMapping("/_test/protected/preauthorize")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<ApiResponse<String>> protectedByPreAuthorize() {
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }
}


