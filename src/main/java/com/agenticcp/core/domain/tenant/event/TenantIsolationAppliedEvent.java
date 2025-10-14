package com.agenticcp.core.domain.tenant.event;

import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TenantIsolationAppliedEvent extends ApplicationEvent {
    
    private final String tenantKey;
    private final TenantIsolation isolationLevel;

    public TenantIsolationAppliedEvent(Object source, String tenantKey, TenantIsolation isolationLevel) {
        super(source);
        this.tenantKey = tenantKey;
        this.isolationLevel = isolationLevel;
    }
}