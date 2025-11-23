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

/**
 * 요청 단위로 MDC 컨텍스트를 구성/정리하는 서블릿 필터입니다.
 * 요청 ID/테넌트/클라이언트 정보 등을 설정합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class MdcLoggingFilter implements Filter {

    private final MdcContextProvider mdcContextProvider;
    private final MdcProperties mdcProperties;

    /**
     * HTTP 요청별로 MDC 컨텍스트를 설정하고 반드시 정리합니다.
     * HTTP 외 요청은 그대로 체인에 위임합니다.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            applyRequestScopedContext(httpRequest);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private void applyRequestScopedContext(HttpServletRequest httpRequest) {
        if (isKeyEnabled(MdcKeys.REQUEST_ID)) {
            MDC.put(MdcKeys.REQUEST_ID, generateRequestId());
        }

        if (isKeyEnabled(MdcKeys.TENANT_ID)) {
            String tenantId = TenantContextHolder.getCurrentTenantKey();
            MDC.put(MdcKeys.TENANT_ID, tenantId != null ? tenantId : "system");
        }

        mdcContextProvider.setContext(httpRequest);
    }

    /**
     * 설정에 따라 시퀀스/타임스탬프/UUID 기반 요청 ID를 생성합니다.
     *
     * @return MDC에 저장할 요청 ID
     */
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
