package com.agenticcp.core.common.security;

import com.agenticcp.core.domain.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.agenticcp.core.common.security.JwtConstants.*;

/**
 * JWT 토큰 생성, 검증, 파싱 서비스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${security.jwt.secret}") String base64Secret,
            @Value("${security.jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs,
            @Value("${security.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

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
        
        List<String> permissions = getUserPermissions(user);
        if (!permissions.isEmpty()) {
            claims.put(CLAIM_PERMISSIONS, permissions);
        }
        
        return generateToken(claims, user.getUsername(), accessTokenExpirationMs);
    }

    /**
     * 리프레시 토큰 생성
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TOKEN_TYPE_REFRESH);
        claims.put(CLAIM_USERNAME, user.getUsername());
        
        return generateToken(claims, user.getUsername(), refreshTokenExpirationMs);
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean isTokenValid(String token, String expectedUsername) {
        String username = extractUsername(token);
        return username != null && username.equals(expectedUsername) && !isTokenExpired(token);
    }

    /**
     * 토큰에서 사용자명 추출
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_EMAIL, String.class));
    }

    /**
     * 토큰에서 역할 추출
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
    }

    /**
     * 토큰에서 테넌트 ID 추출
     */
    public Long extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_TENANT_ID, Long.class));
    }

    /**
     * 토큰에서 권한 목록 추출
     */
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_PERMISSIONS, List.class));
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 토큰 만료 시간 추출
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 토큰에서 특정 클레임 추출
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 토큰 파싱하여 모든 클레임 추출
     */
    public Claims parseAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰 생성 (내부 메서드)
     */
    private String generateToken(Map<String, Object> claims, String subject, long expirationMs) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 사용자 권한 목록 추출 (헬퍼 메서드)
     */
    private List<String> getUserPermissions(User user) {
        // TODO: 실제 권한 시스템 구현 시 User 엔티티의 permissions 필드 활용
        return List.of(); // 임시로 빈 리스트 반환
    }
}
