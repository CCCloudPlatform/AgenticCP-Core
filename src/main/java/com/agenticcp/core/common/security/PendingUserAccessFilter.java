package com.agenticcp.core.common.security;

import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * PENDING 상태 사용자 접근 제한 필터
 * PENDING 상태 사용자는 2FA 설정 API만 접근 가능하도록 제한
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingUserAccessFilter extends OncePerRequestFilter {

    private final UserService userService;

    // 2FA 설정 관련 허용 경로
    private static final String[] ALLOWED_PATHS = {
        "/auth/2fa/setup",
        "/auth/2fa/enable",
        "/auth/2fa/status",
        "/auth/logout",
        "/auth/me"
    };

    /**
     * 필터 내부 로직 처리
     * PENDING 상태 사용자의 접근을 제한하고, 2FA 설정 API만 허용합니다.
     * 
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException IO 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증되지 않은 사용자는 통과 (다른 필터에서 처리)
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = authentication.getName();
        String requestPath = request.getRequestURI();

        try {
            // 사용자 조회
            User user = userService.getUserByUsernameOrThrow(username);
            
            // PENDING 상태 사용자인 경우
            if (user.getStatus() == Status.PENDING) {
                // 허용된 경로인지 확인
                if (!isAllowedPath(requestPath)) {
                    log.warn("[PendingUserAccessFilter] PENDING user attempted to access restricted path: {} username={}", 
                        requestPath, username);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        String.format("{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"2FA 설정이 필요합니다. 먼저 2FA를 설정해주세요.\"}}",
                            AuthErrorCode.ACCOUNT_INACTIVE.getCode())
                    );
                    return;
                }
            }
            
        } catch (Exception e) {
            log.error("[PendingUserAccessFilter] Error checking user status", e);
            // 에러 발생 시 통과 (다른 필터에서 처리)
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * 요청 경로가 허용된 경로인지 확인
     * 
     * @param requestPath 확인할 요청 경로
     * @return 허용된 경로이면 true, 아니면 false
     */
    private boolean isAllowedPath(String requestPath) {
        for (String allowedPath : ALLOWED_PATHS) {
            if (requestPath.startsWith(allowedPath)) {
                return true;
            }
        }
        return false;
    }
}

