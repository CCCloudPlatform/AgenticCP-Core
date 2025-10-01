package com.agenticcp.core.common.logging;

import com.agenticcp.core.common.util.LogMaskingUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

@RequiredArgsConstructor
public abstract class AbstractMdcContextProvider implements MdcContextProvider {
    
    protected final MdcProperties mdcProperties;

    protected String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(MdcKeys.HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String clientIp = extractFirstIpFromForwardedHeader(xForwardedFor);
            return maskClientIpIfNeeded(clientIp);
        }
        
        String xRealIp = request.getHeader(MdcKeys.HEADER_X_REAL_IP);
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return maskClientIpIfNeeded(xRealIp.trim());
        }
        
        String remoteAddr = request.getRemoteAddr();
        return maskClientIpIfNeeded(remoteAddr);
    }

    protected String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader(MdcKeys.HEADER_USER_AGENT);
        if (userAgent != null && mdcProperties.isMaskUserAgent()) {
            return LogMaskingUtils.previewUserAgent(userAgent, mdcProperties.getUserAgentPreviewLength());
        }
        return userAgent;
    }

    protected String getSessionId(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            return request.getSession().getId();
        }
        return null;
    }

    protected void putMdcSafely(String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            MDC.put(key, value.trim());
        }
    }

    private String extractFirstIpFromForwardedHeader(String xForwardedFor) {
        if (xForwardedFor == null || xForwardedFor.trim().isEmpty()) {
            return null;
        }
        String[] ips = xForwardedFor.split(",");
        if (ips.length > 0) {
            String firstIp = ips[0].trim();
            if (firstIp.startsWith("[") && firstIp.contains("]")) {
                int endBracket = firstIp.indexOf("]");
                return firstIp.substring(1, endBracket);
            } else if (firstIp.contains(":")) {
                return firstIp.split(":")[0];
            }
            return firstIp;
        }
        return null;
    }

    private String maskClientIpIfNeeded(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return clientIp;
        }
        
        if (mdcProperties.isMaskClientIp()) {
            return LogMaskingUtils.maskIpAddress(clientIp);
        }
        return clientIp;
    }
}
