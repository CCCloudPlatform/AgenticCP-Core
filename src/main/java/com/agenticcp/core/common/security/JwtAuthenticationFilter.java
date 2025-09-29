package com.agenticcp.core.common.security;

import com.agenticcp.core.domain.user.entity.User;
import org.springframework.data.redis.core.RedisTemplate;
import com.agenticcp.core.domain.user.service.UserService;
import org.springframework.context.annotation.Lazy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.agenticcp.core.common.security.JwtConstants.*;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에서 JWT 토큰을 검증하고 SecurityContext에 인증 정보를 설정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    @Lazy
    private final UserService userService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtService.validateToken(token) && !jwtService.isTokenExpired(token) && !isBlacklisted(token)) {
                authenticateUser(token, request);
            }
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage(), e);
            // 인증 실패 시에도 필터 체인을 계속 진행 (비인증 상태로)
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception e) {
            log.warn("블랙리스트 체크 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * 사용자 인증 처리
     */
    private void authenticateUser(String token, HttpServletRequest request) {
        String username = jwtService.getUsernameFromToken(token);
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<User> userOptional = userService.getUserByUsername(username);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                
                // 사용자 상태 및 계정 잠금 확인
                if (isUserValid(user)) {
                    UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                    user, 
                                    null, 
                                    getAuthorities(user)
                            );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("JWT 인증 성공: username={}, role={}", username, user.getRole());
                } else {
                    log.warn("유효하지 않은 사용자 상태: username={}, status={}, locked={}", 
                            username, user.getStatus(), user.isAccountLocked());
                }
            } else {
                log.warn("사용자를 찾을 수 없음: username={}", username);
            }
        }
    }

    /**
     * 사용자 유효성 검사
     */
    private boolean isUserValid(User user) {
        return user.getStatus() != null 
                && "ACTIVE".equals(user.getStatus().name()) 
                && !user.isAccountLocked();
    }

    /**
     * 사용자 권한 생성
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 기본 역할 추가
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }
        
        // 추가 권한 처리 (Role 엔티티에서)
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
                
                // 역할별 권한 추가
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission -> 
                            authorities.add(new SimpleGrantedAuthority(permission.getPermissionName()))
                    );
                }
            });
        }
        
        // 직접 권한 처리 (Permission 엔티티에서)
        if (user.getPermissions() != null) {
            user.getPermissions().forEach(permission -> 
                    authorities.add(new SimpleGrantedAuthority(permission.getPermissionName()))
            );
        }
        
        return authorities;
    }

    /**
     * 특정 경로는 JWT 필터를 건너뛸 수 있도록 설정
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 인증이 필요없는 경로들
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/health") ||
               path.startsWith("/api/v3/api-docs") ||
               path.startsWith("/api/swagger-ui") ||
               path.startsWith("/api/swagger-resources") ||
               path.startsWith("/api/webjars");
    }
}
