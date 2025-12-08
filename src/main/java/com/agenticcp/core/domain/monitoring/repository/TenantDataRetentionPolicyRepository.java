package com.agenticcp.core.domain.monitoring.repository;

import com.agenticcp.core.domain.monitoring.entity.TenantDataRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 테넌트별 데이터 보관 정책 리포지토리
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Repository
public interface TenantDataRetentionPolicyRepository extends JpaRepository<TenantDataRetentionPolicy, Long> {

    /**
     * 테넌트별 모든 보관 정책 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 보관 정책 목록 (우선순위 순)
     */
    @Query("SELECT p FROM TenantDataRetentionPolicy p WHERE p.tenantId = :tenantId ORDER BY p.priority ASC")
    List<TenantDataRetentionPolicy> findByTenantId(@Param("tenantId") String tenantId);

    /**
     * 테넌트별 활성화된 보관 정책 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 활성화된 보관 정책 목록 (우선순위 순)
     */
    @Query("SELECT p FROM TenantDataRetentionPolicy p WHERE p.tenantId = :tenantId AND p.isEnabled = true ORDER BY p.priority ASC")
    List<TenantDataRetentionPolicy> findEnabledByTenantId(@Param("tenantId") String tenantId);

    /**
     * 테넌트와 데이터 타입별 보관 정책 조회
     * 
     * @param tenantId 테넌트 ID
     * @param dataType 데이터 타입
     * @return 보관 정책 (Optional)
     */
    @Query("SELECT p FROM TenantDataRetentionPolicy p WHERE p.tenantId = :tenantId AND p.dataType = :dataType")
    Optional<TenantDataRetentionPolicy> findByTenantIdAndDataType(@Param("tenantId") String tenantId, @Param("dataType") String dataType);

    /**
     * 정리 실행이 필요한 보관 정책 조회
     * 
     * @param cutoffTime 정리 기준 시각
     * @return 정리가 필요한 보관 정책 목록
     */
    @Query("SELECT p FROM TenantDataRetentionPolicy p WHERE p.isEnabled = true AND " +
           "(p.lastCleanupAt IS NULL OR p.lastCleanupAt < :cutoffTime) " +
           "ORDER BY p.priority ASC, p.tenantId ASC")
    List<TenantDataRetentionPolicy> findPoliciesNeedingCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 특정 데이터 타입의 모든 보관 정책 조회
     * 
     * @param dataType 데이터 타입
     * @return 활성화된 보관 정책 목록
     */
    @Query("SELECT p FROM TenantDataRetentionPolicy p WHERE p.dataType = :dataType AND p.isEnabled = true")
    List<TenantDataRetentionPolicy> findEnabledByDataType(@Param("dataType") String dataType);

    /**
     * 테넌트별 보관 정책 개수 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 보관 정책 개수
     */
    @Query("SELECT COUNT(p) FROM TenantDataRetentionPolicy p WHERE p.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);

    /**
     * 데이터 타입별 보관 정책 개수 조회
     * 
     * @param dataType 데이터 타입
     * @return 보관 정책 개수
     */
    @Query("SELECT COUNT(p) FROM TenantDataRetentionPolicy p WHERE p.dataType = :dataType")
    long countByDataType(@Param("dataType") String dataType);

    /**
     * 활성화된 보관 정책 개수 조회
     * 
     * @return 활성화된 보관 정책 개수
     */
    @Query("SELECT COUNT(p) FROM TenantDataRetentionPolicy p WHERE p.isEnabled = true")
    long countEnabledPolicies();

    /**
     * 테넌트별 기본 보관 정책 설정 (METRIC 데이터 타입)
     * 
     * @param tenantId 테넌트 ID
     * @param now 현재 시각
     */
    @Modifying
    @Query("INSERT INTO TenantDataRetentionPolicy (tenantId, dataType, retentionDays, isEnabled, deletionStrategy, priority, createdAt, updatedAt) " +
           "VALUES (:tenantId, 'METRIC', 30, true, 'DELETE', 10, :now, :now)")
    void createDefaultMetricRetentionPolicy(@Param("tenantId") String tenantId, @Param("now") LocalDateTime now);

    /**
     * 테넌트의 모든 보관 정책 비활성화
     * 
     * @param tenantId 테넌트 ID
     * @param now 현재 시각
     */
    @Modifying
    @Query("UPDATE TenantDataRetentionPolicy p SET p.isEnabled = false, p.updatedAt = :now WHERE p.tenantId = :tenantId")
    void disableAllPoliciesByTenant(@Param("tenantId") String tenantId, @Param("now") LocalDateTime now);

    /**
     * 특정 데이터 타입의 모든 보관 정책 비활성화
     * 
     * @param dataType 데이터 타입
     * @param now 현재 시각
     */
    @Modifying
    @Query("UPDATE TenantDataRetentionPolicy p SET p.isEnabled = false, p.updatedAt = :now WHERE p.dataType = :dataType")
    void disableAllPoliciesByDataType(@Param("dataType") String dataType, @Param("now") LocalDateTime now);

    /**
     * 오래된 보관 정책 삭제 (테스트용)
     * 
     * @param cutoffDate 삭제 기준 시각
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM TenantDataRetentionPolicy p WHERE p.createdAt < :cutoffDate")
    int deleteOldPolicies(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 테넌트별 보관 정책 통계 조회
     * 
     * @return 테넌트별 통계 정보 목록 (tenantId, 총 개수, 활성화 개수, 평균 보관일수, 최소 보관일수, 최대 보관일수)
     */
    @Query("SELECT p.tenantId, COUNT(p), SUM(CASE WHEN p.isEnabled = true THEN 1 ELSE 0 END), " +
           "AVG(p.retentionDays), MIN(p.retentionDays), MAX(p.retentionDays) " +
           "FROM TenantDataRetentionPolicy p GROUP BY p.tenantId")
    List<Object[]> getRetentionPolicyStatisticsByTenant();

    /**
     * 데이터 타입별 보관 정책 통계 조회
     * 
     * @return 데이터 타입별 통계 정보 목록 (dataType, 총 개수, 활성화 개수, 평균 보관일수, 최소 보관일수, 최대 보관일수)
     */
    @Query("SELECT p.dataType, COUNT(p), SUM(CASE WHEN p.isEnabled = true THEN 1 ELSE 0 END), " +
           "AVG(p.retentionDays), MIN(p.retentionDays), MAX(p.retentionDays) " +
           "FROM TenantDataRetentionPolicy p GROUP BY p.dataType")
    List<Object[]> getRetentionPolicyStatisticsByDataType();
}
