package com.agenticcp.core.domain.tenant.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 테넌트 설정 변경 이벤트
 */
@Getter
public class TenantConfigChangeEvent extends ApplicationEvent {

    private final String tenantKey;
    private final String configKey;
    private final String action; // CREATE, UPDATE, DELETE

    public TenantConfigChangeEvent(Object source, String tenantKey, String configKey, String action) {
        super(source);
        this.tenantKey = tenantKey;
        this.configKey = configKey;
        this.action = action;
    }
}



