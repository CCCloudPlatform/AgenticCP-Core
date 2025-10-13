package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditEventDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuditPublishEvent extends ApplicationEvent {

    private final AuditEventDto auditEventDto;

    public AuditPublishEvent(Object source, AuditEventDto auditEventDto) {
        super(source);
        this.auditEventDto = auditEventDto;
    }
}
