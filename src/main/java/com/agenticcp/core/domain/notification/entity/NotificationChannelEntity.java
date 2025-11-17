package com.agenticcp.core.domain.notification.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.notification.enums.ChannelType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 채널 엔티티
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "notification_channels", indexes = {
    @Index(name = "idx_notification_channels_tenant", columnList = "tenant_id"),
    @Index(name = "idx_notification_channels_name", columnList = "channel_name"),
    @Index(name = "idx_notification_channels_type", columnList = "channel_type"),
    @Index(name = "idx_notification_channels_status", columnList = "status"),
    @Index(name = "idx_notification_channels_active", columnList = "is_active"),
    @Index(name = "idx_notification_channels_tenant_active", columnList = "tenant_id,is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotificationChannelEntity extends BaseEntity {

    /** 테넌트 ID */
    @NotBlank(message = "테넌트 ID는 필수입니다")
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** 채널 이름 */
    @NotBlank(message = "채널 이름은 필수입니다")
    @Size(max = 100, message = "채널 이름은 100자를 초과할 수 없습니다")
    @Column(name = "channel_name", nullable = false)
    private String channelName;

    /** 채널 설명 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 채널 타입 (SLACK, EMAIL, DISCORD 등) */
    @NotNull(message = "채널 타입은 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    /** 채널 설정 (JSON) */
    @Column(name = "configuration", columnDefinition = "JSON")
    private String configuration;

    /** 채널 인증 정보 (JSON) */
    @Column(name = "credentials", columnDefinition = "JSON")
    private String credentials;

    /** 활성화 여부 */
    @NotNull(message = "활성화 여부는 필수입니다")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** 채널 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    /** 메타데이터 (JSON) */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /** 마지막 사용 시간 */
    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    /** 성공 횟수 */
    @Min(value = 0, message = "성공 횟수는 0 이상이어야 합니다")
    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;

    /** 실패 횟수 */
    @Min(value = 0, message = "실패 횟수는 0 이상이어야 합니다")
    @Column(name = "failure_count")
    @Builder.Default
    private Integer failureCount = 0;
}
