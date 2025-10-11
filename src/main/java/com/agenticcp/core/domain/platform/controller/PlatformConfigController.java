package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.audit.AuditController;
import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.exception.AuthorizationException;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.service.PlatformConfigService;
import com.agenticcp.core.domain.platform.service.ConfigHistoryQueryService;
import com.agenticcp.core.domain.platform.dto.ConfigHistoryResponse;
import org.springframework.data.domain.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/configs")
@RequiredArgsConstructor
@Tag(name = "Platform Configuration", description = "플랫폼 설정 관리 API")
@AuditController(
    resourceType = AuditResourceType.PLATFORM_CONFIG,
    defaultSeverity = AuditSeverity.HIGH,
    defaultIncludeRequestData = true,
    targetHttpMethods = {"POST", "PUT", "DELETE"}
)
public class PlatformConfigController {

    private final PlatformConfigService platformConfigService;
    private final ConfigHistoryQueryService configHistoryQueryService;

    @GetMapping
    @Operation(summary = "모든 플랫폼 설정 조회")
    @AuditRequired(
        action = "getAllConfigsWithSecrets",
        resourceType = AuditResourceType.PLATFORM_CONFIG,
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        description = "관리자가 모든 플랫폼 설정을 비밀 정보 포함하여 조회"
    )
    public ResponseEntity<ApiResponse<List<PlatformConfig>>> getAllConfigs(
            @RequestParam(value = "showSecret", required = false) Boolean showSecret) {
        boolean reveal = Boolean.TRUE.equals(showSecret);
        if (reveal) {
            enforceAdmin();
        }
        List<PlatformConfig> configs = platformConfigService.getAllConfigs(reveal);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (reveal) {
            builder.header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            builder.header("Pragma", "no-cache");
        }
        return builder.body(ApiResponse.success(configs));
    }

    @GetMapping("/{configKey}")
    @Operation(summary = "특정 플랫폼 설정 조회")
    @AuditRequired(
        action = "getConfigByKeyWithSecrets",
        resourceType = AuditResourceType.PLATFORM_CONFIG,
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        description = "관리자가 특정 플랫폼 설정을 비밀 정보 포함하여 조회"
    )
    public ResponseEntity<ApiResponse<PlatformConfig>> getConfigByKey(
            @PathVariable String configKey,
            @RequestParam(value = "showSecret", required = false) Boolean showSecret) {
        boolean reveal = Boolean.TRUE.equals(showSecret);
        if (reveal) {
            enforceAdmin();
        }
        return platformConfigService.getConfigByKey(configKey, reveal)
                .map(config -> {
                    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
                    if (reveal) {
                        builder.header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                        builder.header("Pragma", "no-cache");
                    }
                    return builder.body(ApiResponse.success(config));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{configType}")
    @Operation(summary = "설정 타입별 조회")
    @AuditRequired(
        action = "getConfigsByTypeWithSecrets",
        resourceType = AuditResourceType.PLATFORM_CONFIG,
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        description = "관리자가 설정 타입별 플랫폼 설정을 비밀 정보 포함하여 조회"
    )
    public ResponseEntity<ApiResponse<List<PlatformConfig>>> getConfigsByType(
            @PathVariable PlatformConfig.ConfigType configType,
            @RequestParam(value = "showSecret", required = false) Boolean showSecret) {
        boolean reveal = Boolean.TRUE.equals(showSecret);
        if (reveal) {
            enforceAdmin();
        }
        List<PlatformConfig> configs = platformConfigService.getConfigsByType(configType, reveal);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (reveal) {
            builder.header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            builder.header("Pragma", "no-cache");
        }
        return builder.body(ApiResponse.success(configs));
    }

    @GetMapping("/system")
    @Operation(summary = "시스템 설정 조회")
    @AuditRequired(
        action = "getSystemConfigsWithSecrets",
        resourceType = AuditResourceType.PLATFORM_CONFIG,
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        description = "관리자가 시스템 설정을 비밀 정보 포함하여 조회"
    )
    public ResponseEntity<ApiResponse<List<PlatformConfig>>> getSystemConfigs(
            @RequestParam(value = "showSecret", required = false) Boolean showSecret) {
        boolean reveal = Boolean.TRUE.equals(showSecret);
        if (reveal) {
            enforceAdmin();
        }
        List<PlatformConfig> configs = platformConfigService.getSystemConfigs(reveal);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (reveal) {
            builder.header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            builder.header("Pragma", "no-cache");
        }
        return builder.body(ApiResponse.success(configs));
    }

    private void enforceAdmin() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            throw new AuthorizationException();
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(role -> role.contains("ADMIN"));
        if (!isAdmin) {
            throw new AuthorizationException();
        }
    }

    @PostMapping
    @Operation(summary = "플랫폼 설정 생성")
    public ResponseEntity<ApiResponse<PlatformConfig>> createConfig(@RequestBody PlatformConfig platformConfig) {
        PlatformConfig createdConfig = platformConfigService.createConfig(platformConfig);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdConfig, "플랫폼 설정이 생성되었습니다."));
    }

    @PutMapping("/{configKey}")
    @Operation(summary = "플랫폼 설정 수정")
    public ResponseEntity<ApiResponse<PlatformConfig>> updateConfig(
            @PathVariable String configKey, 
            @RequestBody PlatformConfig platformConfig) {
        PlatformConfig updatedConfig = platformConfigService.updateConfig(configKey, platformConfig);
        return ResponseEntity.ok(ApiResponse.success(updatedConfig, "플랫폼 설정이 수정되었습니다."));
    }

    @DeleteMapping("/{configKey}")
    @Operation(summary = "플랫폼 설정 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable String configKey) {
        platformConfigService.deleteConfig(configKey);
        return ResponseEntity.ok(ApiResponse.success(null, "플랫폼 설정이 삭제되었습니다."));
    }

    @GetMapping("/{configKey}/history")
    @Operation(summary = "플랫폼 설정 변경 이력 조회")
    @AuditRequired(
        action = "getConfigHistory",
        resourceType = AuditResourceType.PLATFORM_CONFIG,
        severity = AuditSeverity.MEDIUM,
        includeRequestData = true,
        description = "관리자가 플랫폼 설정 변경 이력을 조회"
    )
    public ResponseEntity<ApiResponse<Page<ConfigHistoryResponse>>> getConfigHistory(
            @PathVariable String configKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 관리자 전용 조회
        enforceAdmin();
        Page<ConfigHistoryResponse> history = configHistoryQueryService.getHistory(configKey, page, size);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
