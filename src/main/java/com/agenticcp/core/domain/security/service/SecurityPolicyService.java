package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.enums.SecurityErrorCode;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 보안 정책 관리 서비스
 *
 * 보안 정책의 조회/생성/수정/활성화/비활성화 및 통계 관련 기능을 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecurityPolicyService {

    private final SecurityPolicyRepository securityPolicyRepository;

    public List<SecurityPolicy> getAllPolicies() {
        log.info("[SecurityPolicyService] getAllPolicies");
        List<SecurityPolicy> result = securityPolicyRepository.findAll();
        log.info("[SecurityPolicyService] getAllPolicies - success count={}", result.size());
        return result;
    }

    public List<SecurityPolicy> getActivePolicies() {
        log.info("[SecurityPolicyService] getActivePolicies");
        List<SecurityPolicy> result = securityPolicyRepository.findActivePolicies(Status.ACTIVE);
        log.info("[SecurityPolicyService] getActivePolicies - success count={}", result.size());
        return result;
    }

    public Optional<SecurityPolicy> getPolicyByKey(String policyKey) {
        log.info("[SecurityPolicyService] getPolicyByKey - policyKey={}", LogMaskingUtils.mask(policyKey, 2, 2));
        Optional<SecurityPolicy> result = securityPolicyRepository.findByPolicyKey(policyKey);
        log.info("[SecurityPolicyService] getPolicyByKey - found={} policyKey={}", result.isPresent(), LogMaskingUtils.mask(policyKey, 2, 2));
        return result;
    }

    public SecurityPolicy getPolicyByKeyOrThrow(String policyKey) {
        log.info("[SecurityPolicyService] getPolicyByKeyOrThrow - policyKey={}", LogMaskingUtils.mask(policyKey, 2, 2));
        SecurityPolicy policy = securityPolicyRepository.findByPolicyKey(policyKey)
                .orElseThrow(() -> new ResourceNotFoundException(SecurityErrorCode.POLICY_NOT_FOUND));
        log.info("[SecurityPolicyService] getPolicyByKeyOrThrow - success policyKey={}", LogMaskingUtils.mask(policyKey, 2, 2));
        return policy;
    }

    public List<SecurityPolicy> getPoliciesByTenant(Tenant tenant) {
        log.info("[SecurityPolicyService] getPoliciesByTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        List<SecurityPolicy> result = securityPolicyRepository.findByTenant(tenant);
        log.info("[SecurityPolicyService] getPoliciesByTenant - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        return result;
    }

    public List<SecurityPolicy> getActivePoliciesByTenant(Tenant tenant) {
        log.info("[SecurityPolicyService] getActivePoliciesByTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        List<SecurityPolicy> result = securityPolicyRepository.findActivePoliciesByTenant(tenant, Status.ACTIVE);
        log.info("[SecurityPolicyService] getActivePoliciesByTenant - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        return result;
    }

    public List<SecurityPolicy> getPoliciesByType(SecurityPolicy.PolicyType policyType) {
        return securityPolicyRepository.findByPolicyType(policyType);
    }

    public List<SecurityPolicy> getGlobalPolicies() {
        return securityPolicyRepository.findGlobalPolicies(Status.ACTIVE);
    }

    public List<SecurityPolicy> getSystemPolicies() {
        return securityPolicyRepository.findSystemPolicies(Status.ACTIVE);
    }

    public List<SecurityPolicy> getEffectivePolicies() {
        return securityPolicyRepository.findEffectivePolicies(LocalDateTime.now(), Status.ACTIVE);
    }

    public List<SecurityPolicy> getPoliciesByTypeOrderedByPriority(SecurityPolicy.PolicyType policyType) {
        return securityPolicyRepository.findPoliciesByTypeOrderedByPriority(policyType, Status.ACTIVE);
    }

    public Long getPolicyCountByTenant(Tenant tenant) {
        return securityPolicyRepository.countPoliciesByTenant(tenant, Status.ACTIVE);
    }

    @Transactional
    public SecurityPolicy createPolicy(SecurityPolicy securityPolicy) {
        log.info("Creating security policy: {}", securityPolicy.getPolicyKey());
        
        // 정책 키 중복 체크
        if (securityPolicyRepository.findByPolicyKey(securityPolicy.getPolicyKey()).isPresent()) {
            throw new BusinessException(SecurityErrorCode.POLICY_ALREADY_EXISTS);
        }
        
        // 정책 유효성 검증
        validatePolicy(securityPolicy);
        
        return securityPolicyRepository.save(securityPolicy);
    }

    @Transactional
    public SecurityPolicy updatePolicy(String policyKey, SecurityPolicy updatedPolicy) {
        SecurityPolicy existingPolicy = getPolicyByKeyOrThrow(policyKey);
        
        // 시스템 정책 수정 방지
        if (existingPolicy.getIsSystem()) {
            throw new BusinessException(SecurityErrorCode.POLICY_MODIFICATION_FORBIDDEN);
        }
        
        // 정책 유효성 검증
        validatePolicy(updatedPolicy);
        
        existingPolicy.setPolicyName(updatedPolicy.getPolicyName());
        existingPolicy.setDescription(updatedPolicy.getDescription());
        existingPolicy.setStatus(updatedPolicy.getStatus());
        existingPolicy.setPolicyType(updatedPolicy.getPolicyType());
        existingPolicy.setSeverity(updatedPolicy.getSeverity());
        existingPolicy.setIsGlobal(updatedPolicy.getIsGlobal());
        existingPolicy.setIsSystem(updatedPolicy.getIsSystem());
        existingPolicy.setIsEnabled(updatedPolicy.getIsEnabled());
        existingPolicy.setRules(updatedPolicy.getRules());
        existingPolicy.setConditions(updatedPolicy.getConditions());
        existingPolicy.setActions(updatedPolicy.getActions());
        existingPolicy.setTargetResources(updatedPolicy.getTargetResources());
        existingPolicy.setExceptions(updatedPolicy.getExceptions());
        existingPolicy.setEffectiveFrom(updatedPolicy.getEffectiveFrom());
        existingPolicy.setEffectiveUntil(updatedPolicy.getEffectiveUntil());
        existingPolicy.setPriority(updatedPolicy.getPriority());
        existingPolicy.setMetadata(updatedPolicy.getMetadata());
        
        log.info("Updating security policy: {}", policyKey);
        return securityPolicyRepository.save(existingPolicy);
    }

    @Transactional
    public SecurityPolicy togglePolicy(String policyKey, boolean enabled) {
        SecurityPolicy policy = getPolicyByKeyOrThrow(policyKey);
        policy.setIsEnabled(enabled);
        log.info("Toggling security policy {} to {}", policyKey, enabled);
        return securityPolicyRepository.save(policy);
    }

    @Transactional
    public SecurityPolicy activatePolicy(String policyKey) {
        SecurityPolicy policy = getPolicyByKeyOrThrow(policyKey);
        
        // 이미 활성화된 정책 체크
        if (policy.getStatus() == Status.ACTIVE && policy.getIsEnabled()) {
            throw new BusinessException(SecurityErrorCode.POLICY_ALREADY_ACTIVE);
        }
        
        policy.setStatus(Status.ACTIVE);
        policy.setIsEnabled(true);
        log.info("Activating security policy: {}", policyKey);
        return securityPolicyRepository.save(policy);
    }

    @Transactional
    public SecurityPolicy deactivatePolicy(String policyKey) {
        SecurityPolicy policy = getPolicyByKeyOrThrow(policyKey);
        
        // 이미 비활성화된 정책 체크
        if (policy.getStatus() == Status.INACTIVE && !policy.getIsEnabled()) {
            throw new BusinessException(SecurityErrorCode.POLICY_ALREADY_INACTIVE);
        }
        
        policy.setStatus(Status.INACTIVE);
        policy.setIsEnabled(false);
        log.info("Deactivating security policy: {}", policyKey);
        return securityPolicyRepository.save(policy);
    }

    @Transactional
    public void deletePolicy(String policyKey) {
        SecurityPolicy policy = getPolicyByKeyOrThrow(policyKey);
        
        // 시스템 정책 삭제 방지
        if (policy.getIsSystem()) {
            throw new BusinessException(SecurityErrorCode.POLICY_DELETION_FORBIDDEN);
        }
        
        policy.setIsDeleted(true);
        securityPolicyRepository.save(policy);
        log.info("Soft deleted security policy: {}", policyKey);
    }
    
    /**
     * 보안 정책 유효성 검증
     * 
     * @param policy 검증할 정책
     */
    private void validatePolicy(SecurityPolicy policy) {
        if (policy == null) {
            throw new BusinessException(SecurityErrorCode.POLICY_VALIDATION_FAILED, "정책이 null입니다.");
        }
        
        if (policy.getPolicyKey() == null || policy.getPolicyKey().trim().isEmpty()) {
            throw new BusinessException(SecurityErrorCode.POLICY_VALIDATION_FAILED, "정책 키가 필요합니다.");
        }
        
        if (policy.getPolicyName() == null || policy.getPolicyName().trim().isEmpty()) {
            throw new BusinessException(SecurityErrorCode.POLICY_VALIDATION_FAILED, "정책 이름이 필요합니다.");
        }
        
        if (policy.getPolicyType() == null) {
            throw new BusinessException(SecurityErrorCode.POLICY_VALIDATION_FAILED, "정책 타입이 필요합니다.");
        }
        
        // 유효 기간 검증
        if (policy.getEffectiveFrom() != null && policy.getEffectiveUntil() != null) {
            if (policy.getEffectiveFrom().isAfter(policy.getEffectiveUntil())) {
                throw new BusinessException(SecurityErrorCode.POLICY_EFFECTIVE_DATE_INVALID);
            }
        }
        
        // JSON 형식 검증 (간단한 검증)
        if (policy.getRules() != null && !policy.getRules().trim().isEmpty()) {
            if (!isValidJson(policy.getRules())) {
                throw new BusinessException(SecurityErrorCode.POLICY_JSON_PARSE_ERROR, "정책 규칙 JSON 형식이 올바르지 않습니다.");
            }
        }
        
        if (policy.getConditions() != null && !policy.getConditions().trim().isEmpty()) {
            if (!isValidJson(policy.getConditions())) {
                throw new BusinessException(SecurityErrorCode.POLICY_JSON_PARSE_ERROR, "정책 조건 JSON 형식이 올바르지 않습니다.");
            }
        }
        
        if (policy.getActions() != null && !policy.getActions().trim().isEmpty()) {
            if (!isValidJson(policy.getActions())) {
                throw new BusinessException(SecurityErrorCode.POLICY_JSON_PARSE_ERROR, "정책 액션 JSON 형식이 올바르지 않습니다.");
            }
        }
    }
    
    /**
     * JSON 형식 유효성 검증 (간단한 검증)
     * 
     * @param json 검증할 JSON 문자열
     * @return 유효하면 true, 그렇지 않으면 false
     */
    private boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return true; // 빈 값은 유효한 것으로 간주
        }
        
        try {
            // 간단한 JSON 형식 검증
            String trimmed = json.trim();
            return (trimmed.startsWith("{") && trimmed.endsWith("}")) || 
                   (trimmed.startsWith("[") && trimmed.endsWith("]"));
        } catch (Exception e) {
            return false;
        }
    }
}
