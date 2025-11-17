package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;

/**
 * 알림 채널 인터페이스
 * 
 * <p>확장 가능한 알림 시스템을 위한 플러그인 아키텍처의 핵심 인터페이스입니다.</p>
 * 
 * <p>이 인터페이스를 구현하여 다양한 알림 채널(Slack, Email, Discord 등)을 추가할 수 있습니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public interface NotificationChannel {
    
    /**
     * 채널 타입 반환
     * @return 채널 타입
     */
    String getChannelType();
    
    /**
     * 채널이 활성화되어 있는지 확인
     * @return 활성화 여부
     */
    boolean isEnabled();
    
    /**
     * 알림 발송
     * @param request 알림 요청
     * @return 알림 응답
     */
    NotificationResponse send(NotificationRequest request);
    
    /**
     * 채널 연결 테스트
     * @return 테스트 결과
     */
    boolean testConnection();
    
    /**
     * 채널 설정 검증
     * @return 검증 결과
     */
    boolean validateConfiguration();
}
