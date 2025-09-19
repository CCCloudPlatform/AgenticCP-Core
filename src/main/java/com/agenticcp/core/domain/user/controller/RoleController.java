package com.agenticcp.core.domain.user.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.user.dto.CreateRoleRequest;
import com.agenticcp.core.domain.user.dto.RoleResponse;
import com.agenticcp.core.domain.user.dto.UpdateRoleRequest;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.service.RoleService;
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
 * 역할 관리 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "역할 관리 API")
public class RoleController {
    
    private final RoleService roleService;
    
    @GetMapping
    @Operation(summary = "모든 역할 조회", description = "현재 테넌트의 모든 역할을 조회합니다")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        List<RoleResponse> roleResponses = roles.stream()
                .map(roleService::toRoleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(roleResponses));
    }
    
    @GetMapping("/active")
    @Operation(summary = "활성 역할 조회", description = "현재 테넌트의 활성 역할을 조회합니다")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getActiveRoles() {
        List<Role> roles = roleService.getActiveRoles();
        List<RoleResponse> roleResponses = roles.stream()
                .map(roleService::toRoleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(roleResponses));
    }
    
    @GetMapping("/system")
    @Operation(summary = "시스템 역할 조회", description = "현재 테넌트의 시스템 역할을 조회합니다")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getSystemRoles() {
        List<Role> roles = roleService.getSystemRoles();
        List<RoleResponse> roleResponses = roles.stream()
                .map(roleService::toRoleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(roleResponses));
    }
    
    @GetMapping("/default")
    @Operation(summary = "기본 역할 조회", description = "현재 테넌트의 기본 역할을 조회합니다")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getDefaultRoles() {
        List<Role> roles = roleService.getDefaultRoles();
        List<RoleResponse> roleResponses = roles.stream()
                .map(roleService::toRoleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(roleResponses));
    }
    
    @GetMapping("/search")
    @Operation(summary = "역할 검색", description = "키워드로 역할을 검색합니다")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> searchRoles(
            @Parameter(description = "검색 키워드") @RequestParam String keyword) {
        List<Role> roles = roleService.searchRoles(keyword);
        List<RoleResponse> roleResponses = roles.stream()
                .map(roleService::toRoleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(roleResponses));
    }
    
    @GetMapping("/{roleKey}")
    @Operation(summary = "특정 역할 조회", description = "역할 키로 특정 역할을 조회합니다")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleByKey(
            @Parameter(description = "역할 키") @PathVariable String roleKey) {
        return roleService.getRoleByKey(roleKey)
                .map(role -> ResponseEntity.ok(ApiResponse.success(roleService.toRoleResponse(role))))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "역할 생성", description = "새로운 역할을 생성합니다")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.createRole(request);
        RoleResponse response = roleService.toRoleResponse(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "역할이 생성되었습니다"));
    }
    
    @PutMapping("/{roleKey}")
    @Operation(summary = "역할 수정", description = "기존 역할을 수정합니다")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @Parameter(description = "역할 키") @PathVariable String roleKey,
            @Valid @RequestBody UpdateRoleRequest request) {
        Role role = roleService.updateRole(roleKey, request);
        RoleResponse response = roleService.toRoleResponse(role);
        return ResponseEntity.ok(ApiResponse.success(response, "역할이 수정되었습니다"));
    }
    
    @DeleteMapping("/{roleKey}")
    @Operation(summary = "역할 삭제", description = "역할을 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @Parameter(description = "역할 키") @PathVariable String roleKey) {
        roleService.deleteRole(roleKey);
        return ResponseEntity.ok(ApiResponse.success(null, "역할이 삭제되었습니다"));
    }
    
    @PostMapping("/{roleId}/permissions")
    @Operation(summary = "역할에 권한 할당", description = "역할에 권한을 할당합니다")
    public ResponseEntity<ApiResponse<Void>> assignPermissionsToRole(
            @Parameter(description = "역할 ID") @PathVariable Long roleId,
            @Parameter(description = "권한 키 목록") @RequestBody List<String> permissionKeys) {
        roleService.assignPermissionsToRole(roleId, permissionKeys);
        return ResponseEntity.ok(ApiResponse.success(null, "권한이 할당되었습니다"));
    }
    
    @PutMapping("/{roleId}/permissions")
    @Operation(summary = "역할 권한 업데이트", description = "역할의 권한을 업데이트합니다")
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(
            @Parameter(description = "역할 ID") @PathVariable Long roleId,
            @Parameter(description = "권한 키 목록") @RequestBody List<String> permissionKeys) {
        roleService.updateRolePermissions(roleId, permissionKeys);
        return ResponseEntity.ok(ApiResponse.success(null, "역할 권한이 업데이트되었습니다"));
    }
    
    @DeleteMapping("/{roleId}/permissions/{permissionKey}")
    @Operation(summary = "역할에서 권한 제거", description = "역할에서 특정 권한을 제거합니다")
    public ResponseEntity<ApiResponse<Void>> removePermissionFromRole(
            @Parameter(description = "역할 ID") @PathVariable Long roleId,
            @Parameter(description = "권한 키") @PathVariable String permissionKey) {
        roleService.removePermissionFromRole(roleId, permissionKey);
        return ResponseEntity.ok(ApiResponse.success(null, "권한이 제거되었습니다"));
    }
}
