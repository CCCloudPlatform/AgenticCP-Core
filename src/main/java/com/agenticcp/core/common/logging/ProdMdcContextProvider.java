package com.agenticcp.core.common.logging;

import com.agenticcp.core.common.logging.masking.MaskingService;
import com.agenticcp.core.common.security.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 운영 환경에서 MDC 컨텍스트를 설정하는 구현체입니다.
 * 세션/사용자/클라이언트 정보를 확장 수집합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Slf4j
@Component
@Profile("prod")
public class ProdMdcContextProvider extends AbstractMdcContextProvider {

    private final JwtService jwtService;

    public ProdMdcContextProvider(MdcProperties mdcProperties, MaskingService maskingService, JwtService jwtService) {
        super(mdcProperties, maskingService);
        this.jwtService = jwtService;
    }

    /**
     * 운영 환경에 필요한 세션/사용자/클라이언트 컨텍스트를 MDC에 채웁니다.
     *
     * @param request 현재 HTTP 요청
     */
    @Override
    public void setContext(HttpServletRequest request) {
        // 세션 ID 설정
        String sessionId = getSessionId(request);
        putMdcSafely(MdcKeys.SESSION_ID, sessionId);

        // 사용자 ID 설정 (JWT 토큰에서 추출)
        String userId = extractUserIdFromRequest(request);
        putMdcSafely(MdcKeys.USER_ID, userId);

        // 클라이언트 IP 설정
        String clientIp = getClientIpAddress(request);
        putMdcSafely(MdcKeys.CLIENT_IP, clientIp);

        // 사용자 에이전트 설정
        String userAgent = getUserAgent(request);
        putMdcSafely(MdcKeys.USER_AGENT, userAgent);
    }

    /**
     * Authorization 헤더에서 JWT를 추출해 사용자 ID(=username)을 파싱합니다.
     * 파싱 실패 시 MDC 오염을 막기 위해 null을 반환합니다.
     *
     * @param request HTTP 요청 객체
     * @return JWT에서 추출한 사용자 ID, 없거나 실패 시 {@code null}
     */
    private String extractUserIdFromRequest(HttpServletRequest request) {
        // Authorization 헤더에서 JWT 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtService.extractUsername(token);
            } catch (JwtException | IllegalArgumentException e) {
                // MDC 세팅 실패는 치명적이지 않으므로 디버깅 로그만 남김
                log.debug("Failed to extract user id from JWT: {}", e.getMessage());
            }
        }
        log.trace("Authorization header missing or malformed; skipping user id extraction.");
        return null;
    }
}
