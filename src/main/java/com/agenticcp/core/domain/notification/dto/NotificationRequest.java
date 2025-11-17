package com.agenticcp.core.domain.notification.dto;

import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    /** 알림 ID */
    private String notificationId;
    
    /** 테넌트 ID */
    @NotBlank(message = "테넌트 ID는 필수입니다")
    private String tenantId;
    
    /** 사용자 ID */
    @Positive(message = "사용자 ID는 양수여야 합니다")
    private Long userId;
    
    /** 알림 제목 */
    @NotBlank(message = "알림 제목은 필수입니다")
    @Size(max = 255, message = "알림 제목은 255자를 초과할 수 없습니다")
    private String title;
    
    /** 알림 내용 */
    @NotBlank(message = "알림 내용은 필수입니다")
    @Size(max = 5000, message = "알림 내용은 5000자를 초과할 수 없습니다")
    private String content;
    
    /** 알림 타입 */
    @NotNull(message = "알림 타입은 필수입니다")
    private NotificationType type;
    
    /** 알림 우선순위 */
    @NotNull(message = "알림 우선순위는 필수입니다")
    private NotificationPriority priority;
    
    /** 수신자 */
    @Size(max = 255, message = "수신자는 255자를 초과할 수 없습니다")
    private String recipient;
    
    /** 알림 데이터 (키-값 쌍) */
    private Map<String, Object> data;
    
    /** 알림 메타데이터 */
    private Map<String, Object> metadata;
    
    /** 예약 발송 시간 */
    private LocalDateTime scheduledAt;
    
    /** 템플릿 ID */
    @Size(max = 100, message = "템플릿 ID는 100자를 초과할 수 없습니다")
    private String templateId;
    
    /** 채널 ID */
    @Size(max = 100, message = "채널 ID는 100자를 초과할 수 없습니다")
    private String channelId;
    
    /** 재시도 횟수 */
    @Min(value = 0, message = "재시도 횟수는 0 이상이어야 합니다")
    private Integer retryCount;
}
