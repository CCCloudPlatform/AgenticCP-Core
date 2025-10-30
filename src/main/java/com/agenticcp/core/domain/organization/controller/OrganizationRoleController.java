package com.agenticcp.core.domain.organization.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.organization.dto.OrganizationRoleDtos;
import com.agenticcp.core.domain.organization.entity.OrganizationRole;
import com.agenticcp.core.domain.organization.service.OrganizationRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * 조직별 역할 API 컨트롤러
 * 조직-역할 매핑 CRUD 및 설정 변경 엔드포인트를 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/roles")
@RequiredArgsConstructor
public class OrganizationRoleController {

    private final OrganizationRoleService organizationRoleService;

    @GetMapping
    public ResponseEntity<ApiResponse<OrganizationRoleDtos.OrganizationRoleListResponse>> list(@PathVariable Long organizationId) {
        var list = organizationRoleService.listRoles(organizationId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        var body = OrganizationRoleDtos.OrganizationRoleListResponse.builder()
                .items(list)
                .count(list.size())
                .build();
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationRoleDtos.OrganizationRoleResponse>> assign(
            @PathVariable Long organizationId,
            @Valid @RequestBody OrganizationRoleDtos.AssignRoleRequest request
    ) {
        var saved = organizationRoleService.assignRole(
                organizationId,
                request.getRoleId(),
                Boolean.TRUE.equals(request.getMakeDefault()),
                request.getPriority()
        );
        return ResponseEntity.ok(ApiResponse.success(toResponse(saved)));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable Long organizationId,
            @PathVariable Long roleId
    ) {
        organizationRoleService.removeRole(organizationId, roleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<ApiResponse<OrganizationRoleDtos.OrganizationRoleResponse>> update(
            @PathVariable Long organizationId,
            @PathVariable Long roleId,
            @Valid @RequestBody OrganizationRoleDtos.UpdateRoleSettingsRequest request
    ) {
        if (request.getPriority() != null) {
            var updated = organizationRoleService.updatePriority(organizationId, roleId, request.getPriority());
            if (Boolean.TRUE.equals(request.getMakeDefault())) {
                updated = organizationRoleService.setDefaultRole(organizationId, roleId);
            }
            if (request.getStatus() != null) {
                updated.setStatus(Status.valueOf(request.getStatus()));
            }
            return ResponseEntity.ok(ApiResponse.success(toResponse(updated)));
        }

        if (Boolean.TRUE.equals(request.getMakeDefault())) {
            var updated = organizationRoleService.setDefaultRole(organizationId, roleId);
            if (request.getStatus() != null) {
                updated.setStatus(Status.valueOf(request.getStatus()));
            }
            return ResponseEntity.ok(ApiResponse.success(toResponse(updated)));
        }

        var list = organizationRoleService.listRoles(organizationId);
        var current = list.stream().filter(m -> m.getRole().getId().equals(roleId)).findFirst().orElse(null);
        if (current != null && request.getStatus() != null) {
            current.setStatus(Status.valueOf(request.getStatus()));
        }
        return ResponseEntity.ok(ApiResponse.success(current == null ? null : toResponse(current)));
    }

    private OrganizationRoleDtos.OrganizationRoleResponse toResponse(OrganizationRole orl) {
        return OrganizationRoleDtos.OrganizationRoleResponse.builder()
                .mappingId(orl.getId())
                .roleId(orl.getRole().getId())
                .roleKey(orl.getRole().getRoleKey())
                .roleName(orl.getRole().getRoleName())
                .isDefault(orl.getIsDefault())
                .priority(orl.getPriority())
                .status(String.valueOf(orl.getStatus()))
                .build();
    }
}


