package com.agenticcp.core.domain.monitoring.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 헬스 상태 변화 이벤트
 * 
 * <p>시스템 컴포넌트의 상태가 변경되었을 때 발행됩니다.</p>
 * <p>Issue #15 가이드: 이벤트 기반 알림 시스템</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class HealthStatusChangedEvent extends ApplicationEvent {
    
    /**
     * 서비스 이름 (database, redis 등)
     */
    private final String serviceName;
    
    /**
     * 이전 상태 (HEALTHY, WARNING, CRITICAL)
     */
    private final String previousStatus;
    
    /**
     * 현재 상태 (HEALTHY, WARNING, CRITICAL)
     */
    private final String newStatus;
    
    /**
     * 테넌트 ID (시스템 관리자용)
     */
    private final String tenantId;
    
    /**
     * 이벤트 생성자
     * 
     * @param source 이벤트를 발행한 객체
     * @param serviceName 서비스 이름
     * @param previousStatus 이전 상태
     * @param newStatus 현재 상태
     * @param tenantId 테넌트 ID
     */
    public HealthStatusChangedEvent(Object source, String serviceName, 
                                   String previousStatus, String newStatus, String tenantId) {
        super(source);
        this.serviceName = serviceName;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.tenantId = tenantId;
    }
}

