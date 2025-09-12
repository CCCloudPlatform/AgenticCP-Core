package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
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
public class SecurityPolicyService {

    private final SecurityPolicyRepository securityPolicyRepository;

    public List<SecurityPolicy> getAllPolicies() {
        return securityPolicyRepository.findAll();
    }

    public List<SecurityPolicy> getActivePolicies() {
        return securityPolicyRepository.findActivePolicies(Status.ACTIVE);
    }

    public Optional<SecurityPolicy> getPolicyByKey(String policyKey) {
        return securityPolicyRepository.findByPolicyKey(policyKey);
    }

    public SecurityPolicy getPolicyByKeyOrThrow(String policyKey) {
        return securityPolicyRepository.findByPolicyKey(policyKey)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityPolicy", "policyKey", policyKey));
    }

    public List<SecurityPolicy> getPoliciesByTenant(Tenant tenant) {
        return securityPolicyRepository.findByTenant(tenant);
    }

    public List<SecurityPolicy> getActivePoliciesByTenant(Tenant tenant) {
        return securityPolicyRepository.findActivePoliciesByTenant(tenant, Status.ACTIVE);
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
        return securityPolicyRepository.save(securityPolicy);
    }

    @Transactional
    public SecurityPolicy updatePolicy(String policyKey, SecurityPolicy updatedPolicy) {
        SecurityPolicy existingPolicy = getPolicyByKeyOrThrow(policyKey);
        
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
        policy.setStatus(Status.ACTIVE);
        policy.setIsEnabled(true);
        log.info("Activating security policy: {}", policyKey);
        return securityPolicyRepository.save(policy);
    }

    @Transactional
    public SecurityPolicy deactivatePolicy(String policyKey) {
        SecurityPolicy policy = getPolicyByKeyOrThrow(policyKey);
        policy.setStatus(Status.INACTIVE);
        policy.setIsEnabled(false);
        log.info("Deactivating security policy: {}", policyKey);
        return securityPolicyRepository.save(policy);
    }

    @Transactional
    public void deletePolicy(String policyKey) {
        SecurityPolicy policy = getPolicyByKeyOrThrow(policyKey);
        policy.setIsDeleted(true);
        securityPolicyRepository.save(policy);
        log.info("Soft deleted security policy: {}", policyKey);
    }
}
