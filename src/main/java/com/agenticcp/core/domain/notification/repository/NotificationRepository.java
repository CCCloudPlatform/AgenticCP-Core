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
    Page<Notification> findByTenantIdAndIsDeletedFalse(String tenantId, Pageable pageable);

    /**
     * 사용자별 알림 조회
     */
    Page<Notification> findByTenantIdAndUserIdAndIsDeletedFalse(String tenantId, Long userId, Pageable pageable);

    /**
     * 상태별 알림 조회
     */
    List<Notification> findByStatusAndIsDeletedFalse(NotificationStatus status);

}
