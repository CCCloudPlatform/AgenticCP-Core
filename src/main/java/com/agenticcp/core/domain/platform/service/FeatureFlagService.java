package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.cache.service.FeatureFlagSyncService;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    
    /**
     * Redis Pub/Sub 동기화 서비스 (Optional)
     * <p>
     * Redis가 비활성화된 환경에서는 null이며, 이 경우 이벤트를 발행하지 않습니다.
     * </p>
     */
    private final FeatureFlagSyncService syncService;

    /**
     * 생성자 주입
     * <p>
     * FeatureFlagSyncService는 Optional로 주입받습니다.
     * Redis가 비활성화된 경우 null이 주입됩니다.
     * </p>
     */
    public FeatureFlagService(
            FeatureFlagRepository featureFlagRepository,
            @Autowired(required = false) FeatureFlagSyncService syncService) {
        this.featureFlagRepository = featureFlagRepository;
        this.syncService = syncService;
    }

    public List<FeatureFlag> getAllFlags() {
        log.info("[FeatureFlagService] getAllFlags");
        List<FeatureFlag> result = featureFlagRepository.findAll();
        log.info("[FeatureFlagService] getAllFlags - success count={}", result.size());
        return result;
    }

    public Optional<FeatureFlag> getFlagByKey(String flagKey) {
        log.info("[FeatureFlagService] getFlagByKey - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        Optional<FeatureFlag> result = featureFlagRepository.findByFlagKey(flagKey);
        log.info("[FeatureFlagService] getFlagByKey - found={} flagKey={}", result.isPresent(), LogMaskingUtils.mask(flagKey, 2, 2));
        return result;
    }

    public FeatureFlag getFlagByKeyOrThrow(String flagKey) {
        log.info("[FeatureFlagService] getFlagByKeyOrThrow - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        FeatureFlag flag = featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "flagKey", flagKey));
        log.info("[FeatureFlagService] getFlagByKeyOrThrow - success flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        return flag;
    }

    public List<FeatureFlag> getActiveFlags() {
        log.info("[FeatureFlagService] getActiveFlags");
        List<FeatureFlag> result = featureFlagRepository.findActiveFlags(Status.ACTIVE, LocalDateTime.now());
        log.info("[FeatureFlagService] getActiveFlags - success count={}", result.size());
        return result;
    }

    public boolean isFlagEnabled(String flagKey) {
        log.info("[FeatureFlagService] isFlagEnabled - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        boolean enabled = featureFlagRepository.findActiveFlagByKey(flagKey, Status.ACTIVE, LocalDateTime.now())
                .map(FeatureFlag::getIsEnabled)
                .orElse(false);
        log.info("[FeatureFlagService] isFlagEnabled - result={} flagKey={}", enabled, LogMaskingUtils.mask(flagKey, 2, 2));
        return enabled;
    }

    public List<FeatureFlag> getEnabledFlags() {
        log.info("[FeatureFlagService] getEnabledFlags");
        List<FeatureFlag> result = featureFlagRepository.findByIsEnabled(true);
        log.info("[FeatureFlagService] getEnabledFlags - success count={}", result.size());
        return result;
    }

    @Transactional
    public FeatureFlag createFlag(FeatureFlag featureFlag) {
        log.info("[FeatureFlagService] createFlag - flagKey={} name={}", 
                LogMaskingUtils.mask(featureFlag.getFlagKey(), 2, 2), featureFlag.getFlagName());
        
        FeatureFlag saved = featureFlagRepository.save(featureFlag);
        
        // Redis 활성화 시 이벤트 발행
        publishCreatedEvent(saved.getFlagKey());
        
        log.info("[FeatureFlagService] createFlag - success flagKey={}", 
                LogMaskingUtils.mask(saved.getFlagKey(), 2, 2));
        return saved;
    }

    @Transactional
    public FeatureFlag updateFlag(String flagKey, FeatureFlag updatedFlag) {
        log.info("[FeatureFlagService] updateFlag - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        FeatureFlag existingFlag = getFlagByKeyOrThrow(flagKey);
        
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
        
        // Redis 활성화 시 이벤트 발행
        publishUpdatedEvent(flagKey);
        
        log.info("[FeatureFlagService] updateFlag - success flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        return saved;
    }

    @Transactional
    public FeatureFlag toggleFlag(String flagKey, boolean enabled) {
        log.info("[FeatureFlagService] toggleFlag - flagKey={} enabled={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), enabled);
        
        FeatureFlag flag = getFlagByKeyOrThrow(flagKey);
        flag.setIsEnabled(enabled);
        FeatureFlag saved = featureFlagRepository.save(flag);
        
        // Redis 활성화 시 이벤트 발행
        publishToggledEvent(flagKey);
        
        log.info("[FeatureFlagService] toggleFlag - success flagKey={} enabled={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), enabled);
        return saved;
    }

    @Transactional
    public void deleteFlag(String flagKey) {
        log.info("[FeatureFlagService] deleteFlag - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        FeatureFlag flag = getFlagByKeyOrThrow(flagKey);
        flag.setIsDeleted(true);
        featureFlagRepository.save(flag);
        
        // Redis 활성화 시 이벤트 발행
        publishDeletedEvent(flagKey);
        
        log.info("[FeatureFlagService] deleteFlag - success flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
    }

    /**
     * 플래그 생성 이벤트 발행
     * <p>
     * Redis가 활성화된 경우에만 이벤트를 발행합니다.
     * 이벤트 발행 실패는 로그만 남기고 정상 진행됩니다.
     * </p>
     */
    private void publishCreatedEvent(String flagKey) {
        if (syncService != null) {
            try {
                syncService.publishCreated(flagKey);
            } catch (Exception e) {
                log.warn("[FeatureFlagService] Failed to publish created event - flagKey={}, error={}", 
                        flagKey, e.getMessage());
            }
        }
    }

    /**
     * 플래그 수정 이벤트 발행
     */
    private void publishUpdatedEvent(String flagKey) {
        if (syncService != null) {
            try {
                syncService.publishUpdated(flagKey);
            } catch (Exception e) {
                log.warn("[FeatureFlagService] Failed to publish updated event - flagKey={}, error={}", 
                        flagKey, e.getMessage());
            }
        }
    }

    /**
     * 플래그 토글 이벤트 발행
     */
    private void publishToggledEvent(String flagKey) {
        if (syncService != null) {
            try {
                syncService.publishToggled(flagKey);
            } catch (Exception e) {
                log.warn("[FeatureFlagService] Failed to publish toggled event - flagKey={}, error={}", 
                        flagKey, e.getMessage());
            }
        }
    }

    /**
     * 플래그 삭제 이벤트 발행
     */
    private void publishDeletedEvent(String flagKey) {
        if (syncService != null) {
            try {
                syncService.publishDeleted(flagKey);
            } catch (Exception e) {
                log.warn("[FeatureFlagService] Failed to publish deleted event - flagKey={}, error={}", 
                        flagKey, e.getMessage());
            }
        }
    }
}
