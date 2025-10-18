package com.agenticcp.core.domain.notification.config;

import com.agenticcp.core.domain.notification.enums.ChannelType;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 알림 채널 설정
 * 
 * <p>알림 타입별로 사용할 채널을 설정 파일에서 관리합니다.</p>
 * <p>디스코드, 텔레그램 등 새로운 채널 추가 시 코드 수정 없이 설정만 변경하면 됩니다.</p>
 * 
 * <h3>설정 예시 (application.yml):</h3>
 * <pre>
 * notification:
 *   channel:
 *     default: SLACK
 *     mappings:
 *       ALERT: SLACK
 *       SYSTEM: DISCORD
 *       SECURITY: EMAIL
 * </pre>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "notification.channel")
@Data
public class NotificationChannelConfig {

    /**
     * 기본 채널 타입 (매핑이 없을 때 사용)
     */
    private ChannelType defaultChannel = ChannelType.SLACK;

    /**
     * 기본 관리자 사용자 ID
     * 
     * <p>알림을 받을 기본 관리자의 사용자 ID입니다.</p>
     * <p>MVP: 단일 관리자 ID 사용</p>
     * <p>확장: 나중에 테넌트별 관리자로 변경 가능</p>
     */
    private Long defaultAdminUserId = 1L;

    /**
     * 알림 타입별 채널 매핑
     * 
     * <p>알림 타입(ALERT, SYSTEM 등)에 따라 다른 채널 사용 가능</p>
     */
    private Map<NotificationType, ChannelType> mappings = new HashMap<>();

    /**
     * 우선순위별 채널 매핑 (선택사항)
     * 
     * <p>URGENT는 전화, HIGH는 슬랙, MEDIUM은 이메일 등</p>
     */
    private Map<String, ChannelType> priorityMappings = new HashMap<>();

    /**
     * 폴백 채널 순서
     * 
     * <p>첫 번째 채널 실패 시 다음 채널로 자동 전환</p>
     * <p>예: [SLACK, DISCORD, EMAIL]</p>
     */
    private java.util.List<ChannelType> fallbackOrder = java.util.List.of(
        ChannelType.SLACK, 
        ChannelType.EMAIL
    );

    /**
     * 알림 타입에 맞는 채널 타입 반환
     * 
     * @param notificationType 알림 타입
     * @return 채널 타입 (매핑 없으면 기본 채널)
     */
    public ChannelType getChannelTypeForNotification(NotificationType notificationType) {
        return mappings.getOrDefault(notificationType, defaultChannel);
    }

    /**
     * 우선순위에 맞는 채널 타입 반환
     * 
     * @param priority 우선순위 (URGENT, HIGH 등)
     * @return 채널 타입 (매핑 없으면 null)
     */
    public ChannelType getChannelTypeForPriority(String priority) {
        return priorityMappings.get(priority);
    }
}

