package com.agenticcp.core.domain.tenant.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 플랫폼 설정 변경 이벤트
 */
@Getter
public class PlatformConfigChangeEvent extends ApplicationEvent {

    private final String configKey;
    private final String action; // CREATE, UPDATE, DELETE

    public PlatformConfigChangeEvent(Object source, String configKey, String action) {
        super(source);
        this.configKey = configKey;
        this.action = action;
    }
}



