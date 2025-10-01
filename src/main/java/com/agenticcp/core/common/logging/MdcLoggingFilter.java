package com.agenticcp.core.common.logging;

import com.agenticcp.core.common.context.TenantContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class MdcLoggingFilter implements Filter {

    private final MdcContextProvider mdcContextProvider;
    private final MdcProperties mdcProperties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            if (isKeyEnabled(MdcKeys.REQUEST_ID)) {
                MDC.put(MdcKeys.REQUEST_ID, generateRequestId());
            }

            if (isKeyEnabled(MdcKeys.TENANT_ID)) {
                String tenantId = TenantContextHolder.getCurrentTenantKey();
                MDC.put(MdcKeys.TENANT_ID, tenantId != null ? tenantId : "system");
            }

            mdcContextProvider.setContext(httpRequest);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String generateRequestId() {
        String prefix = mdcProperties.getRequestIdPrefix();

        int len = Math.max(1, mdcProperties.getRequestIdLength());
        return switch (mdcProperties.getRequestIdType().toLowerCase()) {
            case "timestamp" -> prefix + System.currentTimeMillis();
            case "sequence" -> prefix + System.nanoTime();
            default -> {
                String raw = UUID.randomUUID().toString().replace("-", "");
                yield prefix + raw.substring(0, Math.min(len, raw.length()));
            }
        };
    }

    private boolean isKeyEnabled(String key) {
        return mdcProperties.getEnabledKeys().contains(key);
    }
}
