package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import com.agenticcp.core.common.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;

    public List<FeatureFlag> getAllFlags() {
        return featureFlagRepository.findAll();
    }

    public Optional<FeatureFlag> getFlagByKey(String flagKey) {
        return featureFlagRepository.findByFlagKey(flagKey);
    }

    public FeatureFlag getFlagByKeyOrThrow(String flagKey) {
        return featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "flagKey", flagKey));
    }

    public List<FeatureFlag> getActiveFlags() {
        return featureFlagRepository.findActiveFlags(Status.ACTIVE, LocalDateTime.now());
    }

    public boolean isFlagEnabled(String flagKey) {
        return featureFlagRepository.findActiveFlagByKey(flagKey, Status.ACTIVE, LocalDateTime.now())
                .map(FeatureFlag::getIsEnabled)
                .orElse(false);
    }

    public List<FeatureFlag> getEnabledFlags() {
        return featureFlagRepository.findByIsEnabled(true);
    }

    @Transactional
    public FeatureFlag createFlag(FeatureFlag featureFlag) {
        log.info("Creating feature flag: {}", featureFlag.getFlagKey());
        return featureFlagRepository.save(featureFlag);
    }

    @Transactional
    public FeatureFlag updateFlag(String flagKey, FeatureFlag updatedFlag) {
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
        
        log.info("Updating feature flag: {}", flagKey);
        return featureFlagRepository.save(existingFlag);
    }

    @Transactional
    public FeatureFlag toggleFlag(String flagKey, boolean enabled) {
        FeatureFlag flag = getFlagByKeyOrThrow(flagKey);
        flag.setIsEnabled(enabled);
        log.info("Toggling feature flag {} to {}", flagKey, enabled);
        return featureFlagRepository.save(flag);
    }

    @Transactional
    public void deleteFlag(String flagKey) {
        FeatureFlag flag = getFlagByKeyOrThrow(flagKey);
        flag.setIsDeleted(true);
        featureFlagRepository.save(flag);
        log.info("Soft deleted feature flag: {}", flagKey);
    }
}
