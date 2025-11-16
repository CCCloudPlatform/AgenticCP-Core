package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditEventDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 감사 이벤트 발행을 위한 Spring ApplicationEvent입니다.
 * 리스너들이 구독하여 파일/DB 등에 기록합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Getter
public class AuditPublishEvent extends ApplicationEvent {

    private final AuditEventDto auditEventDto;

    public AuditPublishEvent(Object source, AuditEventDto auditEventDto) {
        super(source);
        this.auditEventDto = auditEventDto;
    }
}
