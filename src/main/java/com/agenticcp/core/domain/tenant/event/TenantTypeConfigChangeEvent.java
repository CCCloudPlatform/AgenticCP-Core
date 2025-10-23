package com.agenticcp.core.domain.tenant.event;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 테넌트 타입별 설정 변경 이벤트
 */
@Getter
public class TenantTypeConfigChangeEvent extends ApplicationEvent {

    private final String tenantType;
    private final String configKey;
    private final String action; // CREATE, UPDATE, DELETE

    public TenantTypeConfigChangeEvent(Object source, String tenantType, String configKey, String action) {
        super(source);
        this.tenantType = tenantType;
        this.configKey = configKey;
        this.action = action;
    }

    public TenantTypeConfigChangeEvent(Object source, Tenant.TenantType tenantType, String configKey, String action) {
        super(source);
        this.tenantType = tenantType.name();
        this.configKey = configKey;
        this.action = action;
    }
}



