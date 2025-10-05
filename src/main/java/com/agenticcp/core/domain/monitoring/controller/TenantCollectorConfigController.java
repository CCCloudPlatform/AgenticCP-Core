package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.domain.monitoring.dto.TenantCollectorConfigDto;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.service.TenantCollectorConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
public class TenantCollectorConfigController {

    private final TenantCollectorConfigService configService;

    /**
     * 현재 테넌트의 활성화된 수집기 설정 조회
     */
    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<TenantCollectorConfigDto>>> getEnabledConfigs() {
        try {
            String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.info("활성화된 수집기 설정 조회: tenantId={}", tenantId);
            
            List<TenantCollectorConfigDto> configs = configService.getEnabledConfigsByTenant(tenantId);
            
            return ResponseEntity.ok(ApiResponse.success(configs));
        } catch (Exception e) {
            log.error("활성화된 수집기 설정 조회 중 오류 발생", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "활성화된 수집기 설정 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 현재 테넌트의 모든 수집기 설정 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantCollectorConfigDto>>> getAllConfigs() {
        try {
            String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.info("모든 수집기 설정 조회: tenantId={}", tenantId);
            
            List<TenantCollectorConfigDto> configs = configService.getAllConfigsByTenant(tenantId);
            
            return ResponseEntity.ok(ApiResponse.success(configs));
        } catch (Exception e) {
            log.error("수집기 설정 조회 중 오류 발생", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "수집기 설정 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 특정 수집기 설정 조회
     */
    @GetMapping("/{collectorType}")
    public ResponseEntity<ApiResponse<TenantCollectorConfigDto>> getConfigByType(
            @PathVariable CollectorType collectorType) {
        try {
            String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.info("특정 수집기 설정 조회: tenantId={}, collectorType={}", tenantId, collectorType);
            
            TenantCollectorConfigDto config = configService.getConfigByTenantAndType(tenantId, collectorType);
            
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("특정 수집기 설정 조회 중 오류 발생: collectorType={}", collectorType, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "수집기 설정 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 수집기 설정 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TenantCollectorConfigDto>> createConfig(
            @RequestBody TenantCollectorConfigDto configDto) {
        try {
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
            
            return ResponseEntity.ok(ApiResponse.success(createdConfig));
        } catch (Exception e) {
            log.error("수집기 설정 생성 중 오류 발생", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "수집기 설정 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * 수집기 설정 수정
     */
    @PutMapping("/{configId}")
    public ResponseEntity<ApiResponse<TenantCollectorConfigDto>> updateConfig(
            @PathVariable Long configId,
            @RequestBody TenantCollectorConfigDto configDto) {
        try {
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
        } catch (Exception e) {
            log.error("수집기 설정 수정 중 오류 발생: configId={}", configId, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "수집기 설정 수정 중 오류가 발생했습니다.");
        }
    }

    /**
     * 수집기 설정 삭제
     */
    @DeleteMapping("/{configId}")
    public ResponseEntity<ApiResponse<String>> deleteConfig(@PathVariable Long configId) {
        try {
            String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.info("수집기 설정 삭제: configId={}, tenantId={}", configId, tenantId);
            
            configService.deleteConfig(configId);
            
            return ResponseEntity.ok(ApiResponse.success("수집기 설정이 삭제되었습니다."));
        } catch (Exception e) {
            log.error("수집기 설정 삭제 중 오류 발생: configId={}", configId, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "수집기 설정 삭제 중 오류가 발생했습니다.");
        }
    }

    /**
     * 수집기 활성화/비활성화
     */
    @PatchMapping("/{configId}/toggle")
    public ResponseEntity<ApiResponse<TenantCollectorConfigDto>> toggleConfig(
            @PathVariable Long configId,
            @RequestParam boolean enabled) {
        try {
            String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.info("수집기 활성화 상태 변경: configId={}, enabled={}, tenantId={}", configId, enabled, tenantId);
            
            TenantCollectorConfigDto updatedConfig = configService.toggleConfig(configId, enabled);
            
            return ResponseEntity.ok(ApiResponse.success(updatedConfig));
        } catch (Exception e) {
            log.error("수집기 활성화 상태 변경 중 오류 발생: configId={}, enabled={}", configId, enabled, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "수집기 활성화 상태 변경 중 오류가 발생했습니다.");
        }
    }

    /**
     * 현재 테넌트의 활성화된 수집기 타입 목록 조회
     */
    @GetMapping("/enabled/types")
    public ResponseEntity<ApiResponse<List<CollectorType>>> getEnabledCollectorTypes() {
        try {
            String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.info("활성화된 수집기 타입 조회: tenantId={}", tenantId);
            
            List<CollectorType> types = configService.getEnabledCollectorTypesByTenant(tenantId);
            
            return ResponseEntity.ok(ApiResponse.success(types));
        } catch (Exception e) {
            log.error("활성화된 수집기 타입 조회 중 오류 발생", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "활성화된 수집기 타입 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 현재 테넌트의 활성화된 수집기 수 조회
     */
    @GetMapping("/enabled/count")
    public ResponseEntity<ApiResponse<Long>> getEnabledCollectorCount() {
        try {
            String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.info("활성화된 수집기 수 조회: tenantId={}", tenantId);
            
            long count = configService.countEnabledByTenant(tenantId);
            
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("활성화된 수집기 수 조회 중 오류 발생", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "활성화된 수집기 수 조회 중 오류가 발생했습니다.");
        }
    }
}
