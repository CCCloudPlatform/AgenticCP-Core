package com.agenticcp.core.domain.user.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.user.dto.CreateRoleRequest;
import com.agenticcp.core.domain.user.dto.RoleResponse;
import com.agenticcp.core.domain.user.dto.UpdateRoleRequest;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.service.RoleService;
import com.agenticcp.core.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 역할 관리 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-10
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "역할 관리 API")
public class RoleController {
    
    private final RoleService roleService;
    private final UserService userService;
    
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

    @PostMapping("/users/{username}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "사용자에게 역할 할당",
            description = "사용자에게 역할을 할당합니다. 여러 역할을 동시에 할당할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "역할 할당 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (역할을 찾을 수 없음 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (USER_UPDATE 권한 또는 SUPER_ADMIN 역할 필요)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Void>> assignRolesToUser(
            @Parameter(description = "사용자명", required = true, example = "testuser")
            @PathVariable String username,
            @Parameter(description = "역할 키 목록", required = true)
            @RequestBody List<String> roleKeys) {
        userService.assignRolesToUser(username, roleKeys);
        return ResponseEntity.ok(ApiResponse.success(null, "역할이 할당되었습니다."));
    }

    @DeleteMapping("/users/{username}/{roleKey}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "사용자에서 역할 제거",
            description = "사용자에서 특정 역할을 제거합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "역할 제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Void>> removeRoleFromUser(
            @Parameter(description = "사용자명", required = true, example = "testuser")
            @PathVariable String username,
            @Parameter(description = "역할 키", required = true, example = "OBJECT_STORAGE_ADMIN")
            @PathVariable String roleKey) {
        userService.removeRoleFromUser(username, roleKey);
        return ResponseEntity.ok(ApiResponse.success(null, "역할이 제거되었습니다."));
    }
}
