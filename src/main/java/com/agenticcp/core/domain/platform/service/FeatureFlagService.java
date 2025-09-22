package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 기능 플래그 관리 서비스
 *
 * 기능 플래그의 조회/생성/수정/토글/삭제 등 릴리즈 전략 제어 기능을 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;

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
        log.info("[FeatureFlagService] createFlag - flagKey={} name={}", LogMaskingUtils.mask(featureFlag.getFlagKey(), 2, 2), featureFlag.getFlagName());
        FeatureFlag saved = featureFlagRepository.save(featureFlag);
        log.info("[FeatureFlagService] createFlag - success flagKey={}", LogMaskingUtils.mask(saved.getFlagKey(), 2, 2));
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
        log.info("[FeatureFlagService] updateFlag - success flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        return saved;
    }

    @Transactional
    public FeatureFlag toggleFlag(String flagKey, boolean enabled) {
        log.info("[FeatureFlagService] toggleFlag - flagKey={} enabled={}", LogMaskingUtils.mask(flagKey, 2, 2), enabled);
        FeatureFlag flag = getFlagByKeyOrThrow(flagKey);
        flag.setIsEnabled(enabled);
        FeatureFlag saved = featureFlagRepository.save(flag);
        log.info("[FeatureFlagService] toggleFlag - success flagKey={} enabled={}", LogMaskingUtils.mask(flagKey, 2, 2), enabled);
        return saved;
    }

    @Transactional
    public void deleteFlag(String flagKey) {
        log.info("[FeatureFlagService] deleteFlag - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        FeatureFlag flag = getFlagByKeyOrThrow(flagKey);
        flag.setIsDeleted(true);
        featureFlagRepository.save(flag);
        log.info("[FeatureFlagService] deleteFlag - success flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
    }
}
