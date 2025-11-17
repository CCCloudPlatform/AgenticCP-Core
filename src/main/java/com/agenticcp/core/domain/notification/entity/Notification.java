package com.agenticcp.core.domain.notification.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 엔티티
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_tenant", columnList = "tenant_id"),
    @Index(name = "idx_notifications_user", columnList = "user_id"),
    @Index(name = "idx_notifications_notification_id", columnList = "notification_id"),
    @Index(name = "idx_notifications_status", columnList = "status"),
    @Index(name = "idx_notifications_type", columnList = "notification_type"),
    @Index(name = "idx_notifications_tenant_status", columnList = "tenant_id,status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Notification extends BaseEntity {

    /** 테넌트 ID */
    @NotBlank(message = "테넌트 ID는 필수입니다")
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** 사용자 ID */
    @NotNull(message = "사용자 ID는 필수입니다")
    @Positive(message = "사용자 ID는 양수여야 합니다")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 템플릿 ID */
    @Column(name = "template_id")
    private Long templateId;

    /** 채널 ID */
    @NotNull(message = "채널 ID는 필수입니다")
    @Positive(message = "채널 ID는 양수여야 합니다")
    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    /** 알림 ID (고유 식별자) */
    @NotBlank(message = "알림 ID는 필수입니다")
    @Size(max = 100, message = "알림 ID는 100자를 초과할 수 없습니다")
    @Column(name = "notification_id", unique = true, nullable = false)
    private String notificationId;

    /** 알림 제목 */
    @NotBlank(message = "알림 제목은 필수입니다")
    @Size(max = 200, message = "알림 제목은 200자를 초과할 수 없습니다")
    @Column(name = "title", nullable = false)
    private String title;

    /** 알림 내용 */
    @NotBlank(message = "알림 내용은 필수입니다")
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 알림 타입 */
    @NotNull(message = "알림 타입은 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    /** 알림 우선순위 */
    @NotNull(message = "알림 우선순위는 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private NotificationPriority priority;

    /** 알림 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    /** 알림 데이터 (JSON) */
    @Column(name = "data", columnDefinition = "JSON")
    private String data;

    /** 알림 메타데이터 (JSON) */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /** 예약 발송 시간 */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    /** 발송 시간 */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** 읽은 시간 */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /** 에러 메시지 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 재시도 횟수 */
    @Min(value = 0, message = "재시도 횟수는 0 이상이어야 합니다")
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
}
