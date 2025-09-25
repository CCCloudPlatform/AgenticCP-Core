package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 기능 플래그 타겟팅 규칙 Repository
 * 
 * 타겟팅 규칙의 데이터 접근을 담당합니다.
 * 멀티테넌트 환경을 지원하며, 다양한 조건으로 타겟팅 규칙을 조회할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Repository
public interface FeatureFlagTargetRuleRepository extends TenantAwareRepository<FeatureFlagTargetRule, Long> {

    // 기본 조회 메서드
    List<FeatureFlagTargetRule> findByFlagKey(String flagKey);
    
    Optional<FeatureFlagTargetRule> findByIdAndTenantId(Long id, Long tenantId);
    
    List<FeatureFlagTargetRule> findByFlagKeyAndTenantId(String flagKey, Long tenantId);
    
    boolean existsByFlagKeyAndRuleNameAndTenantId(String flagKey, String ruleName, Long tenantId);

    // 상태별 조회
    List<FeatureFlagTargetRule> findByStatus(Status status);
    
    List<FeatureFlagTargetRule> findByFlagKeyAndStatus(String flagKey, Status status);
    
    List<FeatureFlagTargetRule> findByIsEnabled(Boolean isEnabled);
    
    List<FeatureFlagTargetRule> findByFlagKeyAndIsEnabled(String flagKey, Boolean isEnabled);

    // 활성화된 타겟팅 규칙 조회
    @Query("SELECT ftr FROM FeatureFlagTargetRule ftr WHERE ftr.flagKey = :flagKey " +
           "AND ftr.status = :status AND ftr.isEnabled = true " +
           "AND (ftr.startDate IS NULL OR ftr.startDate <= :now) " +
           "AND (ftr.endDate IS NULL OR ftr.endDate >= :now) " +
           "ORDER BY ftr.priority DESC, ftr.createdAt ASC")
    List<FeatureFlagTargetRule> findActiveTargetRulesByFlagKey(
            @Param("flagKey") String flagKey, 
            @Param("status") Status status, 
            @Param("now") LocalDateTime now);

    // 특정 타겟팅 기준으로 조회
    @Query("SELECT ftr FROM FeatureFlagTargetRule ftr WHERE ftr.flagKey = :flagKey " +
           "AND ftr.status = :status AND ftr.isEnabled = true " +
           "AND (ftr.cloudProvider = :cloudProvider OR ftr.cloudProvider IS NULL) " +
           "AND (ftr.region = :region OR ftr.region IS NULL) " +
           "AND (ftr.tenantType = :tenantType OR ftr.tenantType IS NULL) " +
           "AND (ftr.tenantGrade = :tenantGrade OR ftr.tenantGrade IS NULL) " +
           "AND (ftr.userRole = :userRole OR ftr.userRole IS NULL) " +
           "ORDER BY ftr.priority DESC")
    List<FeatureFlagTargetRule> findMatchingTargetRules(
            @Param("flagKey") String flagKey,
            @Param("status") Status status,
            @Param("cloudProvider") String cloudProvider,
            @Param("region") String region,
            @Param("tenantType") String tenantType,
            @Param("tenantGrade") String tenantGrade,
            @Param("userRole") String userRole);

    // 페이징 조회
    @Query("SELECT ftr FROM FeatureFlagTargetRule ftr WHERE ftr.flagKey = :flagKey")
    Page<FeatureFlagTargetRule> findByFlagKeyWithPagination(
            @Param("flagKey") String flagKey, 
            Pageable pageable);

    @Query("SELECT ftr FROM FeatureFlagTargetRule ftr WHERE ftr.flagKey = :flagKey AND ftr.status = :status")
    Page<FeatureFlagTargetRule> findByFlagKeyAndStatusWithPagination(
            @Param("flagKey") String flagKey, 
            @Param("status") Status status, 
            Pageable pageable);

    // 통계 조회
    @Query("SELECT COUNT(ftr) FROM FeatureFlagTargetRule ftr WHERE ftr.flagKey = :flagKey AND ftr.status = :status")
    long countByFlagKeyAndStatus(@Param("flagKey") String flagKey, @Param("status") Status status);

    @Query("SELECT COUNT(ftr) FROM FeatureFlagTargetRule ftr WHERE ftr.flagKey = :flagKey AND ftr.isEnabled = true")
    long countActiveRulesByFlagKey(@Param("flagKey") String flagKey);

    // 시간 기반 조회
    @Query("SELECT ftr FROM FeatureFlagTargetRule ftr WHERE ftr.flagKey = :flagKey " +
           "AND ftr.status = :status AND ftr.isEnabled = true " +
           "AND ftr.startDate <= :now AND (ftr.endDate IS NULL OR ftr.endDate >= :now)")
    List<FeatureFlagTargetRule> findCurrentlyActiveRules(
            @Param("flagKey") String flagKey, 
            @Param("status") Status status, 
            @Param("now") LocalDateTime now);

    // 만료 예정 규칙 조회
    @Query("SELECT ftr FROM FeatureFlagTargetRule ftr WHERE ftr.endDate IS NOT NULL " +
           "AND ftr.endDate BETWEEN :startDate AND :endDate " +
           "AND ftr.status = :status")
    List<FeatureFlagTargetRule> findExpiringRules(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Status status);

    // 우선순위별 조회
    @Query("SELECT ftr FROM FeatureFlagTargetRule ftr WHERE ftr.flagKey = :flagKey " +
           "AND ftr.status = :status AND ftr.isEnabled = true " +
           "ORDER BY ftr.priority DESC, ftr.createdAt ASC")
    List<FeatureFlagTargetRule> findByFlagKeyOrderByPriority(
            @Param("flagKey") String flagKey, 
            @Param("status") Status status);

    // 삭제 메서드
    void deleteByFlagKeyAndTenantId(String flagKey, Long tenantId);
    
    void deleteByStatusAndTenantId(Status status, Long tenantId);
}
