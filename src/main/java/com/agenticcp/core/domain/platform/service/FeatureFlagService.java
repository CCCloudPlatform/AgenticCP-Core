package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.cache.service.FeatureFlagSyncService;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 기능 플래그 관리 서비스
 * <p>
 * 기능 플래그의 조회/생성/수정/토글/삭제 등 릴리즈 전략 제어 기능을 제공합니다.
 * Redis가 활성화된 경우 플래그 변경 시 캐시 동기화 이벤트를 발행합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    
    /**
     * Redis Pub/Sub 동기화 서비스 (Optional)
     * <p>
     * Redis가 비활성화된 환경에서는 Optional.empty()이며, 이 경우 이벤트를 발행하지 않습니다.
     * </p>
     */
    private final Optional<FeatureFlagSyncService> syncService;

    /**
     * 감사 로깅 서비스
     */
    private final FeatureFlagAuditService auditService;

    /**
     * 정책 검증기
     */
    private final FeatureFlagPolicyValidator policyValidator;

    /**
     * 승인 서비스
     */
    private final FeatureFlagApprovalService approvalService;

    /**
     * 생성자 주입
     * <p>
     * FeatureFlagSyncService는 Optional로 주입받습니다.
     * Redis가 비활성화된 경우 Optional.empty()가 주입됩니다.
     * </p>
     *
     * @param featureFlagRepository 기능 플래그 레포지토리
     * @param syncService Redis Pub/Sub 동기화 서비스 (선택적)
     */
    public FeatureFlagService(
            FeatureFlagRepository featureFlagRepository,
            Optional<FeatureFlagSyncService> syncService) {
            FeatureFlagAuditService auditService,
            FeatureFlagPolicyValidator policyValidator,
            FeatureFlagApprovalService approvalService) {
        this.featureFlagRepository = featureFlagRepository;
        this.auditService = auditService;
        this.policyValidator = policyValidator;
        this.approvalService = approvalService;
        this.syncService = syncService;
    }

    /**
     * 모든 기능 플래그를 조회합니다.
     *
     * @return 모든 기능 플래그 목록
     */
    public List<FeatureFlag> getAllFlags() {
        log.info("[FeatureFlagService] getAllFlags");
        List<FeatureFlag> result = featureFlagRepository.findAll();
        log.info("[FeatureFlagService] getAllFlags - success count={}", result.size());
        return result;
    }

    /**
     * 플래그 키로 기능 플래그를 조회합니다.
     *
     * @param flagKey 기능 플래그 키
     * @return 기능 플래그 (존재하지 않는 경우 Optional.empty())
     */
    public Optional<FeatureFlag> getFlagByKey(String flagKey) {
        log.info("[FeatureFlagService] getFlagByKey - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        Optional<FeatureFlag> result = featureFlagRepository.findByFlagKey(flagKey);
        log.info("[FeatureFlagService] getFlagByKey - found={} flagKey={}", result.isPresent(), LogMaskingUtils.mask(flagKey, 2, 2));
        return result;
    }

    /**
     * 플래그 키로 기능 플래그를 조회합니다. 존재하지 않는 경우 예외를 발생시킵니다.
     *
     * @param flagKey 기능 플래그 키
     * @return 기능 플래그
     * @throws ResourceNotFoundException 기능 플래그를 찾을 수 없는 경우
     */
    public FeatureFlag getFlagByKeyOrThrow(String flagKey) {
        log.info("[FeatureFlagService] getFlagByKeyOrThrow - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        FeatureFlag flag = featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "flagKey", flagKey));
        log.info("[FeatureFlagService] getFlagByKeyOrThrow - success flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        return flag;
    }

    /**
     * 활성화된 모든 기능 플래그를 조회합니다.
     *
     * @return 활성화된 기능 플래그 목록
     */
    public List<FeatureFlag> getActiveFlags() {
        log.info("[FeatureFlagService] getActiveFlags");
        List<FeatureFlag> result = featureFlagRepository.findActiveFlags(Status.ACTIVE, LocalDateTime.now());
        log.info("[FeatureFlagService] getActiveFlags - success count={}", result.size());
        return result;
    }

    /**
     * 기능 플래그가 활성화되어 있는지 확인합니다.
     *
     * @param flagKey 기능 플래그 키
     * @return 활성화 여부 (플래그가 존재하지 않는 경우 false)
     */
    public boolean isFlagEnabled(String flagKey) {
        log.info("[FeatureFlagService] isFlagEnabled - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        boolean enabled = featureFlagRepository.findActiveFlagByKey(flagKey, Status.ACTIVE, LocalDateTime.now())
                .map(FeatureFlag::getIsEnabled)
                .orElse(false);
        log.info("[FeatureFlagService] isFlagEnabled - result={} flagKey={}", enabled, LogMaskingUtils.mask(flagKey, 2, 2));
        return enabled;
    }

    /**
     * 활성화된 기능 플래그 목록을 조회합니다.
     *
     * @return 활성화된 기능 플래그 목록
     */
    public List<FeatureFlag> getEnabledFlags() {
        log.info("[FeatureFlagService] getEnabledFlags");
        List<FeatureFlag> result = featureFlagRepository.findByIsEnabled(true);
        log.info("[FeatureFlagService] getEnabledFlags - success count={}", result.size());
        return result;
    }

    /**
     * 새로운 기능 플래그를 생성합니다.
     *
     * @param featureFlag 생성할 기능 플래그
     * @return 저장된 기능 플래그
     */
    @Transactional
    public FeatureFlag createFlag(FeatureFlag featureFlag, String userId) {
        log.info("[FeatureFlagService] createFlag - flagKey={} name={} userId={}", 
                LogMaskingUtils.mask(featureFlag.getFlagKey(), 2, 2), featureFlag.getFlagName(), userId);
        
        FeatureFlag saved = featureFlagRepository.save(featureFlag);
        
        // 감사 로깅
        auditService.logFlagChange(null, saved, "CREATE", userId);
        
        // Redis 활성화 시 이벤트 발행
        publishCreatedEvent(saved.getFlagKey());
        
        log.info("[FeatureFlagService] createFlag - success flagKey={}", 
                LogMaskingUtils.mask(saved.getFlagKey(), 2, 2));
        return saved;
    }

    /**
     * 플래그 생성 (사용자 ID 없이 - 하위 호환성)
     */
    @Transactional
    public FeatureFlag createFlag(FeatureFlag featureFlag) {
        return createFlag(featureFlag, "system");
    }

    /**
     * 기존 기능 플래그를 수정합니다.
     *
     * @param flagKey 수정할 기능 플래그 키
     * @param updatedFlag 수정할 기능 플래그 정보
     * @return 수정된 기능 플래그
     * @throws ResourceNotFoundException 기능 플래그를 찾을 수 없는 경우
     */
    @Transactional
    public FeatureFlag updateFlag(String flagKey, FeatureFlag updatedFlag, String userId) {
        log.info("[FeatureFlagService] updateFlag - flagKey={} userId={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), userId);
        
        FeatureFlag existingFlag = getFlagByKeyOrThrow(flagKey);
        
        // 변경 전 상태 저장 (감사 로깅용)
        FeatureFlag oldFlag = createSnapshot(existingFlag);
        
        // 승인 상태 확인
        boolean hasApproval = approvalService.hasApproval(existingFlag);
        
        // 업데이트할 플래그의 심각도 설정 (updatedFlag에 severity가 있으면 사용, 없으면 기존 값 유지)
        if (updatedFlag.getSeverity() != null) {
            existingFlag.setSeverity(updatedFlag.getSeverity());
        }
        
        // 정책 검증 (승인이 필요한데 승인이 없으면 예외 발생)
        policyValidator.validateFlagChange(existingFlag, hasApproval);
        
        // 플래그 업데이트
        existingFlag.setFlagName(updatedFlag.getFlagName());
        existingFlag.setDescription(updatedFlag.getDescription());
        existingFlag.setIsEnabled(updatedFlag.getIsEnabled());
        existingFlag.setStatus(updatedFlag.getStatus());
        existingFlag.setTargetTenants(updatedFlag.getTargetTenants());
        existingFlag.setTargetUsers(updatedFlag.getTargetUsers());
        existingFlag.setRolloutPercentage(updatedFlag.getRolloutPercentage());
        existingFlag.setStartDate(updatedFlag.getStartDate());
        existingFlag.setEndDate(updatedFlag.getEndDate());
        existingFlag.setMetadata(updatedFlag.getMetadata());
        
        FeatureFlag saved = featureFlagRepository.save(existingFlag);
        
        // 감사 로깅
        auditService.logFlagChange(oldFlag, saved, "UPDATE", userId);
        
        // Redis 활성화 시 이벤트 발행
        publishUpdatedEvent(flagKey);
        
        log.info("[FeatureFlagService] updateFlag - success flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        return saved;
    }

    /**
     * 플래그 업데이트 (사용자 ID 없이 - 하위 호환성)
     */
    @Transactional
    public FeatureFlag updateFlag(String flagKey, FeatureFlag updatedFlag) {
        return updateFlag(flagKey, updatedFlag, "system");
    }

    @Transactional
    public FeatureFlag toggleFlag(String flagKey, boolean enabled, String userId) {
        log.info("[FeatureFlagService] toggleFlag - flagKey={} enabled={} userId={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), enabled, userId);
        
        FeatureFlag flag = getFlagByKeyOrThrow(flagKey);
        
        // 변경 전 상태 저장 (감사 로깅용)
        FeatureFlag oldFlag = createSnapshot(flag);
        
        // 승인 상태 확인
        boolean hasApproval = approvalService.hasApproval(flag);
        
        // 정책 검증 (승인이 필요한데 승인이 없으면 예외 발생)
        policyValidator.validateFlagChange(flag, hasApproval);
        
        // 플래그 토글
        flag.setIsEnabled(enabled);
        FeatureFlag saved = featureFlagRepository.save(flag);
        
        // 감사 로깅
        auditService.logFlagChange(oldFlag, saved, "TOGGLE", userId);
        
        // Redis 활성화 시 이벤트 발행
        publishToggledEvent(flagKey);
        
        log.info("[FeatureFlagService] toggleFlag - success flagKey={} enabled={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), enabled);
        return saved;
    }

    /**
     * 플래그 토글 (사용자 ID 없이 - 하위 호환성)
     */
    @Transactional
    public FeatureFlag toggleFlag(String flagKey, boolean enabled) {
        return toggleFlag(flagKey, enabled, "system");
    }

    @Transactional
    public void deleteFlag(String flagKey, String userId) {
        log.info("[FeatureFlagService] deleteFlag - flagKey={} userId={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), userId);
        
        FeatureFlag flag = getFlagByKeyOrThrow(flagKey);
        
        // 변경 전 상태 저장 (감사 로깅용)
        FeatureFlag oldFlag = createSnapshot(flag);
        
        // 승인 상태 확인
        boolean hasApproval = approvalService.hasApproval(flag);
        
        // 정책 검증 (승인이 필요한데 승인이 없으면 예외 발생)
        policyValidator.validateFlagChange(flag, hasApproval);
        
        // 플래그 삭제 (Soft Delete)
        flag.setIsDeleted(true);
        featureFlagRepository.save(flag);
        
        // 감사 로깅
        auditService.logFlagChange(oldFlag, null, "DELETE", userId);
        
        // Redis 활성화 시 이벤트 발행
        publishDeletedEvent(flagKey);
        
        log.info("[FeatureFlagService] deleteFlag - success flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
    }

    /**
     * 플래그 삭제 (사용자 ID 없이 - 하위 호환성)
     */
    @Transactional
    public void deleteFlag(String flagKey) {
        deleteFlag(flagKey, "system");
    }

    /**
     * 플래그 생성 이벤트 발행
     * <p>
     * Redis가 활성화된 경우에만 이벤트를 발행합니다.
     * 이벤트 발행 실패는 로그만 남기고 정상 진행됩니다.
     * </p>
     *
     * @param flagKey 기능 플래그 키
     */
    private void publishCreatedEvent(String flagKey) {
        syncService.ifPresent(service -> {
            try {
                service.publishCreated(flagKey);
            } catch (Exception e) {
                log.warn("[FeatureFlagService] Failed to publish created event - flagKey={}, error={}", 
                        flagKey, e.getMessage());
            }
        });
    }

    /**
     * 플래그 수정 이벤트 발행
     * <p>
     * Redis가 활성화된 경우에만 이벤트를 발행합니다.
     * 이벤트 발행 실패는 로그만 남기고 정상 진행됩니다.
     * </p>
     *
     * @param flagKey 기능 플래그 키
     */
    private void publishUpdatedEvent(String flagKey) {
        syncService.ifPresent(service -> {
            try {
                service.publishUpdated(flagKey);
            } catch (Exception e) {
                log.warn("[FeatureFlagService] Failed to publish updated event - flagKey={}, error={}", 
                        flagKey, e.getMessage());
            }
        });
    }

    /**
     * 플래그 토글 이벤트 발행
     * <p>
     * Redis가 활성화된 경우에만 이벤트를 발행합니다.
     * 이벤트 발행 실패는 로그만 남기고 정상 진행됩니다.
     * </p>
     *
     * @param flagKey 기능 플래그 키
     */
    private void publishToggledEvent(String flagKey) {
        syncService.ifPresent(service -> {
            try {
                service.publishToggled(flagKey);
            } catch (Exception e) {
                log.warn("[FeatureFlagService] Failed to publish toggled event - flagKey={}, error={}", 
                        flagKey, e.getMessage());
            }
        });
    }

    /**
     * 플래그 삭제 이벤트 발행
     * <p>
     * Redis가 활성화된 경우에만 이벤트를 발행합니다.
     * 이벤트 발행 실패는 로그만 남기고 정상 진행됩니다.
     * </p>
     *
     * @param flagKey 기능 플래그 키
     */
    private void publishDeletedEvent(String flagKey) {
        syncService.ifPresent(service -> {
            try {
                service.publishDeleted(flagKey);
            } catch (Exception e) {
                log.warn("[FeatureFlagService] Failed to publish deleted event - flagKey={}, error={}", 
                        flagKey, e.getMessage());
            }
        });
    }

    /**
     * 플래그 스냅샷 생성 (감사 로깅용)
     * 변경 전 상태를 저장하기 위해 새로운 객체를 생성합니다.
     * 
     * @param flag 원본 플래그
     * @return 스냅샷 플래그
     */
    private FeatureFlag createSnapshot(FeatureFlag flag) {
        if (flag == null) {
            return null;
        }
        
        return FeatureFlag.builder()
                .flagKey(flag.getFlagKey())
                .flagName(flag.getFlagName())
                .description(flag.getDescription())
                .isEnabled(flag.getIsEnabled())
                .status(flag.getStatus())
                .severity(flag.getSeverity())
                .targetTenants(flag.getTargetTenants())
                .targetUsers(flag.getTargetUsers())
                .rolloutPercentage(flag.getRolloutPercentage())
                .startDate(flag.getStartDate())
                .endDate(flag.getEndDate())
                .metadata(flag.getMetadata())
                .cacheTtlSeconds(flag.getCacheTtlSeconds())
                .build();
    }
}
