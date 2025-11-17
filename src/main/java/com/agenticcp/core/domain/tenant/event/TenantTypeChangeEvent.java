package com.agenticcp.core.domain.tenant.event;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 테넌트 타입 변경 이벤트
 */
@Getter
public class TenantTypeChangeEvent extends ApplicationEvent {

    private final String tenantKey;
    private final Tenant.TenantType oldType;
    private final Tenant.TenantType newType;

    public TenantTypeChangeEvent(Object source, String tenantKey, Tenant.TenantType oldType, Tenant.TenantType newType) {
        super(source);
        this.tenantKey = tenantKey;
        this.oldType = oldType;
        this.newType = newType;
    }
}



