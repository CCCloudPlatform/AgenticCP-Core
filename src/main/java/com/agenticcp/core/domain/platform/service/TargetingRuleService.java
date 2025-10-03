package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.domain.platform.dto.targeting.*;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.enums.TargetingRuleErrorCode;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import com.agenticcp.core.domain.platform.repository.FeatureFlagTargetRuleRepository;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 타겟팅 규칙 관리 서비스
 * 
 * 기능 플래그의 타겟팅 규칙을 관리하는 서비스입니다.
 * 타겟팅 규칙의 생성, 수정, 삭제, 활성화/비활성화 기능을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TargetingRuleService {

    private final FeatureFlagTargetRuleRepository targetingRuleRepository;
    private final FeatureFlagRepository featureFlagRepository;

    /**
     * 기능 플래그의 모든 타겟팅 규칙 조회
     * 
     * @param flagKey 기능 플래그 키
     * @return 타겟팅 규칙 목록
     */
    public TargetRuleListResponse getTargetingRules(String flagKey) {
        log.info("[TargetingRuleService] getTargetingRules - flagKey={}", LogMaskingUtils.mask(flagKey, 2, 2));
        
        FeatureFlag featureFlag = getFeatureFlagByKey(flagKey);
        List<FeatureFlagTargetRule> rules = targetingRuleRepository
                .findAllByFeatureFlagId(featureFlag.getId(), false);
        
        List<TargetRuleResponse> ruleDtos = rules.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
        
        long activeCount = rules.stream()
                .mapToLong(rule -> rule.getIsEnabled() ? 1 : 0)
                .sum();
        long inactiveCount = rules.size() - activeCount;
        
        TargetRuleListResponse response = TargetRuleListResponse.builder()
                .rules(ruleDtos)
                .totalCount((long) rules.size())
                .activeCount(activeCount)
                .inactiveCount(inactiveCount)
                .build();
        
        log.info("[TargetingRuleService] getTargetingRules - success flagKey={} count={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), rules.size());
        return response;
    }

    /**
     * 특정 타겟팅 규칙 조회
     * 
     * @param ruleId 규칙 ID
     * @return 타겟팅 규칙
     */
    public TargetRuleResponse getTargetingRule(Long ruleId) {
        log.info("[TargetingRuleService] getTargetingRule - ruleId={}", ruleId);
        
        FeatureFlagTargetRule rule = targetingRuleRepository
                .findByIdAndNotDeleted(ruleId, false)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlagTargetRule", "id", ruleId.toString()));
        
        TargetRuleResponse response = convertToResponseDto(rule);
        log.info("[TargetingRuleService] getTargetingRule - success ruleId={}", ruleId);
        return response;
    }

    /**
     * 타겟팅 규칙 생성
     * 
     * @param flagKey 기능 플래그 키
     * @param request 생성 요청 DTO
     * @return 생성된 타겟팅 규칙
     */
    @Transactional
    public TargetRuleResponse createTargetingRule(String flagKey, CreateTargetRuleRequest request) {
        log.info("[TargetingRuleService] createTargetingRule - flagKey={} ruleName={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), request.getRuleName());
        
        FeatureFlag featureFlag = getFeatureFlagByKey(flagKey);
        
        // 중복 규칙 이름 체크
        if (targetingRuleRepository.existsByFeatureFlagIdAndRuleName(
                featureFlag.getId(), request.getRuleName(), false)) {
            throw new BusinessException(TargetingRuleErrorCode.TARGETING_RULE_NAME_DUPLICATE, 
                    "이미 존재하는 규칙 이름입니다: " + request.getRuleName());
        }
        
        FeatureFlagTargetRule rule = FeatureFlagTargetRule.builder()
                .featureFlag(featureFlag)
                .ruleName(request.getRuleName())
                .ruleDescription(request.getRuleDescription())
                .ruleType(request.getRuleType())
                .ruleCondition(request.getRuleCondition())
                .ruleValue(request.getRuleValue())
                .priority(request.getPriority())
                .isEnabled(request.getIsEnabled())
                .build();
        
        FeatureFlagTargetRule saved = targetingRuleRepository.save(rule);
        TargetRuleResponse response = convertToResponseDto(saved);
        
        log.info("[TargetingRuleService] createTargetingRule - success flagKey={} ruleId={}", 
                LogMaskingUtils.mask(flagKey, 2, 2), saved.getId());
        return response;
    }

    /**
     * 타겟팅 규칙 수정
     * 
     * @param ruleId 규칙 ID
     * @param request 수정 요청 DTO
     * @return 수정된 타겟팅 규칙
     */
    @Transactional
    public TargetRuleResponse updateTargetingRule(Long ruleId, UpdateTargetRuleRequest request) {
        log.info("[TargetingRuleService] updateTargetingRule - ruleId={} ruleName={}", 
                ruleId, request.getRuleName());
        
        FeatureFlagTargetRule rule = targetingRuleRepository
                .findByIdAndNotDeleted(ruleId, false)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlagTargetRule", "id", ruleId.toString()));
        
        // 중복 규칙 이름 체크 (자기 자신 제외)
        if (targetingRuleRepository.existsByFeatureFlagIdAndRuleNameExcludingId(
                rule.getFeatureFlag().getId(), request.getRuleName(), ruleId, false)) {
            throw new BusinessException(TargetingRuleErrorCode.TARGETING_RULE_NAME_DUPLICATE, 
                    "이미 존재하는 규칙 이름입니다: " + request.getRuleName());
        }
        
        rule.updateRuleInfo(
                request.getRuleName(),
                request.getRuleDescription(),
                request.getRuleCondition(),
                request.getRuleValue()
        );
        
        if (request.getPriority() != null) {
            rule.updatePriority(request.getPriority());
        }
        
        if (request.getIsEnabled() != null) {
            if (request.getIsEnabled()) {
                rule.activate();
            } else {
                rule.deactivate();
            }
        }
        
        FeatureFlagTargetRule saved = targetingRuleRepository.save(rule);
        TargetRuleResponse response = convertToResponseDto(saved);
        
        log.info("[TargetingRuleService] updateTargetingRule - success ruleId={}", ruleId);
        return response;
    }

    /**
     * 타겟팅 규칙 삭제 (논리 삭제)
     * 
     * @param ruleId 규칙 ID
     */
    @Transactional
    public void deleteTargetingRule(Long ruleId) {
        log.info("[TargetingRuleService] deleteTargetingRule - ruleId={}", ruleId);
        
        FeatureFlagTargetRule rule = targetingRuleRepository
                .findByIdAndNotDeleted(ruleId, false)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlagTargetRule", "id", ruleId.toString()));
        
        rule.setIsDeleted(true);
        rule.setUpdatedAt(LocalDateTime.now());
        targetingRuleRepository.save(rule);
        
        log.info("[TargetingRuleService] deleteTargetingRule - success ruleId={}", ruleId);
    }

    /**
     * 타겟팅 규칙 활성화
     * 
     * @param ruleId 규칙 ID
     * @return 활성화된 타겟팅 규칙
     */
    @Transactional
    public TargetRuleResponse activateTargetingRule(Long ruleId) {
        log.info("[TargetingRuleService] activateTargetingRule - ruleId={}", ruleId);
        
        FeatureFlagTargetRule rule = targetingRuleRepository
                .findByIdAndNotDeleted(ruleId, false)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlagTargetRule", "id", ruleId.toString()));
        
        rule.activate();
        rule.setUpdatedAt(LocalDateTime.now());
        FeatureFlagTargetRule saved = targetingRuleRepository.save(rule);
        
        TargetRuleResponse response = convertToResponseDto(saved);
        log.info("[TargetingRuleService] activateTargetingRule - success ruleId={}", ruleId);
        return response;
    }

    /**
     * 타겟팅 규칙 비활성화
     * 
     * @param ruleId 규칙 ID
     * @return 비활성화된 타겟팅 규칙
     */
    @Transactional
    public TargetRuleResponse deactivateTargetingRule(Long ruleId) {
        log.info("[TargetingRuleService] deactivateTargetingRule - ruleId={}", ruleId);
        
        FeatureFlagTargetRule rule = targetingRuleRepository
                .findByIdAndNotDeleted(ruleId, false)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlagTargetRule", "id", ruleId.toString()));
        
        rule.deactivate();
        rule.setUpdatedAt(LocalDateTime.now());
        FeatureFlagTargetRule saved = targetingRuleRepository.save(rule);
        
        TargetRuleResponse response = convertToResponseDto(saved);
        log.info("[TargetingRuleService] deactivateTargetingRule - success ruleId={}", ruleId);
        return response;
    }

    /**
     * 기능 플래그 키로 FeatureFlag 조회
     * 
     * @param flagKey 기능 플래그 키
     * @return FeatureFlag 엔티티
     */
    private FeatureFlag getFeatureFlagByKey(String flagKey) {
        return featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "flagKey", flagKey));
    }

    /**
     * FeatureFlagTargetRule 엔티티를 TargetRuleResponse DTT로 변환
     * 
     * @param rule FeatureFlagTargetRule 엔티티
     * @return TargetRuleResponse DTO
     */
    private TargetRuleResponse convertToResponseDto(FeatureFlagTargetRule rule) {
        return TargetRuleResponse.builder()
                .id(rule.getId())
                .featureFlagId(rule.getFeatureFlag().getId())
                .featureFlagKey(rule.getFeatureFlag().getFlagKey())
                .featureFlagName(rule.getFeatureFlag().getFlagName())
                .ruleName(rule.getRuleName())
                .ruleDescription(rule.getRuleDescription())
                .ruleType(rule.getRuleType())
                .ruleCondition(rule.getRuleCondition())
                .ruleValue(rule.getRuleValue())
                .priority(rule.getPriority())
                .isEnabled(rule.getIsEnabled())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .createdBy(rule.getCreatedBy())
                .updatedBy(rule.getUpdatedBy())
                .build();
    }
}
