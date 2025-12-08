package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.domain.platform.entity.PlatformComplianceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 플랫폼 컴플라이언스 설정 Repository
 * 
 * 플랫폼 컴플라이언스 설정 관련 데이터 접근을 제공합니다.
 * Singleton 패턴으로 구현되어 단일 인스턴스만 존재합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Repository
public interface PlatformComplianceConfigRepository extends JpaRepository<PlatformComplianceConfig, Long> {

    /**
     * 설정 키로 컴플라이언스 설정 조회
     * 
     * @param configKey 설정 키
     * @return 컴플라이언스 설정 (Optional)
     */
    Optional<PlatformComplianceConfig> findByConfigKey(String configKey);

    /**
     * Singleton 인스턴스 조회
     * 
     * @return 컴플라이언스 설정 (Optional)
     */
    @Query("SELECT pcc FROM PlatformComplianceConfig pcc " +
           "WHERE pcc.configKey = 'PLATFORM_COMPLIANCE_CONFIG' " +
           "AND pcc.isDeleted = false")
    Optional<PlatformComplianceConfig> findSingleton();

    /**
     * Singleton 인스턴스 존재 여부 확인
     * 
     * @return 존재 여부
     */
    @Query("SELECT COUNT(pcc) > 0 FROM PlatformComplianceConfig pcc " +
           "WHERE pcc.configKey = 'PLATFORM_COMPLIANCE_CONFIG' " +
           "AND pcc.isDeleted = false")
    boolean existsSingleton();

    /**
     * 보고서 생성이 필요한 설정 조회
     * 다음 보고서 생성 예정 일시가 현재 시간 이하인 설정을 조회합니다.
     * 
     * @param now 현재 시간
     * @return 보고서 생성이 필요한 설정 (Optional)
     */
    @Query("SELECT pcc FROM PlatformComplianceConfig pcc " +
           "WHERE pcc.configKey = 'PLATFORM_COMPLIANCE_CONFIG' " +
           "AND pcc.isDeleted = false " +
           "AND (pcc.nextReportGenerationAt IS NULL " +
           "     OR pcc.nextReportGenerationAt <= :now)")
    Optional<PlatformComplianceConfig> findConfigRequiringReportGeneration(@Param("now") LocalDateTime now);

    /**
     * 활성 설정 조회 (삭제되지 않은 Singleton 인스턴스)
     * 
     * @return 활성 컴플라이언스 설정 (Optional)
     */
    @Query("SELECT pcc FROM PlatformComplianceConfig pcc " +
           "WHERE pcc.configKey = 'PLATFORM_COMPLIANCE_CONFIG' " +
           "AND pcc.isDeleted = false")
    Optional<PlatformComplianceConfig> findActiveConfig();
}

