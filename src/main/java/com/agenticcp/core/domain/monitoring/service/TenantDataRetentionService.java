package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.entity.TenantDataRetentionPolicy;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.repository.TenantDataRetentionPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 테넌트별 데이터 보관 정책 관리 서비스
 * 
 * 테넌트별로 메트릭 데이터의 보관 기간을 관리하고 오래된 데이터를 자동으로 정리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TenantDataRetentionService {

    private final TenantDataRetentionPolicyRepository policyRepository;
    private final MetricRepository metricRepository;

    /**
     * 테넌트별 기본 보관 정책 설정 (30일)
     */
    @Transactional
    public TenantDataRetentionPolicy createDefaultRetentionPolicy(String tenantId) {
        log.info("테넌트별 기본 보관 정책 설정: tenantId={}", tenantId);
        
        // 이미 존재하는지 확인
        if (policyRepository.findByTenantIdAndDataType(tenantId, "METRIC").isPresent()) {
            log.warn("테넌트의 METRIC 보관 정책이 이미 존재합니다: tenantId={}", tenantId);
            return policyRepository.findByTenantIdAndDataType(tenantId, "METRIC").get();
        }

        TenantDataRetentionPolicy policy = TenantDataRetentionPolicy.builder()
                .tenantId(tenantId)
                .dataType("METRIC")
                .retentionDays(30)
                .isEnabled(true)
                .deletionStrategy(TenantDataRetentionPolicy.DeletionStrategy.DELETE)
                .description("기본 메트릭 데이터 보관 정책 (30일)")
                .priority(10)
                .build();

        TenantDataRetentionPolicy saved = policyRepository.save(policy);
        log.info("테넌트별 기본 보관 정책 설정 완료: tenantId={}, policyId={}", tenantId, saved.getId());
        
        return saved;
    }

    /**
     * 테넌트별 보관 정책 조회
     */
    public List<TenantDataRetentionPolicy> getRetentionPolicies(String tenantId) {
        log.debug("테넌트별 보관 정책 조회: tenantId={}", tenantId);
        return policyRepository.findByTenantId(tenantId);
    }

    /**
     * 테넌트별 활성화된 보관 정책 조회
     */
    public List<TenantDataRetentionPolicy> getEnabledRetentionPolicies(String tenantId) {
        log.debug("테넌트별 활성화된 보관 정책 조회: tenantId={}", tenantId);
        return policyRepository.findEnabledByTenantId(tenantId);
    }

    /**
     * 테넌트별 특정 데이터 타입 보관 정책 조회
     */
    public TenantDataRetentionPolicy getRetentionPolicy(String tenantId, String dataType) {
        log.debug("테넌트별 보관 정책 조회: tenantId={}, dataType={}", tenantId, dataType);
        return policyRepository.findByTenantIdAndDataType(tenantId, dataType)
                .orElse(null);
    }

    /**
     * 테넌트별 보관 정책 업데이트
     */
    @Transactional
    public TenantDataRetentionPolicy updateRetentionPolicy(String tenantId, String dataType, 
                                                           Integer retentionDays, 
                                                           TenantDataRetentionPolicy.DeletionStrategy deletionStrategy,
                                                           String description) {
        log.info("테넌트별 보관 정책 업데이트: tenantId={}, dataType={}, retentionDays={}, strategy={}", 
                tenantId, dataType, retentionDays, deletionStrategy);

        TenantDataRetentionPolicy policy = policyRepository.findByTenantIdAndDataType(tenantId, dataType)
                .orElseThrow(() -> new IllegalArgumentException("보관 정책을 찾을 수 없습니다: " + tenantId + ", " + dataType));

        policy.setRetentionDays(retentionDays);
        policy.setDeletionStrategy(deletionStrategy);
        policy.setDescription(description);

        TenantDataRetentionPolicy updated = policyRepository.save(policy);
        log.info("테넌트별 보관 정책 업데이트 완료: tenantId={}, dataType={}", tenantId, dataType);
        
        return updated;
    }

    /**
     * 테넌트별 보관 정책 활성화/비활성화
     */
    @Transactional
    public void toggleRetentionPolicy(String tenantId, String dataType, boolean enabled) {
        log.info("테넌트별 보관 정책 토글: tenantId={}, dataType={}, enabled={}", tenantId, dataType, enabled);
        
        TenantDataRetentionPolicy policy = policyRepository.findByTenantIdAndDataType(tenantId, dataType)
                .orElseThrow(() -> new IllegalArgumentException("보관 정책을 찾을 수 없습니다: " + tenantId + ", " + dataType));

        policy.setIsEnabled(enabled);
        policyRepository.save(policy);
        
        log.info("테넌트별 보관 정책 토글 완료: tenantId={}, dataType={}, enabled={}", tenantId, dataType, enabled);
    }

    /**
     * 매일 자정에 실행되는 자동 데이터 정리 스케줄러
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    @Transactional
    public void cleanupExpiredData() {
        log.info("만료된 데이터 정리 작업 시작");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1); // 1시간 전부터 정리 필요
        List<TenantDataRetentionPolicy> policiesToCleanup = policyRepository.findPoliciesNeedingCleanup(cutoffTime);
        
        log.info("정리 대상 보관 정책 수: {}", policiesToCleanup.size());
        
        int totalCleaned = 0;
        for (TenantDataRetentionPolicy policy : policiesToCleanup) {
            try {
                int cleaned = cleanupTenantData(policy);
                totalCleaned += cleaned;
                log.info("테넌트 데이터 정리 완료: tenantId={}, dataType={}, cleaned={}", 
                        policy.getTenantId(), policy.getDataType(), cleaned);
            } catch (Exception e) {
                log.error("테넌트 데이터 정리 실패: tenantId={}, dataType={}", 
                        policy.getTenantId(), policy.getDataType(), e);
            }
        }
        
        log.info("만료된 데이터 정리 작업 완료: 총 정리된 레코드 수={}", totalCleaned);
    }

    /**
     * 특정 테넌트의 데이터 정리
     */
    @Transactional
    public int cleanupTenantData(TenantDataRetentionPolicy policy) {
        log.debug("테넌트 데이터 정리: tenantId={}, dataType={}, retentionDays={}", 
                policy.getTenantId(), policy.getDataType(), policy.getRetentionDays());
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(policy.getRetentionDays());
        int cleaned = 0;
        
        switch (policy.getDataType()) {
            case "METRIC":
                cleaned = metricRepository.deleteOldMetrics(policy.getTenantId(), cutoffDate);
                break;
            case "ALERT":
                // TODO: Alert 데이터 정리 구현
                log.debug("Alert 데이터 정리는 아직 구현되지 않았습니다");
                break;
            case "DASHBOARD":
                // TODO: Dashboard 데이터 정리 구현
                log.debug("Dashboard 데이터 정리는 아직 구현되지 않았습니다");
                break;
            default:
                log.warn("지원하지 않는 데이터 타입: {}", policy.getDataType());
                break;
        }
        
        // 정리 상태 업데이트
        policy.updateCleanupStatus((long) cleaned);
        policyRepository.save(policy);
        
        log.info("테넌트 데이터 정리 완료: tenantId={}, dataType={}, cleaned={}", 
                policy.getTenantId(), policy.getDataType(), cleaned);
        
        return cleaned;
    }

    /**
     * 수동으로 특정 테넌트 데이터 정리
     */
    @Transactional
    public int manualCleanupTenantData(String tenantId, String dataType) {
        log.info("수동 테넌트 데이터 정리: tenantId={}, dataType={}", tenantId, dataType);
        
        TenantDataRetentionPolicy policy = policyRepository.findByTenantIdAndDataType(tenantId, dataType)
                .orElseThrow(() -> new IllegalArgumentException("보관 정책을 찾을 수 없습니다: " + tenantId + ", " + dataType));
        
        if (!policy.getIsEnabled()) {
            throw new IllegalStateException("비활성화된 보관 정책입니다: " + tenantId + ", " + dataType);
        }
        
        return cleanupTenantData(policy);
    }

    /**
     * 보관 정책 통계 조회
     */
    public RetentionPolicyStatistics getRetentionPolicyStatistics() {
        log.debug("보관 정책 통계 조회");
        
        long totalPolicies = policyRepository.count();
        long enabledPolicies = policyRepository.countEnabledPolicies();
        
        return RetentionPolicyStatistics.builder()
                .totalPolicies(totalPolicies)
                .enabledPolicies(enabledPolicies)
                .disabledPolicies(totalPolicies - enabledPolicies)
                .build();
    }

    /**
     * 보관 정책 통계 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class RetentionPolicyStatistics {
        private final long totalPolicies;
        private final long enabledPolicies;
        private final long disabledPolicies;
        
        public double getEnabledPercentage() {
            if (totalPolicies == 0) return 0.0;
            return (double) enabledPolicies / totalPolicies * 100;
        }
    }
}
