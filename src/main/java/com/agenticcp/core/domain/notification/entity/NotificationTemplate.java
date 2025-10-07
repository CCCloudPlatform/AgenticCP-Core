package com.agenticcp.core.domain.notification.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 알림 템플릿 엔티티
 */
@Entity
@Table(name = "notification_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false)
    private NotificationType templateType;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "variables", columnDefinition = "JSON")
    private String variables; // JSON 형태로 저장

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata; // JSON 형태로 저장

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.agenticcp.core.common.enums.Status status = com.agenticcp.core.common.enums.Status.ACTIVE;
}
