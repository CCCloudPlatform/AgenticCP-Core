package com.agenticcp.core.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProdMdcContextProvider extends AbstractMdcContextProvider {

    public ProdMdcContextProvider(MdcProperties mdcProperties) {
        super(mdcProperties);
    }

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
     * 요청에서 사용자 ID 추출 (JWT 토큰에서)
     * JWT 토큰 파싱 로직 구현 필요(JWT 토큰에서 사용자 ID 추출 구현 후 주석 해제)
     * 
     * @param request HTTP 요청 객체
     * @return 사용자 ID
     */
    private String extractUserIdFromRequest(HttpServletRequest request) {
        // Authorization 헤더에서 JWT 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // String token = authHeader.substring(7);
            // return jwtService.extractUserId(token);
        }
        return null;
    }
}
