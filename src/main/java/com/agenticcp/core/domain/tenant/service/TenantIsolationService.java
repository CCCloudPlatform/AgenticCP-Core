package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import com.agenticcp.core.domain.tenant.event.TenantIsolationAppliedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantIsolationService {

    private final TenantService tenantService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void applyIsolationPolicy(String tenantKey, TenantIsolation isolation) {
        Tenant tenant = tenantService.getTenantByKey(tenantKey)
                .orElseThrow(() -> new IllegalStateException("Invalid tenant key: " + tenantKey));
        switch (isolation.getIsolationLevel()) {
            case SHARED ->

        }
        eventPublisher.publishEvent(new TenantIsolationAppliedEvent(this, tenantKey, isolation));

    }
}
