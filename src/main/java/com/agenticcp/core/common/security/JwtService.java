package com.agenticcp.core.common.security;

import com.agenticcp.core.domain.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.agenticcp.core.common.security.JwtConstants.*;

/**
 * JWT 토큰 생성 및 검증 서비스
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 액세스 토큰 생성
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USERNAME, user.getUsername());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_ROLE, user.getRole().name());
        claims.put(CLAIM_TENANT_ID, user.getTenant() != null ? user.getTenant().getId() : null);
        claims.put(CLAIM_TENANT_KEY, user.getTenant() != null ? user.getTenant().getTenantKey() : null);
        
        // 사용자 권한 추가
        List<String> permissions = getUserPermissions(user);
        if (!permissions.isEmpty()) {
            claims.put(CLAIM_PERMISSIONS, permissions);
        }
        
        return generateToken(claims, user.getUsername(), accessTokenExpiration);
    }

    /**
     * 리프레시 토큰 생성
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TOKEN_TYPE_REFRESH);
        claims.put(CLAIM_USERNAME, user.getUsername());
        
        return generateToken(claims, user.getUsername(), refreshTokenExpiration);
    }

    /**
     * 토큰 생성
     */
    private String generateToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.valueOf(SIGNATURE_ALGORITHM))
                .compact();
    }

    /**
     * 토큰에서 사용자명 추출
     */
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 토큰에서 만료 시간 추출
     */
    public Date getExpirationFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 토큰에서 특정 클레임 추출
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 토큰에서 모든 클레임 추출
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.warn("JWT 토큰 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 토큰 만료 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            return getExpirationFromToken(token).before(new Date());
        } catch (JwtException e) {
            log.warn("JWT 토큰 만료 확인 실패: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.warn("잘못된 JWT 토큰: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있음: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰 유효성 검증 (사용자명과 함께)
     */
    public boolean validateToken(String token, String username) {
        final String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * 서명 키 생성
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 토큰에서 테넌트 ID 추출
     */
    public Long getTenantIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        Object tenantId = claims.get(CLAIM_TENANT_ID);
        return tenantId != null ? Long.valueOf(tenantId.toString()) : null;
    }

    /**
     * 토큰에서 테넌트 키 추출
     */
    public String getTenantKeyFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return (String) claims.get(CLAIM_TENANT_KEY);
    }

    /**
     * 토큰에서 사용자 역할 추출
     */
    public String getRoleFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return (String) claims.get(CLAIM_ROLE);
    }

    /**
     * 토큰에서 사용자 권한 추출
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.get("permissions");
    }

    /**
     * 리프레시 토큰인지 확인
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰 만료까지 남은 시간 (초)
     */
    public long getTokenExpirationTime(String token) {
        Date expiration = getExpirationFromToken(token);
        long now = System.currentTimeMillis();
        return Math.max(0, (expiration.getTime() - now) / 1000);
    }

    /**
     * 사용자 권한 목록 생성
     */
    private List<String> getUserPermissions(User user) {
        List<String> permissions = user.getPermissions() != null 
                ? user.getPermissions().stream()
                    .map(permission -> permission.getPermissionName())
                    .collect(Collectors.toList())
                : List.of();
        
        // 역할 기반 권한도 추가
        if (user.getRoles() != null) {
            List<String> rolePermissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> permission.getPermissionName())
                    .distinct()
                    .collect(Collectors.toList());
            
            permissions.addAll(rolePermissions);
        }
        
        return permissions.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 액세스 토큰 만료 시간 조회
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * 리프레시 토큰 만료 시간 조회
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
