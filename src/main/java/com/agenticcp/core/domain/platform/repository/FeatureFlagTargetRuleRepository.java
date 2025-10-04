package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 기능 플래그 타겟팅 규칙 Repository
 * 
 * 기능 플래그 타겟팅 규칙에 대한 데이터 접근 계층을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface FeatureFlagTargetRuleRepository extends TenantAwareRepository<FeatureFlagTargetRule, Long> {

    /**
     * 기능 플래그 ID로 활성화된 타겟팅 규칙 목록 조회 (우선순위 내림차순)
     * 
     * @param featureFlagId 기능 플래그 ID
     * @param isDeleted 삭제 여부
     * @return 활성화된 타겟팅 규칙 목록
     */
    @Query("SELECT r FROM FeatureFlagTargetRule r WHERE r.featureFlag.id = :featureFlagId " +
           "AND r.isDeleted = :isDeleted AND r.isEnabled = true " +
           "ORDER BY r.priority DESC, r.createdAt ASC")
    List<FeatureFlagTargetRule> findActiveRulesByFeatureFlagId(
            @Param("featureFlagId") Long featureFlagId, 
            @Param("isDeleted") Boolean isDeleted);

    /**
     * 기능 플래그 ID로 모든 타겟팅 규칙 목록 조회 (우선순위 내림차순)
     * 
     * @param featureFlagId 기능 플래그 ID
     * @param isDeleted 삭제 여부
     * @return 타겟팅 규칙 목록
     */
    @Query("SELECT r FROM FeatureFlagTargetRule r WHERE r.featureFlag.id = :featureFlagId " +
           "AND r.isDeleted = :isDeleted " +
           "ORDER BY r.priority DESC, r.createdAt ASC")
    List<FeatureFlagTargetRule> findAllByFeatureFlagId(
            @Param("featureFlagId") Long featureFlagId, 
            @Param("isDeleted") Boolean isDeleted);

    /**
     * 기능 플래그 ID와 규칙 타입으로 타겟팅 규칙 조회
     * 
     * @param featureFlagId 기능 플래그 ID
     * @param ruleType 규칙 타입
     * @param isDeleted 삭제 여부
     * @return 타겟팅 규칙 목록
     */
    @Query("SELECT r FROM FeatureFlagTargetRule r WHERE r.featureFlag.id = :featureFlagId " +
           "AND r.ruleType = :ruleType AND r.isDeleted = :isDeleted " +
           "ORDER BY r.priority DESC, r.createdAt ASC")
    List<FeatureFlagTargetRule> findByFeatureFlagIdAndRuleType(
            @Param("featureFlagId") Long featureFlagId, 
            @Param("ruleType") FeatureFlagTargetRule.RuleType ruleType,
            @Param("isDeleted") Boolean isDeleted);

    /**
     * 기능 플래그 ID와 규칙 타입으로 활성화된 타겟팅 규칙 조회
     * 
     * @param featureFlagId 기능 플래그 ID
     * @param ruleType 규칙 타입
     * @param isDeleted 삭제 여부
     * @return 활성화된 타겟팅 규칙 목록
     */
    @Query("SELECT r FROM FeatureFlagTargetRule r WHERE r.featureFlag.id = :featureFlagId " +
           "AND r.ruleType = :ruleType AND r.isDeleted = :isDeleted AND r.isEnabled = true " +
           "ORDER BY r.priority DESC, r.createdAt ASC")
    List<FeatureFlagTargetRule> findActiveRulesByFeatureFlagIdAndRuleType(
            @Param("featureFlagId") Long featureFlagId, 
            @Param("ruleType") FeatureFlagTargetRule.RuleType ruleType,
            @Param("isDeleted") Boolean isDeleted);

    /**
     * 특정 규칙 ID로 타겟팅 규칙 조회 (삭제되지 않은 것만)
     * 
     * @param ruleId 규칙 ID
     * @param isDeleted 삭제 여부
     * @return 타겟팅 규칙 (Optional)
     */
    @Query("SELECT r FROM FeatureFlagTargetRule r WHERE r.id = :ruleId AND r.isDeleted = :isDeleted")
    Optional<FeatureFlagTargetRule> findByIdAndNotDeleted(
            @Param("ruleId") Long ruleId, 
            @Param("isDeleted") Boolean isDeleted);

    /**
     * 기능 플래그 ID와 규칙 이름으로 중복 체크
     * 
     * @param featureFlagId 기능 플래그 ID
     * @param ruleName 규칙 이름
     * @param isDeleted 삭제 여부
     * @return 존재 여부
     */
    @Query("SELECT COUNT(r) > 0 FROM FeatureFlagTargetRule r WHERE r.featureFlag.id = :featureFlagId " +
           "AND r.ruleName = :ruleName AND r.isDeleted = :isDeleted")
    boolean existsByFeatureFlagIdAndRuleName(
            @Param("featureFlagId") Long featureFlagId, 
            @Param("ruleName") String ruleName,
            @Param("isDeleted") Boolean isDeleted);

    /**
     * 기능 플래그 ID와 규칙 이름으로 중복 체크 (특정 규칙 ID 제외)
     * 
     * @param featureFlagId 기능 플래그 ID
     * @param ruleName 규칙 이름
     * @param ruleId 제외할 규칙 ID
     * @param isDeleted 삭제 여부
     * @return 존재 여부
     */
    @Query("SELECT COUNT(r) > 0 FROM FeatureFlagTargetRule r WHERE r.featureFlag.id = :featureFlagId " +
           "AND r.ruleName = :ruleName AND r.id != :ruleId AND r.isDeleted = :isDeleted")
    boolean existsByFeatureFlagIdAndRuleNameExcludingId(
            @Param("featureFlagId") Long featureFlagId, 
            @Param("ruleName") String ruleName,
            @Param("ruleId") Long ruleId,
            @Param("isDeleted") Boolean isDeleted);

    /**
     * 활성화된 타겟팅 규칙 개수 조회
     * 
     * @param featureFlagId 기능 플래그 ID
     * @param isDeleted 삭제 여부
     * @return 활성화된 규칙 개수
     */
    @Query("SELECT COUNT(r) FROM FeatureFlagTargetRule r WHERE r.featureFlag.id = :featureFlagId " +
           "AND r.isDeleted = :isDeleted AND r.isEnabled = true")
    long countActiveRulesByFeatureFlagId(
            @Param("featureFlagId") Long featureFlagId, 
            @Param("isDeleted") Boolean isDeleted);

    /**
     * 특정 우선순위 범위의 타겟팅 규칙 조회
     * 
     * @param featureFlagId 기능 플래그 ID
     * @param minPriority 최소 우선순위
     * @param maxPriority 최대 우선순위
     * @param isDeleted 삭제 여부
     * @return 타겟팅 규칙 목록
     */
    @Query("SELECT r FROM FeatureFlagTargetRule r WHERE r.featureFlag.id = :featureFlagId " +
           "AND r.priority BETWEEN :minPriority AND :maxPriority AND r.isDeleted = :isDeleted " +
           "ORDER BY r.priority DESC, r.createdAt ASC")
    List<FeatureFlagTargetRule> findByPriorityRange(
            @Param("featureFlagId") Long featureFlagId,
            @Param("minPriority") Integer minPriority,
            @Param("maxPriority") Integer maxPriority,
            @Param("isDeleted") Boolean isDeleted);
}
