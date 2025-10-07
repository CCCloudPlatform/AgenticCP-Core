package com.agenticcp.core.domain.notification.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.notification.enums.ChannelType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 채널 엔티티
 */
@Entity
@Table(name = "notification_channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationChannelEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "channel_name", nullable = false)
    private String channelName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    @Column(name = "configuration", columnDefinition = "JSON")
    private String configuration; // JSON 형태로 저장

    @Column(name = "credentials", columnDefinition = "JSON")
    private String credentials; // JSON 형태로 저장

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.agenticcp.core.common.enums.Status status = com.agenticcp.core.common.enums.Status.ACTIVE;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata; // JSON 형태로 저장

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "failure_count")
    private Integer failureCount = 0;
}
