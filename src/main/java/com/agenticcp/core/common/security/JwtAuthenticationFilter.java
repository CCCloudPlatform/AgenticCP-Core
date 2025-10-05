package com.agenticcp.core.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.agenticcp.core.common.security.JwtConstants.*;

/**
 * JWT 인증 필터
 * 요청 헤더에서 JWT 토큰을 추출하고 검증하여 SecurityContext에 인증 정보를 설정
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    
    @Autowired(required = false) // RedisTemplate이 필수가 아님을 명시
    private RedisTemplate<String, Object> redisTemplate;
    
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtService.isTokenValid(token, extractUsernameFromToken(token))) {
                // Redis 블랙리스트 확인
                if (isTokenBlacklisted(token)) {
                    log.warn("Blacklisted token attempted: {}", token.substring(0, 20) + "...");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // SecurityContext에 인증 정보 설정
                setAuthenticationInContext(token);
            }
            
        } catch (Exception e) {
            log.error("JWT authentication failed", e);
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * 토큰에서 사용자명 추출
     */
    private String extractUsernameFromToken(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (Exception e) {
            log.warn("Failed to extract username from token", e);
            return null;
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            if (redisTemplate == null) {
                log.debug("RedisTemplate is not available. Token blacklist check skipped.");
                return false;
            }
            String blacklistKey = "blacklist:" + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.warn("Failed to check token blacklist", e);
            return false;
        }
    }

    /**
     * SecurityContext에 인증 정보 설정
     */
    private void setAuthenticationInContext(String token) {
        try {
            String username = jwtService.extractUsername(token);
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            Long tenantId = jwtService.extractTenantId(token);
            List<String> permissions = jwtService.extractPermissions(token);
            
            // 권한 목록 생성
            List<SimpleGrantedAuthority> authorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            
            // 역할도 권한에 추가
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            
            // 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            
            // 추가 정보 설정
            authentication.setDetails(new JwtAuthenticationDetails(email, tenantId, permissions));
            
            // SecurityContext에 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("JWT authentication successful for user: {}", username);
            
        } catch (Exception e) {
            log.error("Failed to set authentication in context", e);
        }
    }

    /**
     * JWT 인증 상세 정보를 담는 클래스
     */
    public static class JwtAuthenticationDetails {
        private final String email;
        private final Long tenantId;
        private final List<String> permissions;

        public JwtAuthenticationDetails(String email, Long tenantId, List<String> permissions) {
            this.email = email;
            this.tenantId = tenantId;
            this.permissions = permissions;
        }

        public String getEmail() { return email; }
        public Long getTenantId() { return tenantId; }
        public List<String> getPermissions() { return permissions; }
    }
}
