package com.agenticcp.core.domain.platform.repository;

import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagApproval;
import com.agenticcp.core.domain.platform.enums.ApprovalStatus;
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
 * 기능 플래그 승인 Repository
 * 
 * 기능 플래그 승인 워크플로우 관련 데이터 접근을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Repository
public interface FeatureFlagApprovalRepository extends JpaRepository<FeatureFlagApproval, Long> {

    /**
     * 기능 플래그별 승인 목록 조회
     * 
     * @param featureFlag 기능 플래그
     * @return 승인 목록
     */
    List<FeatureFlagApproval> findByFeatureFlag(FeatureFlag featureFlag);

    /**
     * 기능 플래그별 승인 목록 조회 (페이징)
     * 
     * @param featureFlag 기능 플래그
     * @param pageable 페이징 정보
     * @return 승인 목록 (페이징)
     */
    Page<FeatureFlagApproval> findByFeatureFlag(FeatureFlag featureFlag, Pageable pageable);

    /**
     * 기능 플래그 ID로 승인 목록 조회
     * 
     * @param featureFlagId 기능 플래그 ID
     * @return 승인 목록
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa WHERE fa.featureFlag.id = :featureFlagId AND fa.isDeleted = false")
    List<FeatureFlagApproval> findByFeatureFlagId(@Param("featureFlagId") Long featureFlagId);

    /**
     * 상태별 승인 목록 조회
     * 
     * @param status 승인 상태
     * @return 승인 목록
     */
    List<FeatureFlagApproval> findByStatus(ApprovalStatus status);

    /**
     * 상태별 승인 목록 조회 (페이징)
     * 
     * @param status 승인 상태
     * @param pageable 페이징 정보
     * @return 승인 목록 (페이징)
     */
    Page<FeatureFlagApproval> findByStatus(ApprovalStatus status, Pageable pageable);

    /**
     * 요청자별 승인 목록 조회
     * 
     * @param requestedBy 요청자 ID
     * @return 승인 목록
     */
    List<FeatureFlagApproval> findByRequestedBy(String requestedBy);

    /**
     * 요청자별 승인 목록 조회 (페이징)
     * 
     * @param requestedBy 요청자 ID
     * @param pageable 페이징 정보
     * @return 승인 목록 (페이징)
     */
    Page<FeatureFlagApproval> findByRequestedBy(String requestedBy, Pageable pageable);

    /**
     * 기능 플래그와 상태로 승인 조회
     * 
     * @param featureFlag 기능 플래그
     * @param status 승인 상태
     * @return 승인 목록
     */
    List<FeatureFlagApproval> findByFeatureFlagAndStatus(FeatureFlag featureFlag, ApprovalStatus status);

    /**
     * 기능 플래그의 대기 중인 승인 조회
     * 
     * @param featureFlag 기능 플래그
     * @return 대기 중인 승인 (Optional)
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa " +
           "WHERE fa.featureFlag = :featureFlag " +
           "AND fa.status = :status " +
           "AND fa.isDeleted = false " +
           "ORDER BY fa.requestedAt DESC")
    Optional<FeatureFlagApproval> findPendingByFeatureFlag(
        @Param("featureFlag") FeatureFlag featureFlag,
        @Param("status") ApprovalStatus status
    );

    /**
     * 기능 플래그의 대기 중인 승인 존재 여부 확인
     * 
     * @param featureFlag 기능 플래그
     * @return 대기 중인 승인 존재 여부
     */
    @Query("SELECT COUNT(fa) > 0 FROM FeatureFlagApproval fa " +
           "WHERE fa.featureFlag = :featureFlag " +
           "AND fa.status = :status " +
           "AND fa.isDeleted = false")
    boolean existsPendingByFeatureFlag(
        @Param("featureFlag") FeatureFlag featureFlag,
        @Param("status") ApprovalStatus status
    );

    /**
     * 날짜 범위로 승인 목록 조회
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 승인 목록
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa " +
           "WHERE fa.requestedAt >= :startDate " +
           "AND fa.requestedAt <= :endDate " +
           "AND fa.isDeleted = false " +
           "ORDER BY fa.requestedAt DESC")
    List<FeatureFlagApproval> findByRequestedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 날짜 범위로 승인 목록 조회 (페이징)
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @param pageable 페이징 정보
     * @return 승인 목록 (페이징)
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa " +
           "WHERE fa.requestedAt >= :startDate " +
           "AND fa.requestedAt <= :endDate " +
           "AND fa.isDeleted = false " +
           "ORDER BY fa.requestedAt DESC")
    Page<FeatureFlagApproval> findByRequestedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * 대기 중인 승인 목록 조회
     * 
     * @return 대기 중인 승인 목록
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa " +
           "WHERE fa.status = :status " +
           "AND fa.isDeleted = false " +
           "ORDER BY fa.requestedAt ASC")
    List<FeatureFlagApproval> findPendingApprovals(@Param("status") ApprovalStatus status);

    /**
     * 대기 중인 승인 목록 조회 (페이징)
     * 
     * @param status 승인 상태
     * @param pageable 페이징 정보
     * @return 대기 중인 승인 목록 (페이징)
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa " +
           "WHERE fa.status = :status " +
           "AND fa.isDeleted = false " +
           "ORDER BY fa.requestedAt ASC")
    Page<FeatureFlagApproval> findPendingApprovals(
        @Param("status") ApprovalStatus status,
        Pageable pageable
    );

    /**
     * 승인 완료된 승인 목록 조회
     * 
     * @param status 승인 상태
     * @return 승인 완료된 승인 목록
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa " +
           "WHERE fa.status = :status " +
           "AND fa.isDeleted = false " +
           "ORDER BY fa.approvedAt DESC")
    List<FeatureFlagApproval> findApprovedApprovals(@Param("status") ApprovalStatus status);

    /**
     * 거부된 승인 목록 조회
     * 
     * @param status 승인 상태
     * @return 거부된 승인 목록
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa " +
           "WHERE fa.status = :status " +
           "AND fa.isDeleted = false " +
           "ORDER BY fa.rejectedAt DESC")
    List<FeatureFlagApproval> findRejectedApprovals(@Param("status") ApprovalStatus status);

    /**
     * 승인자별 승인 목록 조회
     * 
     * @param approvedBy 승인자 ID
     * @return 승인 목록
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa " +
           "WHERE fa.approvedBy = :approvedBy " +
           "AND fa.isDeleted = false " +
           "ORDER BY fa.approvedAt DESC")
    List<FeatureFlagApproval> findByApprovedBy(@Param("approvedBy") String approvedBy);

    /**
     * 거부자별 승인 목록 조회
     * 
     * @param rejectedBy 거부자 ID
     * @return 승인 목록
     */
    @Query("SELECT fa FROM FeatureFlagApproval fa " +
           "WHERE fa.rejectedBy = :rejectedBy " +
           "AND fa.isDeleted = false " +
           "ORDER BY fa.rejectedAt DESC")
    List<FeatureFlagApproval> findByRejectedBy(@Param("rejectedBy") String rejectedBy);
}

