package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.util.LogMaskingUtils;
import com.agenticcp.core.domain.platform.enums.PlatformErrorCode;
import com.agenticcp.core.domain.platform.dto.CreateTargetRuleRequestDto;
import com.agenticcp.core.domain.platform.dto.TargetRuleResponseDto;
import com.agenticcp.core.domain.platform.dto.UpdateTargetRuleRequestDto;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import com.agenticcp.core.domain.platform.repository.FeatureFlagTargetRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 타겟팅 규칙 관리 서비스
 * 
 * 고급 타겟팅 시스템의 타겟팅 규칙 CRUD 기능을 제공합니다.
 * 다양한 타겟팅 기준과 롤아웃 전략을 지원합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TargetingRuleService {

    private final FeatureFlagTargetRuleRepository targetRuleRepository;
    private final FeatureFlagRepository featureFlagRepository;

    /**
     * 특정 기능 플래그의 모든 타겟팅 규칙 조회
     * 
     * @param flagKey 기능 플래그 키
     * @return 타겟팅 규칙 목록
     */
    public List<TargetRuleResponseDto> getTargetRulesByFlagKey(String flagKey) {
        log.info("[TargetingRuleService] getTargetRulesByFlagKey - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        
        validateFeatureFlagExists(flagKey);
        
        List<FeatureFlagTargetRule> targetRules = targetRuleRepository.findByFlagKeyAndTenantId(
                flagKey, TenantContextHolder.getCurrentTenantOrThrow().getId());
        
        List<TargetRuleResponseDto> result = TargetRuleResponseDto.from(targetRules);
        log.info("[TargetingRuleService] getTargetRulesByFlagKey - success count={} flagKey={}", 
                result.size(), LogMaskingUtils.mask(flagKey, 2, 2));
        
        return result;
    }

    /**
     * 특정 기능 플래그의 활성화된 타겟팅 규칙 조회
     * 
     * @param flagKey 기능 플래그 키
     * @return 활성화된 타겟팅 규칙 목록
     */
    public List<TargetRuleResponseDto> getActiveTargetRulesByFlagKey(String flagKey) {
        log.info("[TargetingRuleService] getActiveTargetRulesByFlagKey - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        
        validateFeatureFlagExists(flagKey);
        
        List<FeatureFlagTargetRule> targetRules = targetRuleRepository.findActiveTargetRulesByFlagKey(
                flagKey, Status.ACTIVE, LocalDateTime.now());
        
        List<TargetRuleResponseDto> result = TargetRuleResponseDto.from(targetRules);
        log.info("[TargetingRuleService] getActiveTargetRulesByFlagKey - success count={} flagKey={}", 
                result.size(), LogMaskingUtils.mask(flagKey, 2, 2));
        
        return result;
    }

    /**
     * 페이징을 통한 타겟팅 규칙 조회
     * 
     * @param flagKey 기능 플래그 키
     * @param pageable 페이징 정보
     * @return 페이징된 타겟팅 규칙 목록
     */
    public Page<TargetRuleResponseDto> getTargetRulesByFlagKeyWithPagination(String flagKey, Pageable pageable) {
        log.info("[TargetingRuleService] getTargetRulesByFlagKeyWithPagination - flagKey={} page={} size={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), pageable.getPageNumber(), pageable.getPageSize());
        
        validateFeatureFlagExists(flagKey);
        
        Page<FeatureFlagTargetRule> targetRulePage = targetRuleRepository.findByFlagKeyWithPagination(flagKey, pageable);
        Page<TargetRuleResponseDto> result = targetRulePage.map(TargetRuleResponseDto::from);
        
        log.info("[TargetingRuleService] getTargetRulesByFlagKeyWithPagination - success totalElements={} flagKey={}", 
                result.getTotalElements(), LogMaskingUtils.mask(flagKey, 2, 2));
        
        return result;
    }

    /**
     * ID로 타겟팅 규칙 조회
     * 
     * @param ruleId 타겟팅 규칙 ID
     * @return 타겟팅 규칙 (Optional)
     */
    public Optional<TargetRuleResponseDto> getTargetRuleById(Long ruleId) {
        log.info("[TargetingRuleService] getTargetRuleById - ruleId={}", ruleId);
        
        Optional<FeatureFlagTargetRule> targetRule = targetRuleRepository.findByIdAndTenantId(
                ruleId, TenantContextHolder.getCurrentTenantOrThrow().getId());
        
        Optional<TargetRuleResponseDto> result = targetRule.map(TargetRuleResponseDto::from);
        log.info("[TargetingRuleService] getTargetRuleById - found={} ruleId={}", result.isPresent(), ruleId);
        
        return result;
    }

    /**
     * 타겟팅 규칙 생성
     * 
     * @param request 타겟팅 규칙 생성 요청
     * @return 생성된 타겟팅 규칙
     */
    @Transactional
    public TargetRuleResponseDto createTargetRule(CreateTargetRuleRequestDto request) {
        log.info("[TargetingRuleService] createTargetRule - flagKey={} ruleName={}", 
                LogMaskingUtils.mask(request.getFlagKey(), 2, 2), request.getRuleName());
        
        // 비즈니스 검증
        validateCreateRequest(request);
        
        // 타겟팅 규칙 생성
        FeatureFlagTargetRule targetRule = FeatureFlagTargetRule.builder()
                .flagKey(request.getFlagKey())
                .ruleName(request.getRuleName())
                .description(request.getDescription())
                .priority(request.getPriority())
                .isEnabled(request.getIsEnabled())
                .cloudProvider(request.getCloudProvider())
                .region(request.getRegion())
                .tenantType(request.getTenantType())
                .tenantGrade(request.getTenantGrade())
                .userRole(request.getUserRole())
                .userAttributes(request.getUserAttributes())
                .customAttributes(request.getCustomAttributes())
                .rolloutPercentage(request.getRolloutPercentage())
                .rolloutStrategy(request.getRolloutStrategy())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .metadata(request.getMetadata())
                .tenantId(TenantContextHolder.getCurrentTenantOrThrow().getId())
                .build();
        
        FeatureFlagTargetRule savedRule = targetRuleRepository.save(targetRule);
        TargetRuleResponseDto result = TargetRuleResponseDto.from(savedRule);
        
        log.info("[TargetingRuleService] createTargetRule - success ruleId={} flagKey={}", 
                savedRule.getId(), LogMaskingUtils.mask(request.getFlagKey(), 2, 2));
        
        return result;
    }

    /**
     * 타겟팅 규칙 수정
     * 
     * @param ruleId 타겟팅 규칙 ID
     * @param request 타겟팅 규칙 수정 요청
     * @return 수정된 타겟팅 규칙
     */
    @Transactional
    public TargetRuleResponseDto updateTargetRule(Long ruleId, UpdateTargetRuleRequestDto request) {
        log.info("[TargetingRuleService] updateTargetRule - ruleId={}", ruleId);
        
        // 비즈니스 검증
        validateUpdateRequest(request);
        
        // 기존 타겟팅 규칙 조회
        FeatureFlagTargetRule existingRule = targetRuleRepository.findByIdAndTenantId(
                ruleId, TenantContextHolder.getCurrentTenantOrThrow().getId())
                .orElseThrow(() -> new ResourceNotFoundException(PlatformErrorCode.TARGET_RULE_NOT_FOUND));
        
        // 필드 업데이트
        updateTargetRuleFields(existingRule, request);
        
        FeatureFlagTargetRule savedRule = targetRuleRepository.save(existingRule);
        TargetRuleResponseDto result = TargetRuleResponseDto.from(savedRule);
        
        log.info("[TargetingRuleService] updateTargetRule - success ruleId={}", ruleId);
        
        return result;
    }

    /**
     * 타겟팅 규칙 삭제
     * 
     * @param ruleId 타겟팅 규칙 ID
     */
    @Transactional
    public void deleteTargetRule(Long ruleId) {
        log.info("[TargetingRuleService] deleteTargetRule - ruleId={}", ruleId);
        
        // 기존 타겟팅 규칙 조회
        FeatureFlagTargetRule existingRule = targetRuleRepository.findByIdAndTenantId(
                ruleId, TenantContextHolder.getCurrentTenantOrThrow().getId())
                .orElseThrow(() -> new ResourceNotFoundException(PlatformErrorCode.TARGET_RULE_NOT_FOUND));
        
        targetRuleRepository.delete(existingRule);
        
        log.info("[TargetingRuleService] deleteTargetRule - success ruleId={}", ruleId);
    }

    /**
     * 타겟팅 규칙 활성화/비활성화 토글
     * 
     * @param ruleId 타겟팅 규칙 ID
     * @param enabled 활성화 여부
     * @return 수정된 타겟팅 규칙
     */
    @Transactional
    public TargetRuleResponseDto toggleTargetRule(Long ruleId, boolean enabled) {
        log.info("[TargetingRuleService] toggleTargetRule - ruleId={} enabled={}", ruleId, enabled);
        
        FeatureFlagTargetRule existingRule = targetRuleRepository.findByIdAndTenantId(
                ruleId, TenantContextHolder.getCurrentTenantOrThrow().getId())
                .orElseThrow(() -> new ResourceNotFoundException(PlatformErrorCode.TARGET_RULE_NOT_FOUND));
        
        existingRule.setIsEnabled(enabled);
        if (enabled) {
            existingRule.activate();
        } else {
            existingRule.deactivate();
        }
        
        FeatureFlagTargetRule savedRule = targetRuleRepository.save(existingRule);
        TargetRuleResponseDto result = TargetRuleResponseDto.from(savedRule);
        
        log.info("[TargetingRuleService] toggleTargetRule - success ruleId={} enabled={}", ruleId, enabled);
        
        return result;
    }

    // 비즈니스 검증 메서드들
    private void validateFeatureFlagExists(String flagKey) {
        if (!featureFlagRepository.findByFlagKey(flagKey).isPresent()) {
            throw new ResourceNotFoundException(PlatformErrorCode.FEATURE_FLAG_NOT_FOUND);
        }
    }

    private void validateCreateRequest(CreateTargetRuleRequestDto request) {
        // 기능 플래그 존재 확인
        validateFeatureFlagExists(request.getFlagKey());
        
        // 규칙명 중복 확인
        if (targetRuleRepository.existsByFlagKeyAndRuleNameAndTenantId(
                request.getFlagKey(), request.getRuleName(), TenantContextHolder.getCurrentTenantOrThrow().getId())) {
            throw new BusinessException(PlatformErrorCode.TARGET_RULE_DUPLICATE_NAME, 
                    "해당 기능 플래그에 동일한 규칙명이 이미 존재합니다: " + request.getRuleName());
        }
        
        // 비즈니스 규칙 검증
        request.validateBusinessRules();
    }

    private void validateUpdateRequest(UpdateTargetRuleRequestDto request) {
        // 비즈니스 규칙 검증
        request.validateBusinessRules();
    }

    private void updateTargetRuleFields(FeatureFlagTargetRule targetRule, UpdateTargetRuleRequestDto request) {
        if (request.getRuleName() != null) {
            targetRule.setRuleName(request.getRuleName());
        }
        if (request.getDescription() != null) {
            targetRule.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            targetRule.setPriority(request.getPriority());
        }
        if (request.getIsEnabled() != null) {
            targetRule.setIsEnabled(request.getIsEnabled());
        }
        if (request.getCloudProvider() != null) {
            targetRule.setCloudProvider(request.getCloudProvider());
        }
        if (request.getRegion() != null) {
            targetRule.setRegion(request.getRegion());
        }
        if (request.getTenantType() != null) {
            targetRule.setTenantType(request.getTenantType());
        }
        if (request.getTenantGrade() != null) {
            targetRule.setTenantGrade(request.getTenantGrade());
        }
        if (request.getUserRole() != null) {
            targetRule.setUserRole(request.getUserRole());
        }
        if (request.getUserAttributes() != null) {
            targetRule.setUserAttributes(request.getUserAttributes());
        }
        if (request.getCustomAttributes() != null) {
            targetRule.setCustomAttributes(request.getCustomAttributes());
        }
        if (request.getRolloutPercentage() != null) {
            targetRule.updateRolloutPercentage(request.getRolloutPercentage());
        }
        if (request.getRolloutStrategy() != null) {
            targetRule.setRolloutStrategy(request.getRolloutStrategy());
        }
        if (request.getStartDate() != null) {
            targetRule.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            targetRule.setEndDate(request.getEndDate());
        }
        if (request.getMetadata() != null) {
            targetRule.setMetadata(request.getMetadata());
        }
    }
}
