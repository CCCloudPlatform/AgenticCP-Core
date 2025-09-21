package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.dto.PlatformConfigDtos;
import com.agenticcp.core.domain.platform.service.PlatformConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * 플랫폼 설정 관리 REST 컨트롤러.
 * - 목록/타입별/시스템 설정 조회는 페이징을 지원
 * - 단건 조회는 showSecret 파라미터로 ENCRYPTED 값 평문 노출 여부 선택
 * - 생성/수정 시 DTO 기반의 유효성 검증 수행
 */
@RestController
@RequestMapping("/api/platform/configs")
@RequiredArgsConstructor
@Tag(name = "Platform Configuration", description = "플랫폼 설정 관리 API")
public class PlatformConfigController {

    private final PlatformConfigService platformConfigService;

    /**
     * 전체 설정을 페이징으로 조회합니다.
     */
    @GetMapping
    @Operation(summary = "모든 플랫폼 설정 조회")
    public ResponseEntity<ApiResponse<Page<PlatformConfigDtos.Response>>> getAllConfigs(
            @RequestParam(required = false) String configKeyPattern,
            @RequestParam(required = false) PlatformConfig.ConfigType configType,
            @RequestParam(required = false) Boolean isSystem,
            @PageableDefault Pageable pageable) {
        Page<PlatformConfig> configs = platformConfigService.getFilteredConfigs(configKeyPattern, configType, isSystem, pageable);
        Page<PlatformConfigDtos.Response> responses = configs.map(pc -> PlatformConfigDtos.Response.of(pc));
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 설정 키로 단건을 조회합니다.
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "특정 플랫폼 설정 조회")
    public ResponseEntity<ApiResponse<PlatformConfigDtos.Response>> getConfigByKey(
            @PathVariable String configKey) {
        return platformConfigService.getConfigByKey(configKey)
                .map(config -> ResponseEntity.ok(ApiResponse.success(PlatformConfigDtos.Response.of(config))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 설정 타입으로 페이징 조회합니다.
     */
    @GetMapping("/type/{configType}")
    @Operation(summary = "설정 타입별 조회")
    public ResponseEntity<ApiResponse<Page<PlatformConfigDtos.Response>>> getConfigsByType(
            @PathVariable PlatformConfig.ConfigType configType,
            @PageableDefault Pageable pageable) {
        Page<PlatformConfig> configs = platformConfigService.getConfigsByType(configType, pageable);
        Page<PlatformConfigDtos.Response> responses = configs.map(pc -> PlatformConfigDtos.Response.of(pc));
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 시스템 설정만 페이징 조회합니다.
     */
    @GetMapping("/system")
    @Operation(summary = "시스템 설정 조회")
    public ResponseEntity<ApiResponse<Page<PlatformConfigDtos.Response>>> getSystemConfigs(@PageableDefault Pageable pageable) {
        Page<PlatformConfig> configs = platformConfigService.getSystemConfigs(pageable);
        Page<PlatformConfigDtos.Response> responses = configs.map(pc -> PlatformConfigDtos.Response.of(pc));
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 설정을 생성합니다. 키 정책 및 타입별 값 검증이 수행됩니다.
     */
    @PostMapping
    @Operation(summary = "플랫폼 설정 생성")
    public ResponseEntity<ApiResponse<PlatformConfigDtos.Response>> createConfig(
            @Valid @RequestBody PlatformConfigDtos.CreateRequest request) {
        PlatformConfig toCreate = PlatformConfig.builder()
                .configKey(request.getKey())
                .configType(request.getType())
                .configValue(request.getValue())
                .isSystem(Boolean.TRUE.equals(request.getIsSystem()))
                .isEncrypted(request.getType() == PlatformConfig.ConfigType.ENCRYPTED)
                .description(request.getDescription())
                .build();
        PlatformConfig createdConfig = platformConfigService.createConfig(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PlatformConfigDtos.Response.of(createdConfig), "플랫폼 설정이 생성되었습니다."));
    }

    /**
     * 설정을 수정합니다. 타입별 값 검증이 수행됩니다.
     */
    @PutMapping("/{configKey}")
    @Operation(summary = "플랫폼 설정 수정")
    public ResponseEntity<ApiResponse<PlatformConfigDtos.Response>> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody PlatformConfigDtos.UpdateRequest request) {
        PlatformConfig toUpdate = PlatformConfig.builder()
                .configType(request.getType())
                .configValue(request.getValue())
                .description(request.getDescription())
                .build();
        PlatformConfig updatedConfig = platformConfigService.updateConfig(configKey, toUpdate);
        return ResponseEntity.ok(ApiResponse.success(PlatformConfigDtos.Response.of(updatedConfig), "플랫폼 설정이 수정되었습니다."));
    }

    /**
     * 설정을 삭제합니다. 시스템 설정은 정책에 따라 삭제가 금지됩니다.
     */
    @DeleteMapping("/{configKey}")
    @Operation(summary = "플랫폼 설정 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable String configKey) {
        platformConfigService.deleteConfig(configKey);
        return ResponseEntity.ok(ApiResponse.success(null, "플랫폼 설정이 삭제되었습니다."));
    }
}
