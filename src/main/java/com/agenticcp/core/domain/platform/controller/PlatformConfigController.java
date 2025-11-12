package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.audit.AuditController;
import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.exception.AuthorizationException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.service.PlatformConfigService;
import com.agenticcp.core.domain.platform.service.ConfigHistoryQueryService;
import com.agenticcp.core.domain.platform.dto.ConfigHistoryResponse;
import com.agenticcp.core.common.util.LogMaskingUtils;
import org.springframework.data.domain.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 플랫폼 설정 관리 컨트롤러
 * <p>
 * 플랫폼 전역 설정의 조회/생성/수정/삭제 기능을 제공하는 REST API 엔드포인트입니다.
 * 민감 정보(showSecret) 조회 시 관리자 권한이 필요하며, 캐시 금지 헤더가 적용됩니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/v1/platform/configs")
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

    /**
     * 전체 플랫폼 설정 조회
     * <p>
     * 모든 플랫폼 설정을 조회합니다. isSystem 파라미터로 시스템/사용자 설정을 필터링할 수 있습니다.
     * showSecret=true인 경우 관리자 권한이 필요하며, 민감 정보가 복호화되어 반환됩니다.
     * </p>
     *
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @param isSystem 시스템 설정 필터 (true: 시스템 설정만, false: 사용자 설정만, null: 전체)
     * @return 플랫폼 설정 목록
     */
    @GetMapping
    @Operation(summary = "플랫폼 설정 조회", 
               description = "isSystem 파라미터로 시스템/사용자 설정 필터링 가능")
    @AuditRequired(
        action = "getAllConfigsWithSecrets",
        resourceType = AuditResourceType.PLATFORM_CONFIG,
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        description = "관리자가 모든 플랫폼 설정을 비밀 정보 포함하여 조회"
    )
    public ResponseEntity<ApiResponse<List<PlatformConfig>>> getAllConfigs(
            @RequestParam(value = "showSecret", required = false) Boolean showSecret,
            @RequestParam(value = "isSystem", required = false) Boolean isSystem) {
        log.info("[PlatformConfigController] getAllConfigs - showSecret={}, isSystem={}", showSecret, isSystem);
        boolean reveal = Boolean.TRUE.equals(showSecret);
        if (reveal) {
            enforceAdmin();
        }
        List<PlatformConfig> configs = platformConfigService.getAllConfigs(reveal, isSystem);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (reveal) {
            builder.header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            builder.header("Pragma", "no-cache");
        }
        return builder.body(ApiResponse.success(configs));
    }

    /**
     * 특정 플랫폼 설정 조회
     * <p>
     * configKey로 특정 플랫폼 설정을 조회합니다.
     * showSecret=true인 경우 관리자 권한이 필요하며, 민감 정보가 복호화되어 반환됩니다.
     * </p>
     *
     * @param configKey 조회할 설정 키
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @return 플랫폼 설정
     * @throws com.agenticcp.core.common.exception.ResourceNotFoundException 설정을 찾을 수 없는 경우
     */
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
        log.info("[PlatformConfigController] getConfigByKey - configKey={}, showSecret={}", 
                LogMaskingUtils.mask(configKey, 2, 2), showSecret);
        boolean reveal = Boolean.TRUE.equals(showSecret);
        if (reveal) {
            enforceAdmin();
        }
        PlatformConfig config = platformConfigService.getConfigByKey(configKey, reveal)
                .orElseThrow(() -> new ResourceNotFoundException(PlatformConfigErrorCode.CONFIG_NOT_FOUND));
        
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (reveal) {
            builder.header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            builder.header("Pragma", "no-cache");
        }
        return builder.body(ApiResponse.success(config));
    }

    /**
     * 설정 타입별 플랫폼 설정 조회
     * <p>
     * 지정된 타입(STRING, NUMBER, BOOLEAN, JSON, ENCRYPTED)의 플랫폼 설정을 조회합니다.
     * showSecret=true인 경우 관리자 권한이 필요하며, 민감 정보가 복호화되어 반환됩니다.
     * </p>
     *
     * @param configType 조회할 설정 타입
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @return 해당 타입의 플랫폼 설정 목록
     */
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
        log.info("[PlatformConfigController] getConfigsByType - configType={}, showSecret={}", configType, showSecret);
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

    /**
     * 시스템 설정 조회
     * <p>
     * 시스템 설정(isSystem=true)만 조회합니다.
     * showSecret=true인 경우 관리자 권한이 필요하며, 민감 정보가 복호화되어 반환됩니다.
     * </p>
     *
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @return 시스템 설정 목록
     */
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
        log.info("[PlatformConfigController] getSystemConfigs - showSecret={}", showSecret);
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

    /**
     * 관리자 권한 강제 검증
     * <p>
     * 현재 인증된 사용자가 관리자 권한을 가지고 있는지 확인합니다.
     * 권한이 없거나 인증 정보가 없는 경우 AuthorizationException을 발생시킵니다.
     * </p>
     *
     * @throws AuthorizationException 관리자 권한이 없는 경우
     */
    private void enforceAdmin() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            log.warn("[PlatformConfigController] enforceAdmin - authentication or authorities is null");
            throw new AuthorizationException();
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(role -> role.contains("ADMIN"));
        if (!isAdmin) {
            log.warn("[PlatformConfigController] enforceAdmin - user does not have ADMIN role, authorities={}", auth.getAuthorities());
            throw new AuthorizationException();
        }
    }

    /**
     * 플랫폼 설정 생성
     * <p>
     * 새로운 플랫폼 설정을 생성합니다. 설정 키는 고유해야 하며, 네임스페이스 기반으로 isSystem이 자동 설정됩니다.
     * ENCRYPTED 타입의 경우 자동으로 암호화되어 저장됩니다.
     * </p>
     *
     * @param platformConfig 생성할 플랫폼 설정 정보
     * @return 생성된 플랫폼 설정
     * @throws com.agenticcp.core.domain.platform.exception.ConfigValidationException 설정 검증 실패 시
     * @throws com.agenticcp.core.common.exception.BusinessException 중복 키 또는 암호화 실패 시
     */
    @PostMapping
    @Operation(summary = "플랫폼 설정 생성")
    public ResponseEntity<ApiResponse<PlatformConfig>> createConfig(@Valid @RequestBody PlatformConfig platformConfig) {
        log.info("[PlatformConfigController] createConfig - configKey={}", 
                LogMaskingUtils.mask(platformConfig.getConfigKey(), 2, 2));
        PlatformConfig createdConfig = platformConfigService.createConfig(platformConfig);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdConfig, "플랫폼 설정이 생성되었습니다."));
    }

    /**
     * 플랫폼 설정 수정
     * <p>
     * 기존 플랫폼 설정을 수정합니다. 시스템 설정의 타입 변경은 금지되며,
     * ENCRYPTED 타입의 경우 자동으로 암호화되어 저장됩니다.
     * </p>
     *
     * @param configKey 수정할 설정 키
     * @param platformConfig 수정할 플랫폼 설정 정보
     * @return 수정된 플랫폼 설정
     * @throws com.agenticcp.core.common.exception.ResourceNotFoundException 설정을 찾을 수 없는 경우
     * @throws com.agenticcp.core.domain.platform.exception.ConfigValidationException 설정 검증 실패 시
     * @throws com.agenticcp.core.common.exception.BusinessException 시스템 설정 타입 변경 시도 또는 암호화 실패 시
     */
    @PutMapping("/{configKey}")
    @Operation(summary = "플랫폼 설정 수정")
    public ResponseEntity<ApiResponse<PlatformConfig>> updateConfig(
            @PathVariable String configKey, 
            @Valid @RequestBody PlatformConfig platformConfig) {
        log.info("[PlatformConfigController] updateConfig - configKey={}", 
                LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig updatedConfig = platformConfigService.updateConfig(configKey, platformConfig);
        return ResponseEntity.ok(ApiResponse.success(updatedConfig, "플랫폼 설정이 수정되었습니다."));
    }

    /**
     * 플랫폼 설정 삭제
     * <p>
     * 플랫폼 설정을 소프트 삭제(isDeleted=true)합니다.
     * 시스템 설정은 삭제할 수 없으며, 삭제 시 감사 로그가 기록됩니다.
     * </p>
     *
     * @param configKey 삭제할 설정 키
     * @return 삭제 성공 응답
     * @throws com.agenticcp.core.common.exception.ResourceNotFoundException 설정을 찾을 수 없는 경우
     * @throws com.agenticcp.core.domain.platform.exception.ConfigValidationException 시스템 설정 삭제 시도 시
     */
    @DeleteMapping("/{configKey}")
    @Operation(summary = "플랫폼 설정 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable String configKey) {
        log.info("[PlatformConfigController] deleteConfig - configKey={}", 
                LogMaskingUtils.mask(configKey, 2, 2));
        platformConfigService.deleteConfig(configKey);
        return ResponseEntity.ok(ApiResponse.success(null, "플랫폼 설정이 삭제되었습니다."));
    }

    /**
     * 플랫폼 설정 변경 이력 조회
     * <p>
     * 특정 플랫폼 설정의 변경 이력을 페이지네이션으로 조회합니다.
     * 관리자 권한이 필요하며, 생성/수정/삭제 이력이 포함됩니다.
     * </p>
     *
     * @param configKey 조회할 설정 키
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 설정 변경 이력 페이지
     * @throws com.agenticcp.core.common.exception.AuthorizationException 관리자 권한이 없는 경우
     */
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
        log.info("[PlatformConfigController] getConfigHistory - configKey={}, page={}, size={}", 
                LogMaskingUtils.mask(configKey, 2, 2), page, size);
        // 관리자 전용 조회
        enforceAdmin();
        Page<ConfigHistoryResponse> history = configHistoryQueryService.getHistory(configKey, page, size);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
