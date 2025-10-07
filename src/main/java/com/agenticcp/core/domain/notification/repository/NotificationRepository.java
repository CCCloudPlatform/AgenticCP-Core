package com.agenticcp.core.domain.notification.repository;

import com.agenticcp.core.domain.notification.entity.Notification;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.enums.NotificationType;
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
 * 알림 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 알림 ID로 조회
     */
    Optional<Notification> findByNotificationId(String notificationId);

    /**
     * 테넌트별 알림 조회
     */
    Page<Notification> findByTenantIdAndIsDeletedFalse(Long tenantId, Pageable pageable);

    /**
     * 사용자별 알림 조회
     */
    Page<Notification> findByTenantIdAndUserIdAndIsDeletedFalse(Long tenantId, Long userId, Pageable pageable);

    /**
     * 상태별 알림 조회
     */
    List<Notification> findByStatusAndIsDeletedFalse(NotificationStatus status);

    /**
     * 예약된 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.scheduledAt <= :now AND n.status = :status AND n.isDeleted = false")
    List<Notification> findScheduledNotifications(@Param("now") LocalDateTime now, @Param("status") NotificationStatus status);

    /**
     * 재시도가 필요한 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < :maxRetry AND n.isDeleted = false")
    List<Notification> findRetryableNotifications(@Param("status") NotificationStatus status, @Param("maxRetry") Integer maxRetry);

    /**
     * 알림 타입별 통계
     */
    @Query("SELECT n.notificationType, COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.isDeleted = false GROUP BY n.notificationType")
    List<Object[]> getNotificationStatsByType(@Param("tenantId") Long tenantId);

    /**
     * 기간별 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.createdAt BETWEEN :startDate AND :endDate AND n.isDeleted = false")
    List<Notification> findByTenantIdAndCreatedAtBetween(@Param("tenantId") Long tenantId, 
                                                        @Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate);

}
