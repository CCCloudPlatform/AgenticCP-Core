package com.agenticcp.core.domain.platform;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.platform.dto.CreateTargetRuleRequestDto;
import com.agenticcp.core.domain.platform.dto.UpdateTargetRuleRequestDto;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;

import java.time.LocalDateTime;

/**
 * 플랫폼 도메인 테스트 데이터 빌더
 * 
 * 단위 테스트에서 사용할 테스트 데이터를 생성합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public class TestDataBuilder {

    // FeatureFlag 관련 빌더
    public static FeatureFlag.FeatureFlagBuilder featureFlagBuilder() {
        return FeatureFlag.builder()
                .flagKey("test-feature-flag")
                .flagName("테스트 기능 플래그")
                .description("테스트용 기능 플래그입니다")
                .isEnabled(true)
                .status(Status.ACTIVE)
                .rolloutPercentage(100);
    }

    // FeatureFlagTargetRule 관련 빌더
    public static FeatureFlagTargetRule.FeatureFlagTargetRuleBuilder targetRuleBuilder() {
        return FeatureFlagTargetRule.builder()
                .flagKey("test-feature-flag")
                .ruleName("테스트 타겟팅 규칙")
                .description("테스트용 타겟팅 규칙입니다")
                .status(Status.ACTIVE)
                .priority(0)
                .isEnabled(true)
                .cloudProvider("AWS")
                .region("ap-northeast-2")
                .tenantType("ENTERPRISE")
                .tenantGrade("PREMIUM")
                .userRole("ADMIN")
                .rolloutPercentage(100)
                .rolloutStrategy("PERCENTAGE")
                .tenantId(1L);
    }

    // CreateTargetRuleRequestDto 관련 빌더
    public static CreateTargetRuleRequestDto.CreateTargetRuleRequestDtoBuilder createTargetRuleRequestBuilder() {
        return CreateTargetRuleRequestDto.builder()
                .flagKey("test-feature-flag")
                .ruleName("새로운 타겟팅 규칙")
                .description("새로 생성할 타겟팅 규칙입니다")
                .priority(1)
                .isEnabled(true)
                .cloudProvider("AWS")
                .region("ap-northeast-2")
                .tenantType("ENTERPRISE")
                .tenantGrade("PREMIUM")
                .userRole("ADMIN")
                .rolloutPercentage(50)
                .rolloutStrategy("PERCENTAGE");
    }

    // UpdateTargetRuleRequestDto 관련 빌더
    public static UpdateTargetRuleRequestDto.UpdateTargetRuleRequestDtoBuilder updateTargetRuleRequestBuilder() {
        return UpdateTargetRuleRequestDto.builder()
                .ruleName("수정된 타겟팅 규칙")
                .description("수정된 타겟팅 규칙입니다")
                .priority(2)
                .isEnabled(false)
                .cloudProvider("GCP")
                .region("asia-northeast1")
                .tenantType("STARTUP")
                .tenantGrade("BASIC")
                .userRole("USER")
                .rolloutPercentage(75)
                .rolloutStrategy("USER_ID_HASH");
    }

    // 특정 시나리오용 빌더들
    public static CreateTargetRuleRequestDto.CreateTargetRuleRequestDtoBuilder invalidRolloutPercentageRequestBuilder() {
        return createTargetRuleRequestBuilder()
                .rolloutPercentage(150); // 잘못된 롤아웃 비율
    }

    public static CreateTargetRuleRequestDto.CreateTargetRuleRequestDtoBuilder invalidDateRangeRequestBuilder() {
        return createTargetRuleRequestBuilder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now()); // 잘못된 날짜 범위
    }

    public static CreateTargetRuleRequestDto.CreateTargetRuleRequestDtoBuilder invalidPriorityRequestBuilder() {
        return createTargetRuleRequestBuilder()
                .priority(1000); // 잘못된 우선순위
    }

    public static FeatureFlagTargetRule.FeatureFlagTargetRuleBuilder expiredTargetRuleBuilder() {
        return targetRuleBuilder()
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().minusDays(1)); // 만료된 규칙
    }

    public static FeatureFlagTargetRule.FeatureFlagTargetRuleBuilder inactiveTargetRuleBuilder() {
        return targetRuleBuilder()
                .status(Status.INACTIVE)
                .isEnabled(false); // 비활성 규칙
    }
}
