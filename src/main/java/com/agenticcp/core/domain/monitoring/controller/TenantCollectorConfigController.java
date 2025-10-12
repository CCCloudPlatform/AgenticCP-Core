package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.monitoring.dto.QuotaRequestDto;
import com.agenticcp.core.domain.monitoring.dto.TenantCollectorConfigDto;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.service.TenantCollectorConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 테넌트별 수집기 설정 API 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/monitoring/collectors/configs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Metric Collector Configuration", description = "테넌트별 메트릭 수집기 설정 및 할당량 관리 API")
public class TenantCollectorConfigController {

    private final TenantCollectorConfigService configService;

    /**
     * 현재 테넌트의 활성화된 수집기 설정 조회
     */
    @Operation(summary = "활성화된 수집기 설정 조회", description = "현재 테넌트의 활성 설정만 조회합니다")
    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<TenantCollectorConfigDto>>> getEnabledConfigs() {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("활성화된 수집기 설정 조회: tenantId={}", tenantId);
        
        List<TenantCollectorConfigDto> configs = configService.getEnabledConfigsByTenant(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 현재 테넌트의 모든 수집기 설정 조회
     */
    @Operation(summary = "모든 수집기 설정 조회", description = "현재 테넌트의 전체 수집기 설정을 조회합니다")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantCollectorConfigDto>>> getAllConfigs() {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("모든 수집기 설정 조회: tenantId={}", tenantId);
        
        List<TenantCollectorConfigDto> configs = configService.getAllConfigsByTenant(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 특정 수집기 설정 조회
     */
    @Operation(summary = "특정 수집기 설정 조회", description = "수집기 타입으로 설정을 조회합니다")
    @GetMapping("/{collectorType}")
    public ResponseEntity<ApiResponse<TenantCollectorConfigDto>> getConfigByType(
            @Parameter(description = "수집기 타입", required = true)
            @PathVariable @NotNull CollectorType collectorType) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("특정 수집기 설정 조회: tenantId={}, collectorType={}", tenantId, collectorType);
        
        TenantCollectorConfigDto config = configService.getConfigByTenantAndType(tenantId, collectorType);
        
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * 수집기 설정 생성
     */
    @Operation(summary = "수집기 설정 생성", description = "현재 테넌트 기준으로 수집기 설정을 생성합니다")
    @PostMapping
    public ResponseEntity<ApiResponse<TenantCollectorConfigDto>> createConfig(
            @Valid @RequestBody TenantCollectorConfigDto configDto) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("수집기 설정 생성: tenantId={}, collectorType={}", tenantId, configDto.getCollectorType());
        
        // 테넌트 ID 설정 (보안상 현재 테넌트로 고정)
        TenantCollectorConfigDto requestDto = TenantCollectorConfigDto.builder()
                .tenantId(tenantId)
                .collectorType(configDto.getCollectorType())
                .isEnabled(configDto.getIsEnabled())
                .collectionInterval(configDto.getCollectionInterval())
                .retryCount(configDto.getRetryCount())
                .timeout(configDto.getTimeout())
                .targetMetrics(configDto.getTargetMetrics())
                .collectorSettings(configDto.getCollectorSettings())
                .priority(configDto.getPriority())
                .metadata(configDto.getMetadata())
                .build();
        
        TenantCollectorConfigDto createdConfig = configService.createConfig(requestDto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdConfig));
    }

    /**
     * 수집기 설정 수정
     */
    @Operation(summary = "수집기 설정 수정", description = "설정 식별자와 요청 본문으로 설정을 수정합니다")
    @PutMapping("/{configId}")
    public ResponseEntity<ApiResponse<TenantCollectorConfigDto>> updateConfig(
            @Parameter(description = "설정 ID", required = true)
            @PathVariable @Positive Long configId,
            @Valid @RequestBody TenantCollectorConfigDto configDto) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("수집기 설정 수정: configId={}, tenantId={}", configId, tenantId);
        
        // 테넌트 ID 설정 (보안상 현재 테넌트로 고정)
        TenantCollectorConfigDto requestDto = TenantCollectorConfigDto.builder()
                .tenantId(tenantId)
                .collectorType(configDto.getCollectorType())
                .isEnabled(configDto.getIsEnabled())
                .collectionInterval(configDto.getCollectionInterval())
                .retryCount(configDto.getRetryCount())
                .timeout(configDto.getTimeout())
                .targetMetrics(configDto.getTargetMetrics())
                .collectorSettings(configDto.getCollectorSettings())
                .priority(configDto.getPriority())
                .metadata(configDto.getMetadata())
                .build();
        
        TenantCollectorConfigDto updatedConfig = configService.updateConfig(configId, requestDto);
        
        return ResponseEntity.ok(ApiResponse.success(updatedConfig));
    }

    /**
     * 수집기 설정 삭제
     */
    @Operation(summary = "수집기 설정 삭제", description = "설정 식별자로 설정을 삭제합니다")
    @DeleteMapping("/{configId}")
    public ResponseEntity<Void> deleteConfig(
            @Parameter(description = "설정 ID", required = true)
            @PathVariable @Positive Long configId) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("수집기 설정 삭제: configId={}, tenantId={}", configId, tenantId);
        
        configService.deleteConfig(configId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * 수집기 활성화/비활성화
     */
    @Operation(summary = "수집기 활성/비활성 전환", description = "설정의 활성 상태를 변경합니다")
    @PatchMapping("/{configId}/toggle")
    public ResponseEntity<ApiResponse<TenantCollectorConfigDto>> toggleConfig(
            @Parameter(description = "설정 ID", required = true)
            @PathVariable @Positive Long configId,
            @Parameter(description = "활성화 여부", required = true)
            @RequestParam boolean enabled) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("수집기 활성화 상태 변경: configId={}, enabled={}, tenantId={}", configId, enabled, tenantId);
        
        TenantCollectorConfigDto updatedConfig = configService.toggleConfig(configId, enabled);
        
        return ResponseEntity.ok(ApiResponse.success(updatedConfig));
    }

    /**
     * 현재 테넌트의 활성화된 수집기 타입 목록 조회
     */
    @Operation(summary = "활성 수집기 타입 목록", description = "활성화된 수집기 타입만 반환합니다")
    @GetMapping("/enabled/types")
    public ResponseEntity<ApiResponse<List<CollectorType>>> getEnabledCollectorTypes() {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("활성화된 수집기 타입 조회: tenantId={}", tenantId);
        
        List<CollectorType> types = configService.getEnabledCollectorTypesByTenant(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    /**
     * 현재 테넌트의 활성화된 수집기 수 조회
     */
    @Operation(summary = "활성 수집기 개수", description = "활성화된 수집기 설정의 개수를 반환합니다")
    @GetMapping("/enabled/count")
    public ResponseEntity<ApiResponse<Long>> getEnabledCollectorCount() {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("활성화된 수집기 수 조회: tenantId={}", tenantId);
        
        long count = configService.countEnabledByTenant(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ===== 할당량 관련 API =====
    
    /**
     * 테넌트별 할당량 설정
     * 
     * @param quotaRequest 할당량 설정 요청 정보
     * @return 할당량 설정 완료 메시지
     */
    @Operation(summary = "할당량 설정", description = "일일 제한/스토리지/초과 시 액션을 설정합니다")
    @PostMapping("/quota")
    public ResponseEntity<ApiResponse<String>> setQuota(@Valid @RequestBody QuotaRequestDto quotaRequest) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("테넌트별 할당량 설정: tenantId={}, dailyLimit={}, storageQuota={}, action={}", 
                tenantId, quotaRequest.getDailyMetricLimit(), quotaRequest.getStorageQuotaMb(), 
                quotaRequest.getQuotaExceededAction());
        
        configService.setQuotaForTenant(
                tenantId, 
                quotaRequest.getDailyMetricLimit(), 
                quotaRequest.getStorageQuotaMb(), 
                quotaRequest.getQuotaExceededAction()
        );
        
        return ResponseEntity.ok(ApiResponse.success("할당량 설정이 완료되었습니다."));
    }
    
    /**
     * 테넌트별 할당량 조회
     */
    @Operation(summary = "할당량 조회", description = "현재 테넌트의 할당량 설정을 조회합니다")
    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<TenantCollectorConfigDto>> getQuota() {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("테넌트별 할당량 조회: tenantId={}", tenantId);
        
        TenantCollectorConfigDto quota = configService.getQuotaForTenant(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(quota));
    }
    
    /**
     * 할당량 초과 여부 확인
     */
    @Operation(summary = "할당량 초과 여부", description = "현재 테넌트의 할당량 초과 여부를 반환합니다")
    @GetMapping("/quota/exceeded")
    public ResponseEntity<ApiResponse<Boolean>> isQuotaExceeded() {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("할당량 초과 여부 확인: tenantId={}", tenantId);
        
        boolean isExceeded = configService.isQuotaExceeded(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(isExceeded));
    }
    
    /**
     * 할당량 초과 처리
     */
    @Operation(summary = "할당량 초과 처리", description = "초과 시 후속 조치를 수행합니다")
    @PostMapping("/quota/handle-exceeded")
    public ResponseEntity<ApiResponse<String>> handleQuotaExceeded() {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("할당량 초과 처리: tenantId={}", tenantId);
        
        configService.handleQuotaExceeded(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success("할당량 초과 처리가 완료되었습니다."));
    }
}
