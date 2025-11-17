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
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannelEntity, Long> {

    /**
     * 채널명으로 조회
     * 
     * @param tenantId 테넌트 ID
     * @param channelName 채널명
     * @return 채널 정보 (Optional)
     */
    Optional<NotificationChannelEntity> findByTenantIdAndChannelNameAndIsDeletedFalse(String tenantId, String channelName);

    /**
     * 타입별 채널 조회
     * 
     * @param tenantId 테넌트 ID
     * @param channelType 채널 타입
     * @return 활성 채널 목록
     */
    List<NotificationChannelEntity> findByTenantIdAndChannelTypeAndIsActiveTrueAndIsDeletedFalse(String tenantId, ChannelType channelType);

    /**
     * 활성화된 채널 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 활성 채널 목록
     */
    @Query("SELECT nc FROM NotificationChannelEntity nc WHERE nc.tenantId = :tenantId AND nc.isActive = true AND nc.isDeleted = false")
    List<NotificationChannelEntity> findActiveChannelsByTenant(@Param("tenantId") String tenantId);
}
