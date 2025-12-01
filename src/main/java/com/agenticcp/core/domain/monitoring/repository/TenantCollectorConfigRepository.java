package com.agenticcp.core.domain.monitoring.repository;

import com.agenticcp.core.domain.monitoring.entity.TenantCollectorConfig;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 테넌트별 수집기 설정 Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Repository
public interface TenantCollectorConfigRepository extends JpaRepository<TenantCollectorConfig, Long> {

    /**
     * 테넌트별 활성화된 수집기 설정 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 활성화된 수집기 설정 목록 (우선순위 순)
     */
    @Query("SELECT tcc FROM TenantCollectorConfig tcc WHERE tcc.tenantId = :tenantId AND tcc.isEnabled = true ORDER BY tcc.priority ASC")
    List<TenantCollectorConfig> findEnabledByTenantId(@Param("tenantId") String tenantId);

    /**
     * 테넌트별 특정 수집기 설정 조회
     * 
     * @param tenantId 테넌트 ID
     * @param collectorType 수집기 타입
     * @return 수집기 설정 (Optional)
     */
    @Query("SELECT tcc FROM TenantCollectorConfig tcc WHERE tcc.tenantId = :tenantId AND tcc.collectorType = :collectorType")
    Optional<TenantCollectorConfig> findByTenantIdAndCollectorType(@Param("tenantId") String tenantId, 
                                                                  @Param("collectorType") CollectorType collectorType);

    /**
     * 테넌트별 모든 수집기 설정 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 수집기 설정 목록 (우선순위 순)
     */
    @Query("SELECT tcc FROM TenantCollectorConfig tcc WHERE tcc.tenantId = :tenantId ORDER BY tcc.priority ASC")
    List<TenantCollectorConfig> findAllByTenantId(@Param("tenantId") String tenantId);

    /**
     * 특정 수집기 타입을 사용하는 테넌트 목록 조회
     * 
     * @param collectorType 수집기 타입
     * @return 해당 수집기 타입을 사용하는 테넌트 ID 목록
     */
    @Query("SELECT DISTINCT tcc.tenantId FROM TenantCollectorConfig tcc WHERE tcc.collectorType = :collectorType AND tcc.isEnabled = true")
    List<String> findTenantIdsByCollectorType(@Param("collectorType") CollectorType collectorType);

    /**
     * 테넌트별 활성화된 수집기 타입 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 활성화된 수집기 타입 목록 (우선순위 순)
     */
    @Query("SELECT tcc.collectorType FROM TenantCollectorConfig tcc WHERE tcc.tenantId = :tenantId AND tcc.isEnabled = true ORDER BY tcc.priority ASC")
    List<CollectorType> findEnabledCollectorTypesByTenantId(@Param("tenantId") String tenantId);

    /**
     * 테넌트별 수집기 설정 존재 여부 확인
     * 
     * @param tenantId 테넌트 ID
     * @param collectorType 수집기 타입
     * @return 존재 여부
     */
    @Query("SELECT COUNT(tcc) > 0 FROM TenantCollectorConfig tcc WHERE tcc.tenantId = :tenantId AND tcc.collectorType = :collectorType")
    boolean existsByTenantIdAndCollectorType(@Param("tenantId") String tenantId, 
                                            @Param("collectorType") CollectorType collectorType);

    /**
     * 테넌트별 활성화된 수집기 수 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 활성화된 수집기 수
     */
    @Query("SELECT COUNT(tcc) FROM TenantCollectorConfig tcc WHERE tcc.tenantId = :tenantId AND tcc.isEnabled = true")
    long countEnabledByTenantId(@Param("tenantId") String tenantId);

    /**
     * 특정 수집기 타입의 활성화된 설정 수 조회
     * 
     * @param collectorType 수집기 타입
     * @return 활성화된 설정 수
     */
    @Query("SELECT COUNT(tcc) FROM TenantCollectorConfig tcc WHERE tcc.collectorType = :collectorType AND tcc.isEnabled = true")
    long countEnabledByCollectorType(@Param("collectorType") CollectorType collectorType);
}
