package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.security.annotation.RequirePermission;
import com.agenticcp.core.domain.security.annotation.RequireRole;
import com.agenticcp.core.domain.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

/**
 * 보안 인스펙터 REST API 컨트롤러
 *
 * <p>인증된 사용자의 권한 및 역할 상태를 조회하고 캐시를 관리하기 위한 진단용 API를 제공합니다.</p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@RestController
@RequestMapping("/v1/security")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Security Inspector", description = "권한 및 역할 검증, 캐시 진단 API")
public class SecurityInspectorController {

    private final AuthorizationService authorizationService;

    @GetMapping("/check/permission")
    @Operation(summary = "권한 확인", description = "현재 사용자에게 특정 권한이 있는지 확인합니다.")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkPermission(@RequestParam String permissionKey) {
        String username = getCurrentUsername();
        log.info("[SecurityInspectorController] checkPermission - username={}, permissionKey={}", username, permissionKey);
        boolean result = authorizationService.hasPermission(username, permissionKey);
        return ResponseEntity.ok(ApiResponse.success(Map.of("hasPermission", result), "권한 확인을 완료했습니다."));
    }

    @GetMapping("/check/role")
    @Operation(summary = "역할 확인", description = "현재 사용자에게 특정 역할이 있는지 확인합니다.")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkRole(@RequestParam String roleKey) {
        String username = getCurrentUsername();
        log.info("[SecurityInspectorController] checkRole - username={}, roleKey={}", username, roleKey);
        boolean result = authorizationService.hasRole(username, roleKey);
        return ResponseEntity.ok(ApiResponse.success(Map.of("hasRole", result), "역할 확인을 완료했습니다."));
    }

    @PostMapping("/tenant/validate")
    @Operation(summary = "테넌트 검증", description = "현재 사용자가 요청한 테넌트에 접근할 수 있는지 검증합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateTenant(
            @RequestHeader(value = "X-Tenant-Key", required = false) String tenantHeader,
            @RequestBody(required = false) Map<String, String> body) {
        String tenantKey = tenantHeader != null ? tenantHeader : (body != null ? body.get("tenantKey") : null);
        String username = getCurrentUsername();
        log.info("[SecurityInspectorController] validateTenant - username={}, tenantKey={}", username, tenantKey);
        authorizationService.validateTenantAccess(username, tenantKey);
        return ResponseEntity.ok(ApiResponse.success(Map.of("tenantKey", tenantKey), "테넌트 접근이 허용되었습니다."));
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보", description = "인증된 사용자의 기본 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, String>>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("[SecurityInspectorController] me - username={}", username);
        return ResponseEntity.ok(ApiResponse.success(Map.of("username", username), "사용자 정보를 조회했습니다."));
    }

    @GetMapping("/me/permissions")
    @Operation(summary = "현재 사용자 권한", description = "인증된 사용자가 보유한 권한 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myPermissions() {
        String username = getCurrentUsername();
        log.info("[SecurityInspectorController] myPermissions - username={}", username);
        var permissions = authorizationService.getUserPermissions(username);
        return ResponseEntity.ok(ApiResponse.success(Map.of("permissions", permissions), "사용자 권한을 조회했습니다."));
    }

    @PostMapping("/cache/permissions/evict")
    @Operation(summary = "권한 캐시 무효화", description = "사용자 권한 캐시를 무효화합니다.")
    public ResponseEntity<ApiResponse<Map<String, String>>> evictCache(
            @RequestBody(required = false) Map<String, String> body) {
        String username = getCurrentUsername();
        String target = body != null && body.get("username") != null ? body.get("username") : username;
        log.info("[SecurityInspectorController] evictCache - requester={}, target={}", username, target);
        authorizationService.evictUserPermissionCache(target);
        return ResponseEntity.ok(ApiResponse.success(Map.of("username", target), "권한 캐시를 무효화했습니다."));
    }

    @PostMapping("/cache/permissions/warm")
    @Operation(summary = "권한 캐시 워밍업", description = "사용자 권한 캐시를 선제적으로 로드합니다.")
    public ResponseEntity<ApiResponse<Map<String, String>>> warmCache(
            @RequestBody(required = false) Map<String, String> body) {
        String username = getCurrentUsername();
        String target = body != null && body.get("username") != null ? body.get("username") : username;
        log.info("[SecurityInspectorController] warmCache - requester={}, target={}", username, target);
        authorizationService.warmUserPermissionCache(target);
        return ResponseEntity.ok(ApiResponse.success(Map.of("username", target), "권한 캐시를 워밍업했습니다."));
    }

    @GetMapping("/_test/protected/permission")
    @RequirePermission("sample.permission")
    @Operation(summary = "권한 보호 테스트", description = "권한 기반 보호가 정상 동작하는지 확인합니다.")
    public ResponseEntity<ApiResponse<String>> protectedByPermission() {
        String username = getCurrentUsername();
        log.info("[SecurityInspectorController] protectedByPermission - username={}", username);
        return ResponseEntity.ok(ApiResponse.success("OK", "권한 검증을 통과했습니다."));
    }

    @GetMapping("/_test/protected/role")
    @RequireRole({"TESTER"})
    @Operation(summary = "역할 보호 테스트", description = "역할 기반 보호가 정상 동작하는지 확인합니다.")
    public ResponseEntity<ApiResponse<String>> protectedByRole() {
        String username = getCurrentUsername();
        log.info("[SecurityInspectorController] protectedByRole - username={}", username);
        return ResponseEntity.ok(ApiResponse.success("OK", "역할 검증을 통과했습니다."));
    }

    @GetMapping("/_test/protected/preauthorize")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    @Operation(summary = "PreAuthorize 테스트", description = "Spring Security의 PreAuthorize 보호를 검증합니다.")
    public ResponseEntity<ApiResponse<String>> protectedByPreAuthorize() {
        String username = getCurrentUsername();
        log.info("[SecurityInspectorController] protectedByPreAuthorize - username={}", username);
        return ResponseEntity.ok(ApiResponse.success("OK", "PreAuthorize 검증을 통과했습니다."));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("[SecurityInspectorController] getCurrentUsername - unauthenticated access detected");
            throw new AccessDeniedException("인증되지 않은 사용자입니다");
        }
        return authentication.getName();
    }
}


