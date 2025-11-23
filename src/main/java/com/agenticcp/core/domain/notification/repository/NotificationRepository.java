package com.agenticcp.core.domain.notification.repository;

import com.agenticcp.core.domain.notification.entity.Notification;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 Repository
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 알림 ID로 조회
     * 
     * @param notificationId 알림 ID
     * @return 알림 정보 (Optional)
     */
    Optional<Notification> findByNotificationId(String notificationId);

    /**
     * 테넌트별 알림 조회
     * 
     * @param tenantId 테넌트 ID
     * @param pageable 페이징 정보
     * @return 알림 목록 (페이징)
     */
    Page<Notification> findByTenantIdAndIsDeletedFalse(String tenantId, Pageable pageable);

    /**
     * 사용자별 알림 조회
     * 
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 알림 목록 (페이징)
     */
    Page<Notification> findByTenantIdAndUserIdAndIsDeletedFalse(String tenantId, Long userId, Pageable pageable);

    /**
     * 상태별 알림 조회
     * 
     * @param status 알림 상태
     * @return 알림 목록
     */
    List<Notification> findByStatusAndIsDeletedFalse(NotificationStatus status);

}
