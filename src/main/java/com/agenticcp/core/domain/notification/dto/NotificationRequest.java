package com.agenticcp.core.domain.notification.dto;

import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    private String notificationId;
    private String tenantId;
    private Long userId;
    private String title;
    private String content;
    private NotificationType type;
    private NotificationPriority priority;
    private String recipient;
    private Map<String, Object> data;
    private Map<String, Object> metadata;
    private LocalDateTime scheduledAt;
    private String templateId;
    private String channelId;
    private Integer retryCount;
}
