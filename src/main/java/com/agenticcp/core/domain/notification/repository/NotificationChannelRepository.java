package com.agenticcp.core.domain.notification.repository;

import com.agenticcp.core.domain.notification.entity.NotificationChannelEntity;
import com.agenticcp.core.domain.notification.enums.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 채널 Repository
 */
@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannelEntity, Long> {

    /**
     * 테넌트별 활성 채널 조회
     */
    List<NotificationChannelEntity> findByTenantIdAndIsActiveTrueAndIsDeletedFalse(String tenantId);

    /**
     * 채널명으로 조회
     */
    Optional<NotificationChannelEntity> findByTenantIdAndChannelNameAndIsDeletedFalse(String tenantId, String channelName);

    /**
     * 타입별 채널 조회
     */
    List<NotificationChannelEntity> findByTenantIdAndChannelTypeAndIsActiveTrueAndIsDeletedFalse(String tenantId, ChannelType channelType);

    /**
     * 활성화된 채널 조회
     */
    @Query("SELECT nc FROM NotificationChannelEntity nc WHERE nc.tenantId = :tenantId AND nc.isActive = true AND nc.isDeleted = false")
    List<NotificationChannelEntity> findActiveChannelsByTenant(@Param("tenantId") String tenantId);
}
