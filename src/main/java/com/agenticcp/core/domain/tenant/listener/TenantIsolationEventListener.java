package com.agenticcp.core.domain.tenant.listener;

import com.agenticcp.core.domain.tenant.event.TenantIsolationAppliedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantIsolationEventListener {

    @EventListener
    @Async("tenantTaskExecutor")
    public void handleTenantIsolationAppliedEvent(TenantIsolationAppliedEvent event) {
        // 현재 리스너는 로깅용으로 사용, 추후 알림 발송 등으로 확장
        String tenantKey = event.getTenantKey();
        String isolationLevel = event.getIsolationLevel();

        log.info("tenant isolation applied - tenantKey={}, isolationLevel={}", tenantKey, isolationLevel + "");

    }

}
