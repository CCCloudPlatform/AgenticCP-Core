package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.service.CloudProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cloud/providers")
@RequiredArgsConstructor
@Tag(name = "Cloud Provider Management", description = "클라우드 프로바이더 관리 API")
public class CloudProviderController {

    private final CloudProviderService cloudProviderService;

    @GetMapping
    @Operation(summary = "모든 클라우드 프로바이더 조회")
    public ResponseEntity<ApiResponse<List<CloudProvider>>> getAllProviders() {
        List<CloudProvider> providers = cloudProviderService.getAllProviders();
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @GetMapping("/active")
    @Operation(summary = "활성 클라우드 프로바이더 조회")
    public ResponseEntity<ApiResponse<List<CloudProvider>>> getActiveProviders() {
        List<CloudProvider> providers = cloudProviderService.getActiveProviders();
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @GetMapping("/{providerKey}")
    @Operation(summary = "특정 클라우드 프로바이더 조회")
    public ResponseEntity<ApiResponse<CloudProvider>> getProviderByKey(@PathVariable String providerKey) {
        return cloudProviderService.getProviderByKey(providerKey)
                .map(provider -> ResponseEntity.ok(ApiResponse.success(provider)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{providerType}")
    @Operation(summary = "프로바이더 타입별 조회")
    public ResponseEntity<ApiResponse<List<CloudProvider>>> getProvidersByType(
            @PathVariable CloudProvider.ProviderType providerType) {
        List<CloudProvider> providers = cloudProviderService.getProvidersByType(providerType);
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @GetMapping("/global")
    @Operation(summary = "글로벌 프로바이더 조회")
    public ResponseEntity<ApiResponse<List<CloudProvider>>> getGlobalProviders() {
        List<CloudProvider> providers = cloudProviderService.getGlobalProviders();
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @GetMapping("/government")
    @Operation(summary = "정부용 프로바이더 조회")
    public ResponseEntity<ApiResponse<List<CloudProvider>>> getGovernmentProviders() {
        List<CloudProvider> providers = cloudProviderService.getGovernmentProviders();
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @GetMapping("/sync-needed")
    @Operation(summary = "동기화가 필요한 프로바이더 조회")
    public ResponseEntity<ApiResponse<List<CloudProvider>>> getProvidersNeedingSync(
            @RequestParam(defaultValue = "24") int hoursSinceLastSync) {
        List<CloudProvider> providers = cloudProviderService.getProvidersNeedingSync(hoursSinceLastSync);
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @GetMapping("/count/active")
    @Operation(summary = "활성 프로바이더 수 조회")
    public ResponseEntity<ApiResponse<Long>> getActiveProviderCount() {
        Long count = cloudProviderService.getActiveProviderCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping
    @Operation(summary = "클라우드 프로바이더 생성")
    public ResponseEntity<ApiResponse<CloudProvider>> createProvider(@RequestBody CloudProvider cloudProvider) {
        CloudProvider createdProvider = cloudProviderService.createProvider(cloudProvider);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdProvider, "클라우드 프로바이더가 생성되었습니다."));
    }

    @PutMapping("/{providerKey}")
    @Operation(summary = "클라우드 프로바이더 수정")
    public ResponseEntity<ApiResponse<CloudProvider>> updateProvider(
            @PathVariable String providerKey, 
            @RequestBody CloudProvider cloudProvider) {
        CloudProvider updatedProvider = cloudProviderService.updateProvider(providerKey, cloudProvider);
        return ResponseEntity.ok(ApiResponse.success(updatedProvider, "클라우드 프로바이더가 수정되었습니다."));
    }

    @PatchMapping("/{providerKey}/sync")
    @Operation(summary = "프로바이더 동기화 시간 업데이트")
    public ResponseEntity<ApiResponse<CloudProvider>> updateLastSync(@PathVariable String providerKey) {
        CloudProvider updatedProvider = cloudProviderService.updateLastSync(providerKey);
        return ResponseEntity.ok(ApiResponse.success(updatedProvider, "동기화 시간이 업데이트되었습니다."));
    }

    @PatchMapping("/{providerKey}/activate")
    @Operation(summary = "프로바이더 활성화")
    public ResponseEntity<ApiResponse<CloudProvider>> activateProvider(@PathVariable String providerKey) {
        CloudProvider activatedProvider = cloudProviderService.activateProvider(providerKey);
        return ResponseEntity.ok(ApiResponse.success(activatedProvider, "프로바이더가 활성화되었습니다."));
    }

    @PatchMapping("/{providerKey}/deactivate")
    @Operation(summary = "프로바이더 비활성화")
    public ResponseEntity<ApiResponse<CloudProvider>> deactivateProvider(@PathVariable String providerKey) {
        CloudProvider deactivatedProvider = cloudProviderService.deactivateProvider(providerKey);
        return ResponseEntity.ok(ApiResponse.success(deactivatedProvider, "프로바이더가 비활성화되었습니다."));
    }

    @DeleteMapping("/{providerKey}")
    @Operation(summary = "클라우드 프로바이더 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(@PathVariable String providerKey) {
        cloudProviderService.deleteProvider(providerKey);
        return ResponseEntity.ok(ApiResponse.success(null, "클라우드 프로바이더가 삭제되었습니다."));
    }
}
