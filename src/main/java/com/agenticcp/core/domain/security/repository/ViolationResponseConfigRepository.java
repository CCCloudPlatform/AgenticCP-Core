package com.agenticcp.core.domain.security.repository;

import com.agenticcp.core.domain.security.entity.PolicyViolation;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.entity.ViolationResponseConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 위반 대응 설정 Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface ViolationResponseConfigRepository extends JpaRepository<ViolationResponseConfig, Long> {

    /**
     * 테넌트별 설정 조회
     */
    List<ViolationResponseConfig> findByTenantId(Long tenantId);

    /**
     * 글로벌 설정 조회
     */
    List<ViolationResponseConfig> findByIsGlobalTrue();

    /**
     * 활성화된 설정만 조회
     */
    List<ViolationResponseConfig> findByIsEnabledTrue();

    /**
     * 위반 타입별 설정 조회
     */
    List<ViolationResponseConfig> findByViolationType(PolicyViolation.ViolationType violationType);

    /**
     * 심각도별 설정 조회
     */
    List<ViolationResponseConfig> findBySeverity(SecurityPolicy.Severity severity);

    /**
     * 정책별 설정 조회
     */
    List<ViolationResponseConfig> findByPolicyId(Long policyId);

    /**
     * 테넌트 + 위반 타입별 활성화된 설정 조회
     */
    @Query("SELECT vrc FROM ViolationResponseConfig vrc WHERE " +
           "(vrc.tenantId = :tenantId OR vrc.isGlobal = true) AND " +
           "vrc.violationType = :violationType AND " +
           "vrc.isEnabled = true " +
           "ORDER BY vrc.priority DESC, vrc.isGlobal ASC")
    List<ViolationResponseConfig> findByTenantAndViolationType(@Param("tenantId") Long tenantId,
                                                                @Param("violationType") PolicyViolation.ViolationType violationType);

    /**
     * 테넌트 + 심각도별 활성화된 설정 조회
     */
    @Query("SELECT vrc FROM ViolationResponseConfig vrc WHERE " +
           "(vrc.tenantId = :tenantId OR vrc.isGlobal = true) AND " +
           "vrc.severity = :severity AND " +
           "vrc.isEnabled = true " +
           "ORDER BY vrc.priority DESC, vrc.isGlobal ASC")
    List<ViolationResponseConfig> findByTenantAndSeverity(@Param("tenantId") Long tenantId,
                                                           @Param("severity") SecurityPolicy.Severity severity);

    /**
     * 테넌트 + 정책별 활성화된 설정 조회
     */
    @Query("SELECT vrc FROM ViolationResponseConfig vrc WHERE " +
           "(vrc.tenantId = :tenantId OR vrc.isGlobal = true) AND " +
           "vrc.policyId = :policyId AND " +
           "vrc.isEnabled = true " +
           "ORDER BY vrc.priority DESC, vrc.isGlobal ASC")
    List<ViolationResponseConfig> findByTenantAndPolicy(@Param("tenantId") Long tenantId,
                                                         @Param("policyId") Long policyId);

    /**
     * 위반에 적용 가능한 모든 설정 조회 (우선순위 순)
     */
    @Query("SELECT vrc FROM ViolationResponseConfig vrc WHERE " +
           "(vrc.tenantId = :tenantId OR vrc.isGlobal = true) AND " +
           "(vrc.violationType = :violationType OR vrc.violationType IS NULL) AND " +
           "(vrc.severity = :severity OR vrc.severity IS NULL) AND " +
           "(vrc.policyId = :policyId OR vrc.policyId IS NULL) AND " +
           "vrc.isEnabled = true AND " +
           "vrc.isDeleted = false " +
           "ORDER BY vrc.priority DESC, vrc.isGlobal ASC")
    List<ViolationResponseConfig> findApplicableConfigs(@Param("tenantId") Long tenantId,
                                                         @Param("violationType") PolicyViolation.ViolationType violationType,
                                                         @Param("severity") SecurityPolicy.Severity severity,
                                                         @Param("policyId") Long policyId);

    /**
     * 자동 실행 가능한 설정 조회
     */
    @Query("SELECT vrc FROM ViolationResponseConfig vrc WHERE " +
           "(vrc.tenantId = :tenantId OR vrc.isGlobal = true) AND " +
           "vrc.isEnabled = true AND " +
           "vrc.autoExecute = true AND " +
           "vrc.isDeleted = false " +
           "ORDER BY vrc.priority DESC")
    List<ViolationResponseConfig> findAutoExecutableConfigs(@Param("tenantId") Long tenantId);

    /**
     * 대응 액션별 설정 조회
     */
    List<ViolationResponseConfig> findByResponseAction(ViolationResponseConfig.ResponseAction responseAction);

    /**
     * 설정 이름으로 조회
     */
    Optional<ViolationResponseConfig> findByConfigName(String configName);
}

