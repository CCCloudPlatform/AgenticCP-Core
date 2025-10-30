package com.agenticcp.core.domain.user.repository;

import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.entity.UserAuthHistory;
import com.agenticcp.core.domain.user.enums.AuthType;
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
 * 사용자 인증 이력 Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Repository
public interface UserAuthHistoryRepository extends JpaRepository<UserAuthHistory, Long> {

    /**
     * 특정 사용자의 인증 이력 조회
     */
    Page<UserAuthHistory> findByUserOrderByAuthTimestampDesc(User user, Pageable pageable);

    /**
     * 특정 사용자의 특정 타입 인증 이력 조회
     */
    List<UserAuthHistory> findByUserAndAuthTypeOrderByAuthTimestampDesc(User user, AuthType authType);

    /**
     * 특정 사용자의 성공한 인증 이력 조회
     */
    List<UserAuthHistory> findByUserAndIsSuccessTrueOrderByAuthTimestampDesc(User user);

    /**
     * 특정 사용자의 실패한 인증 이력 조회
     */
    List<UserAuthHistory> findByUserAndIsSuccessFalseOrderByAuthTimestampDesc(User user);

    /**
     * 특정 사용자의 PENDING 인증 이력 조회
     */
    List<UserAuthHistory> findByUserAndIsPendingTrueAndExpiresAtAfterOrderByAuthTimestampDesc(
            User user, LocalDateTime now);

    /**
     * 특정 사용자의 최근 로그인 이력 조회
     */
    Optional<UserAuthHistory> findFirstByUserAndAuthTypeAndIsSuccessTrueOrderByAuthTimestampDesc(
            User user, AuthType authType);

    /**
     * 특정 기간 내 로그인 실패 횟수 조회
     */
    @Query("SELECT COUNT(h) FROM UserAuthHistory h " +
           "WHERE h.user = :user " +
           "AND h.authType = :authType " +
           "AND h.isSuccess = false " +
           "AND h.authTimestamp >= :since")
    Long countFailedAttemptsSince(
            @Param("user") User user,
            @Param("authType") AuthType authType,
            @Param("since") LocalDateTime since);

    /**
     * 특정 IP에서의 로그인 실패 횟수 조회
     */
    @Query("SELECT COUNT(h) FROM UserAuthHistory h " +
           "WHERE h.ipAddress = :ipAddress " +
           "AND h.authType = :authType " +
           "AND h.isSuccess = false " +
           "AND h.authTimestamp >= :since")
    Long countFailedAttemptsByIpSince(
            @Param("ipAddress") String ipAddress,
            @Param("authType") AuthType authType,
            @Param("since") LocalDateTime since);

    /**
     * 특정 사용자의 최근 N개 인증 이력 조회
     */
    List<UserAuthHistory> findTop10ByUserOrderByAuthTimestampDesc(User user);

    /**
     * 특정 기간의 인증 이력 조회
     */
    @Query("SELECT h FROM UserAuthHistory h " +
           "WHERE h.user = :user " +
           "AND h.authTimestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY h.authTimestamp DESC")
    List<UserAuthHistory> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 만료된 PENDING 인증 이력 조회
     */
    @Query("SELECT h FROM UserAuthHistory h " +
           "WHERE h.isPending = true " +
           "AND h.expiresAt < :now")
    List<UserAuthHistory> findExpiredPendingAuth(@Param("now") LocalDateTime now);

    /**
     * 특정 사용자의 2FA 관련 이력 조회
     */
    @Query("SELECT h FROM UserAuthHistory h " +
           "WHERE h.user = :user " +
           "AND h.authType IN ('TWO_FACTOR_SETUP', 'TWO_FACTOR_ENABLE', 'TWO_FACTOR_VERIFY', 'TWO_FACTOR_DISABLE') " +
           "ORDER BY h.authTimestamp DESC")
    List<UserAuthHistory> findTwoFactorHistoryByUser(@Param("user") User user);
}

