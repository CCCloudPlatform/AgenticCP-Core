package com.agenticcp.core.common.config;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.springframework.core.task.TaskDecorator;

public class TenantContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. 현재 스레드의 tenant context 캡처
        Tenant currentTenant = TenantContextHolder.getCurrentTenant();
        String currentTenantKey = TenantContextHolder.getCurrentTenantKey();

        return () -> {
            try {
                // 2. 새로운 스레드에서 tenant context 복원
                if (currentTenant != null) {
                    TenantContextHolder.setTenant(currentTenant);
                }
                else if (currentTenantKey != null) {
                    TenantContextHolder.setTenantKey(currentTenantKey);
                }
                // 3. 실제 작업 실행
                runnable.run();
            } finally {
                // 4. 작업 완료 후 컨텍스트 정리
                TenantContextHolder.clear();
            }
        };

    }
}
