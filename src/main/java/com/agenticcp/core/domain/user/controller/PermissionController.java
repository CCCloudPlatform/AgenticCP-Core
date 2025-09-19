package com.agenticcp.core.domain.user.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.user.dto.CreatePermissionRequest;
import com.agenticcp.core.domain.user.dto.PermissionResponse;
import com.agenticcp.core.domain.user.dto.UpdatePermissionRequest;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 권한 관리 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "권한 관리 API")
public class PermissionController {
    
    private final PermissionService permissionService;
    
    @GetMapping
    @Operation(summary = "모든 권한 조회", description = "현재 테넌트의 모든 권한을 조회합니다")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(permissionService::toPermissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissionResponses));
    }
    
    @GetMapping("/active")
    @Operation(summary = "활성 권한 조회", description = "현재 테넌트의 활성 권한을 조회합니다")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getActivePermissions() {
        List<Permission> permissions = permissionService.getActivePermissions();
        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(permissionService::toPermissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissionResponses));
    }
    
    @GetMapping("/system")
    @Operation(summary = "시스템 권한 조회", description = "현재 테넌트의 시스템 권한을 조회합니다")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getSystemPermissions() {
        List<Permission> permissions = permissionService.getSystemPermissions();
        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(permissionService::toPermissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissionResponses));
    }
    
    @GetMapping("/category/{category}")
    @Operation(summary = "카테고리별 권한 조회", description = "특정 카테고리의 권한을 조회합니다")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissionsByCategory(
            @Parameter(description = "카테고리") @PathVariable String category) {
        List<Permission> permissions = permissionService.getPermissionsByCategory(category);
        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(permissionService::toPermissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissionResponses));
    }
    
    @GetMapping("/resource/{resource}")
    @Operation(summary = "리소스별 권한 조회", description = "특정 리소스의 권한을 조회합니다")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissionsByResource(
            @Parameter(description = "리소스") @PathVariable String resource) {
        List<Permission> permissions = permissionService.getPermissionsByResource(resource);
        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(permissionService::toPermissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissionResponses));
    }
    
    @GetMapping("/action/{action}")
    @Operation(summary = "액션별 권한 조회", description = "특정 액션의 권한을 조회합니다")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissionsByAction(
            @Parameter(description = "액션") @PathVariable String action) {
        List<Permission> permissions = permissionService.getPermissionsByAction(action);
        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(permissionService::toPermissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissionResponses));
    }
    
    @GetMapping("/resource/{resource}/action/{action}")
    @Operation(summary = "리소스와 액션으로 권한 조회", description = "특정 리소스와 액션의 권한을 조회합니다")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissionsByResourceAndAction(
            @Parameter(description = "리소스") @PathVariable String resource,
            @Parameter(description = "액션") @PathVariable String action) {
        List<Permission> permissions = permissionService.getPermissionsByResourceAndAction(resource, action);
        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(permissionService::toPermissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissionResponses));
    }
    
    @GetMapping("/search")
    @Operation(summary = "권한 검색", description = "키워드로 권한을 검색합니다")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> searchPermissions(
            @Parameter(description = "검색 키워드") @RequestParam String keyword) {
        List<Permission> permissions = permissionService.searchPermissions(keyword);
        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(permissionService::toPermissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissionResponses));
    }
    
    @GetMapping("/{permissionKey}")
    @Operation(summary = "특정 권한 조회", description = "권한 키로 특정 권한을 조회합니다")
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermissionByKey(
            @Parameter(description = "권한 키") @PathVariable String permissionKey) {
        return permissionService.getPermissionByKey(permissionKey)
                .map(permission -> ResponseEntity.ok(ApiResponse.success(permissionService.toPermissionResponse(permission))))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "권한 생성", description = "새로운 권한을 생성합니다")
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        Permission permission = permissionService.createPermission(request);
        PermissionResponse response = permissionService.toPermissionResponse(permission);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "권한이 생성되었습니다"));
    }
    
    @PutMapping("/{permissionKey}")
    @Operation(summary = "권한 수정", description = "기존 권한을 수정합니다")
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
            @Parameter(description = "권한 키") @PathVariable String permissionKey,
            @Valid @RequestBody UpdatePermissionRequest request) {
        Permission permission = permissionService.updatePermission(permissionKey, request);
        PermissionResponse response = permissionService.toPermissionResponse(permission);
        return ResponseEntity.ok(ApiResponse.success(response, "권한이 수정되었습니다"));
    }
    
    @DeleteMapping("/{permissionKey}")
    @Operation(summary = "권한 삭제", description = "권한을 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> deletePermission(
            @Parameter(description = "권한 키") @PathVariable String permissionKey) {
        permissionService.deletePermission(permissionKey);
        return ResponseEntity.ok(ApiResponse.success(null, "권한이 삭제되었습니다"));
    }
}
