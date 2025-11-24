package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.util.LogMaskingUtils;
import com.agenticcp.core.domain.security.dto.DefaultPolicyTemplate;
import com.agenticcp.core.domain.security.dto.EffectivePolicySetDTO;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.enums.SecurityErrorCode;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 테넌트별 정책 관리 서비스
 * 
 * <p>테넌트별 정책 격리, 글로벌 정책 조합, 기본 정책 자동 생성 등을 담당합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantPolicyService {
    
    private final SecurityPolicyRepository policyRepository;
    private final TenantRepository tenantRepository;
    
    /**
     * 테넌트별 유효한 정책 조회 (글로벌 + 테넌트 정책)
     * - 캐시 적용 (TTL: 10분)
     * 
     * @param tenantId 테넌트 ID
     * @return 유효한 정책 집합
     */
    @Cacheable(value = "tenantPolicies", key = "#tenantId", unless = "#result == null", cacheManager = "policyCacheManager")
    public EffectivePolicySetDTO getEffectivePolicies(Long tenantId) {
        log.info("[TenantPolicyService] getEffectivePolicies - tenantId={}", tenantId);
        
        // 테넌트 조회
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(SecurityErrorCode.POLICY_CONTEXT_MISSING, 
                        "테넌트를 찾을 수 없습니다: " + tenantId));
        
        // 글로벌 정책 조회
        List<SecurityPolicy> globalPolicies = policyRepository.findByIsGlobalTrueAndIsEnabledTrue();
        log.info("[TenantPolicyService] getEffectivePolicies - globalPolicies count={}", globalPolicies.size());
        
        // 테넌트 정책 조회
        List<SecurityPolicy> tenantPolicies = policyRepository.findByTenantIdAndIsEnabledTrue(tenantId);
        log.info("[TenantPolicyService] getEffectivePolicies - tenantPolicies count={}", tenantPolicies.size());
        
        // DTO 생성
        EffectivePolicySetDTO result = EffectivePolicySetDTO.builder()
                .tenantId(String.valueOf(tenantId))
                .tenantKey(LogMaskingUtils.maskTenantKey(tenant.getTenantKey()))
                .globalPolicies(EffectivePolicySetDTO.toPolicySummaryList(globalPolicies))
                .tenantPolicies(EffectivePolicySetDTO.toPolicySummaryList(tenantPolicies))
                .queriedAt(LocalDateTime.now())
                .cacheHit(false)
                .build();
        
        result.calculateCounts();
        
        log.info("[TenantPolicyService] getEffectivePolicies - success tenantId={}, total={}", 
                tenantId, result.getTotalPolicyCount());
        
        return result;
    }
    
    /**
     * 테넌트별 유효한 정책 조회 (우선순위 정렬)
     * 
     * @param tenantId 테넌트 ID
     * @param ascending 오름차순 여부
     * @return 정렬된 정책 목록
     */
    public List<SecurityPolicy> getSortedEffectivePolicies(Long tenantId, boolean ascending) {
        log.info("[TenantPolicyService] getSortedEffectivePolicies - tenantId={}, ascending={}", tenantId, ascending);
        
        // 글로벌 + 테넌트 정책 조합
        List<SecurityPolicy> globalPolicies = policyRepository.findByIsGlobalTrueAndIsEnabledTrue();
        List<SecurityPolicy> tenantPolicies = policyRepository.findByTenantIdAndIsEnabledTrue(tenantId);
        
        // 병합 및 정렬
        List<SecurityPolicy> combined = Stream.concat(globalPolicies.stream(), tenantPolicies.stream())
                .sorted(ascending ? 
                        Comparator.comparing(SecurityPolicy::getPriority, Comparator.nullsLast(Comparator.naturalOrder())) :
                        Comparator.comparing(SecurityPolicy::getPriority, Comparator.nullsFirst(Comparator.reverseOrder())))
                .toList();
        
        log.info("[TenantPolicyService] getSortedEffectivePolicies - success count={}", combined.size());
        return combined;
    }
    
    /**
     * 테넌트 생성 시 기본 보안 정책 자동 생성
     * 
     * @param tenant 테넌트 엔티티
     * @return 생성된 정책 목록
     */
    @Transactional
    @CacheEvict(value = "tenantPolicies", key = "#tenant.id", cacheManager = "policyCacheManager")
    public List<SecurityPolicy> initializeDefaultPolicies(Tenant tenant) {
        log.info("[TenantPolicyService] initializeDefaultPolicies - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenant.getTenantKey()));
        
        List<SecurityPolicy> policies = new ArrayList<>();
        
        // 1. 접근 제어 정책
        policies.add(createPolicyFromTemplate(tenant, DefaultPolicyTemplate.accessControlTemplate(tenant.getTenantKey())));
        
        // 2. 인증 정책
        policies.add(createPolicyFromTemplate(tenant, DefaultPolicyTemplate.authenticationTemplate(tenant.getTenantKey())));
        
        // 3. 인가 정책
        policies.add(createPolicyFromTemplate(tenant, DefaultPolicyTemplate.authorizationTemplate(tenant.getTenantKey())));
        
        // 4. 데이터 보호 정책
        policies.add(createPolicyFromTemplate(tenant, DefaultPolicyTemplate.dataProtectionTemplate(tenant.getTenantKey())));
        
        // 5. 감사 로깅 정책
        policies.add(createPolicyFromTemplate(tenant, DefaultPolicyTemplate.auditLoggingTemplate(tenant.getTenantKey())));
        
        log.info("[TenantPolicyService] initializeDefaultPolicies - success tenantId={}, count={}", 
                tenant.getId(), policies.size());
        
        return policies;
    }
    
    /**
     * 테넌트 정책 캐시 무효화
     * 
     * @param tenantId 테넌트 ID
     */
    @CacheEvict(value = "tenantPolicies", key = "#tenantId", cacheManager = "policyCacheManager")
    public void evictTenantPolicyCache(Long tenantId) {
        log.info("[TenantPolicyService] evictTenantPolicyCache - tenantId={}", tenantId);
    }
    
    /**
     * 전체 테넌트 정책 캐시 무효화
     */
    @CacheEvict(value = "tenantPolicies", allEntries = true, cacheManager = "policyCacheManager")
    public void evictAllTenantPolicyCache() {
        log.info("[TenantPolicyService] evictAllTenantPolicyCache");
    }
    
    /**
     * 테넌트별 정책 타입별 조회
     * 
     * @param tenantId 테넌트 ID
     * @param policyType 정책 타입
     * @return 정책 목록
     */
    public List<SecurityPolicy> getPoliciesByType(Long tenantId, SecurityPolicy.PolicyType policyType) {
        log.info("[TenantPolicyService] getPoliciesByType - tenantId={}, policyType={}", tenantId, policyType);
        
        List<SecurityPolicy> allPolicies = getSortedEffectivePolicies(tenantId, false);
        List<SecurityPolicy> filtered = allPolicies.stream()
                .filter(p -> p.getPolicyType() == policyType)
                .toList();
        
        log.info("[TenantPolicyService] getPoliciesByType - success count={}", filtered.size());
        return filtered;
    }
    
    /**
     * 테넌트 정책 통계
     * 
     * @param tenantId 테넌트 ID
     * @return 정책 통계
     */
    public PolicyStatisticsDTO getPolicyStatistics(Long tenantId) {
        log.info("[TenantPolicyService] getPolicyStatistics - tenantId={}", tenantId);
        
        EffectivePolicySetDTO policies = getEffectivePolicies(tenantId);
        
        PolicyStatisticsDTO stats = PolicyStatisticsDTO.builder()
                .tenantId(String.valueOf(tenantId))
                .totalPolicies(policies.getTotalPolicyCount())
                .globalPolicies(policies.getGlobalPolicyCount())
                .tenantPolicies(policies.getTenantPolicyCount())
                .build();
        
        // 타입별 통계
        List<SecurityPolicy> allPolicies = getSortedEffectivePolicies(tenantId, false);
        stats.setPoliciesByType(countByType(allPolicies));
        stats.setPoliciesBySeverity(countBySeverity(allPolicies));
        
        log.info("[TenantPolicyService] getPolicyStatistics - success");
        return stats;
    }
    
    // ==================== Private 메서드 ====================
    
    /**
     * 템플릿으로부터 정책 생성
     */
    private SecurityPolicy createPolicyFromTemplate(Tenant tenant, DefaultPolicyTemplate template) {
        SecurityPolicy policy = SecurityPolicy.builder()
                .policyKey(template.getPolicyKey())
                .policyName(template.getPolicyName())
                .description(template.getDescription())
                .tenant(tenant)
                .status(Status.ACTIVE)
                .policyType(template.getPolicyType())
                .severity(template.getSeverity())
                .isGlobal(false)  // 테넌트 전용 정책
                .isSystem(template.getIsSystem())
                .isEnabled(true)
                .rules(template.getRules())
                .conditions(template.getConditions())
                .actions(template.getActions())
                .priority(template.getPriority())
                .version("1.0")
                .build();
        
        return policyRepository.save(policy);
    }
    
    /**
     * 타입별 정책 개수 계산
     */
    private java.util.Map<String, Integer> countByType(List<SecurityPolicy> policies) {
        return policies.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getPolicyType() != null ? p.getPolicyType().name() : "UNKNOWN",
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.counting(),
                                Long::intValue
                        )
                ));
    }
    
    /**
     * 심각도별 정책 개수 계산
     */
    private java.util.Map<String, Integer> countBySeverity(List<SecurityPolicy> policies) {
        return policies.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getSeverity() != null ? p.getSeverity().name() : "UNKNOWN",
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.counting(),
                                Long::intValue
                        )
                ));
    }
    
    /**
     * 정책 통계 DTO (내부 클래스)
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PolicyStatisticsDTO {
        private String tenantId;
        private Integer totalPolicies;
        private Integer globalPolicies;
        private Integer tenantPolicies;
        private java.util.Map<String, Integer> policiesByType;
        private java.util.Map<String, Integer> policiesBySeverity;
    }
}

