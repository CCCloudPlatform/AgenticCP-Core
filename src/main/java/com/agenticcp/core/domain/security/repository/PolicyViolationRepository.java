package com.agenticcp.core.domain.security.repository;

import com.agenticcp.core.domain.security.entity.PolicyViolation;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 정책 위반 Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface PolicyViolationRepository extends JpaRepository<PolicyViolation, Long> {

    /**
     * 테넌트별 위반 조회
     */
    List<PolicyViolation> findByTenantId(Long tenantId);

    /**
     * 테넌트별 위반 조회 (페이징)
     */
    Page<PolicyViolation> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * 정책별 위반 조회
     */
    List<PolicyViolation> findByPolicyId(Long policyId);

    /**
     * 사용자별 위반 조회
     */
    List<PolicyViolation> findByUserId(Long userId);

    /**
     * IP 주소별 위반 조회
     */
    List<PolicyViolation> findByIpAddress(String ipAddress);

    /**
     * 위반 타입별 조회
     */
    List<PolicyViolation> findByViolationType(PolicyViolation.ViolationType violationType);

    /**
     * 심각도별 조회
     */
    List<PolicyViolation> findBySeverity(SecurityPolicy.Severity severity);

    /**
     * 상태별 조회
     */
    List<PolicyViolation> findByStatus(PolicyViolation.ViolationStatus status);

    /**
     * 테넌트 + 상태별 조회
     */
    @Query("SELECT pv FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.status = :status ORDER BY pv.detectedAt DESC")
    List<PolicyViolation> findByTenantIdAndStatus(@Param("tenantId") Long tenantId, 
                                                   @Param("status") PolicyViolation.ViolationStatus status);

    /**
     * 기간별 위반 조회
     */
    @Query("SELECT pv FROM PolicyViolation pv WHERE pv.detectedAt BETWEEN :startTime AND :endTime")
    List<PolicyViolation> findByDetectedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 테넌트 + 기간별 위반 조회
     */
    @Query("SELECT pv FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.detectedAt BETWEEN :startTime AND :endTime ORDER BY pv.detectedAt DESC")
    List<PolicyViolation> findByTenantIdAndDetectedAtBetween(@Param("tenantId") Long tenantId,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 미해결 위반 조회
     */
    @Query("SELECT pv FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.status IN ('DETECTED', 'PROCESSING') ORDER BY pv.severity DESC, pv.detectedAt DESC")
    List<PolicyViolation> findUnresolvedViolations(@Param("tenantId") Long tenantId);

    /**
     * 심각도별 미해결 위반 건수
     */
    @Query("SELECT COUNT(pv) FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.severity = :severity AND pv.status IN ('DETECTED', 'PROCESSING')")
    Long countUnresolvedBySeverity(@Param("tenantId") Long tenantId, 
                                    @Param("severity") SecurityPolicy.Severity severity);

    /**
     * 위반 타입별 건수
     */
    @Query("SELECT pv.violationType, COUNT(pv) FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.detectedAt BETWEEN :startTime AND :endTime GROUP BY pv.violationType")
    List<Object[]> countByViolationType(@Param("tenantId") Long tenantId,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 심각도별 건수
     */
    @Query("SELECT pv.severity, COUNT(pv) FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.detectedAt BETWEEN :startTime AND :endTime GROUP BY pv.severity")
    List<Object[]> countBySeverity(@Param("tenantId") Long tenantId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 정책별 위반 건수 (Top N)
     */
    @Query("SELECT pv.policyName, COUNT(pv) as cnt FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.detectedAt BETWEEN :startTime AND :endTime GROUP BY pv.policyName ORDER BY cnt DESC")
    List<Object[]> countByPolicy(@Param("tenantId") Long tenantId,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 사용자별 위반 건수 (Top N)
     */
    @Query("SELECT pv.username, COUNT(pv) as cnt FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.detectedAt BETWEEN :startTime AND :endTime AND pv.username IS NOT NULL GROUP BY pv.username ORDER BY cnt DESC")
    List<Object[]> countByUser(@Param("tenantId") Long tenantId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * IP별 위반 건수 (Top N)
     */
    @Query("SELECT pv.ipAddress, COUNT(pv) as cnt FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.detectedAt BETWEEN :startTime AND :endTime AND pv.ipAddress IS NOT NULL GROUP BY pv.ipAddress ORDER BY cnt DESC")
    List<Object[]> countByIp(@Param("tenantId") Long tenantId,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);

    /**
     * 자동 대응 실행 건수
     */
    @Query("SELECT COUNT(pv) FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.autoResponseExecuted = true AND pv.detectedAt BETWEEN :startTime AND :endTime")
    Long countAutoResponseExecuted(@Param("tenantId") Long tenantId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 오탐지 건수
     */
    @Query("SELECT COUNT(pv) FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.falsePositive = true AND pv.detectedAt BETWEEN :startTime AND :endTime")
    Long countFalsePositives(@Param("tenantId") Long tenantId,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);

    /**
     * 평균 해결 시간 (분)
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, pv.detectedAt, pv.resolvedAt)) FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.resolvedAt IS NOT NULL AND pv.detectedAt BETWEEN :startTime AND :endTime")
    Double calculateAverageResolutionTime(@Param("tenantId") Long tenantId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 시간대별 위반 분포
     */
    @Query("SELECT HOUR(pv.detectedAt), COUNT(pv) FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.detectedAt BETWEEN :startTime AND :endTime GROUP BY HOUR(pv.detectedAt)")
    List<Object[]> countByHour(@Param("tenantId") Long tenantId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 일별 위반 추이
     */
    @Query("SELECT DATE(pv.detectedAt), COUNT(pv) FROM PolicyViolation pv WHERE pv.tenantId = :tenantId AND pv.detectedAt BETWEEN :startTime AND :endTime GROUP BY DATE(pv.detectedAt) ORDER BY DATE(pv.detectedAt)")
    List<Object[]> countByDate(@Param("tenantId") Long tenantId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);
}

