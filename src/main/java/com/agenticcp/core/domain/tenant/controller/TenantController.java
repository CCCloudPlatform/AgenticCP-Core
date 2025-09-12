package com.agenticcp.core.domain.tenant.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "테넌트 관리 API")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @Operation(summary = "모든 테넌트 조회")
    public ResponseEntity<ApiResponse<List<Tenant>>> getAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }

    @GetMapping("/active")
    @Operation(summary = "활성 테넌트 조회")
    public ResponseEntity<ApiResponse<List<Tenant>>> getActiveTenants() {
        List<Tenant> tenants = tenantService.getActiveTenants();
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }

    @GetMapping("/{tenantKey}")
    @Operation(summary = "특정 테넌트 조회")
    public ResponseEntity<ApiResponse<Tenant>> getTenantByKey(@PathVariable String tenantKey) {
        return tenantService.getTenantByKey(tenantKey)
                .map(tenant -> ResponseEntity.ok(ApiResponse.success(tenant)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{tenantType}")
    @Operation(summary = "테넌트 타입별 조회")
    public ResponseEntity<ApiResponse<List<Tenant>>> getTenantsByType(
            @PathVariable Tenant.TenantType tenantType) {
        List<Tenant> tenants = tenantService.getTenantsByType(tenantType);
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }

    @GetMapping("/trial/active")
    @Operation(summary = "활성 트라이얼 테넌트 조회")
    public ResponseEntity<ApiResponse<List<Tenant>>> getActiveTrialTenants() {
        List<Tenant> tenants = tenantService.getActiveTrialTenants();
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }

    @GetMapping("/expired")
    @Operation(summary = "만료된 테넌트 조회")
    public ResponseEntity<ApiResponse<List<Tenant>>> getExpiredTenants() {
        List<Tenant> tenants = tenantService.getExpiredTenants();
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }

    @GetMapping("/count/active")
    @Operation(summary = "활성 테넌트 수 조회")
    public ResponseEntity<ApiResponse<Long>> getActiveTenantCount() {
        Long count = tenantService.getActiveTenantCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping
    @Operation(summary = "테넌트 생성")
    public ResponseEntity<ApiResponse<Tenant>> createTenant(@RequestBody Tenant tenant) {
        Tenant createdTenant = tenantService.createTenant(tenant);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdTenant, "테넌트가 생성되었습니다."));
    }

    @PutMapping("/{tenantKey}")
    @Operation(summary = "테넌트 수정")
    public ResponseEntity<ApiResponse<Tenant>> updateTenant(
            @PathVariable String tenantKey, 
            @RequestBody Tenant tenant) {
        Tenant updatedTenant = tenantService.updateTenant(tenantKey, tenant);
        return ResponseEntity.ok(ApiResponse.success(updatedTenant, "테넌트가 수정되었습니다."));
    }

    @PatchMapping("/{tenantKey}/suspend")
    @Operation(summary = "테넌트 일시정지")
    public ResponseEntity<ApiResponse<Tenant>> suspendTenant(@PathVariable String tenantKey) {
        Tenant suspendedTenant = tenantService.suspendTenant(tenantKey);
        return ResponseEntity.ok(ApiResponse.success(suspendedTenant, "테넌트가 일시정지되었습니다."));
    }

    @PatchMapping("/{tenantKey}/activate")
    @Operation(summary = "테넌트 활성화")
    public ResponseEntity<ApiResponse<Tenant>> activateTenant(@PathVariable String tenantKey) {
        Tenant activatedTenant = tenantService.activateTenant(tenantKey);
        return ResponseEntity.ok(ApiResponse.success(activatedTenant, "테넌트가 활성화되었습니다."));
    }

    @DeleteMapping("/{tenantKey}")
    @Operation(summary = "테넌트 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable String tenantKey) {
        tenantService.deleteTenant(tenantKey);
        return ResponseEntity.ok(ApiResponse.success(null, "테넌트가 삭제되었습니다."));
    }
}
