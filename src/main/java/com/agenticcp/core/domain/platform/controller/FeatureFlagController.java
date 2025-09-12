package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/feature-flags")
@RequiredArgsConstructor
@Tag(name = "Feature Flag", description = "기능 플래그 관리 API")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    @Operation(summary = "모든 기능 플래그 조회")
    public ResponseEntity<ApiResponse<List<FeatureFlag>>> getAllFlags() {
        List<FeatureFlag> flags = featureFlagService.getAllFlags();
        return ResponseEntity.ok(ApiResponse.success(flags));
    }

    @GetMapping("/active")
    @Operation(summary = "활성 기능 플래그 조회")
    public ResponseEntity<ApiResponse<List<FeatureFlag>>> getActiveFlags() {
        List<FeatureFlag> flags = featureFlagService.getActiveFlags();
        return ResponseEntity.ok(ApiResponse.success(flags));
    }

    @GetMapping("/{flagKey}")
    @Operation(summary = "특정 기능 플래그 조회")
    public ResponseEntity<ApiResponse<FeatureFlag>> getFlagByKey(@PathVariable String flagKey) {
        return featureFlagService.getFlagByKey(flagKey)
                .map(flag -> ResponseEntity.ok(ApiResponse.success(flag)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{flagKey}/enabled")
    @Operation(summary = "기능 플래그 활성화 상태 확인")
    public ResponseEntity<ApiResponse<Boolean>> isFlagEnabled(@PathVariable String flagKey) {
        boolean isEnabled = featureFlagService.isFlagEnabled(flagKey);
        return ResponseEntity.ok(ApiResponse.success(isEnabled));
    }

    @PostMapping
    @Operation(summary = "기능 플래그 생성")
    public ResponseEntity<ApiResponse<FeatureFlag>> createFlag(@RequestBody FeatureFlag featureFlag) {
        FeatureFlag createdFlag = featureFlagService.createFlag(featureFlag);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdFlag, "기능 플래그가 생성되었습니다."));
    }

    @PutMapping("/{flagKey}")
    @Operation(summary = "기능 플래그 수정")
    public ResponseEntity<ApiResponse<FeatureFlag>> updateFlag(
            @PathVariable String flagKey, 
            @RequestBody FeatureFlag featureFlag) {
        FeatureFlag updatedFlag = featureFlagService.updateFlag(flagKey, featureFlag);
        return ResponseEntity.ok(ApiResponse.success(updatedFlag, "기능 플래그가 수정되었습니다."));
    }

    @PatchMapping("/{flagKey}/toggle")
    @Operation(summary = "기능 플래그 토글")
    public ResponseEntity<ApiResponse<FeatureFlag>> toggleFlag(
            @PathVariable String flagKey, 
            @RequestParam boolean enabled) {
        FeatureFlag toggledFlag = featureFlagService.toggleFlag(flagKey, enabled);
        return ResponseEntity.ok(ApiResponse.success(toggledFlag, "기능 플래그가 토글되었습니다."));
    }

    @DeleteMapping("/{flagKey}")
    @Operation(summary = "기능 플래그 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteFlag(@PathVariable String flagKey) {
        featureFlagService.deleteFlag(flagKey);
        return ResponseEntity.ok(ApiResponse.success(null, "기능 플래그가 삭제되었습니다."));
    }
}
