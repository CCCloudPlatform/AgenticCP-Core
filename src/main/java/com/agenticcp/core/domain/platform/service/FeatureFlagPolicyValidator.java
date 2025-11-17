package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.PlatformComplianceConfig;
import com.agenticcp.core.domain.platform.enums.FeatureFlagSeverity;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 기능 플래그 정책 검증기
 * 
 * 기능 플래그 변경에 대한 정책 검증을 수행합니다.
 * severity 기반으로 승인 필요 여부를 판단하고, 승인 없이 변경을 시도할 경우 예외를 발생시킵니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureFlagPolicyValidator {

    private final PlatformComplianceService complianceService;

    /**
     * 심각도에 따라 승인이 필요한지 확인
     * 
     * @param severity 플래그 심각도
     * @return 승인이 필요한 경우 true
     */
    public boolean requiresApproval(FeatureFlagSeverity severity) {
        if (severity == null) {
            return false;
        }

        PlatformComplianceConfig config = complianceService.getConfig();
        String requiredSeverity = config.getRequiredApprovalSeverity();

        // HIGH 또는 CRITICAL 심각도인 경우 승인 필요
        if (severity == FeatureFlagSeverity.HIGH || severity == FeatureFlagSeverity.CRITICAL) {
            // 설정에서 "HIGH" 또는 "CRITICAL"로 지정된 경우
            if ("HIGH".equalsIgnoreCase(requiredSeverity)) {
                return severity.isAtLeast(FeatureFlagSeverity.HIGH);
            } else if ("CRITICAL".equalsIgnoreCase(requiredSeverity)) {
                return severity == FeatureFlagSeverity.CRITICAL;
            }
            // 기본값: HIGH 이상이면 승인 필요
            return severity.isAtLeast(FeatureFlagSeverity.HIGH);
        }

        return false;
    }

    /**
     * 플래그 변경 시 승인 필요 여부 검증
     * 승인이 필요한데 승인이 없으면 예외를 발생시킵니다.
     * 
     * @param flag 변경할 플래그
     * @param hasApproval 승인 여부
     * @throws BusinessException 승인이 필요한데 승인이 없는 경우
     */
    public void validateFlagChange(FeatureFlag flag, boolean hasApproval) {
        if (flag == null) {
            log.warn("[FeatureFlagPolicyValidator] validateFlagChange - flag is null");
            return;
        }

        FeatureFlagSeverity severity = flag.getSeverity();
        if (severity == null) {
            severity = FeatureFlagSeverity.LOW;
        }

        boolean needsApproval = requiresApproval(severity);

        if (needsApproval && !hasApproval) {
            log.warn("[FeatureFlagPolicyValidator] validateFlagChange - approval required but not found " +
                    "flagKey={} severity={}", flag.getFlagKey(), severity);
            throw new BusinessException(
                    PlatformConfigErrorCode.FLAG_CHANGE_REQUIRES_APPROVAL,
                    String.format("심각도 %s 플래그 변경은 승인이 필요합니다. (플래그: %s)", 
                            severity.getDescription(), flag.getFlagKey())
            );
        }

        log.debug("[FeatureFlagPolicyValidator] validateFlagChange - validation passed " +
                "flagKey={} severity={} needsApproval={} hasApproval={}", 
                flag.getFlagKey(), severity, needsApproval, hasApproval);
    }
}

