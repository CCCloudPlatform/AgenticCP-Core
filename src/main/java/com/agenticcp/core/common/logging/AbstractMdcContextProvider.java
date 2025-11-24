package com.agenticcp.core.common.logging;

import com.agenticcp.core.common.logging.masking.MaskingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

/**
 * MDC 컨텍스트 공통 제공 추상 클래스입니다.
 * HTTP 요청으로부터 클라이언트 IP, User-Agent, 세션 ID를 추출해
 * 마스킹 여부에 따라 {@link MDC}에 안전하게 저장합니다.
 *
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@RequiredArgsConstructor
public abstract class AbstractMdcContextProvider implements MdcContextProvider {
    
    protected final MdcProperties mdcProperties;
    protected final MaskingService maskingService;

    /**
     * X-Forwarded-For, X-Real-IP, RemoteAddr 순으로 클라이언트 IP를 탐색합니다.
     * 마스킹 설정이 켜져 있으면 {@link MaskingService#maskIpAddress(String)}를 적용합니다.
     *
     * @param request 현재 HTTP 요청
     * @return 마스킹 여부가 반영된 클라이언트 IP
     */
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

    /**
     * User-Agent 헤더를 읽어 마스킹 옵션에 따라 미리보기 형태로 반환합니다.
     *
     * @param request 현재 HTTP 요청
     * @return 마스킹 설정이 반영된 User-Agent 문자열
     */
    protected String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader(MdcKeys.HEADER_USER_AGENT);
        if (userAgent != null && mdcProperties.isMaskUserAgent()) {
            return maskingService.previewUserAgent(userAgent, mdcProperties.getUserAgentPreviewLength());
        }
        return userAgent;
    }

    /**
     * 현재 요청에 세션이 존재하는 경우 세션 ID를 반환합니다.
     *
     * @param request 현재 HTTP 요청
     * @return 세션 ID 또는 {@code null}
     */
    protected String getSessionId(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            return request.getSession().getId();
        }
        return null;
    }

    /**
     * 값이 비어 있지 않은 경우에만 MDC에 키-값을 저장합니다.
     *
     * @param key   MDC 키
     * @param value 저장할 값
     */
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
            return maskingService.maskIpAddress(clientIp);
        }
        return clientIp;
    }
}
