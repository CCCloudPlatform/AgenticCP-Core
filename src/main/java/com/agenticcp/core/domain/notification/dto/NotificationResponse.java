package com.agenticcp.core.domain.notification.dto;

import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    /** 알림 ID */
    private String notificationId;
    
    /** 알림 상태 */
    private NotificationStatus status;
    
    /** 응답 메시지 */
    private String message;
    
    /** 에러 메시지 */
    private String errorMessage;
    
    /** 발송 시간 */
    private LocalDateTime sentAt;
    
    /** 전달 시간 */
    private LocalDateTime deliveredAt;
    
    /** 메타데이터 */
    private Map<String, Object> metadata;
    
    /** 외부 시스템 ID */
    private String externalId;
    
    /** 재시도 횟수 */
    private Integer retryCount;
    
    /** 성공 여부 */
    private boolean success;
}
